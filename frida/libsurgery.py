#!/usr/bin/env python3

import argparse
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import time
import zipfile

try:
    import frida
except ImportError:
    frida = None

ELF_MAGIC = b"\x7fELF"
PT_NULL = 0
PT_LOAD = 1
PT_DYNAMIC = 2
DT_NULL = 0
DT_NEEDED = 1
DT_STRTAB = 5
DT_SYMTAB = 6
DT_INIT = 12
DT_INIT_ARRAY = 25
DT_INIT_ARRAYSZ = 27
OLD_EXECUTE_PROGRAM = b"ExecuteProgram\0"
NEW_MEMSET = b"memset" + b"\0" * (len(OLD_EXECUTE_PROGRAM) - len(b"memset"))
ARM64_STUB = bytes([
    0xc0, 0x00, 0x80, 0x52,
    0x20, 0x00, 0xa0, 0x72,
    0xc0, 0x03, 0x5f, 0xd6,
])

FRIDA_SCRIPT = r"""
function collectPaths() {
  Java.perform(function () {
    try {
      var ActivityThread = Java.use('android.app.ActivityThread');
      var application = ActivityThread.currentApplication();

      if (application === null) {
        setTimeout(collectPaths, 500);
        return;
      }

      var context = application.getApplicationContext();
      var info = context.getApplicationInfo();
      var paths = [info.publicSourceDir.value];
      var splits = info.splitSourceDirs.value;

      if (splits !== null) {
        for (var i = 0; i < splits.length; i++) {
          paths.push(splits[i]);
        }
      }

      send({event: 'paths', paths: paths});
    } catch (error) {
      send({event: 'error', message: String(error)});
    }
  });
}

collectPaths();
"""


def u16(data, offset):
    return struct.unpack_from("<H", data, offset)[0]


def u32(data, offset):
    return struct.unpack_from("<I", data, offset)[0]


def u64(data, offset):
    return struct.unpack_from("<Q", data, offset)[0]


def cstring(data, offset):
    if offset is None or offset < 0 or offset >= len(data):
        return ""

    end = data.find(b"\0", offset)

    if end < 0:
        end = len(data)

    return data[offset:end].decode("utf-8", "replace")


def patch_single_lib(data):
    if len(data) < 64 or data[:4] != ELF_MAGIC or data[4] != 2 or data[5] != 1:
        return None, "Invalid ELF header"

    mPhOff = u64(data, 0x20)
    mPhEntSize = u16(data, 0x36)
    mPhNum = u16(data, 0x38)

    if mPhEntSize < 56:
        return None, "Invalid program header size"

    mProgramHeaders = []

    for i in range(mPhNum):
        mOffset = mPhOff + i * mPhEntSize

        if mOffset + mPhEntSize > len(data):
            break

        mProgramHeaders.append((
            u32(data, mOffset),
            u64(data, mOffset + 8),
            u64(data, mOffset + 16),
            u64(data, mOffset + 32),
            u64(data, mOffset + 40),
            mOffset,
        ))

    mDynamic = next((mHeader for mHeader in mProgramHeaders if mHeader[0] == PT_DYNAMIC), None)

    if mDynamic is None:
        return None, "No PT_DYNAMIC header found"

    mDynOffset = mDynamic[1]
    mDynSize = mDynamic[3]
    mEntries = []
    mStrtabVa = None
    mSymtabVa = None

    for mPos in range(0, mDynSize, 16):
        mOffset = mDynOffset + mPos

        if mOffset + 16 > len(data):
            break

        mTag = u64(data, mOffset)
        mValue = u64(data, mOffset + 8)

        if mTag == DT_NULL:
            break

        mEntries.append((mTag, mValue))

        if mTag == DT_STRTAB:
            mStrtabVa = mValue
        elif mTag == DT_SYMTAB:
            mSymtabVa = mValue

    def vaddr_to_offset(mVaddr):
        for mType, mPOffset, mPVaddr, mFileSize, mMemSize, _ in mProgramHeaders:
            if mType == PT_LOAD and mPVaddr <= mVaddr < mPVaddr + mMemSize:
                return mPOffset + (mVaddr - mPVaddr)
        return None

    mStrtabOffset = vaddr_to_offset(mStrtabVa) if mStrtabVa is not None else None
    mSymtabOffset = vaddr_to_offset(mSymtabVa) if mSymtabVa is not None else None
    mInitVa = None
    mInitArrayVa = None
    mInitArraySize = None
    mHasPairip = False
    mNewEntries = []

    for mTag, mValue in mEntries:
        if mTag == DT_INIT:
            mInitVa = mValue
            continue

        if mTag == DT_INIT_ARRAY:
            mInitArrayVa = mValue
            continue

        if mTag == DT_INIT_ARRAYSZ:
            mInitArraySize = mValue
            continue

        if mTag == DT_NEEDED and mStrtabOffset is not None:
            mName = cstring(data, mStrtabOffset + mValue)

            if "pairip" in mName.lower():
                mHasPairip = True
                continue

        mNewEntries.append((mTag, mValue))

    if not mHasPairip and data.find(OLD_EXECUTE_PROGRAM) == -1:
        return None, "Not protected by PairIP"

    for mIndex, (mTag, mValue) in enumerate(mNewEntries + [(DT_NULL, 0)]):
        mOffset = mDynOffset + mIndex * 16

        if mOffset + 16 <= len(data):
            struct.pack_into("<QQ", data, mOffset, mTag, mValue)

    for mOffset in range(mDynOffset + (len(mNewEntries) + 1) * 16, mDynOffset + mDynSize):
        data[mOffset] = 0

    if mInitVa is not None:
        for mType, mPOffset, mVaddr, mFileSize, mMemSize, mHeaderOffset in mProgramHeaders:
            if mType == PT_LOAD and mVaddr <= mInitVa < mVaddr + mMemSize:
                struct.pack_into("<I", data, mHeaderOffset, PT_NULL)

    if mInitArrayVa is not None and mInitArraySize is not None:
        mOffset = vaddr_to_offset(mInitArrayVa)

        if mOffset is not None and mOffset >= 0 and mOffset + mInitArraySize <= len(data):
            data[mOffset:mOffset + mInitArraySize] = b"\0" * mInitArraySize

    mPos = data.find(OLD_EXECUTE_PROGRAM)

    if mPos != -1:
        data[mPos:mPos + len(OLD_EXECUTE_PROGRAM)] = NEW_MEMSET

    if mSymtabOffset is not None and mStrtabOffset is not None:
        for i in range(3000):
            mEntryOffset = mSymtabOffset + i * 24

            if mEntryOffset + 24 > len(data):
                break

            mNameIndex = u32(data, mEntryOffset)

            if mNameIndex == 0:
                continue

            mName = cstring(data, mStrtabOffset + mNameIndex)

            if mName == "JNI_OnLoad":
                mValue = u64(data, mEntryOffset + 8)
                mOffset = vaddr_to_offset(mValue)

                if mOffset is not None and mOffset >= 0 and mOffset + len(ARM64_STUB) <= len(data):
                    data[mOffset:mOffset + len(ARM64_STUB)] = ARM64_STUB

                break
    else:
        print("[INFO] JNI_OnLoad symbol not found in ELF, skipping stub patch safely.")

    return bytes(data), None


def adb_pull(remote_path, local_path):
    mResult = subprocess.run(
        ["adb", "pull", remote_path, local_path],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.PIPE,
        text=True,
    )

    if mResult.returncode == 0:
        return True

    with open(local_path, "wb") as mFile:
        mResult = subprocess.run(
            ["adb", "exec-out", "su", "-c", "cat " + remote_path],
            stdout=mFile,
            stderr=subprocess.PIPE,
        )

    return mResult.returncode == 0


def resolve_pid(package):
    mResult = subprocess.run(
        ["adb", "shell", "pidof", package],
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        text=True,
    )

    for mToken in mResult.stdout.split():
        try:
            return int(mToken)
        except ValueError:
            continue

    return None


def collect_paths(package, launch, timeout):
    if frida is None:
        print("[ERROR] Python frida module is not installed", file=sys.stderr)
        return None

    if launch:
        subprocess.run(
            ["adb", "shell", "monkey", "-p", package, "-c", "android.intent.category.LAUNCHER", "1"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        time.sleep(5)

    try:
        mDevice = frida.get_usb_device(timeout=5)
    except Exception as mError:
        print("[ERROR] USB Frida device not found : " + str(mError), file=sys.stderr)
        return None

    mPid = resolve_pid(package)

    if mPid is None:
        print("[ERROR] Process is not running : " + package, file=sys.stderr)
        return None

    mSession = None

    try:
        try:
            mSession = mDevice.attach(mPid)
        except Exception as mError:
            print("[ERROR] Unable to attach to " + package + " (pid " + str(mPid)
                  + ") : " + str(mError), file=sys.stderr)
            return None

        mScript = mSession.create_script(FRIDA_SCRIPT)
        mState = {"paths": None, "error": None}

        def on_message(mMessage, mData):
            if mMessage.get("type") != "send":
                print("[FRIDA] " + str(mMessage), file=sys.stderr)
                return

            mPayload = mMessage.get("payload", {})

            if mPayload.get("event") == "paths":
                mState["paths"] = mPayload.get("paths", [])
            elif mPayload.get("event") == "error":
                mState["error"] = mPayload.get("message")

        mScript.on("message", on_message)
        mScript.load()

        mDeadline = time.time() + timeout

        while mState["paths"] is None and mState["error"] is None and time.time() < mDeadline:
            time.sleep(0.25)

        if mState["error"] is not None:
            print("[ERROR] Frida script failed : " + mState["error"], file=sys.stderr)
            return None

        if mState["paths"] is None:
            print("[ERROR] Timed out waiting for ApplicationInfo", file=sys.stderr)
            return None

        return mState["paths"]
    finally:
        if mSession is not None:
            try:
                mSession.detach()
            except Exception:
                pass


def main():
    mParser = argparse.ArgumentParser(description="Frida-based PairIP native library surgery")
    mParser.add_argument("-p", "--package", required=True, help="Target package name")
    mParser.add_argument("-o", "--outdir", default="libsurgery", help="Output directory")
    mParser.add_argument("-l", "--launch", action="store_true", help="Launch the app via adb before attaching")
    mParser.add_argument("--timeout", type=int, default=30, help="ApplicationInfo timeout in seconds")
    mArgs = mParser.parse_args()

    mPaths = collect_paths(mArgs.package, mArgs.launch, mArgs.timeout)

    if not mPaths:
        return 1

    os.makedirs(mArgs.outdir, exist_ok=True)
    mPatched = 0
    mTmpDir = tempfile.mkdtemp(prefix="pairip_libsurgery_")

    try:
        for mPath in mPaths:
            if not mPath.endswith(".apk"):
                continue

            mLocalPath = os.path.join(mTmpDir, os.path.basename(mPath))

            if not adb_pull(mPath, mLocalPath):
                print("[ERROR] Unable to pull " + mPath, file=sys.stderr)
                continue

            try:
                with zipfile.ZipFile(mLocalPath) as mZip:
                    for mEntry in mZip.namelist():
                        if not mEntry.startswith("lib/arm64-v8a/") or not mEntry.endswith(".so"):
                            continue

                        mLibName = os.path.basename(mEntry)

                        if mLibName == "libpairipcore.so":
                            continue

                        mData = bytearray(mZip.read(mEntry))
                        mPatchedData, mReason = patch_single_lib(mData)

                        if mPatchedData is None:
                            print("[SKIP] " + mLibName + " : " + mReason)
                            continue

                        mOutput = os.path.join(mArgs.outdir, mLibName.replace(".so", "_patched.so"))

                        with open(mOutput, "wb") as mFile:
                            mFile.write(mPatchedData)

                        print("[BUILD] " + mLibName + " -> " + os.path.basename(mOutput)
                              + " (" + str(len(mPatchedData)) + " bytes)")
                        mPatched += 1
            except Exception as mError:
                print("[ERROR] " + os.path.basename(mPath) + " : " + str(mError), file=sys.stderr)
    finally:
        shutil.rmtree(mTmpDir, ignore_errors=True)

    print("[BUILD] Process completed (" + str(mPatched) + " libraries patched)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

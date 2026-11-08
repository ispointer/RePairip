# Frida capture — no CorePatch, no root reinstall

The stock root flow needs a patched APK installed with signature verification
disabled (CorePatch / LSPosed), because the logging build is unsigned and the
PairIP VM dies on a modified APK. A Frida capture removes that whole dance:

- install the app normally from Play (original signature, license works)
- launch it once so the PairIP VM populates every junk class
- attach and dump the populated static fields straight to `pairip.json`
- translate the clean merged APK and sign it normally

No CorePatch, no unsigned install, no base.apk swap, no ART dex-cache tricks.

## Requirements

- A rooted device (Magisk or equivalent) running **frida-server 16.x**.
- A matching **frida Python 16.x** client. Frida 17 removed the built-in Java
  bridge; the capture script fails with `ReferenceError: 'Java' is not defined`
  against a 17.x server. Use 16.7.19 on both sides (verified working), or
  bundle `frida-java-bridge` with `frida-compile` if you must stay on 17.
- `adb`, `java` 17+, and Python 3.
- The latest RePairip release JAR from
  <https://github.com/ispointer/RePairip/releases/latest>. No build is needed;
  the developer publishes prebuilt JARs for users.

## Get RePairip

Download the newest `RePairip.*.jar` from the Releases page and save it as
`RePairip.jar`:

```bash
# example; adjust the version to the current release
curl -L -o RePairip.jar \
  https://github.com/ispointer/RePairip/releases/download/1.7.10/RePairip.1.7.10.jar
```

The 1.7.10 release predates the `junk_classes.txt` writer, so step 2 also uses
the bundled `frida/JunkClasses.java` helper. Builds from `main` write that file
directly and can skip the helper. Both `JunkClasses.java` and
`capture_pairip.py` live in this repository, so run the steps from a repo
checkout (or copy the two files alongside the JAR).

## 1. Pull the installed splits and build an `.apks`

```bash
PKG=com.example.app
mkdir -p work
for p in $(adb shell pm path "$PKG" | sed 's/package://;s/\r//'); do adb pull "$p" work/; done

python3 - <<'EOF'
import zipfile, glob, os
with zipfile.ZipFile("work/app.apks", "w", zipfile.ZIP_STORED) as z:
    for f in ["work/base.apk"] + sorted(glob.glob("work/split_config*.apk")):
        z.write(f, os.path.basename(f))
EOF
```

`zip` is not required; the Python snippet builds the archive.

## 2. Merge and produce the junk class list

```bash
java -jar RePairip.jar -i work/app.apks
# -> work/app_merged.apk, work/app_merged_pairip.apk
```

Builds from `main` also write `work/junk_classes.txt`. The 1.7.10 release does
not, so generate it from the merged APK using the helper (it reuses the dexlib2
classes bundled in the release JAR):

```bash
java -cp RePairip.jar frida/JunkClasses.java work/app_merged.apk > work/junk_classes.txt
wc -l work/junk_classes.txt   # 36 for the sample app
```

## 3. Start frida-server 16.x

```bash
adb shell 'kill $(pidof frida-server) 2>/dev/null'
adb shell 'nohup /data/local/tmp/frida-server -D >/dev/null 2>&1 &'
adb shell '/data/local/tmp/frida-server --version'
adb shell 'ps -A | grep -i frida'
```

Use `su -c` for the commands if the adb shell is not already root. The server
version must match the Python client version (for example, 16.7.19). Adjust the
path if your 16.x binary is named differently (for example `frida16`).

## 4. Launch the stock app

Install the Play-signed APK normally and leave it in the foreground; PairIP
populates the junk-class fields during startup.

```bash
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
```

## 5. Capture pairip.json

```bash
python3 -m venv .venv
.venv/bin/pip install 'frida==16.7.19'
.venv/bin/python frida/capture_pairip.py -p "$PKG" -c work/junk_classes.txt -o work/pairip.json
```

Expected output:

```text
attach pid 12345
dump: {'event': 'dump', 'bytes': 73194, 'classes': 36, 'errors': []}
wrote work/pairip.json
```

- Frida may report the process under the app label (`Location Changer`) instead
  of the package name. The script resolves the PID with `adb shell pidof` as a
  fallback, so `-p <package>` still works.
- With several devices connected, pass `-d <frida-device-id>` (the USB serial).
- `--spawn` resumes the app and waits 10 seconds before reading fields, but some
  Samsung/Android builds reject Frida spawning with
  `unable to perform ptrace pokedata`. Launch the app normally and attach; that
  is the reliable path. Use `--wait N` to adjust the wait when spawning works.
- If the capture is partial, the script still writes the JSON but exits 1 with a
  `WARNING` naming the missing classes.

## 6. Translate

```bash
java -jar RePairip.jar -i work/app.apks -t work/pairip.json
# -> work/app_merged_translated.apk
```

This removes the `com.pairip.*` classes, the VM assets, and
`lib/arm64-v8a/libpairipcore.so`, and restores the decrypted classes.

## 7. Align and sign

```bash
BT=${ANDROID_HOME:-$HOME/Android/Sdk}/build-tools/35.0.0  # adjust to your install
keytool -genkeypair -keystore key.jks -storetype pkcs12 -alias key \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android -dname "CN=RePairip"

$BT/zipalign -p -f 4 work/app_merged_translated.apk app-aligned.apk
$BT/apksigner sign --ks key.jks --ks-pass pass:android \
  --key-pass pass:android --out app-signed.apk app-aligned.apk
$BT/apksigner verify --verbose app-signed.apk
```

On Android 15+ devices with 16 KB pages, align with `zipalign -P 16` instead.

## 8. Install

The stock APK is Play-signed, so a normal `adb install -r` fails with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Either:

- keep CorePatch/LSPosed signature verification disabled and install over the
  stock app (data preserved), or
- `adb uninstall "$PKG"` (loses app data) and `adb install app-signed.apk`.

If installation fails with `Can't find service: package` or a
`NullPointerException` in `StorageManagerService.allocateBytes`, the device's
`system_server` is restarting. Wait for it to settle, then retry with:

```bash
adb push app-signed.apk /data/local/tmp/app-signed.apk
adb shell pm install -r /data/local/tmp/app-signed.apk
```

## Troubleshooting

- `ReferenceError: 'Java' is not defined` — frida-server is 17.x. Switch the
  device to a 16.x server and use a matching 16.x Python client.
- `unable to find process '<package>'` — the app is not running, or adb is
  targeting the wrong device. Launch it, or pass `-d <serial>`.
- `cannot reach frida-server` — start frida-server 16.x on the device and make
  sure its version matches the Python client.
- `junk_classes.txt` missing — the 1.7.10 release does not write it. Generate it
  with `frida/JunkClasses.java` as shown in step 2.
- `no dictionary received` — the app is not running, PairIP has not initialized
  yet, or the class list belongs to a different APK. Launch the app, wait for
  its UI, and rerun.
- `spawn failed (... ptrace pokedata)` — the device/kernel rejects Frida
  spawning. Launch the app normally and omit `--spawn`.
- Empty field values — attached too early or attached to the wrong process.
  Launch the app first, wait for the main UI, then capture.
- `zipalign: error while loading shared libraries: libc++.so` — incomplete SDK
  install. Use the official Android build-tools package, not a partial copy.
- `JunkClasses.java` fails with a source-file mode error — the installed Java
  is a runtime-only build. Compile it once with
  `javac -cp RePairip.jar frida/JunkClasses.java`, then run
  `java -cp RePairip.jar:. JunkClasses work/app_merged.apk > work/junk_classes.txt`.

## Notes

- `junk_classes.txt` lists the PairIP junk class descriptors. Builds from
  `main` write it during the logging step; for the 1.7.10 release use
  `JunkClasses.java`.
- The capture reads the same static `String` / `reflect.Method` fields that the
  injected `PairipLog` writes, so the resulting JSON is format-compatible with
  the root-mode capture.
- Works on Play-installed apps where the license passes, so PairIP's native
  init and VM stay healthy while fields are populated.
- The translated build injects the captured values back into the junk classes
  (`SPUT_OBJECT` in `MethodMaker`), so re-capturing from a translated install
  returns the same dictionary.

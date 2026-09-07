package com.antik.librarySurgery;

import com.antik.ui.banner;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LibSurgery {

    public record PatchResult(boolean success, String message) {}

    public static List<File> getInstalledApks(String packageName) {
        String out = Memory.runRootCmd("pm path " + packageName);
        List<File> apks = new ArrayList<>();
        for (String l : out.split("\r?\n")) {
            String trimmed = l.trim();
            if (trimmed.startsWith("package:")) {
                String apkPath = trimmed.replace("package:", "").trim();
                File f = new File(apkPath);
                if (f.exists()) {
                    apks.add(f);
                } else {
                    File tmpCopy = new File("/data/local/tmp/temp_base.apk");
                    Memory.runRootCmd("cp " + apkPath + " " + tmpCopy.getAbsolutePath() + " && chmod 666 " + tmpCopy.getAbsolutePath());
                    if (tmpCopy.exists()) {
                        apks.add(tmpCopy);
                    }
                }
            }
        }
        if (apks.isEmpty()) {
            System.err.println("[ERROR] Package not installed or APK paths unavailable : " + packageName);
            System.exit(1);
        }
        System.out.println("[INFO] Found " + apks.size() + " APK path(s) for package : " + packageName);
        return apks;
    }

    public static Map<String, File> extractArm64Libs(List<File> apks, File tmpDir) {
        Map<String, File> extractedLibs = new HashMap<>();
        for (File apkFile : apks) {
            try (ZipFile zip = new ZipFile(apkFile)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String fname = entry.getName();
                    if (fname.endsWith(".so") && fname.contains("arm64")) {
                        String libName = new File(fname).getName();
                        if (!extractedLibs.containsKey(libName)) {
                            File extPath = new File(tmpDir, libName);
                            try (InputStream is = zip.getInputStream(entry);
                                 OutputStream os = new FileOutputStream(extPath)) {
                                is.transferTo(os);
                            }
                            extractedLibs.put(libName, extPath);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Extracting native libs failed : " + e.getMessage());
            }
        }
        System.out.println("[INFO] Extracted " + extractedLibs.size() + " ARM64 native library/libraries");
        return extractedLibs;
    }

    public static PatchResult patchSingleLib(byte[] data, byte[] memDump) {
        if (data.length < 0x40 || data[0] != 0x7f || data[1] != 'E' || data[2] != 'L' || data[3] != 'F') {
            return new PatchResult(false, "Invalid ELF header");
        }

        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        long ePhOff = buf.getLong(0x20);
        int ePhNum = buf.getShort(0x38) & 0xFFFF;

        Long dynOff = null;
        Long dynSz = null;

        for (int i = 0; i < ePhNum; i++) {
            int o = (int) (ePhOff + i * 56L);
            if (o + 56 > data.length) break;
            int pType = buf.getInt(o);
            if (pType == 2) { // PT_DYNAMIC
                dynOff = buf.getLong(o + 8);
                dynSz = buf.getLong(o + 32);
                break;
            }
        }

        if (dynOff == null) {
            return new PatchResult(false, "No PT_DYNAMIC header found");
        }

        Long strtabVa = null;
        int dynOffInt = dynOff.intValue();
        int dynSzInt = dynSz.intValue();

        for (int j = 0; j < dynSzInt; j += 16) {
            int entryPos = dynOffInt + j;
            if (entryPos + 16 > data.length) break;
            long t = buf.getLong(entryPos);
            long v = buf.getLong(entryPos + 8);
            if (t == 5) { // DT_STRTAB
                strtabVa = v;
                break;
            }
        }

        Long strtabOff = null;
        if (strtabVa != null) {
            for (int i = 0; i < ePhNum; i++) {
                int o = (int) (ePhOff + i * 56L);
                if (o + 56 > data.length) break;
                int pt = buf.getInt(o);
                long pVa = buf.getLong(o + 16);
                long pFsz = buf.getLong(o + 32);
                if (pt == 1 && pVa <= strtabVa && strtabVa < pVa + pFsz) { // PT_LOAD
                    long pOff = buf.getLong(o + 8);
                    strtabOff = pOff + (strtabVa - pVa);
                    break;
                }
            }
        }

        Long initVa = null;
        List<long[]> newEntries = new ArrayList<>();
        boolean hasPairip = false;

        for (int j = 0; j < dynSzInt; j += 16) {
            int entryPos = dynOffInt + j;
            if (entryPos + 16 > data.length) break;
            long t = buf.getLong(entryPos);
            long v = buf.getLong(entryPos + 8);

            if (t == 0) { // DT_NULL
                break;
            }
            if (t == 12) { // DT_INIT
                initVa = v;
                continue;
            }
            if (t == 1 && strtabOff != null) { // DT_NEEDED
                int strPos = (int) (strtabOff + v);
                String name = readNullTerminatedString(data, strPos);
                if (name.toLowerCase().contains("pairip")) {
                    hasPairip = true;
                    continue;
                }
            }
            newEntries.add(new long[]{t, v});
        }

        if (!hasPairip && initVa == null) {
            return new PatchResult(false, "Not protected by PairIP");
        }

        newEntries.add(new long[]{0, 0});

        for (int k = 0; k < newEntries.size(); k++) {
            int pos = dynOffInt + k * 16;
            if (pos + 16 <= data.length) {
                buf.putLong(pos, newEntries.get(k)[0]);
                buf.putLong(pos + 8, newEntries.get(k)[1]);
            }
        }

        for (int k = newEntries.size() * 16; k < dynSzInt; k += 16) {
            int pos = dynOffInt + k;
            if (pos + 16 <= data.length) {
                buf.putLong(pos, 0L);
                buf.putLong(pos + 8, 0L);
            }
        }

        if (initVa != null) {
            for (int i = 0; i < ePhNum; i++) {
                int o = (int) (ePhOff + i * 56L);
                if (o + 56 > data.length) break;
                int pt = buf.getInt(o);
                long pVa = buf.getLong(o + 16);
                long pMsz = buf.getLong(o + 40);
                if (pt == 1 && pVa <= initVa && initVa < pVa + pMsz) {
                    buf.putInt(o, 0); // PT_NULL
                }
            }
        }

        byte[] oldStr = "ExecuteProgram\0".getBytes(StandardCharsets.US_ASCII);
        byte[] newStr = "memset\0\0\0\0\0\0\0\0\0".getBytes(StandardCharsets.US_ASCII);
        int posStr = indexOf(data, oldStr);
        if (posStr != -1 && posStr + newStr.length <= data.length) {
            System.arraycopy(newStr, 0, data, posStr, newStr.length);
        }

        int pos4af0 = 0x4af0;
        byte[] arm64Stub = new byte[] {
            (byte) 0xc0, 0x00, (byte) 0x80, 0x52,
            0x20, 0x00, (byte) 0xa0, 0x72,
            (byte) 0xc0, 0x03, 0x5f, (byte) 0xd6
        };
        if (data.length > pos4af0 + arm64Stub.length) {
            System.arraycopy(arm64Stub, 0, data, pos4af0, arm64Stub.length);
        }

        if (memDump != null && memDump.length > 0) {
            int copyLen = Math.min(data.length, memDump.length);
            System.arraycopy(memDump, 0, data, 0, copyLen);
        }

        return new PatchResult(true, "Successfully patched");
    }

    private static String readNullTerminatedString(byte[] data, int offset) {
        if (offset < 0 || offset >= data.length) return "";
        int end = offset;
        while (end < data.length && data[end] != 0) {
            end++;
        }
        return new String(data, offset, end - offset, StandardCharsets.US_ASCII);
    }

    private static int indexOf(byte[] array, byte[] target) {
        if (target.length == 0) return 0;
        outer:
        for (int i = 0; i <= array.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public static void process(String packageName, String outDirStr) {
        banner.banner();

        File outDir = new File(outDirStr);
        if (!outDir.exists()) {
            boolean created = outDir.mkdirs();
            if (!created && !outDir.exists()) {
                System.err.println("[ERROR] Could not create output directory : " + outDirStr);
            }
        }

        File tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory("pairip_surgery_").toFile();

            List<File> apks = getInstalledApks(packageName);
            Map<String, File> extractedLibs = extractArm64Libs(apks, tmpDir);

            String pid = Memory.waitForProcess(packageName);

            String rxAddr = Memory.findRxAddress(pid);
            byte[] memDumpData = Memory.dumpMemory(pid, rxAddr);

            int patchedCount = 0;
            for (Map.Entry<String, File> entry : extractedLibs.entrySet()) {
                String libName = entry.getKey();
                File libPath = entry.getValue();

                if ("libpairipcore.so".equals(libName)) {
                    continue;
                }

                byte[] data = Files.readAllBytes(libPath.toPath());
                byte[] dumpToUse = (libName.contains("mjpg") || libName.contains("unity") || libName.contains("il2cpp"))
                        ? memDumpData : null;

                PatchResult res = patchSingleLib(data, dumpToUse);
                if (res.success()) {
                    String outName = libName.replace(".so", "") + "_patched.so";
                    File outPath = new File(outDir, outName);
                    Files.write(outPath.toPath(), data);
                    System.out.println("[BUILD] " + libName + " -> Saved " + outName + " (" + data.length + " bytes)");
                    patchedCount++;
                } else {
                    System.out.println("[SKIP] " + libName + " : " + res.message());
                }
            }

            System.out.println("[BUILD] Process completed (" + patchedCount + " libraries patched)");

        } catch (Exception e) {
            System.err.println("[ERROR] Process failed : " + e.getMessage());
        } finally {
            if (tmpDir != null) {
                deleteDirectory(tmpDir);
            }
        }
    }

    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    if (!f.delete()) {
                        f.deleteOnExit();
                    }
                }
            }
        }
        boolean deleted = dir.delete();
        if (!deleted) {
            dir.deleteOnExit();
        }
    }

    public static void main(String[] args) {
        String packageName = null;
        String outDir = null;

        for (int i = 0; i < args.length; i++) {
            if (("-p".equals(args[i]) || "--package".equals(args[i])) && i + 1 < args.length) {
                packageName = args[++i];
            } else if (("-o".equals(args[i]) || "--outdir".equals(args[i])) && i + 1 < args.length) {
                outDir = args[++i];
            }
        }

        if (packageName == null || outDir == null) {
            System.out.println("Usage: java -jar RePairip.jar -p <package_name> -o <out_dir>");
            System.exit(1);
        }

        process(packageName, outDir);
    }
}

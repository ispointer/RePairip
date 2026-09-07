package com.antik.librarySurgery;

import com.antik.librarySurgery.memory.Memory;
import com.antik.librarySurgery.memory.MemoryRegion;
import com.antik.librarySurgery.process.ProcessFinder;
import com.antik.ui.banner;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class LibSurgery {

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

            List<File> apks = LibraryExtractor.getInstalledApks(packageName);
            Map<String, File> extractedLibs = LibraryExtractor.extractArm64Libs(apks, tmpDir);

            String pid = ProcessFinder.waitForProcess(packageName);

            MemoryRegion<Long> rxRegion = Memory.findRxRegion(pid);
            String rxAddr = rxRegion != null ? Long.toHexString(rxRegion.getStartAddress()) : null;
            String dumpedPath = rxRegion != null ? rxRegion.getPathname() : "";
            String dumpedLibName = !dumpedPath.isEmpty() ? new File(dumpedPath).getName() : "";

            byte[] memDumpData = Memory.dumpMemory(pid, rxAddr);

            int patchedCount = 0;
            for (Map.Entry<String, File> entry : extractedLibs.entrySet()) {
                String libName = entry.getKey();
                File libPath = entry.getValue();

                if ("libpairipcore.so".equals(libName)) {
                    continue;
                }

                byte[] data = Files.readAllBytes(libPath.toPath());
                byte[] dumpToUse = (dumpedLibName.isEmpty() || libName.equalsIgnoreCase(dumpedLibName) || dumpedLibName.contains(libName))
                        ? memDumpData : null;

                PatchResult res = LibraryPatcher.patchSingleLib(data, dumpToUse);
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
                FileUtils.deleteDirectory(tmpDir);
            }
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

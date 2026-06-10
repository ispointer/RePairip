package com.antik;

import com.antik.DexPatcher.DexPatcher;
import com.antik.DexPatcher.Translation.TranslationPatcher;
import com.antik.crc32.crc32;
import com.antik.manifest.manifestP;
import com.antik.ui.*;
import com.antik.utils.*;
import com.reandroid.apk.ApkBundle;
import com.reandroid.apk.ApkModule;
import com.reandroid.archive.WriteProgress;
import java.io.*;
import java.nio.file.Files;

public class Main {
    public static void main(String[] args) {

        if (args.length < 2) {
            help.help();
            return;
        }

        String inputPath = null;
        String translatePath = null;

        for (int i = 0; i < args.length; i++) {
            if ("-i".equals(args[i]) && i + 1 < args.length) {
                inputPath = args[i + 1];
            } else if ("-t".equals(args[i]) && i + 1 < args.length) {
                translatePath = args[i + 1];
            }
        }

        if (inputPath == null) {
            help.help();
            return;
        }

        File inputApk = new File(inputPath);

        if (!inputApk.exists()) {
            System.err.println("Input file not found: " + inputPath);
            return;
        }

        banner.banner();

        File tempDir = null;

        try {
            ApkModule module;
            File mergedApkFile;

            if (inputPath.endsWith(".apk")) {
                System.out.println("[INFO] Loading APK...");
                module = ApkModule.loadApkFile(inputApk);
                mergedApkFile = inputApk;
            } else {
                tempDir = Files.createTempDirectory("antik_merge").toFile();
                System.out.println("[MERGE] Extracting APKS...");
                AntikUtils.ex_apks(inputApk, tempDir);

                System.out.println("[MERGE] Merging APK...");
                ApkBundle bundle = new ApkBundle();
                bundle.loadApkDirectory(tempDir);
                module = bundle.mergeModules();

                System.out.println("[INFO] Patching AndroidManifest.xml");
                try {
                    manifestP.patch(module);
                } catch (Exception e) {
                    System.err.println("[ERROR] Manifest patching failed: " + e.getMessage());
                }

                String name = inputApk.getName();
                int dot = name.lastIndexOf('.');
                name = (dot > 0 ? name.substring(0, dot) : name) + "_merged.apk";
                mergedApkFile = new File(output.get_out(inputApk, name));

                System.out.println("[BUILD] Writing merged APK...");
                writeWithProgress(module, mergedApkFile);
                System.out.println("\n[MERGE] APK merged successfully: " + mergedApkFile.getAbsolutePath());
            }

            if (translatePath != null) {
                File jsonFile = new File(translatePath);
                if (jsonFile.exists()) {
                    System.out.println("[INFO] Starting Translation Patch...");
                    TranslationPatcher.patch(module, jsonFile);

                    String tn = mergedApkFile.getName();
                    int td = tn.lastIndexOf('.');
                    tn = (td > 0 ? tn.substring(0, td) : tn) + "_translated.apk";
                    File transFile = new File(output.get_out(inputApk, tn));

                    System.out.println("[BUILD] Building Translated APK...");
                    writeSilently(module, transFile);
                    System.out.println("[BUILD] Translated APK built at: " + transFile.getAbsolutePath());
                } else {
                    System.err.println("[ERROR] Translation file not found: " + translatePath);
                }
            } else if (!inputPath.endsWith(".apk")) {
                // Default logging patch only if merging and no translation requested
                System.out.println("[INFO] Patching classes.dex for logging...");
                try {
                    DexPatcher.patch(module);
                } catch (Exception e) {
                    System.err.println("[ERROR] Patching failed: " + e.getMessage());
                }

                String pn = mergedApkFile.getName();
                int pd = pn.lastIndexOf('.');
                pn = (pd > 0 ? pn.substring(0, pd) : pn) + "_pairip.apk";
                File paiFile = new File(output.get_out(inputApk, pn));

                System.out.println("[BUILD] Building Logging APK...");
                writeSilently(module, paiFile);
                crc32.patch(mergedApkFile, paiFile);
                System.out.println("[BUILD] Logging APK built at: " + paiFile.getAbsolutePath());
            }

            System.out.println("[BUILD] Process completed");

        } catch (Exception e) {
            System.err.println("[ERROR] Process failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (tempDir != null) {
                deleteDir.del_dir(tempDir);
            }
        }
    }

    private static void writeWithProgress(ApkModule module, File file) throws IOException {
        int total = module.getZipEntryMap().size();
        if (module.hasTableBlock()) {
            total--;
        }
        int[] count = {0};
        final int finalTotal = total;
        module.writeApk(file, new WriteProgress() {
            @Override
            public void onCompressFile(String path, int method, long length) {
                count[0]++;
                loading.progress(count[0], finalTotal);
            }
        });
        loading.progress(finalTotal, finalTotal);
    }

    private static void writeSilently(ApkModule module, File file) throws IOException {
        module.writeApk(file, new WriteProgress() {
            @Override
            public void onCompressFile(String path, int method, long length) {
            }
        });
    }
}

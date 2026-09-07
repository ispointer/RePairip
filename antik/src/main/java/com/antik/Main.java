package com.antik;

import com.antik.DexPatcher.DexPatcher;
import com.antik.DexPatcher.Translation.TranslationPatcher;
import com.antik.crc32.crc32;
import com.antik.manifest.manifestP;
import com.antik.root.*;
import com.antik.root.PackageM.Installer;
import com.antik.root.PackageM.uninstaller;
import com.antik.ui.*;
import com.antik.utils.*;
import com.reandroid.apk.ApkBundle;
import com.reandroid.apk.ApkModule;

import java.io.*;
import java.nio.file.Files;

public class Main {
    public static void main(String[] args) {

        for (String arg : args) {
            if ("-p".equals(arg) || "--package".equals(arg)) {
                com.antik.librarySurgery.LibSurgery.main(args);
                return;
            }
        }

        if (args.length < 2) {
            help.help();
            return;
        }

        String inputPath = null;
        String translatePath = null;
        boolean R_mod = false;

        for (int i = 0; i < args.length; i++) {
            if ("-i".equals(args[i]) && i + 1 < args.length) {
                inputPath = args[i + 1];
            } else if ("-t".equals(args[i]) && i + 1 < args.length) {
                translatePath = args[i + 1];
            } else if ("-r".equals(args[i])) {
                R_mod = true;
            }
        }

        if (inputPath == null) {
            help.help();
            return;
        }

        File inputApk = new File(inputPath);

        if (!inputApk.exists()) {
            System.err.println("Input file not found : " + inputPath);
            return;
        }

        banner.banner();

        File T_dIR = null;

        try {
            ApkModule module;
            File mergedApkFile;

            if (inputPath.endsWith(".apk")) {
                System.out.println("[INFO] Loading APK ");
                module = ApkModule.loadApkFile(inputApk);
                mergedApkFile = inputApk;
            } else {
                T_dIR = Files.createTempDirectory("antik_merge").toFile();
                System.out.println("[MERGE] Extracting APKS ");
                AntikUtils.ex_apks(inputApk, T_dIR);

                System.out.println("[MERGE] Merging APK ");
                ApkBundle bundle = new ApkBundle();
                bundle.loadApkDirectory(T_dIR);
                module = bundle.mergeModules();

                System.out.println("[INFO] Patching AndroidManifest.xml");
                try {
                    manifestP.patch(module);
                } catch (Exception e) {
                    System.err.println("[ERROR] Manifest patching failed : " + e.getMessage());
                }

                String name = inputApk.getName();
                int dot = name.lastIndexOf('.');
                name = (dot > 0 ? name.substring(0, dot) : name) + "_merged.apk";
                mergedApkFile = new File(output.get_out(inputApk, name));

                System.out.println("[BUILD] Writing merged APK ");
                loading.progress(module, mergedApkFile);
                System.out.println("[MERGE] APK merged successfully: " + mergedApkFile.getAbsolutePath());
            }

            if (translatePath != null) {
                File jsonFile = new File(translatePath);
                if (jsonFile.exists()) {
                    System.out.println("[INFO] Starting Translation Patch ");
                    TranslationPatcher.patch(module, jsonFile);

                    String tn = mergedApkFile.getName();
                    int td = tn.lastIndexOf('.');
                    tn = (td > 0 ? tn.substring(0, td) : tn) + "_translated.apk";
                    File transFile = new File(output.get_out(inputApk, tn));

                    System.out.println("[BUILD] Building Translated APK ");
                    output.write(module, transFile);
                    System.out.println("[BUILD] Translated APK built at : " + transFile.getAbsolutePath());
                } else {
                    System.err.println("[ERROR] Translation file not found : " + translatePath);
                }
            } else if (!inputPath.endsWith(".apk")) {
                System.out.println("[INFO] Patching classes.dex for logging ");
                try {
                    DexPatcher.patch(module);
                } catch (Exception e) {
                    System.err.println("[ERROR] Patching failed : " + e.getMessage());
                }

                String pn = mergedApkFile.getName();
                int pd = pn.lastIndexOf('.');
                pn = (pd > 0 ? pn.substring(0, pd) : pn) + "_pairip.apk";
                File paiFile = new File(output.get_out(inputApk, pn));

                System.out.println("[BUILD] Building Logging APK ");
                output.write(module, paiFile);
                crc32.patch(mergedApkFile, paiFile);
                System.out.println("[BUILD] Logging APK built at : " + paiFile.getAbsolutePath());

                if (R_mod) {
                    String pkg = module.getPackageName();
                    System.out.println("[ROOT] Starting Root Mode for package : " + pkg);
                    uninstaller.uninstall(pkg);
                    Installer.install(paiFile);

                    if (T_dIR != null) {
                        File originalBase = new File(T_dIR, "base.apk");
                        if (originalBase.exists()) {
                            Base.replaceBase(pkg, originalBase);
                            launchApp.launchApp(pkg);
                            
                            File pulledJson = new File(output.get_out(inputApk, "pairip.json"));
                            if (Pull.waitForFileAndPull(pkg, "dictionary/pairip.json", pulledJson)) {
                                System.out.println("[ROOT] pairip.json captured!");
                                Runtime.getRuntime().exec(new String[]{"su", "-c", "am force-stop " + pkg}).waitFor();
                                uninstaller.uninstall(pkg);

                                System.out.println("[INFO] Reloading clean merged APK for final patching ");
                                module = ApkModule.loadApkFile(mergedApkFile);
                                
                                System.out.println("[INFO] Starting Translation Patch with captured JSON ");
                                TranslationPatcher.patch(module, pulledJson);

                                String tn = mergedApkFile.getName();
                                int td = tn.lastIndexOf('.');
                                tn = (td > 0 ? tn.substring(0, td) : tn) + "_translated.apk";
                                File transFile = new File(output.get_out(inputApk, tn));

                                System.out.println("[BUILD] Building Final Translated APK ");
                                output.write(module, transFile);
                                System.out.println("[BUILD] Final APK built at : " + transFile.getAbsolutePath());
                            }
                        } else {
                            System.err.println("[ERROR] Original base.apk not found in APKS for root replacement");
                        }
                    } else {
                        System.out.println("[ROOT] Root mode replacement skipped (Input was not an APKS file)");
                    }
                }
            }

            System.out.println("[BUILD] Process completed");

        } catch (Exception e) {
            System.err.println("[ERROR] Process failed : " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (T_dIR != null) {
                deleteDir.del_dir(T_dIR);
            }
        }
    }
}

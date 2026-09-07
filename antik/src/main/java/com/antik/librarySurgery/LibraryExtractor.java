package com.antik.librarySurgery;

import com.antik.librarySurgery.shell.Shell;
import com.antik.librarySurgery.shell.ShellResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LibraryExtractor {

    public static List<File> getInstalledApks(String packageName) {
        ShellResult result = Shell.runRootCmd("pm path " + packageName);
        List<File> apks = new ArrayList<>();
        if (result.isSuccess() && !result.getStdout().isEmpty()) {
            for (String line : result.getStdout().split("\r?\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("package:")) {
                    String apkPath = trimmed.replace("package:", "").trim();
                    File f = new File(apkPath);
                    if (f.exists()) {
                        apks.add(f);
                    } else {
                        File tmpCopy = new File("/data/local/tmp/temp_base.apk");
                        Shell.runRootCmd("cp " + apkPath + " " + tmpCopy.getAbsolutePath() + " && chmod 666 " + tmpCopy.getAbsolutePath());
                        if (tmpCopy.exists()) {
                            apks.add(tmpCopy);
                        }
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
}

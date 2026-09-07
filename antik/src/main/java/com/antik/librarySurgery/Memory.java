package com.antik.librarySurgery;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;

public class Memory {

    public static String runRootCmd(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                sb.append(line).append("\n");
            }
            process.waitFor();
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getPidForPackage(String packageName) {
        String pidOut = runRootCmd("ps -A | grep " + packageName);
        if (pidOut.isEmpty()) {
            pidOut = runRootCmd("pidof " + packageName);
            if (!pidOut.isEmpty()) {
                String[] pids = pidOut.trim().split("\\s+");
                return pids[0];
            }
            return null;
        }

        for (String line : pidOut.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.contains(packageName)) {
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 2) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    public static String waitForProcess(String packageName) {
        System.out.println("[INFO] Waiting for package : " + packageName);
        System.out.println("[INFO] Please launch the app on your device ");

        while (true) {
            String pid = getPidForPackage(packageName);
            if (pid != null && !pid.isEmpty()) {
                System.out.println("[ROOT] App process detected with PID : " + pid);
                return pid;
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}
        }
    }

    public static String findRxAddress(String pid) {
        String mapsOut = runRootCmd("grep 'r-xp' /proc/" + pid + "/maps");
        for (String line : mapsOut.split("\r?\n")) {
            if (line.contains("apk") || line.contains("lib")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 0) {
                    return parts[0].split("-")[0];
                }
            }
        }
        return null;
    }

    public static byte[] dumpMemory(String pid, String rxAddr) {
        if (rxAddr == null || rxAddr.isEmpty()) {
            return null;
        }
        String dumpPath = "/data/local/tmp/pairip_dump.bin";
        System.out.println("[ROOT] Dumping decrypted .text memory from RAM (Address 0x" + rxAddr + ")");
        runRootCmd("dd if=/proc/" + pid + "/mem of=" + dumpPath + " bs=1024 count=576 skip=$((0x" + rxAddr + "/1024))");

        File dumpFile = new File(dumpPath);
        byte[] data = null;
        if (dumpFile.exists() && dumpFile.length() > 0) {
            try {
                data = Files.readAllBytes(dumpFile.toPath());
                System.out.println("[ROOT] Captured " + data.length + " bytes decrypted memory dump");
            } catch (Exception e) {
                System.err.println("[ERROR] Reading memory dump failed : " + e.getMessage());
            }
        }
        runRootCmd("rm -f " + dumpPath);
        return data;
    }
}

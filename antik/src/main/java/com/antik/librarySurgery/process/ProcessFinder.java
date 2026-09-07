package com.antik.librarySurgery.process;

import com.antik.librarySurgery.shell.Shell;
import com.antik.librarySurgery.shell.ShellResult;

public class ProcessFinder {

    public static String getPidForPackage(String packageName) {
        ShellResult result = Shell.runRootCmd("ps -A | grep " + packageName);
        if (!result.isSuccess() || result.getStdout().isEmpty()) {
            result = Shell.runRootCmd("pidof " + packageName);
            if (result.isSuccess() && !result.getStdout().isEmpty()) {
                String[] pids = result.getStdout().trim().split("\\s+");
                return pids[0];
            }
            return null;
        }

        for (String line : result.getStdout().split("\r?\n")) {
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
}

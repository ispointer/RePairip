package com.antik.librarySurgery.shell;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Shell {

    public static ShellResult runRootCmd(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder outSb = new StringBuilder();
            for (String line = outReader.readLine(); line != null; line = outReader.readLine()) {
                outSb.append(line).append("\n");
            }

            StringBuilder errSb = new StringBuilder();
            for (String line = errReader.readLine(); line != null; line = errReader.readLine()) {
                errSb.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            return new ShellResult(exitCode, outSb.toString(), errSb.toString());
        } catch (Exception e) {
            return new ShellResult(-1, "", e.getMessage());
        }
    }

    public static ShellResult runCmd(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder outSb = new StringBuilder();
            for (String line = outReader.readLine(); line != null; line = outReader.readLine()) {
                outSb.append(line).append("\n");
            }
            int exitCode = process.waitFor();
            return new ShellResult(exitCode, outSb.toString(), "");
        } catch (Exception e) {
            return runRootCmd(command);
        }
    }
}

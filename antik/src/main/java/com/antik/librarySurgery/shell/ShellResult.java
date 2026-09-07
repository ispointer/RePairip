package com.antik.librarySurgery.shell;

public class ShellResult {

    private final int exitCode;
    private final String stdout;
    private final String stderr;

    public ShellResult(int exitCode, String stdout, String stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout != null ? stdout.trim() : "";
        this.stderr = stderr != null ? stderr.trim() : "";
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public boolean isSuccess() {
        return exitCode == 0;
    }
}

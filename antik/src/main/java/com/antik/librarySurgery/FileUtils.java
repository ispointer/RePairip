package com.antik.librarySurgery;

import java.io.File;

public class FileUtils {

    public static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
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
}

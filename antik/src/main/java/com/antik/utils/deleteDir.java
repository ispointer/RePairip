package com.antik.utils;

import java.io.File;

public class deleteDir {
    public static void del_dir(File d) {
        File[] fs = d.listFiles();
        if (fs != null) {
            for (File f : fs) {
                if (f.isDirectory()) {
                    del_dir(f);
                } else {
                    f.delete();
                }
            }
        }
        d.delete();
    }
}

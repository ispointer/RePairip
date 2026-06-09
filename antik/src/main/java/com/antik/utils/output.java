package com.antik.utils;

import java.io.File;

public class output {
    public static String get_out(File i, String n) {
        File p = i.getParentFile();
        if (p == null) {
            return n;
        }
        return new File(p, n).getPath();
    }
}

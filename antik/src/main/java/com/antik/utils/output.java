package com.antik.utils;

import com.reandroid.apk.ApkModule;
import com.reandroid.archive.WriteProgress;

import java.io.File;
import java.io.IOException;

public class output {
    public static String get_out(File i, String n) {
        File p = i.getParentFile();
        if (p == null) {
            return n;
        }
        return new File(p, n).getPath();
    }
    public static void write(ApkModule module, File file) throws IOException {
        module.writeApk(file, new WriteProgress() {
            @Override
            public void onCompressFile(String path, int method, long length) {
            }
        });
    }
}

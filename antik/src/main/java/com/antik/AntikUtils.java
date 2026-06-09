package com.antik;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AntikUtils {
    public static String get_out(File i, String n) {
        File p = i.getParentFile();
        if (p == null) {
            return n;
        }
        return new File(p, n).getPath();
    }

    public static void ex_apks(File zf, File dd) throws Exception {
        try (ZipFile z = new ZipFile(zf)) {
            Enumeration<? extends ZipEntry> es = z.entries();
            while (es.hasMoreElements()) {
                ZipEntry e = es.nextElement();
                if (e.getName().endsWith(".apk")) {
                    File of = new File(dd, new File(e.getName()).getName());
                    try (InputStream i = z.getInputStream(e);
                         FileOutputStream fo = new FileOutputStream(of)) {
                        byte[] b = new byte[8192];
                        int l;
                        while ((l = i.read(b)) > 0) {
                            fo.write(b, 0, l);
                        }
                    }
                }
            }
        }
    }

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

    public static void banner() {
        System.out.println("░░░░░█▀▄░█▀▀░█░█░█▀▄░█▀▀░█░█░░░░░");
        System.out.println("░░░░░█▀▄░█▀▀░▀▄▀░█░█░█▀▀░▄▀▄░░░░░");
        System.out.println("░░░░░▀░▀░▀▀▀░░▀░░▀▀░░▀▀▀░▀░▀░░░░░");
        System.out.println("░█▀▄░█▀▀░█▀█░█▀█░▀█▀░█▀▄░▀█▀░█▀█░");
        System.out.println("░█▀▄░█▀▀░█▀▀░█▀█░░█░░█▀▄░░█░░█▀▀░");
        System.out.println("░▀░▀░▀▀▀░▀░░░▀░▀░▀▀▀░▀░▀░▀▀▀░▀░░░");
        System.out.println("Version : 1.3.10");
        System.out.println("--------------------------------------");
        System.out.println("Dev     : Antik (Fox)");
        System.out.println("Channel : https://t.me/RevDex");
        System.out.println("GitHub : https://github.com/ispointer/RePairip");
        System.out.println("--------------------------------------");
    }

    public static void help() {
        System.out.println("java -jar antik.jar -i <input.apks>");
    }

    public static void progress(int c, int t) {
        if (t <= 0) t = 1;
        int w = 30;
        float p = (float) c / t;
        if (p > 1.0f) p = 1.0f;
        int f = (int) (p * w);

        StringBuilder s = new StringBuilder("\r[");
        for (int i = 0; i < f; i++) {
            s.append("#");
        }
        for (int i = f; i < w; i++) {
            s.append("-");
        }
        s.append("] ");
        s.append(String.format("%d%%", (int) (p * 100)));
        System.out.print(s.toString());
        System.out.flush();
    }
}

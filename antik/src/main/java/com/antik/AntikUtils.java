package com.antik;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AntikUtils {
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
}

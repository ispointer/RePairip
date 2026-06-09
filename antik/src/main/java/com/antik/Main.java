package com.antik;

import com.antik.crc32.crc32;
import com.antik.manifest.manifestP;
import com.antik.ui.*;
import com.antik.utils.*;
import com.reandroid.apk.ApkBundle;
import com.reandroid.apk.ApkModule;
import java.io.*;
import java.nio.file.Files;

/*
 * Created by aantik
 * 3/20/2026 6:20 PM
 *
 *   ⋆    ႔ ႔
 *     ᠸ^ ^ ⸝⸝
 *       |、˜〵
 *      じしˍ,)⁐̤ᐷ
 *
 * Fox Mode 🍺
 */

/**
 *
 * @life is butterfly flyfly
 * I am a professional Android Developer
 *
 */

public class Main {
    public static void main(String[] a) {

        if (a.length < 2 || !"-i".equals(a[0]))
        {
            help.help();
            return;
        }

        String P_ta = a[1];
        File I_pa = new File(P_ta);

        if (!I_pa.exists()) {
            System.err.println("Input file not found : " + P_ta);
            return;
        }

        banner.banner();

        File T_Dir = null;

        try {

            T_Dir = Files.createTempDirectory("antik_merge").toFile();


            System.out.println("[MERGE] Extracting APKs...");
            AntikUtils.ex_apks(I_pa, T_Dir);

            System.out.println("[MERGE] Merging APK...");
            ApkBundle bd = new ApkBundle();
            bd.loadApkDirectory(T_Dir);


            ApkModule Sp_T = bd.mergeModules();


            System.out.println("[INFO] Patching AndroidManifest.xml");

            try {
                manifestP.patch(Sp_T);
            } catch (Exception e) {
                System.err.println("[ERROR] Manifest patching failed: " + e.getMessage());
                e.printStackTrace();
            }



            String o = I_pa.getName();

            int d = o.lastIndexOf('.');

            if (d > 0) {
                o = o.substring(0, d) + "_merged.apk";
            } else {
                o = o + "_merged.apk";
            }


            File o_f = new File(output.get_out(I_pa, o));

            int T_tl = Sp_T.getZipEntryMap().size();

            if (Sp_T.hasTableBlock()) {
                T_tl = T_tl - 1;
            }

            int[] c = {0};
            final int T_l = T_tl;
            Sp_T.writeApk(o_f, new com.reandroid.archive.WriteProgress() {
                @Override
                public void onCompressFile(String p, int m, long w) {
                    c[0]++;
                    loading.progress(c[0], T_l);
                }
            });

            loading.progress(T_l, T_l);

            System.out.println();
            System.out.println("[MERGE] APK merged successfully..");

            String pkg = "unknown";
            try {
                pkg = Sp_T.getPackageName();
                System.out.println("[INFO] Package Name: " + pkg);
            } catch (Exception ignored) {}

            String p_n = I_pa.getName();
            int d2 = p_n.lastIndexOf('.');
            if (d2 > 0) {
                p_n = p_n.substring(0, d2) + "_pairip.apk";
            } else {
                p_n = p_n + "_pairip.apk";
            }
            File Pai_Dir = new File(output.get_out(I_pa, p_n));

            System.out.println("[INFO] Patching classes.dex");
            try {
                DexPatcher.patch(Sp_T);
            } catch (Exception e) {
                System.err.println("[ERROR] Patching failed: " + e.getMessage());
            }
            

            System.out.println("[BUILD] Building APK...");
            Sp_T.writeApk(Pai_Dir);
            crc32.patch(o_f, Pai_Dir);

            System.out.println("[BUILD] APK built successfully at " + Pai_Dir.getAbsolutePath());
            System.out.println("[BUILD] Process completed");

            System.out.println("--------------------------------------");
            System.out.println("⚠️ DO NOT use the pairip app (" + Pai_Dir.getAbsolutePath() + ") for translation.");
            System.out.println("It is incomplete and intended for logging only.");
            System.out.println("Please use the merged app instead (" + o_f.getAbsolutePath() + ").");
            System.out.println("--------------------------------------");

        } catch (Exception e) {
            System.err.println("[ERROR] Merge failed: " + e.getMessage());
        } finally {
            if (T_Dir != null) {
                deleteDir.del_dir(T_Dir);
            }
        }
    }
}

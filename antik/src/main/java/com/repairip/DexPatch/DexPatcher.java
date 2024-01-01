/*
 *
 * Copyright (c) 2024 Antik
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.repairip.DexPatch;

import com.repairip.DexPatch.Logging.LauncherPatcher;
import com.repairip.DexPatch.Logging.SecurityBypassPatcher;
import com.repairip.Main;
import com.reandroid.apk.ApkModule;
import com.reandroid.archive.ByteInputSource;
import com.reandroid.archive.InputSource;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

/*
 * Created by aantik
 * 11/11/2024
 *
 *   ⋆   ႔ ႔
 *     ᠸ^ ^ ⸝⸝
 *      |、˜〵
 *      じしˍ,)⁐̤ᐷ
 *
 * Fox Mode 🍺
 */
public class DexPatcher {
    public static List<String> patch(ApkModule m) throws Exception {
        List<String> dx_ns = new ArrayList<String>();
        for (InputSource s : m.getInputSources()) {
            if (s.getName().endsWith(".dex")) {
                dx_ns.add(s.getName());
            }
        }

        List<ClassDef> l_cds = new ArrayList<ClassDef>();
        Set<String> a_ts = new HashSet<String>();
        String pkg = "nill";
        try {
            pkg = m.getPackageName();
        } catch (Exception ignored) {
            ////////////
        }

        try (InputStream i = Main.class.getResourceAsStream("/log.dex")) {
            if (i != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int len;
                while ((len = i.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                }
                byte[] l_bs = baos.toByteArray();
                File t_l_dx = File.createTempFile("log", ".dex");
                Files.write(t_l_dx.toPath(), l_bs);
                DexFile l_df = DexFileFactory.loadDexFile(t_l_dx, Opcodes.getDefault());
                t_l_dx.delete();

                for (ClassDef c : l_df.getClasses()) {
                    if ("Lcom/pairip/PairipLog;".equals(c.getType())) {
                        List<org.jf.dexlib2.iface.Field> s_fs = new ArrayList<org.jf.dexlib2.iface.Field>();
                        for (org.jf.dexlib2.iface.Field f : c.getStaticFields()) {
                            if ("DIR_PATH".equals(f.getName())) {
                                String n_v = "/data/data/" + pkg + "/dictionary";
                                s_fs.add(new org.jf.dexlib2.immutable.ImmutableField(f.getDefiningClass(), f.getName(), f.getType(), f.getAccessFlags(), new org.jf.dexlib2.immutable.value.ImmutableStringEncodedValue(n_v), f.getAnnotations(), f.getHiddenApiRestrictions()));
                            } else {
                                s_fs.add(f);
                            }
                        }
                        l_cds.add(new ImmutableClassDef(c.getType(), c.getAccessFlags(), c.getSuperclass(), c.getInterfaces(), c.getSourceFile(), c.getAnnotations(), s_fs, c.getInstanceFields(), c.getDirectMethods(), c.getVirtualMethods()));
                    } else {
                        l_cds.add(c);
                    }
                    a_ts.add(c.getType());
                }
                System.out.println("[INFO] Added classes from log.dex " + pkg);
            }
        }

        List<String> j_ts = new ArrayList<String>();
        for (String dn : dx_ns) {
            InputSource s = m.getInputSource(dn);
            if (s == null) continue;
            byte[] d_bs;
            try (InputStream i = s.openStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int len;
                while ((len = i.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                }
                d_bs = baos.toByteArray();
            }
            File t_dx = File.createTempFile("temp", ".dex");
            Files.write(t_dx.toPath(), d_bs);
            DexFile d_f = DexFileFactory.loadDexFile(t_dx, Opcodes.getDefault());
            t_dx.delete();
            for (ClassDef cd : d_f.getClasses()) {
                if (cd.getMethods().iterator().hasNext()) continue;
                if (!cd.getFields().iterator().hasNext()) continue;
                if (!"Ljava/lang/Object;".equals(cd.getSuperclass())) continue;
                if (cd.getAccessFlags() != 1) continue;

                boolean o_t = true;
                for (org.jf.dexlib2.iface.Field f : cd.getFields()) {
                    if (f.getAccessFlags() != 9) {
                        o_t = false;
                        break;
                    }
                    String t = f.getType();
                    if (!t.equals("Ljava/lang/String;") && !t.equals("Ljava/lang/reflect/Method;")) {
                        o_t = false;
                        break;
                    }
                    if (f.getInitialValue() != null) {
                        o_t = false;
                        break;
                    }
                }
                if (o_t) {
                    j_ts.add(cd.getType());
                }
            }
        }
        System.out.println("[INFO] Found " + j_ts.size() + " junk classes");
        for (String dn : dx_ns) {
            InputSource s = m.getInputSource(dn);
            if (s == null) continue;
            byte[] d_bs;
            try (InputStream i = s.openStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int len;
                while ((len = i.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                }
                d_bs = baos.toByteArray();
            }
            File t_dx = File.createTempFile("temp", ".dex");
            Files.write(t_dx.toPath(), d_bs);
            DexFile d_f = DexFileFactory.loadDexFile(t_dx, Opcodes.getDefault());
            t_dx.delete();

            List<ClassDef> cds = new ArrayList<ClassDef>();
            boolean mod = false;

            for (ClassDef cd : d_f.getClasses()) {
                if (a_ts.contains(cd.getType())) {
                    continue;
                }
                if ("Lcom/pairip/StartupLauncher;".equals(cd.getType())) {
                    mod = true;
                    cds.add(LauncherPatcher.patchStartupLauncher(cd, j_ts));
                } else if ("Lcom/pairip/SignatureCheck;".equals(cd.getType()) || "Lcom/pairip/licensecheck/LicenseClient;".equals(cd.getType()) || "Lcom/pairip/licensecheck3/LicenseClientV3;".equals(cd.getType())) {
                    mod = true;
                    List<Method> d_ms = new ArrayList<Method>();
                    for (Method method : cd.getDirectMethods()) {
                        d_ms.add(SecurityBypassPatcher.patchMethodIfTarget(method));
                    }
                    List<Method> v_ms = new ArrayList<Method>();
                    for (Method method : cd.getVirtualMethods()) {
                        v_ms.add(SecurityBypassPatcher.patchMethodIfTarget(method));
                    }
                    cds.add(new ImmutableClassDef(cd.getType(), cd.getAccessFlags(), cd.getSuperclass(), cd.getInterfaces(), cd.getSourceFile(), cd.getAnnotations(), cd.getStaticFields(), cd.getInstanceFields(), d_ms, v_ms));
                } else {
                    cds.add(cd);
                }
            }

            if (dn.equals("classes.dex")) {
                if (!l_cds.isEmpty()) {
                    cds.addAll(l_cds);
                    mod = true;
                }
            }

            if (mod) {
                MemoryDataStore ds = new MemoryDataStore();
                DexPool dp = new DexPool(Opcodes.getDefault());
                for (ClassDef c : cds) {
                    dp.internClass(c);
                }
                dp.writeTo(ds);
                byte[] r_bs = Arrays.copyOf(ds.getData(), ds.getSize());
                m.add(new ByteInputSource(r_bs, dn));
                System.out.println("[BUILD] Patched " + dn);
            }
        }
        return j_ts;
    }
}

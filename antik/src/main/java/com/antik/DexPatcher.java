package com.antik;

import com.reandroid.apk.ApkModule;
import com.reandroid.archive.ByteInputSource;
import com.reandroid.archive.InputSource;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11n;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21c;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class DexPatcher {
    public static void patch(ApkModule m) throws Exception {
        List<String> dx_ns = new ArrayList<String>();
        for (InputSource s : m.getInputSources()) {
            if (s.getName().endsWith(".dex")) {
                dx_ns.add(s.getName());
            }
        }

        List<ClassDef> l_cds = new ArrayList<ClassDef>();
        Set<String> a_ts = new HashSet<String>();
        String pkg = "unknown";
        try {
            pkg = m.getPackageName();
        } catch (Exception ignored) {}

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
                                s_fs.add(new org.jf.dexlib2.immutable.ImmutableField(
                                        f.getDefiningClass(),
                                        f.getName(),
                                        f.getType(),
                                        f.getAccessFlags(),
                                        new org.jf.dexlib2.immutable.value.ImmutableStringEncodedValue(n_v),
                                        f.getAnnotations(),
                                        f.getHiddenApiRestrictions()));
                            } else {
                                s_fs.add(f);
                            }
                        }
                        l_cds.add(new ImmutableClassDef(
                                c.getType(), c.getAccessFlags(), c.getSuperclass(),
                                c.getInterfaces(), c.getSourceFile(), c.getAnnotations(),
                                s_fs, c.getInstanceFields(),
                                c.getDirectMethods(), c.getVirtualMethods()));
                    } else {
                        l_cds.add(c);
                    }
                    a_ts.add(c.getType());
                }
                System.out.println("[INFO] Added classes from log.dex (Package: " + pkg + ")");
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
                    cds.add(patchStartupLauncher(cd, j_ts));
                } else if ("Lcom/pairip/SignatureCheck;".equals(cd.getType()) ||
                        "Lcom/pairip/licensecheck/LicenseClient;".equals(cd.getType()) ||
                        "Lcom/pairip/licensecheck3/LicenseClientV3;".equals(cd.getType())) {
                    mod = true;

                    List<Method> d_ms = new ArrayList<Method>();
                    for (Method method : cd.getDirectMethods()) {
                        d_ms.add(patchMethodIfTarget(method));
                    }

                    List<Method> v_ms = new ArrayList<Method>();
                    for (Method method : cd.getVirtualMethods()) {
                        v_ms.add(patchMethodIfTarget(method));
                    }

                    cds.add(new ImmutableClassDef(
                            cd.getType(), cd.getAccessFlags(), cd.getSuperclass(),
                            cd.getInterfaces(), cd.getSourceFile(), cd.getAnnotations(),
                            cd.getStaticFields(), cd.getInstanceFields(),
                            d_ms, v_ms));
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
    }

    private static Method patchMethodIfTarget(Method m) {
        String n = m.getName();
        if ("verifySignatureMatches".equals(n)) {
            System.out.println("[INFO] Bypassing verifySignatureMatches");
            List<Instruction> ins = Arrays.asList(
                    new ImmutableInstruction11n(Opcode.CONST_4, 0, 1),
                    new ImmutableInstruction11x(Opcode.RETURN, 0)
            );
            return new ImmutableMethod(
                    m.getDefiningClass(), n, m.getParameters(),
                    m.getReturnType(), m.getAccessFlags(),
                    m.getAnnotations(), m.getHiddenApiRestrictions(),
                    new ImmutableMethodImplementation(1, ins, null, null));
        } else if ("verifyIntegrity".equals(n)) {
            System.out.println("[INFO] Bypassing " + n);
            String rt = m.getReturnType();
            List<Instruction> ins;
            if ("V".equals(rt)) {
                ins = Collections.singletonList(new ImmutableInstruction10x(Opcode.RETURN_VOID));
            } else if (rt.startsWith("L") || rt.startsWith("[")) {
                ins = Arrays.asList(
                        new ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                        new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)
                );
            } else {
                ins = Arrays.asList(
                        new ImmutableInstruction11n(Opcode.CONST_4, 0, 1),
                        new ImmutableInstruction11x(Opcode.RETURN, 0)
                );
            }
            return new ImmutableMethod(
                    m.getDefiningClass(), n, m.getParameters(),
                    rt, m.getAccessFlags(),
                    m.getAnnotations(), m.getHiddenApiRestrictions(),
                    new ImmutableMethodImplementation(1, ins, null, null));
        }
        return m;
    }

    private static ClassDef patchStartupLauncher(ClassDef cd, List<String> j_ts) {
        System.out.println("[INFO] Patching StartupLauncher");
        List<Method> d_ms = new ArrayList<Method>();
        for (Method m : cd.getDirectMethods()) {
            if ("launch".equals(m.getName())) {
                d_ms.add(patchLaunchMethod(m));
            } else if (!"pairip".equals(m.getName())) {
                d_ms.add(m);
            }
        }
        d_ms.add(createPairipMethod(cd.getType(), j_ts));

        return new ImmutableClassDef(
                cd.getType(), cd.getAccessFlags(), cd.getSuperclass(),
                cd.getInterfaces(), cd.getSourceFile(), cd.getAnnotations(),
                cd.getStaticFields(), cd.getInstanceFields(),
                d_ms, cd.getVirtualMethods());
    }

    private static Method patchLaunchMethod(Method m) {
        MethodImplementation im = m.getImplementation();
        if (im == null) return m;

        List<Instruction> o_ins = new ArrayList<Instruction>();
        for (Instruction i : im.getInstructions()) {
            o_ins.add(i);
        }

        List<Instruction> n_ins = new ArrayList<Instruction>();
        int p_off = -1;
        int c_off = 0;

        for (int i = 0; i < o_ins.size(); i++) {
            Instruction inst = o_ins.get(i);
            n_ins.add(inst);
            int u = inst.getCodeUnits();
            if (inst instanceof ReferenceInstruction) {
                ReferenceInstruction r_i = (ReferenceInstruction) inst;
                if (r_i.getReference() instanceof MethodReference) {
                    MethodReference m_r = (MethodReference) r_i.getReference();
                    if ("Lcom/pairip/VMRunner;".equals(m_r.getDefiningClass()) && "invoke".equals(m_r.getName())) {

                        boolean a_p = false;
                        if (i + 1 < o_ins.size()) {
                            Instruction nx = o_ins.get(i + 1);
                            if (nx instanceof ReferenceInstruction) {
                                ReferenceInstruction n_r = (ReferenceInstruction) nx;
                                if (n_r.getReference() instanceof MethodReference) {
                                    MethodReference n_m = (MethodReference) n_r.getReference();
                                    if ("pairip".equals(n_m.getName())) {
                                        a_p = true;
                                    }
                                }
                            }
                        }

                        if (!a_p) {
                            p_off = c_off + u;
                            n_ins.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0,
                                    new ImmutableMethodReference("Lcom/pairip/StartupLauncher;", "pairip", null, "V")));
                        }
                    }
                }
            }
            c_off += u;
        }

        if (p_off == -1) return m;

        List<? extends org.jf.dexlib2.iface.TryBlock<? extends org.jf.dexlib2.iface.ExceptionHandler>> o_tbs = im.getTryBlocks();
        List<org.jf.dexlib2.immutable.ImmutableTryBlock> n_tbs = new ArrayList<org.jf.dexlib2.immutable.ImmutableTryBlock>();

        for (org.jf.dexlib2.iface.TryBlock<? extends org.jf.dexlib2.iface.ExceptionHandler> t : o_tbs) {
            int s = t.getStartCodeAddress();
            int c = t.getCodeUnitCount();
            int e = s + c;

            List<org.jf.dexlib2.immutable.ImmutableExceptionHandler> h = new ArrayList<org.jf.dexlib2.immutable.ImmutableExceptionHandler>();
            for (org.jf.dexlib2.iface.ExceptionHandler ha : t.getExceptionHandlers()) {
                int h_a = ha.getHandlerCodeAddress();
                if (h_a >= p_off) {
                    h.add(new org.jf.dexlib2.immutable.ImmutableExceptionHandler(ha.getExceptionType(), h_a + 3));
                } else {
                    h.add(org.jf.dexlib2.immutable.ImmutableExceptionHandler.of(ha));
                }
            }

            if (s < p_off && e >= p_off) {
                n_tbs.add(new org.jf.dexlib2.immutable.ImmutableTryBlock(s, c + 3, h));
            } else if (s >= p_off) {
                n_tbs.add(new org.jf.dexlib2.immutable.ImmutableTryBlock(s + 3, c, h));
            } else {
                n_tbs.add(new org.jf.dexlib2.immutable.ImmutableTryBlock(s, c, h));
            }
        }

        return new ImmutableMethod(
                m.getDefiningClass(), m.getName(), m.getParameters(),
                m.getReturnType(), m.getAccessFlags(),
                m.getAnnotations(), m.getHiddenApiRestrictions(),
                new ImmutableMethodImplementation(im.getRegisterCount(), n_ins, n_tbs, im.getDebugItems()));
    }

    private static Method createPairipMethod(String dc, List<String> j_ts) {
        List<Instruction> ins = new ArrayList<Instruction>();
        for (String t : j_ts) {
            ins.add(new ImmutableInstruction21c(Opcode.CONST_CLASS, 0, new ImmutableTypeReference(t)));
            ins.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 1, 0, 0, 0, 0, 0,
                    new ImmutableMethodReference("Lcom/pairip/PairipLog;", "put", Collections.singletonList("Ljava/lang/Class;"), "V")));
        }
        ins.add(new ImmutableInstruction10x(Opcode.RETURN_VOID));

        return new ImmutableMethod(
                dc, "pairip", null, "V", 0x08,
                null, null,
                new ImmutableMethodImplementation(1, ins, null, null));
    }
}

package com.antik.DexPatcher.MethodT;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;

import java.util.ArrayList;
import java.util.List;

public class patchLaunchMethod {
    public static Method patchLaunchMethod(Method m) {
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
            int u = inst.getCodeUnits();

            n_ins.add(inst);
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
                            n_ins.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, new ImmutableMethodReference("Lcom/pairip/StartupLauncher;", "pairip", null, "V")));
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

        return new ImmutableMethod(m.getDefiningClass(), m.getName(), m.getParameters(), m.getReturnType(), m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(), new ImmutableMethodImplementation(im.getRegisterCount(), n_ins, n_tbs, im.getDebugItems()));
    }
}

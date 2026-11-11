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

package com.repairip.DexPatch.Logging;

import com.repairip.Utils.SpannableBanner;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11n;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11x;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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


/**
 * Handles bypassing security check methods such as signature and integrity verifications.
 */
public class SecurityBypassPatcher {

    /**
     * Replaces signature and integrity verification method implementations with stub return values.
     *
     * @param method method to check and potentially patch
     * @return patched method if matching target security check, or original method
     */
    public static Method patchMethodIfTarget(Method method) {
        String name = method.getName();
        if ("verifySignatureMatches".equals(name)) {
            System.out.println(SpannableBanner.CYAN + "[INFO] " + SpannableBanner.RESET + "Bypassing verifySignatureMatches");
            List<Instruction> instructions = Arrays.asList(
                    new ImmutableInstruction11n(Opcode.CONST_4, 0, 1),
                    new ImmutableInstruction11x(Opcode.RETURN, 0)
            );
            return new ImmutableMethod(
                    method.getDefiningClass(),
                    name,
                    method.getParameters(),
                    method.getReturnType(),
                    method.getAccessFlags(),
                    method.getAnnotations(),
                    method.getHiddenApiRestrictions(),
                    new ImmutableMethodImplementation(1, instructions, null, null)
            );
        } else if ("verifyIntegrity".equals(name)) {
            System.out.println(SpannableBanner.CYAN + "[INFO] " + SpannableBanner.RESET + "Bypassing " + name);
            String returnType = method.getReturnType();
            List<Instruction> instructions;
            if ("V".equals(returnType)) {
                instructions = Collections.singletonList(new ImmutableInstruction10x(Opcode.RETURN_VOID));
            } else if (returnType.startsWith("L") || returnType.startsWith("[")) {
                instructions = Arrays.asList(
                        new ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
                        new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0)
                );
            } else {
                instructions = Arrays.asList(
                        new ImmutableInstruction11n(Opcode.CONST_4, 0, 1),
                        new ImmutableInstruction11x(Opcode.RETURN, 0)
                );
            }
            return new ImmutableMethod(
                    method.getDefiningClass(),
                    name,
                    method.getParameters(),
                    returnType,
                    method.getAccessFlags(),
                    method.getAnnotations(),
                    method.getHiddenApiRestrictions(),
                    new ImmutableMethodImplementation(1, instructions, null, null)
            );
        }
        return method;
    }
}

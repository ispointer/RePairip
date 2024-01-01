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

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21c;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;

import java.util.ArrayList;
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
 * Generates synthetic bytecode methods for Pairip logging registration.
 */
public class PairipMethodGenerator {

    /**
     * Creates the static {@code pairip()} method that passes junk classes to {@code PairipLog.put(Class)}.
     *
     * @param definingClass class descriptor in which the method will be defined
     * @param junkClasses list of junk class descriptors to register
     * @return synthetic {@code pairip()} method definition
     */
    public static Method createPairipMethod(String definingClass, List<String> junkClasses) {
        List<Instruction> instructions = new ArrayList<>();
        for (String type : junkClasses) {
            instructions.add(new ImmutableInstruction21c(Opcode.CONST_CLASS, 0, new ImmutableTypeReference(type)));
            instructions.add(new ImmutableInstruction35c(
                    Opcode.INVOKE_STATIC, 1, 0, 0, 0, 0, 0,
                    new ImmutableMethodReference("Lcom/pairip/PairipLog;", "put", Collections.singletonList("Ljava/lang/Class;"), "V")
            ));
        }
        instructions.add(new ImmutableInstruction10x(Opcode.RETURN_VOID));
        return new ImmutableMethod(definingClass, "pairip", null, "V", 0x08, null, null, new ImmutableMethodImplementation(1, instructions, null, null));
    }
}

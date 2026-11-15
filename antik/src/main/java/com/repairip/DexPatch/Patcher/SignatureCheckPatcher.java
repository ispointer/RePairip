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

package com.repairip.DexPatch.Patcher;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;

import java.util.ArrayList;
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

public class SignatureCheckPatcher {

    /**
     * removes the pairip signature check
     * @param method
     * @return
     */
    public static Method patchMethodIfTarget(Method method) {

        List<Instruction> instructions = new ArrayList<Instruction>();
        String mSignatureCheck = "Lcom/pairip/SignatureCheck;->verifyIntegrity(Landroid/content/Context;)V";

        for (Instruction instruction : method.getImplementation().getInstructions()) {
            if (instruction instanceof ReferenceInstruction) {
                String ref = ((ReferenceInstruction) instruction).getReference().toString();

                //TODO: it's not final
                if (ref.equals(mSignatureCheck)) {
                    continue;
                }
            }

            instructions.add(instruction);
        }

        return new ImmutableMethod(
                method.getDefiningClass(),
                method.getName(),
                method.getParameters(),
                method.getReturnType(),
                method.getAccessFlags(),
                method.getAnnotations(),
                method.getHiddenApiRestrictions(),
                new ImmutableMethodImplementation(
                        method.getImplementation().getRegisterCount(),
                        instructions,
                        method.getImplementation().getTryBlocks(),
                        method.getImplementation().getDebugItems()
                )
        );
    }
}

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
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.ImmutableExceptionHandler;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.ImmutableTryBlock;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;

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


/**
 * Patches the target {@code launch} method by injecting a static invocation to
 * {@code com.pairip.StartupLauncher.pairip()} after the VMRunner invoke instruction.
 */
public class LaunchMethodPatcher {

    /**
     * Injects the call to {@code StartupLauncher.pairip()} into the provided method's instruction sequence.
     *
     * @param method the original {@code launch} method to patch
     * @return the patched method if modified, or the original method if no target pattern was found
     */
    public static Method patchLaunchMethod(Method method) {
        MethodImplementation implementation = method.getImplementation();
        if (implementation == null) {
            return method;
        }

        List<Instruction> originalInstructions = new ArrayList<>();
        for (Instruction instruction : implementation.getInstructions()) {
            originalInstructions.add(instruction);
        }

        List<Instruction> newInstructions = new ArrayList<>();
        int patchedOffset = -1;
        int currentOffset = 0;

        for (int i = 0; i < originalInstructions.size(); i++) {
            Instruction instruction = originalInstructions.get(i);
            int codeUnits = instruction.getCodeUnits();

            newInstructions.add(instruction);
            if (instruction instanceof ReferenceInstruction) {
                ReferenceInstruction referenceInstruction = (ReferenceInstruction) instruction;
                if (referenceInstruction.getReference() instanceof MethodReference) {
                    MethodReference methodReference = (MethodReference) referenceInstruction.getReference();
                    if ("Lcom/pairip/VMRunner;".equals(methodReference.getDefiningClass())
                            && "invoke".equals(methodReference.getName())) {

                        boolean alreadyPatched = false;
                        if (i + 1 < originalInstructions.size()) {
                            Instruction nextInstruction = originalInstructions.get(i + 1);
                            if (nextInstruction instanceof ReferenceInstruction) {
                                ReferenceInstruction nextRefInst = (ReferenceInstruction) nextInstruction;
                                if (nextRefInst.getReference() instanceof MethodReference) {
                                    MethodReference nextMethodRef = (MethodReference) nextRefInst.getReference();
                                    if ("pairip".equals(nextMethodRef.getName())) {
                                        alreadyPatched = true;
                                    }
                                }
                            }
                        }
                        if (!alreadyPatched) {
                            patchedOffset = currentOffset + codeUnits;
                            newInstructions.add(new ImmutableInstruction35c(
                                    Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0,
                                    new ImmutableMethodReference("Lcom/pairip/StartupLauncher;", "pairip", null, "V")
                            ));
                        }
                    }
                }
            }
            currentOffset += codeUnits;
        }

        if (patchedOffset == -1) {
            return method;
        }

        List<? extends TryBlock<? extends ExceptionHandler>> originalTryBlocks = implementation.getTryBlocks();
        List<ImmutableTryBlock> newTryBlocks = new ArrayList<>();

        for (TryBlock<? extends ExceptionHandler> tryBlock : originalTryBlocks) {
            int start = tryBlock.getStartCodeAddress();
            int count = tryBlock.getCodeUnitCount();
            int end = start + count;

            List<ImmutableExceptionHandler> newHandlers = new ArrayList<>();
            for (ExceptionHandler handler : tryBlock.getExceptionHandlers()) {
                int handlerAddress = handler.getHandlerCodeAddress();
                if (handlerAddress >= patchedOffset) {
                    newHandlers.add(new ImmutableExceptionHandler(handler.getExceptionType(), handlerAddress + 3));
                } else {
                    newHandlers.add(ImmutableExceptionHandler.of(handler));
                }
            }

            if (start < patchedOffset && end >= patchedOffset) {
                newTryBlocks.add(new ImmutableTryBlock(start, count + 3, newHandlers));
            } else if (start >= patchedOffset) {
                newTryBlocks.add(new ImmutableTryBlock(start + 3, count, newHandlers));
            } else {
                newTryBlocks.add(new ImmutableTryBlock(start, count, newHandlers));
            }
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
                        implementation.getRegisterCount(),
                        newInstructions,
                        newTryBlocks,
                        implementation.getDebugItems()
                )
        );
    }
}

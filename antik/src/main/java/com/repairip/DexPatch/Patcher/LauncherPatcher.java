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

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
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


public class LauncherPatcher {


    /**
     * patches StartupLauncher to invoke the RePairip logger after VMRunner
     * @param mClass
     * @return
     */
    public static ClassDef patchStartupLauncher(ClassDef mClass) {

        List<Method> mMethods = new ArrayList<Method>();

        String mVMRunner = "Lcom/pairip/VMRunner;->invoke(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;";

        String mVmrunnerlogger = "Lcom/aantik/vmrunnerlogger/Object;->pairip()V";


        for (Method mMethod : mClass.getDirectMethods()) {

            if ("launch".equals(mMethod.getName()) && mMethod.getImplementation() != null) {

                MutableMethodImplementation mCode = new MutableMethodImplementation(mMethod.getImplementation());

                for (int i = 0; i < mCode.getInstructions().size(); i++) {

                    Instruction mInstruction = mCode.getInstructions().get(i);

                    if (!(mInstruction instanceof ReferenceInstruction)) {
                        continue;
                    }

                    String mReference = ((ReferenceInstruction) mInstruction).getReference().toString();

                    if (!mVMRunner.equals(mReference)) {
                        continue;
                    }

                    if (i + 1 < mCode.getInstructions().size()) {

                        Instruction mNext = mCode.getInstructions().get(i + 1);

                        if (mNext instanceof ReferenceInstruction) {

                            String mNextReference = ((ReferenceInstruction) mNext).getReference().toString();

                            if (mVmrunnerlogger.equals(mNextReference)) {
                                // no xref
                                break;
                            }
                        }
                    }

                    mCode.addInstruction(i + 1, new BuilderInstruction35c(
                            Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0,
                            new ImmutableMethodReference(
                                    mVmrunnerlogger.substring(0, mVmrunnerlogger.indexOf("->")),
                                    mVmrunnerlogger.substring(mVmrunnerlogger.indexOf("->") + 2, mVmrunnerlogger.indexOf("(")),
                                    null,
                                    "V"
                            )
                    ));

                    break;
                }

                mMethod = new ImmutableMethod(
                        mMethod.getDefiningClass(),
                        mMethod.getName(),
                        mMethod.getParameters(),
                        mMethod.getReturnType(),
                        mMethod.getAccessFlags(),
                        mMethod.getAnnotations(),
                        mMethod.getHiddenApiRestrictions(),
                        mCode
                );
            }

            mMethods.add(mMethod);
        }

        return new ImmutableClassDef(
                mClass.getType(),
                mClass.getAccessFlags(),
                mClass.getSuperclass(),
                mClass.getInterfaces(),
                mClass.getSourceFile(),
                mClass.getAnnotations(),
                mClass.getStaticFields(),
                mClass.getInstanceFields(),
                mMethods,
                mClass.getVirtualMethods()
        );
    }

}

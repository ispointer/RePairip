/*
 *
 * Copyright (c) 2026 Antik
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
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21c;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/*
 * Created by aantik
 * 11/15/2026
 *
 *   ⋆   ႔ ႔
 *     ᠸ^ ^ ⸝⸝
 *      |、˜〵
 *      じしˍ,)⁐̤ᐷ
 *
 * Fox Mode 🍺
 */

public class ObjectClassGenerator {

    /**
     * generates Object class and registers junk classes with RePairipLog ;
     * @param mJunkClasses
     * @return
     */
    public static ClassDef createClassDef(Collection<String> mJunkClasses) {

        String mClass = "Lcom/aantik/vmrunnerlogger/Object;";
        String mLogger = "Lcom/aantik/vmrunnerlogger/RePairipLog;";

        List<Instruction> mCode = new ArrayList<Instruction>();

        mCode.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, 0, new ImmutableTypeReference(mLogger)));

        mCode.add(new ImmutableInstruction35c(Opcode.INVOKE_DIRECT, 1, 0, 0, 0, 0, 0, new ImmutableMethodReference(mLogger, "<init>", null, "V")));

        mCode.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0, new ImmutableMethodReference("Ljava/lang/Thread;", "start", null, "V")));

        for (String mType : mJunkClasses) {

            mCode.add(new ImmutableInstruction21c(Opcode.CONST_CLASS, 1, new ImmutableTypeReference(mType)));
            mCode.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 0, 1, 0, 0, 0, new ImmutableMethodReference(mLogger, "put", Collections.singletonList("Ljava/lang/Class;"), "V")));

        }

        mCode.add(new ImmutableInstruction10x(Opcode.RETURN_VOID));

        Method mMethod = new ImmutableMethod(mClass, "pairip", null, "V", 0x9, null, null, new ImmutableMethodImplementation(2, mCode, null, null));

        return new ImmutableClassDef(mClass, 0x1, "Ljava/lang/Object;", null, "Object.java", null, null, null, Collections.singletonList(mMethod), null);
    }
}


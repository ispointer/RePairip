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

package com.repairip.DexPatch.Translation;

import com.reandroid.json.JSONObject;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11n;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableField;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodParameter;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.immutable.value.ImmutableBooleanEncodedValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
 * Created by aantik
 * 11/16/2026
 *
 *   ⋆   ႔ ႔
 *     ᠸ^ ^ ⸝⸝
 *      |、˜〵
 *      じしˍ,)⁐̤ᐷ
 *
 * Fox Mode 🍺
 */

public class TranslationObjectGenerator {

    /**
     * generates a new dex class or modifies an existing class
     *
     * @param mJson
     * @param mClass
     * @return
     * @param <T>
     */
    public static <T extends ClassDef> ClassDef generate(JSONObject mJson, T mClass) {

        String mObjectType = "Lcom/aantik/vmrunnerlogger/Object;";
        String mRestoreType = "Lcom/aantik/vmrunnerlogger/RePairipRestore;";
        String mStringType = "Ljava/lang/String;";
        String mMethodType = "Ljava/lang/reflect/Method;";

        List<Method> mMethods = new ArrayList<Method>();

        if (mClass == null) {

            MethodImplementationBuilder mRestore = new MethodImplementationBuilder(1);

            ImmutableMethodReference mGet = new ImmutableMethodReference(mRestoreType, "get", Collections.singletonList(mStringType), mMethodType);

            for (String mName : mJson.keySet()) {

                JSONObject mObject = mJson.getJSONObject(mName);
                JSONObject mFields = mObject.getJSONObject("fields");

                String mTarget = "L" + mName.replace('.', '/') + ";";
                boolean mIsMethod = mObject.getString("type").equals("java.lang.reflect.Method");

                String mFieldType = mStringType;

                if (mIsMethod) {
                    mFieldType = mMethodType;
                }

                for (String mField : mFields.keySet()) {

                    mRestore.addInstruction(new BuilderInstruction21c(Opcode.CONST_STRING, 0, new ImmutableStringReference(mFields.getString(mField))));

                    if (mIsMethod) {

                        mRestore.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 0, 0, 0, 0, 0, mGet));

                        mRestore.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));

                    }

                    mRestore.addInstruction(new BuilderInstruction21c(Opcode.SPUT_OBJECT, 0, new ImmutableFieldReference(mTarget, mField, mFieldType)));

                }
            }

            mRestore.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

            mMethods.add(new ImmutableMethod(mObjectType, "restore", null, "V", AccessFlags.PRIVATE.getValue() | AccessFlags.STATIC.getValue(), null, null, mRestore.getMethodImplementation()));


            // launch()
            MethodImplementationBuilder mLaunch = new MethodImplementationBuilder(1);

            ImmutableFieldReference mFlag =
                    new ImmutableFieldReference(mObjectType, "launchCalled", "Z");

            ImmutableMethodReference mRestoreMethod =
                    new ImmutableMethodReference(mObjectType, "restore", null, "V");

            mLaunch.addInstruction(
                    new BuilderInstruction21c(Opcode.SGET_BOOLEAN, 0, mFlag)
            );

            mLaunch.addInstruction(
                    new BuilderInstruction21t(Opcode.IF_NEZ, 0, mLaunch.getLabel("done"))
            );

            mLaunch.addInstruction(
                    new BuilderInstruction11n(Opcode.CONST_4, 0, 1)
            );

            mLaunch.addInstruction(
                    new BuilderInstruction21c(Opcode.SPUT_BOOLEAN, 0, mFlag)
            );

            mLaunch.addInstruction(new BuilderInstruction35c(
                    Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, mRestoreMethod
            ));

            mLaunch.addLabel("done");
            mLaunch.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

            mMethods.add(new ImmutableMethod(mObjectType, "launch", null, "V",
                    AccessFlags.PUBLIC.getValue() | AccessFlags.STATIC.getValue() | AccessFlags.SYNCHRONIZED.getValue(),
                    null, null, mLaunch.getMethodImplementation()));



            // invoke()
            MethodImplementationBuilder mInvoke = new MethodImplementationBuilder(3);

            mInvoke.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
            mInvoke.addInstruction(new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));

            mMethods.add(new ImmutableMethod(mObjectType, "invoke",
                    Arrays.asList(new ImmutableMethodParameter(mStringType, null, null),
                            new ImmutableMethodParameter("[Ljava/lang/Object;", null, null)),
                    "Ljava/lang/Object;", AccessFlags.PUBLIC.getValue() | AccessFlags.STATIC.getValue(),
                    null, null, mInvoke.getMethodImplementation()));

            return new ImmutableClassDef(mObjectType, AccessFlags.PUBLIC.getValue(),
                    "Ljava/lang/Object;", null, "Object.java", null,
                    Collections.singletonList(new ImmutableField(mObjectType, "launchCalled", "Z",
                            AccessFlags.PRIVATE.getValue() | AccessFlags.STATIC.getValue(),
                            ImmutableBooleanEncodedValue.FALSE_VALUE, null, null)),
                    null, mMethods, null);

        }



        // Inject launch() into application
        ImmutableMethodReference mLaunch = new ImmutableMethodReference(mObjectType, "launch", null, "V");

        boolean mFound = false;

        for (Method mMethod : mClass.getDirectMethods()) {

            if ("<clinit>".equals(mMethod.getName())) {

                mFound = true;

                MutableMethodImplementation mCode = new MutableMethodImplementation(mMethod.getImplementation());

                mCode.addInstruction(0, new BuilderInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, mLaunch));

                mMethod = new ImmutableMethod(mMethod.getDefiningClass(), mMethod.getName(), mMethod.getParameters(), mMethod.getReturnType(), mMethod.getAccessFlags(), mMethod.getAnnotations(), mMethod.getHiddenApiRestrictions(), mCode);


            }

            mMethods.add(mMethod);

        }

        if (!mFound) {

            MethodImplementationBuilder mCode = new MethodImplementationBuilder(0);

            mCode.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, mLaunch));

            mCode.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

            mMethods.add(new ImmutableMethod(mClass.getType(), "<clinit>", null, "V", AccessFlags.STATIC.getValue() | AccessFlags.CONSTRUCTOR.getValue(), null, null, mCode.getMethodImplementation()));

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
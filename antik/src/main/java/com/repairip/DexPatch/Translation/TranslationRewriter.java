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

package com.repairip.DexPatch.Translation;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableField;
import org.jf.dexlib2.immutable.value.ImmutableBooleanEncodedValue;

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
public class TranslationRewriter {
    private final TranslationData data;

    public TranslationRewriter(TranslationData data) {
        this.data = data;
    }

    public ClassDef rewrite(ClassDef cd) {
        String type = cd.getType();

        if (PairipClass.APPLICATION.type.equals(type)) {
            return patchApplication(cd);
        } else if (PairipClass.STARTUP_LAUNCHER.type.equals(type)) {
            return patchStartupLauncher(cd);
        } else if (PairipClass.VM_RUNNER.type.equals(type)) {
            return patchVMRunner(cd);
        } else if (PairipClass.LICENSE_CLIENT_V3.type.equals(type)) {
            return patchLicenseClient(cd);
        }

        return cd;
    }

    private ClassDef patchApplication(ClassDef cd) {
        List<Method> methods = new ArrayList<>();
        for (Method m : cd.getMethods()) {
            if ("<init>".equals(m.getName())) {
                methods.add(TranslationMethodBuilder.createCleanConstructor(m, cd.getSuperclass()));
            }
        }
        methods.add(TranslationMethodBuilder.createClinit());
        return new ImmutableClassDef(cd.getType(), cd.getAccessFlags(), cd.getSuperclass(), cd.getInterfaces(), cd.getSourceFile(), cd.getAnnotations(), cd.getStaticFields(), cd.getInstanceFields(), methods, null);
    }

    private ClassDef patchVMRunner(ClassDef cd) {
        List<Method> methods = new ArrayList<>();
        methods.add(TranslationMethodBuilder.createPrivateConstructor(cd.getType(), cd.getSuperclass()));

        for (Method m : cd.getMethods()) {
            if ("invoke".equals(m.getName())) {
                methods.add(TranslationMethodBuilder.createReturnNull(m));
            }
        }
        return new ImmutableClassDef(cd.getType(), AccessFlags.PUBLIC.getValue(), cd.getSuperclass(), cd.getInterfaces(), cd.getSourceFile(), null, null, null, methods, null);
    }

    private ClassDef patchLicenseClient(ClassDef cd) {
        List<Method> methods = new ArrayList<>();
        for (Method m : cd.getMethods()) {
            if ("<init>".equals(m.getName())) {
                methods.add(TranslationMethodBuilder.createCleanConstructor(m, cd.getSuperclass()));
            } else if ("onActivityCreate".equals(m.getName())) {
                methods.add(TranslationMethodBuilder.createReturnVoid(m));
            }
        }
        return new ImmutableClassDef(cd.getType(), cd.getAccessFlags(), cd.getSuperclass(), cd.getInterfaces(), cd.getSourceFile(), cd.getAnnotations(), cd.getStaticFields(), cd.getInstanceFields(), methods, null);
    }

    private ClassDef patchStartupLauncher(ClassDef cd) {
        List<Method> methods = new ArrayList<>();
        for (Method m : cd.getMethods()) {
            if ("<init>".equals(m.getName())) {
                methods.add(m);
            }
        }
        methods.add(TranslationMethodBuilder.createLaunch(data.hasMethods));
        methods.add(TranslationMethodBuilder.createRestoreString(data));
        if (data.hasMethods) {
            methods.add(TranslationMethodBuilder.createRestoreMethod(data));
        }

        List<ImmutableField> fields = new ArrayList<>();
        fields.add(new ImmutableField(cd.getType(), "launchCalled", "Z", AccessFlags.STATIC.getValue() | AccessFlags.PRIVATE.getValue(), ImmutableBooleanEncodedValue.FALSE_VALUE, null, null));

        return new ImmutableClassDef(cd.getType(), cd.getAccessFlags(), cd.getSuperclass(), cd.getInterfaces(), cd.getSourceFile(), cd.getAnnotations(), fields, cd.getInstanceFields(), methods, null);
    }
}

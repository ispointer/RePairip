package com.antik.DexPatcher.Translation;

import com.antik.DexPatcher.Translation.MethodT.MethodMaker;
import com.antik.DexPatcher.Translation.antik.PairipClass;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableField;
import org.jf.dexlib2.immutable.value.ImmutableBooleanEncodedValue;

import java.util.ArrayList;
import java.util.List;

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
                methods.add(MethodMaker.createCleanConstructor(m, cd.getSuperclass()));
            }
        }
        methods.add(MethodMaker.createClinit());
        return new ImmutableClassDef(cd.getType(), cd.getAccessFlags(), cd.getSuperclass(), cd.getInterfaces(), cd.getSourceFile(), cd.getAnnotations(), cd.getStaticFields(), cd.getInstanceFields(), methods, null);
    }

    private ClassDef patchVMRunner(ClassDef cd) {
        List<Method> methods = new ArrayList<>();
        methods.add(MethodMaker.createPrivateConstructor(cd.getType(), cd.getSuperclass()));
        
        for (Method m : cd.getMethods()) {
            if ("invoke".equals(m.getName())) {
                methods.add(MethodMaker.createReturnNull(m));
            }
        }
        return new ImmutableClassDef(cd.getType(), AccessFlags.PUBLIC.getValue(), cd.getSuperclass(), cd.getInterfaces(), cd.getSourceFile(), null, null, null, methods, null);
    }

    private ClassDef patchLicenseClient(ClassDef cd) {
        List<Method> methods = new ArrayList<>();
        for (Method m : cd.getMethods()) {
            if ("<init>".equals(m.getName())) {
                methods.add(MethodMaker.createCleanConstructor(m, cd.getSuperclass()));
            } else if ("onActivityCreate".equals(m.getName())) {
                methods.add(MethodMaker.createReturnVoid(m));
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
        methods.add(MethodMaker.createLaunch(data.hasMethods));
        methods.add(MethodMaker.createRestoreString(data));
        if (data.hasMethods) {
            methods.add(MethodMaker.createRestoreMethod(data));
        }

        List<ImmutableField> fields = new ArrayList<>();
        fields.add(new ImmutableField(cd.getType(), "launchCalled", "Z", AccessFlags.STATIC.getValue() | AccessFlags.PRIVATE.getValue(), ImmutableBooleanEncodedValue.FALSE_VALUE, null, null));

        return new ImmutableClassDef(cd.getType(), cd.getAccessFlags(), cd.getSuperclass(), cd.getInterfaces(), cd.getSourceFile(), cd.getAnnotations(), fields, cd.getInstanceFields(), methods, null);
    }
}

package com.antik.DexPatcher.Translation.MethodT;

import com.antik.DexPatcher.Translation.TranslationData;
import com.antik.DexPatcher.Translation.antik.PairipClass;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;

import java.util.Collections;
import java.util.Map;

public class MethodMaker {

    public static ImmutableMethod createCleanConstructor(Method method, String superclass) {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(method.getParameters().size() + 1);
        builder.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_DIRECT, 1, 0, 0, 0, 0, 0, new ImmutableMethodReference(superclass, "<init>", null, "V")));
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        return new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(), method.getReturnType(), method.getAccessFlags(), method.getAnnotations(), method.getHiddenApiRestrictions(), builder.getMethodImplementation());
    }

    public static ImmutableMethod createPrivateConstructor(String className, String superclass) {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(1);
        builder.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_DIRECT, 1, 0, 0, 0, 0, 0, new ImmutableMethodReference(superclass, "<init>", null, "V")));
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        return new ImmutableMethod(className, "<init>", null, "V", AccessFlags.PRIVATE.getValue() | AccessFlags.CONSTRUCTOR.getValue(), null, null, builder.getMethodImplementation());
    }

    public static ImmutableMethod createReturnNull(Method method) {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(2);
        builder.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
        builder.addInstruction(new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));
        return new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(), method.getReturnType(), method.getAccessFlags(), method.getAnnotations(), method.getHiddenApiRestrictions(), builder.getMethodImplementation());
    }

    public static ImmutableMethod createReturnVoid(Method method) {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(2);
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        return new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(), method.getReturnType(), method.getAccessFlags(), method.getAnnotations(), method.getHiddenApiRestrictions(), builder.getMethodImplementation());
    }

    public static ImmutableMethod createLaunch(boolean hasMethods) {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(3);
        String startupLauncher = PairipClass.STARTUP_LAUNCHER.type;

        builder.addInstruction(new BuilderInstruction21c(Opcode.CONST_CLASS, 0, new ImmutableTypeReference(startupLauncher)));
        builder.addInstruction(new BuilderInstruction11x(Opcode.MONITOR_ENTER, 0));

        Label tryStart = builder.addLabel("try_start");
        builder.addInstruction(new BuilderInstruction21c(Opcode.SGET_BOOLEAN, 1, new ImmutableFieldReference(startupLauncher, "launchCalled", "Z")));
        Label tryEnd = builder.addLabel("try_end");

        builder.addInstruction(new BuilderInstruction21t(Opcode.IF_EQZ, 1, builder.getLabel("cond_false")));
        builder.addInstruction(new BuilderInstruction11x(Opcode.MONITOR_EXIT, 0));
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        builder.addLabel("cond_false");
        builder.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 1, 1));

        Label tryStart2 = builder.addLabel("try_start_2");
        builder.addInstruction(new BuilderInstruction21c(Opcode.SPUT_BOOLEAN, 1, new ImmutableFieldReference(startupLauncher, "launchCalled", "Z")));
        builder.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, new ImmutableMethodReference(startupLauncher, "restoreString", null, "V")));

        if (hasMethods) {
            builder.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, new ImmutableMethodReference(startupLauncher, "restoreMethod", null, "V")));
        }
        Label tryEnd2 = builder.addLabel("try_end_2");

        builder.addInstruction(new BuilderInstruction11x(Opcode.MONITOR_EXIT, 0));
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        Label catchAll = builder.addLabel("catchall");
        builder.addInstruction(new BuilderInstruction11x(Opcode.MOVE_EXCEPTION, 1));
        Label tryStart3 = builder.addLabel("try_start_3");
        builder.addInstruction(new BuilderInstruction11x(Opcode.MONITOR_EXIT, 0));
        Label tryEnd3 = builder.addLabel("try_end_3");
        builder.addInstruction(new BuilderInstruction11x(Opcode.THROW, 1));

        builder.addCatch(tryStart, tryEnd, catchAll);
        builder.addCatch(tryStart2, tryEnd2, catchAll);
        builder.addCatch(tryStart3, tryEnd3, catchAll);

        return new ImmutableMethod(startupLauncher, "launch", null, "V", AccessFlags.STATIC.getValue() | AccessFlags.PUBLIC.getValue() | AccessFlags.DECLARED_SYNCHRONIZED.getValue(), null, null, builder.getMethodImplementation());
    }

    public static ImmutableMethod createRestoreString(TranslationData data) {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(1);
        String startupLauncher = PairipClass.STARTUP_LAUNCHER.type;

        for (Map.Entry<String, TranslationData.Entry> entry : data.entries.entrySet()) {
            if ("java.lang.String".equals(entry.getValue().type)) {
                String targetClass = TranslationData.normalizeClassName(entry.getKey());
                for (Map.Entry<String, String> field : entry.getValue().fields.entrySet()) {
                    builder.addInstruction(new BuilderInstruction21c(Opcode.CONST_STRING, 0, new ImmutableStringReference(field.getValue())));
                    builder.addInstruction(new BuilderInstruction21c(Opcode.SPUT_OBJECT, 0, new ImmutableFieldReference(targetClass, field.getKey(), "Ljava/lang/String;")));
                }
            }
        }
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        return new ImmutableMethod(startupLauncher, "restoreString", null, "V", AccessFlags.STATIC.getValue() | AccessFlags.PRIVATE.getValue(), null, null, builder.getMethodImplementation());
    }

    public static ImmutableMethod createRestoreMethod(TranslationData data) {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(1);
        String startupLauncher = PairipClass.STARTUP_LAUNCHER.type;

        for (Map.Entry<String, TranslationData.Entry> entry : data.entries.entrySet()) {
            if ("java.lang.reflect.Method".equals(entry.getValue().type)) {
                String targetClass = TranslationData.normalizeClassName(entry.getKey());
                for (Map.Entry<String, String> field : entry.getValue().fields.entrySet()) {
                    builder.addInstruction(new BuilderInstruction21c(Opcode.CONST_STRING, 0, new ImmutableStringReference(field.getValue())));
                    builder.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 0, 0, 0, 0, 0, new ImmutableMethodReference("Lcom/pairip/RestoreMethod;", "get", Collections.singletonList("Ljava/lang/String;"), "Ljava/lang/reflect/Method;")));
                    builder.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
                    builder.addInstruction(new BuilderInstruction21c(Opcode.SPUT_OBJECT, 0, new ImmutableFieldReference(targetClass, field.getKey(), "Ljava/lang/reflect/Method;")));
                }
            }
        }
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        return new ImmutableMethod(startupLauncher, "restoreMethod", null, "V", AccessFlags.STATIC.getValue() | AccessFlags.PRIVATE.getValue(), null, null, builder.getMethodImplementation());
    }

    public static ImmutableMethod createClinit() {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(1);
        builder.addInstruction(new BuilderInstruction21c(Opcode.CONST_STRING, 0, new ImmutableStringReference("Pairip Patcher v1.3.10")));
        builder.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, new ImmutableMethodReference(PairipClass.STARTUP_LAUNCHER.type, "launch", null, "V")));
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        return new ImmutableMethod(PairipClass.APPLICATION.type, "<clinit>", null, "V", AccessFlags.STATIC.getValue() | AccessFlags.CONSTRUCTOR.getValue(), null, null, builder.getMethodImplementation());
    }
}

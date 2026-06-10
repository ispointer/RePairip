package com.antik.DexPatcher.Translation.MethodT;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodParameter;

import java.util.Collections;

public class RestoreMethodMaker {

    public static ImmutableClassDef createRestoreMethodClass() {
        String type = "Lcom/pairip/RestoreMethod;";
        MethodImplementationBuilder builder = new MethodImplementationBuilder(3);
        builder.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 0, 0));
        builder.addInstruction(new BuilderInstruction11x(Opcode.RETURN_OBJECT, 0));

        ImmutableMethod getMethod = new ImmutableMethod(
                type, 
                "get", 
                Collections.singletonList(new ImmutableMethodParameter("Ljava/lang/String;", null, null)), 
                "Ljava/lang/reflect/Method;", 
                AccessFlags.PUBLIC.getValue() | AccessFlags.STATIC.getValue(), 
                null, 
                null, 
                builder.getMethodImplementation());

        return new ImmutableClassDef(
                type, 
                AccessFlags.PUBLIC.getValue(), 
                "Ljava/lang/Object;", 
                null, 
                null, 
                null, 
                null, 
                null, 
                Collections.singletonList(getMethod), 
                null);
    }
}

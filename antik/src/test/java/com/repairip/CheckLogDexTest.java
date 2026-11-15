package com.repairip;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CheckLogDexTest {

    @Test
    public void restoreDexUsesResolvableMethodReferences() throws Exception {
        Path mDex = Files.createTempFile("restore", ".dex");

        try {
            try (InputStream mInput = getClass().getResourceAsStream("/restore.dex")) {
                assertNotNull(mInput);
                Files.copy(mInput, mDex, StandardCopyOption.REPLACE_EXISTING);
            }

            DexFile mFile = DexFileFactory.loadDexFile(mDex.toFile(), Opcodes.getDefault());
            boolean mFoundClass = false;

            for (ClassDef mClass : mFile.getClasses()) {
                if (!"Lcom/aantik/vmrunnerlogger/RePairipRestore;".equals(mClass.getType())) {
                    continue;
                }

                mFoundClass = true;

                for (Method mMethod : mClass.getMethods()) {
                    if (!"get".equals(mMethod.getName()) || mMethod.getImplementation() == null) {
                        continue;
                    }

                    boolean mGetDeclaredMethods = false;
                    boolean mToString = false;

                    for (Instruction mInstruction : mMethod.getImplementation().getInstructions()) {
                        if (!(mInstruction instanceof ReferenceInstruction)) {
                            continue;
                        }

                        Reference mReference = ((ReferenceInstruction) mInstruction).getReference();

                        if (!(mReference instanceof MethodReference)) {
                            continue;
                        }

                        MethodReference mMethodReference = (MethodReference) mReference;

                        if ("Ljava/lang/Class;".equals(mMethodReference.getDefiningClass())
                                && "getDeclaredMethods".equals(mMethodReference.getName())) {
                            mGetDeclaredMethods = true;
                            assertEquals("[Ljava/lang/reflect/Method;", mMethodReference.getReturnType());
                        }

                        if ("Ljava/lang/reflect/Method;".equals(mMethodReference.getDefiningClass())
                                && "toString".equals(mMethodReference.getName())) {
                            mToString = true;
                        }
                    }

                    assertTrue(mGetDeclaredMethods);
                    assertTrue(mToString);
                }
            }

            assertTrue(mFoundClass);
        } finally {
            Files.deleteIfExists(mDex);
        }
    }
}

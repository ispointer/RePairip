package com.repairip;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.junit.Test;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.zip.Adler32;

import static org.junit.Assert.assertArrayEquals;
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

            assertValidDexHeader(mDex);

            DexFile mFile = DexFileFactory.loadDexFile(mDex.toFile(), Opcodes.getDefault());
            ClassDef mClass = null;

            for (ClassDef mCandidate : mFile.getClasses()) {
                if ("Lcom/aantik/vmrunnerlogger/RePairipRestore;".equals(mCandidate.getType())) {
                    mClass = mCandidate;
                    break;
                }
            }

            assertNotNull(mClass);

            Method mConstructor = null;
            Method mGet = null;

            for (Method mMethod : mClass.getMethods()) {
                if ("<init>".equals(mMethod.getName())) {
                    mConstructor = mMethod;
                } else if ("get".equals(mMethod.getName())) {
                    mGet = mMethod;
                }
            }

            assertNotNull(mConstructor);
            assertTrue((mConstructor.getAccessFlags() & 0x10000) != 0);
            assertTrue((mConstructor.getAccessFlags() & 0x2) != 0);
            assertNotNull(mGet);
            assertEquals("Ljava/lang/reflect/Method;", mGet.getReturnType());
            assertEquals("[Ljava/lang/String;]", mGet.getParameterTypes().toString());
            assertTrue(mGet.getImplementation().getDebugItems().iterator().hasNext());

            boolean mTypedCatch = false;

            for (TryBlock<? extends ExceptionHandler> mTryBlock : mGet.getImplementation().getTryBlocks()) {
                for (ExceptionHandler mHandler : mTryBlock.getExceptionHandlers()) {
                    if ("Ljava/lang/Exception;".equals(mHandler.getExceptionType())) {
                        mTypedCatch = true;
                    }
                }
            }

            assertTrue(mTypedCatch);

            boolean mGetDeclaredMethods = false;
            boolean mToString = false;
            boolean mToGenericString = false;
            boolean mForName = false;

            for (Instruction mInstruction : mGet.getImplementation().getInstructions()) {
                if (!(mInstruction instanceof ReferenceInstruction)) {
                    continue;
                }

                Reference mReference = ((ReferenceInstruction) mInstruction).getReference();

                if (!(mReference instanceof MethodReference)) {
                    continue;
                }

                MethodReference mMethodReference = (MethodReference) mReference;

                if ("Ljava/lang/Class;".equals(mMethodReference.getDefiningClass())) {
                    if ("getDeclaredMethods".equals(mMethodReference.getName())) {
                        mGetDeclaredMethods = true;
                        assertEquals("[Ljava/lang/reflect/Method;", mMethodReference.getReturnType());
                    } else if ("forName".equals(mMethodReference.getName())) {
                        mForName = true;
                        assertEquals("Ljava/lang/Class;", mMethodReference.getReturnType());
                        assertEquals(3, mMethodReference.getParameterTypes().size());
                        assertEquals("Ljava/lang/String;",
                                mMethodReference.getParameterTypes().get(0).toString());
                        assertEquals("Z",
                                mMethodReference.getParameterTypes().get(1).toString());
                        assertEquals("Ljava/lang/ClassLoader;",
                                mMethodReference.getParameterTypes().get(2).toString());
                    }
                } else if ("Ljava/lang/reflect/Method;".equals(mMethodReference.getDefiningClass())) {
                    if ("toString".equals(mMethodReference.getName())) {
                        mToString = true;
                    } else if ("toGenericString".equals(mMethodReference.getName())) {
                        mToGenericString = true;
                    }
                }
            }

            assertTrue(mGetDeclaredMethods);
            assertTrue(mToString);
            assertTrue(mToGenericString);
            assertTrue(mForName);

            Path mBuilder = Paths.get(
                    "src/main/java/com/repairip/DexPatch/Translation/TranslationMethodBuilder.java");

            if (!Files.exists(mBuilder)) {
                mBuilder = Paths.get(
                        "antik/src/main/java/com/repairip/DexPatch/Translation/TranslationMethodBuilder.java");
            }

            assertTrue(Files.exists(mBuilder));
            String mBuilderSource = new String(Files.readAllBytes(mBuilder), StandardCharsets.UTF_8);
            assertTrue(mBuilderSource.contains("Lcom/aantik/vmrunnerlogger/RePairipRestore;"));
        } finally {
            Files.deleteIfExists(mDex);
        }
    }

    private static void assertValidDexHeader(Path mDex) throws Exception {
        byte[] mBytes = Files.readAllBytes(mDex);

        assertEquals('d', mBytes[0]);
        assertEquals('e', mBytes[1]);
        assertEquals('x', mBytes[2]);
        assertEquals('\n', mBytes[3]);
        assertEquals(0, mBytes[7]);

        int mChecksum = ByteBuffer.wrap(mBytes, 8, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();
        Adler32 mAdler = new Adler32();
        mAdler.update(mBytes, 12, mBytes.length - 12);
        assertEquals(mChecksum, (int) mAdler.getValue());

        byte[] mExpectedSignature = Arrays.copyOfRange(mBytes, 12, 32);
        byte[] mActualSignature = MessageDigest.getInstance("SHA-1")
                .digest(Arrays.copyOfRange(mBytes, 32, mBytes.length));
        assertArrayEquals(mExpectedSignature, mActualSignature);
    }
}

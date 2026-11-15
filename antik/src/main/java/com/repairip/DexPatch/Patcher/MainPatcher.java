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

import com.repairip.Main;
import com.reandroid.apk.ApkModule;
import com.reandroid.archive.ByteInputSource;
import com.reandroid.archive.InputSource;
import com.repairip.Utils.SpannableBanner;

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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

public class MainPatcher {

    /**
     * patches apk dex && injects logger classes && collects junk classes
     * @param mApk
     * @return
     */
    public static List<String> patch(ApkModule mApk) {

        Map<String, DexBackedDexFile> mDexFiles = new LinkedHashMap<String, DexBackedDexFile>();
        Queue<ClassDef> mLogger = new ArrayDeque<ClassDef>();
        Queue<String> mJunk = new ArrayDeque<String>();
        Set<String> mTypes = new HashSet<String>();

        try {

            // load logger dex from jar resource
            InputStream mInput = Main.class.getResourceAsStream("/log.dex");

            if (mInput == null) {

                //System.err.println("[ERROR] logger not found");
                return new ArrayList<String>(mJunk);

            }

            try {

                DexBackedDexFile mDex = new DexBackedDexFile(Opcodes.getDefault(), mInput.readAllBytes());

                for (ClassDef mClass : mDex.getClasses()) {

                    mLogger.add(mClass);
                    mTypes.add(mClass.getType());

                }

            } finally {

                mInput.close();

            }

            // find junk class from loadded dex >
            for (InputSource mSource : mApk.getInputSources()) {

                String mName = mSource.getName();

                if (!mName.startsWith("classes") || !mName.endsWith(".dex")) {
                    continue;
                }

                try (InputStream mStream = mSource.openStream()) {

                    DexBackedDexFile mDex = new DexBackedDexFile(Opcodes.getDefault(), mStream.readAllBytes());

                    mDexFiles.put(mName, mDex);

                    for (ClassDef mClass : mDex.getClasses()) {

                        if (mClass.getMethods().iterator().hasNext()) {
                            continue;
                        }

                        if (!mClass.getFields().iterator().hasNext()) {
                            continue;
                        }

                        String mObject = "Ljava/lang/Object;";

                        if (!mObject.equals(mClass.getSuperclass())) {
                            continue;
                        }

                        if (mClass.getAccessFlags() != 1) {
                            continue;
                        }

                        boolean mValid = true;

                        for (Field mField : mClass.getFields()) {

                            if (mField.getAccessFlags() != 9) {
                                mValid = false;
                                break;
                            }

                            if (mField.getInitialValue() != null) {
                                mValid = false;
                                break;
                            }

                            String mType = mField.getType();

                            String mString = "Ljava/lang/String;";
                            String mMethod = "Ljava/lang/reflect/Method;";

                            if (!mString.equals(mType) && !mMethod.equals(mType)) {
                                mValid = false;
                                break;
                            }
                        }

                        if (mValid) {
                            mJunk.add(mClass.getType());
                        }
                    }
                }
            }

            System.out.println(SpannableBanner.CYAN + "[INFO] " + SpannableBanner.RESET + "Found " + mJunk.size() + " junk classes");

            // generate object class
            ClassDef mObject = ObjectClassGenerator.createClassDef(mJunk);

            mLogger.add(mObject);
            mTypes.add(mObject.getType());

            // patch module dex class here
            for (Map.Entry<String, DexBackedDexFile> mEntry : mDexFiles.entrySet()) {

                String mName = mEntry.getKey();
                DexPool mPool = new DexPool(Opcodes.getDefault());
                boolean mModified = false;

                for (ClassDef mClass : mEntry.getValue().getClasses()) {

                    if (mTypes.contains(mClass.getType())) {

                        mModified = true;
                        continue;

                    }

                    String mStartupLauncher = "Lcom/pairip/StartupLauncher;";
                    String mApplication = "Lcom/pairip/application/Application;";
                    String mAttachBaseContext = "attachBaseContext";

                    if (mStartupLauncher.equals(mClass.getType())) {

                        mClass = LauncherPatcher.patchStartupLauncher(mClass);
                        mModified = true;

                    } else if (mApplication.equals(mClass.getType())) {

                        Queue<Method> mMethods = new ArrayDeque<Method>();

                        for (Method mMethod : mClass.getVirtualMethods()) {

                            if (mAttachBaseContext.equals(mMethod.getName())) {

                                mMethods.add(SignatureCheckPatcher.patchMethodIfTarget(mMethod));

                            } else {

                                mMethods.add(mMethod);

                            }
                        }

                        mClass = new ImmutableClassDef(
                                mClass.getType(),
                                mClass.getAccessFlags(),
                                mClass.getSuperclass(),
                                mClass.getInterfaces(),
                                mClass.getSourceFile(),
                                mClass.getAnnotations(),
                                mClass.getStaticFields(),
                                mClass.getInstanceFields(),
                                mClass.getDirectMethods(),
                                mMethods
                        );

                        mModified = true;
                    }

                    mPool.internClass(mClass);
                }

                String mInjectDexName = "classes.dex";

                if (mInjectDexName.equals(mName)) {

                    for (ClassDef mClass : mLogger) {

                        mPool.internClass(mClass);

                    }

                    mModified = true;
                }

                if (!mModified) {

                    continue;

                }

                MemoryDataStore mStore = new MemoryDataStore();
                mPool.writeTo(mStore);

                byte[] mData = Arrays.copyOf(mStore.getData(), mStore.getSize());

                mApk.add(new ByteInputSource(mData, mName));

                System.out.println(SpannableBanner.GREEN + "[BUILD] " + SpannableBanner.RESET + "Patched " + mName);
            }

        } catch (Exception opps) {
            // Ooop'ss
            opps.printStackTrace();
        }

        return new ArrayList<String>(mJunk);
    }
}

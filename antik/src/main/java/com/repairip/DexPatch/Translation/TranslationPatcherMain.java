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

import com.repairip.Main;
import com.repairip.ManifestPatch.ManifestPatcher;
import com.reandroid.apk.ApkModule;
import com.reandroid.archive.ByteInputSource;
import com.reandroid.archive.InputSource;
import com.reandroid.json.JSONObject;
import com.repairip.Utils.SpannableBanner;

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

public class TranslationPatcherMain {

    /**
     * patches the apk using the provided translation json file
     * @param mModule
     * @param mJsonFile
     * @throws Exception
     */
    public static void patch(ApkModule mModule, File mJsonFile)
            throws Exception {

        JSONObject mJson = new JSONObject(new String(Files.readAllBytes(mJsonFile.toPath()), StandardCharsets.UTF_8));

        boolean mHasMethods = false;

        for (String mName : mJson.keySet())
        {
            {

                String mType = mJson.getJSONObject(mName).getString("type");

                if (mType.equals("java.lang.reflect.Method") || mType.equals("Ljava/lang/reflect/Method;")) {

                    mHasMethods = true;
                    break;

                }

            }
        }

        // load restore.dex
        List<ClassDef> mRestoreClasses = new ArrayList<ClassDef>();

        if (mHasMethods)
        {
            try (InputStream mInput = Main.class.getResourceAsStream("/restore.dex"))
            {
                DexBackedDexFile mDex = new DexBackedDexFile(Opcodes.getDefault(), mInput.readAllBytes());
                mRestoreClasses.addAll(mDex.getClasses());
            }
        }

        // scan apk
        List<String> mDexNames = new ArrayList<String>();
        List<InputSource> mAssets = new ArrayList<InputSource>();
        Set<String> mExistingTypes = new HashSet<String>();
        String mAppType = null;

        for (InputSource mSource : mModule.getInputSources()) {

            String mName = mSource.getName();

            if (mName.startsWith("assets/"))
            {

                mAssets.add(mSource);

            }

            if (!mName.startsWith("classes") || !mName.endsWith(".dex") || mName.contains("/"))
            {

                continue;

            }

            mDexNames.add(mName);

            try (InputStream mInput = mSource.openStream())
            {
                {

                    DexBackedDexFile mDex = new DexBackedDexFile(Opcodes.getDefault(), mInput.readAllBytes());

                    for (ClassDef mClass : mDex.getClasses()) {

                        mExistingTypes.add(mClass.getType());

                        if (mClass.getType().equals("Lcom/pairip/application/Application;")) {

                            String mParent = mClass.getSuperclass();

                            if (mParent != null && !mParent.equals("Ljava/lang/Object;") && !mParent.equals("Landroid/app/Application;"))
                            {

                                mAppType = mParent;

                            }
                        }
                    }
                }
            }
        }

        // rewrite class references
        final Map<String, String> mTypes = new HashMap<String, String>();
        String mObjectType = "Lcom/aantik/vmrunnerlogger/Object;";
        String mRestoreType = "Lcom/aantik/vmrunnerlogger/RePairipRestore;";

        mTypes.put("Lcom/pairip/VMRunner;", mObjectType);
        mTypes.put("Lantik/VMRunner;", mObjectType);
        mTypes.put("Lcom/pairip/StartupLauncher;", mObjectType);
        mTypes.put("Lantik/store;", mObjectType);
        mTypes.put("Lcom/pairip/RestoreMethod;", mRestoreType);
        mTypes.put("Lantik/RestoreMethod;", mRestoreType);
        mTypes.put("Lcom/pairip/PairipLog;", "Lcom/aantik/vmrunnerlogger/RePairipLog;");

        DexRewriter mRewriter = new DexRewriter(new RewriterModule() {

            @Override
            public Rewriter<String> getTypeRewriter(Rewriters mRewriters) {

                return new Rewriter<String>() {

                    @Override
                    public String rewrite(String mType) {

                        return mTypes.getOrDefault(mType, mType);

                    }
                };
            }
        });

        // generate object
        ClassDef mObject = TranslationObjectGenerator.generate(mJson, null);

        // rebuild apk dex
        for (String mName : mDexNames) {

            InputSource mSource = mModule.getInputSource(mName);
            DexPool mPool = new DexPool(Opcodes.getDefault());

            try (InputStream mInput = mSource.openStream()) {

                DexBackedDexFile mDex = new DexBackedDexFile(Opcodes.getDefault(), mInput.readAllBytes());

                for (ClassDef mClass : mDex.getClasses()) {

                    if (mClass.getType().startsWith("Lcom/pairip/"))
                    {

                        continue;

                    }

                    ClassDef mChanged = mRewriter.getClassDefRewriter().rewrite(mClass);

                    if (mClass.getType().equals(mAppType))
                    {

                        mChanged = TranslationObjectGenerator.generate(mJson, mChanged);

                    }

                    mPool.internClass(mChanged);
                }
            }

            if (mName.equals("classes.dex")) {

                mPool.internClass(mObject);

                for (ClassDef mClass : mRestoreClasses) {

                    mPool.internClass(mClass);

                }
            }

            MemoryDataStore mStore = new MemoryDataStore();
            mPool.writeTo(mStore);

            mModule.add(new ByteInputSource(Arrays.copyOf(mStore.getData(), mStore.getSize()), mName));

            System.out.println(SpannableBanner.GREEN + "[BUILD] " + SpannableBanner.RESET + "Translated " + mName);

        }

        // extract embedded dex from assets
        if (mHasMethods) {

            mExistingTypes.add(mObject.getType());

            for (ClassDef mClass : mRestoreClasses) {
                mExistingTypes.add(mClass.getType());
            }

            for (InputSource mSource : mAssets) {

                List<byte[]> mExtraDexes;

                try (InputStream mInput = mSource.openStream())
                {

                    mExtraDexes = DexExtractor.extract(mInput);

                }

                for (byte[] mBytes : mExtraDexes) {

                    DexBackedDexFile mDex = new DexBackedDexFile(Opcodes.getDefault(), mBytes);

                    Set<String> mClasses = new HashSet<String>();
                    boolean mDuplicate = false;

                    for (ClassDef mClass : mDex.getClasses()) {

                        if (mExistingTypes.contains(mClass.getType())) {
                            mDuplicate = true;
                            break;
                        }

                        mClasses.add(mClass.getType());
                    }

                    if (mDuplicate || mClasses.isEmpty()) {
                        continue;
                    }

                    int mNumber = 2;

                    for (; mDexNames.contains("classes" + mNumber + ".dex"); mNumber++)
                    {

                    }
                    String mName = "classes" + mNumber + ".dex";


                    mModule.add(new ByteInputSource(mBytes, mName));

                    mDexNames.add(mName);
                    mExistingTypes.addAll(mClasses);

                    System.out.println(SpannableBanner.GREEN + "[BUILD] " + SpannableBanner.RESET + "Added " + mName);
                }
            }
        }

        // Remove old libraries
        List<String> mLibraries = new ArrayList<String>();

        for (InputSource mSource : mModule.getInputSources()) {

            if (mSource.getName().contains("libpairipcore.so")) {
                mLibraries.add(mSource.getName());
            }

        }

        for (String mName : mLibraries) {
            mModule.getZipEntryMap().remove(mName);
        }

        // restore application in manifest
        String mApplication = null;

        if (mAppType != null) {
            mApplication = mAppType.substring(1, mAppType.length() - 1).replace('/', '.');
        }

        ManifestPatcher.patch(mModule, mApplication);
    }

}

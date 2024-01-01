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

package com.repairip;

import com.repairip.DexPatch.DexPatcher;
import com.repairip.DexPatch.Translation.TranslationPatcher;
import com.repairip.DexCrc32Patch.DexCrc32Patcher;
import com.repairip.ManifestPatch.ManifestPatcher;
import com.repairip.Utils.ApkExtractor;
import com.repairip.Utils.Loading;
import com.repairip.Utils.LoadingException;
import com.repairip.Utils.SpannableBanner;
import com.reandroid.apk.ApkBundle;
import com.reandroid.apk.ApkModule;
import com.reandroid.archive.WriteProgress;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
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
@SuppressWarnings("ALL")
public class Main {

    public static void main(String[] mArgs) {

        if (mArgs.length < 2) {
            SpannableBanner.help();
            return;
        }

        String mInputPath = null;
        String mTranslatePath = null;
        boolean mRMod = false;

        for (int mIndex = 0; mIndex < mArgs.length; mIndex++)
        {

            if ("-i".equals(mArgs[mIndex]) && mIndex + 1 < mArgs.length)
            {

                mInputPath = mArgs[++mIndex];

            } else if ("-t".equals(mArgs[mIndex]) && mIndex + 1 < mArgs.length)
            {

                mTranslatePath = mArgs[++mIndex];

            } else if ("-r".equals(mArgs[mIndex]))
            {

                mRMod = true;

            }
        }

        if (mInputPath == null) {
            SpannableBanner.help();
            return;
        }

        File mInput = new File(mInputPath);

        if (!mInput.exists()) {
            System.err.println("Input file not found : " + mInputPath);
            return;
        }

        SpannableBanner.banner();

        File mTemp = null;

        try {

            ApkModule mModule;
            File mMerged;
            File mParent = mInput.getAbsoluteFile().getParentFile();

            WriteProgress mProgress = new WriteProgress() {
                @Override
                public void onCompressFile(String mPath, int mMethod, long mLength) {

                }
            };

            if (mInputPath.endsWith(".apk")) {

                System.out.println("[INFO] Loading APK ");

                mModule = ApkModule.loadApkFile(mInput);
                mMerged = mInput;

            } else {

                mTemp = Files.createTempDirectory("antik_merge").toFile();

                System.out.println("[MERGE] Extracting APKS ");

                ApkExtractor.mExtract(mInput, mTemp);

                System.out.println("[MERGE] Merging APK ");

                ApkBundle mBundle = new ApkBundle();
                mBundle.loadApkDirectory(mTemp);
                mModule = mBundle.mergeModules();

                System.out.println("[INFO] Patching AndroidManifest.xml");

                ManifestPatcher.patch(mModule);

                String mName = mInput.getName();
                int mDot = mName.lastIndexOf('.');

                mName = (mDot > 0 ? mName.substring(0, mDot) : mName)
                        + "_merged.apk";

                mMerged = new File(mParent, mName);

                System.out.println("[BUILD] Writing merged APK ");

                try {
                    Loading.progress(mModule, mMerged);
                } catch (LoadingException mError) {
                    System.err.println(mError.getMessage());
                    mError.printStackTrace();
                }

                System.out.println("[MERGE] APK merged successfully: "
                        + mMerged.getAbsolutePath());
            }

            if (mTranslatePath != null) {

                File mJson = new File(mTranslatePath);

                if (mJson.exists()) {

                    System.out.println("[INFO] Starting Translation Patch ");

                    TranslationPatcher.patch(mModule, mJson);

                    String mName = mInput.getName();
                    int mDot = mName.lastIndexOf('.');
                    mName = (mDot > 0 ? mName.substring(0, mDot) : mName);

                    if (mName.endsWith("_merged")) {
                        mName = mName.substring(0, mName.length() - "_merged".length());
                    } else if (mName.endsWith("_patched")) {
                        mName = mName.substring(0, mName.length() - "_patched".length());
                    }

                    mName = mName + "_translated.apk";

                    File mTranslated = new File(mParent, mName);

                    System.out.println("[BUILD] Building Translated APK ");

                    mModule.writeApk(mTranslated, mProgress);

                    System.out.println("[BUILD] Translated APK built at : "
                            + mTranslated.getAbsolutePath());

                } else {

                    System.err.println("[ERROR] Translation file not found : "
                            + mTranslatePath);
                }

            } else if (!mInputPath.endsWith(".apk")) {

                System.out.println("[INFO] Patching classes.dex for logging ");

                List<String> mJunk = new ArrayList<String>();

                try {
                    mJunk = DexPatcher.patch(mModule);
                } catch (Exception mError) {
                    System.err.println("[ERROR] Patching failed : "
                            + mError.getMessage());
                }

                String mName = mInput.getName();
                int mDot = mName.lastIndexOf('.');
                mName = (mDot > 0 ? mName.substring(0, mDot) : mName);

                if (mName.endsWith("_merged")) {
                    mName = mName.substring(0, mName.length() - "_merged".length());
                }

                mName = mName + "_patched.apk";

                File mPairip = new File(mParent, mName);

                System.out.println("[BUILD] Building Logging APK ");

                mModule.writeApk(mPairip, mProgress);

                DexCrc32Patcher.patch(mMerged, mPairip);

                System.out.println("[BUILD] Logging APK built at : "
                        + mPairip.getAbsolutePath());

                File mJunkFile = new File(mParent, "junk_classes.txt");

                try {

                    StringBuilder mText = new StringBuilder();

                    for (String mClass : mJunk) {
                        if (mText.length() > 0) {
                            mText.append('\n');
                        }
                        mText.append(mClass);
                    }

                    Files.write(mJunkFile.toPath(),
                            mText.toString().getBytes(StandardCharsets.UTF_8));

                    System.out.println("[BUILD] Junk class list written : "
                            + mJunkFile.getAbsolutePath());

                    System.out.println(
                            "[INFO] Use frida/capture_pairip.py to capture pairip.json without CorePatch");

                } catch (Exception mError) {

                    System.err.println("[ERROR] Writing junk class list failed : "
                            + mError.getMessage());
                }
            }

            System.out.println("[BUILD] Process completed");

        } catch (Exception mError) {

            System.err.println("[ERROR] Process failed : "
                    + mError.getMessage());

            mError.printStackTrace();

        } finally {

            if (mTemp != null) {

                File[] mFiles = mTemp.listFiles();

                if (mFiles != null) {
                    for (File mFile : mFiles) {
                        if (mFile.isDirectory()) {

                            /**
                             * @Nested directories require recursive cleanup
                             */
                            continue;
                        }
                        mFile.delete();
                    }
                }

                mTemp.delete();
            }
        }
    }
}
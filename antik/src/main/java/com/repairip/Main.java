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
import com.repairip.Utils.Update;

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

        boolean helpRequested = false;
        boolean versionRequested = false;
        String mInputPath = null;
        String mOutputPath = null;
        String mTranslatePath = null;
        boolean mRMod = false;

        List<String> errors = new ArrayList<>();

        for (int mIndex = 0; mIndex < mArgs.length; mIndex++) {
            String arg = mArgs[mIndex];

            if ("-h".equals(arg) || "--help".equals(arg)) {
                helpRequested = true;
            } else if ("-v".equals(arg) || "--version".equals(arg)) {
                versionRequested = true;
            } else if ("-i".equals(arg) || "--input".equals(arg)) {
                if (mIndex + 1 < mArgs.length && !mArgs[mIndex + 1].startsWith("-")) {
                    mInputPath = mArgs[++mIndex];
                } else {
                    errors.add("option " + SpannableBanner.CYAN + arg + SpannableBanner.RESET + " requires a value");
                }
            } else if (arg.startsWith("-i=") || arg.startsWith("--input=")) {
                mInputPath = arg.substring(arg.indexOf('=') + 1);
            } else if ("-o".equals(arg) || "--output".equals(arg)) {
                if (mIndex + 1 < mArgs.length && !mArgs[mIndex + 1].startsWith("-")) {
                    mOutputPath = mArgs[++mIndex];
                } else {
                    errors.add("option " + SpannableBanner.CYAN + arg + SpannableBanner.RESET + " requires a value");
                }
            } else if (arg.startsWith("-o=") || arg.startsWith("--output=")) {
                mOutputPath = arg.substring(arg.indexOf('=') + 1);
            } else if ("-t".equals(arg) || "--translate".equals(arg)) {
                if (mIndex + 1 < mArgs.length && !mArgs[mIndex + 1].startsWith("-")) {
                    mTranslatePath = mArgs[++mIndex];
                } else {
                    errors.add("option " + SpannableBanner.CYAN + arg + SpannableBanner.RESET + " requires a value");
                }
            } else if (arg.startsWith("-t=") || arg.startsWith("--translate=")) {
                mTranslatePath = arg.substring(arg.indexOf('=') + 1);
            } else if ("-r".equals(arg) || "--lazymod".equals(arg)) {
                mRMod = true;
            } else if ("-help".equals(arg)) {
                errors.add("no such option " + SpannableBanner.CYAN + "-e" + SpannableBanner.RESET + ". Did you mean " + SpannableBanner.CYAN + "--help" + SpannableBanner.RESET + "?");
            } else if (arg.startsWith("-")) {
                errors.add("no such option " + SpannableBanner.CYAN + arg + SpannableBanner.RESET + ". Did you mean " + SpannableBanner.CYAN + "--help" + SpannableBanner.RESET + "?");
            }
        }

        if (helpRequested) {
            SpannableBanner.help();
            return;
        }

        if (versionRequested) {
            System.out.println("RePairip Version : " + Update.CURRENT_VERSION);
            return;
        }

        if (mArgs.length == 0 || (mInputPath == null && errors.isEmpty())) {
            SpannableBanner.usage();
            System.out.println();
            SpannableBanner.printError("missing option " + SpannableBanner.CYAN + "--input" + SpannableBanner.RESET);
            return;
        }

        if (!errors.isEmpty()) {
            SpannableBanner.usage();
            System.out.println();
            for (String err : errors) {
                SpannableBanner.printError(err);
            }
            if (mInputPath == null) {
                SpannableBanner.printError("missing option " + SpannableBanner.CYAN + "--input" + SpannableBanner.RESET);
            }
            return;
        }

        File mInput = new File(mInputPath);

        if (!mInput.exists()) {
            SpannableBanner.printError("Input file not found : " + mInputPath);
            return;
        }

        SpannableBanner.banner();

        File mTemp = null;

        try {

            ApkModule mModule;
            File mMerged;
            File mParent = (mOutputPath != null) ? new File(mOutputPath) : mInput.getAbsoluteFile().getParentFile();

            WriteProgress mProgress = new WriteProgress() {
                @Override
                public void onCompressFile(String mPath, int mMethod, long mLength) {

                }
            };

            if (mInputPath.endsWith(".apk")) {

                System.out.println(SpannableBanner.CYAN + "[INFO] " + SpannableBanner.RESET + "Loading APK ");

                mModule = ApkModule.loadApkFile(mInput);
                mMerged = mInput;

            } else {

                mTemp = Files.createTempDirectory("antik_merge").toFile();

                System.out.println(SpannableBanner.YELLOW + "[MERGE] " + SpannableBanner.RESET + "Extracting APKS ");

                ApkExtractor.mExtract(mInput, mTemp);

                System.out.println(SpannableBanner.YELLOW + "[MERGE] " + SpannableBanner.RESET + "Merging APK ");

                ApkBundle mBundle = new ApkBundle();
                mBundle.loadApkDirectory(mTemp);
                mModule = mBundle.mergeModules();

                System.out.println(SpannableBanner.CYAN + "[INFO] " + SpannableBanner.RESET + "Patching AndroidManifest.xml");

                ManifestPatcher.patch(mModule);

                String mName = mInput.getName();
                int mDot = mName.lastIndexOf('.');

                mName = (mDot > 0 ? mName.substring(0, mDot) : mName)
                        + "_merged.apk";

                mMerged = new File(mParent, mName);

                System.out.println(SpannableBanner.GREEN + "[BUILD] " + SpannableBanner.RESET + "Writing merged APK ");

                try {
                    Loading.progress(mModule, mMerged);
                } catch (LoadingException mError) {
                    System.err.println(mError.getMessage());
                    mError.printStackTrace();
                }

                System.out.println(SpannableBanner.YELLOW + "[MERGE] " + SpannableBanner.RESET + "APK merged successfully: "
                        + mMerged.getAbsolutePath());
            }

            if (mTranslatePath != null) {

                File mJson = new File(mTranslatePath);

                if (mJson.exists()) {

                    System.out.println(SpannableBanner.CYAN + "[INFO] " + SpannableBanner.RESET + "Starting Translation Patch ");

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

                    System.out.println(SpannableBanner.GREEN + "[BUILD] " + SpannableBanner.RESET + "Building Translated APK ");

                    mModule.writeApk(mTranslated, mProgress);

                    System.out.println(SpannableBanner.GREEN + "[BUILD] " + SpannableBanner.RESET + "Translated APK built at : "
                            + mTranslated.getAbsolutePath());

                } else {

                    SpannableBanner.printError("Translation file not found : "
                            + mTranslatePath);
                }

            } else if (!mInputPath.endsWith(".apk")) {

                System.out.println(SpannableBanner.CYAN + "[INFO] " + SpannableBanner.RESET + "Patching classes.dex for logging ");

                List<String> mJunk = new ArrayList<String>();

                try {
                    mJunk = DexPatcher.patch(mModule);
                } catch (Exception mError) {
                    SpannableBanner.printError("Patching failed : "
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

                System.out.println(SpannableBanner.GREEN + "[BUILD] " + SpannableBanner.RESET + "Building Logging APK ");

                mModule.writeApk(mPairip, mProgress);

                DexCrc32Patcher.patch(mMerged, mPairip);

                System.out.println(SpannableBanner.GREEN + "[BUILD] " + SpannableBanner.RESET + "Logging APK built at : "
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

                    System.out.println(SpannableBanner.GREEN + "[BUILD] " + SpannableBanner.RESET + "Junk class list written : "
                            + mJunkFile.getAbsolutePath());

                    System.out.println(SpannableBanner.CYAN + "[INFO] " + SpannableBanner.RESET +
                            "Use frida/capture_pairip.py to capture pairip.json without CorePatch");

                } catch (Exception mError) {

                    SpannableBanner.printError("Writing junk class list failed : "
                            + mError.getMessage());
                }
            }

            System.out.println(SpannableBanner.GREEN + "[BUILD] " + SpannableBanner.RESET + "Process completed");

        } catch (Exception mError) {

            SpannableBanner.printError("Process failed : "
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
            Update.mUpdate();
        }
    }
}
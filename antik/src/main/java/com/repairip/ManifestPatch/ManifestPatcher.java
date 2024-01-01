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

package com.repairip.ManifestPatch;

import com.reandroid.apk.ApkModule;
import com.reandroid.app.AndroidManifest;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import java.util.ArrayList;
import java.util.Iterator;

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

public final class ManifestPatcher {

    /**
     * patch AndroidManifest.xml
     * @param mModule
     */
    public static void patch(ApkModule mModule) {

        String[][] mTargets = {
                {
                    "id", String.valueOf(AndroidManifest.ID_requiredSplitTypes)
                },
                {
                    "id", String.valueOf(AndroidManifest.ID_splitTypes)
                },
                {
                    "id", String.valueOf(AndroidManifest.ID_isSplitRequired)
                },
                {
                    "id", String.valueOf(AndroidManifest.ID_isFeatureSplit)
                },

                {
                    "attribute", "requiredSplitTypes"
                },
                {
                    "attribute", "splitTypes"
                },
                {
                    "attribute", "isSplitRequired"
                },
                {
                    "attribute", "isFeatureSplit"
                },
                {
                    "attribute", "split"
                },

                {
                    "meta-data", "com.android.stamp.source"
                },
                {
                    "meta-data", "com.android.stamp.type"
                },
                {
                    "meta-data", "com.android.vending.splits"
                },
                {
                    "meta-data", "com.android.vending.derived.apk.id"
                },
                {
                    "meta-data", "com.android.dynamic.apk.fused.modules"
                },
                {
                    "meta-data", "com.android.vending.splits.required"
                },
                {
                    "activity", "com.pairip.licensecheck.LicenseActivity"
                },
                {
                    "provider", "com.pairip.licensecheck.LicenseContentProvider"
                },
                {
                    "uses-permission", "com.android.vending.CHECK_LICENSE"
                }
        };

        AndroidManifestBlock mManifest = mModule.getAndroidManifest();

        if (mManifest == null) {
            return;
        }

        Boolean mExtractNativeLibs = mManifest.isExtractNativeLibs();

        if (Boolean.FALSE.equals(mExtractNativeLibs)) {
            mManifest.setExtractNativeLibs(true);
        }

        ResXmlElement mElement = mManifest.getManifestElement();

        if (mElement == null) {
            return;
        }

        ArrayList<ResXmlElement> mRemoved = new ArrayList<>();

        for (String[] mTarget : mTargets) {

            String mTag = mTarget[0];
            String mName = mTarget[1];

            if ("id".equals(mTag)) {
                mElement.removeAttributesWithId(Integer.parseInt(mName));
                continue;
            }

            if ("attribute".equals(mTag)) {
                mElement.removeAttributesWithName(mName);
                continue;
            }

            Iterator<ResXmlElement> mIterator;

            if ("uses-permission".equals(mTag)) {
                mIterator = mElement.getElements(mTag);
            } else {
                mIterator = mManifest.getApplicationElementsByTag(mTag);
            }

            for ( ; mIterator.hasNext() ; ) {

                ResXmlElement mChild = mIterator.next();

                if (mName.equals(
                        AndroidManifestBlock.getAndroidNameValue(mChild))) {
                    mRemoved.add(mChild);
                }
            }
        }

        for (ResXmlElement mChild : mRemoved) {
            mChild.removeSelf();
        }

        mModule.setManifest(mManifest);
    }
}

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

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public class DexExtractor {

    /**
     *  extracts embedded dex files from an input stream
     *
     * @param mInput
     * @return
     * @throws Exception
     */
    public static List<byte[]> extract(InputStream mInput) throws Exception {

        List<byte[]> mResult = new ArrayList<byte[]>();
        byte[] mBytes = mInput.readAllBytes();

        byte[] mHeader = {0, 'I', 'A', 'P', 2};
        byte[] mMagic = {'d', 'e', 'x', '\n'};

        // extract zip dex
       /* try (ZipInputStream mZip = new ZipInputStream(new ByteArrayInputStream(mBytes)))
        {

            ZipEntry mEntry;

            for ( ; (mEntry = mZip.getNextEntry()) != null; ) {

                if (mEntry.isDirectory() || !mEntry.getName().endsWith(".dex")) {
                    continue;
                }

                ByteArrayOutputStream mOutput = new ByteArrayOutputStream();
                mZip.transferTo(mOutput);

                byte[] mDex = mOutput.toByteArray();

                if (new DexBackedDexFile(Opcodes.getDefault(), mDex).getClasses().iterator().hasNext())
                {

                    mResult.add(mDex);

                }

                mZip.closeEntry();
            }
        }

        if (!mResult.isEmpty()) {
            return mResult;
        }*/

        // extract embedded dex from assets
        if (mBytes.length < 117 || !Arrays.equals(mBytes, 0, 5, mHeader, 0, 5))
        {

            return mResult;

        }

        ByteBuffer mBuffer = ByteBuffer.wrap(mBytes).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 5; i <= mBytes.length - 112; i++) {

            if (!Arrays.equals(mBytes, i, i + 4, mMagic, 0, 4))
            {

                continue;

            }

            int mSize = mBuffer.getInt(i + 32);

            if (mSize < 112 || mSize > mBytes.length - i)
            {

                continue;

            }

            byte[] mDex = Arrays.copyOfRange(mBytes, i, i + mSize);

            try {

                if (new DexBackedDexFile(Opcodes.getDefault(), mDex) .getClasses().iterator().hasNext())
                {

                    mResult.add(mDex);

                }

            } catch (Exception mError) {
                // Skip fucking dex
            }
        }

        return mResult;
    }
}
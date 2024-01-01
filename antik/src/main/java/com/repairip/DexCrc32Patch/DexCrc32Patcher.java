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

package com.repairip.DexCrc32Patch;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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


public final class DexCrc32Patcher {

    /**
     * patches the zip crc32 values of dex files in the target apk
     * using the CRC32 values from the source apk
     *
     * @param mSourceApk the original source APK
     * @param mTargetApk the target APK to patch
     * @throws IOException if an I/O error occurs
     * @throws CrcException if the ZIP structure is invalid
     */
    public static void patch(File mSourceApk, File mTargetApk) throws IOException, CrcException {

        Map<String, Long> mSourceCrc = new HashMap<String, Long>();

        try (ZipFile mZip = new ZipFile(mSourceApk)) {

            Enumeration<? extends ZipEntry> mEntries = mZip.entries();

            while (mEntries.hasMoreElements()) {

                ZipEntry mEntry = mEntries.nextElement();
                String mName = mEntry.getName();

                if (mName.startsWith("classes") && mName.endsWith(".dex")) {

                    mSourceCrc.put(mName, mEntry.getCrc());

                 //////   System.err.println("error found");
                }
            }
        }


        if (mSourceCrc.isEmpty()) {
            throw new CrcException("No source DEX files found");
        }

        try (RandomAccessFile mArchive = new RandomAccessFile(mTargetApk, "rw")) {

            long mLength = mArchive.length();
            long mSearch = mLength - ZipFormat.END_HEADER_SIZE;

            long mMinimum = Math.max(0L, mLength - ZipFormat.END_HEADER_SIZE - ZipFormat.MAX_COMMENT_SIZE);

            long mDirectory = -1L;

            for (; mSearch >= mMinimum; mSearch--) {

                mArchive.seek(mSearch);

                int mSignature = Integer.reverseBytes(mArchive.readInt());

                if (mSignature == ZipFormat.END_OF_DIRECTORY) {
                    mDirectory = mSearch;
                    break;
                }
            }

            if (mDirectory < 0L) {
                throw new CrcException("ZIP directory not found");
            }

            mArchive.seek(mDirectory + 12);

            long mDirectorySize = Integer.reverseBytes(mArchive.readInt()) & ZipFormat.UINT_MASK;

            long mDirectoryOffset = Integer.reverseBytes(mArchive.readInt()) & ZipFormat.UINT_MASK;

            long mLimit = mDirectoryOffset + mDirectorySize;

            if (mLimit < mDirectoryOffset || mLimit > mLength) {
                throw new CrcException("Invalid ZIP directory");
            }

            for (long mPosition = mDirectoryOffset; mPosition < mLimit; ) {

                if (mLimit - mPosition < ZipFormat.CENTRAL_HEADER_SIZE) {
                    throw new CrcException("Incomplete ZIP header");
                }

                mArchive.seek(mPosition);

                int mSignature = Integer.reverseBytes(mArchive.readInt());

                if (mSignature != ZipFormat.CENTRAL_DIRECTORY) {
                    throw new CrcException("Invalid ZIP header");
                }

                mArchive.seek(mPosition + ZipFormat.CRC_OFFSET);

                long mCurrentCrc = Integer.reverseBytes(mArchive.readInt()) & ZipFormat.UINT_MASK;

                mArchive.seek(mPosition + ZipFormat.NAME_OFFSET);

                int mNameLength = Short.reverseBytes(mArchive.readShort()) & ZipFormat.USHORT_MASK;

                int mExtraLength = Short.reverseBytes(mArchive.readShort()) & ZipFormat.USHORT_MASK;

                int mCommentLength = Short.reverseBytes(mArchive.readShort()) & ZipFormat.USHORT_MASK;

                mArchive.seek(mPosition + ZipFormat.LOCAL_OFFSET);

                long mLocalOffset = Integer.reverseBytes(mArchive.readInt()) & ZipFormat.UINT_MASK;

                long mNextPosition = mPosition + ZipFormat.CENTRAL_HEADER_SIZE + mNameLength + mExtraLength + mCommentLength;

                if (mNextPosition > mLimit) {
                    throw new CrcException("Invalid ZIP entry size");
                }

                byte[] mNameBytes = new byte[mNameLength];

                mArchive.seek(mPosition + ZipFormat.CENTRAL_HEADER_SIZE);

                mArchive.readFully(mNameBytes);

                String mName = new String(mNameBytes, StandardCharsets.UTF_8);

                Long mOriginalCrc = mSourceCrc.get(mName);

                if (mOriginalCrc != null && mOriginalCrc.longValue() != mCurrentCrc) {

                    if (mLocalOffset > mLength - ZipFormat.LOCAL_HEADER_SIZE) {
                        throw new CrcException("Invalid local header");
                    }

                    mArchive.seek(mLocalOffset);

                    int mLocalSignature = Integer.reverseBytes(mArchive.readInt());

                    if (mLocalSignature != ZipFormat.LOCAL_FILE_HEADER) {
                        throw new CrcException("Invalid local signature");
                    }

                    int mCrc = Integer.reverseBytes(mOriginalCrc.intValue());

                    mArchive.seek(mPosition + ZipFormat.CRC_OFFSET);
                    mArchive.writeInt(mCrc);

                    mArchive.seek(mLocalOffset + ZipFormat.LOCAL_CRC_OFFSET);

                    mArchive.writeInt(mCrc);

                    System.out.println("[CRC32] Patched: " + mName);

                }

                mPosition = mNextPosition;
            }
        }
    }
}
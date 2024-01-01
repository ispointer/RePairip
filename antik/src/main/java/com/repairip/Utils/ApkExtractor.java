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

package com.repairip.Utils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
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

public class ApkExtractor {

    /**
     * extracts apk files from an apks archive
     * @param mInput
     * @param mDir
     * @throws Exception
     */
    public static void mExtract(File mInput, File mDir) throws Exception
    {

        try (ZipFile mZip = new ZipFile(mInput))
        {

            Enumeration<? extends ZipEntry> mEntries = mZip.entries();

            while (mEntries.hasMoreElements())
            {

                ZipEntry mEntry = mEntries.nextElement();

                if (!mEntry.isDirectory() && mEntry.getName().endsWith(".apk"))
                {

                    File mFile = new File(mDir, new File(mEntry.getName()).getName());

                    try (InputStream mStream = mZip.getInputStream(mEntry))
                    {
                        Files.copy(mStream, mFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }
    ApkExtractor()
    {
        {
            {
                //TODO: NULL CONSTRUCTOR :(
            }
        }
    }
}

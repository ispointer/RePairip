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

final class ZipFormat {

    static final int LOCAL_FILE_HEADER = 0x04034b50;
    static final int CENTRAL_DIRECTORY = 0x02014b50;
    static final int END_OF_DIRECTORY = 0x06054b50;

    static final int LOCAL_HEADER_SIZE = 30;
    static final int CENTRAL_HEADER_SIZE = 46;
    static final int END_HEADER_SIZE = 22;

    static final int MAX_COMMENT_SIZE = 65535;

    static final int CRC_OFFSET = 16;
    static final int LOCAL_CRC_OFFSET = 14;
    static final int NAME_OFFSET = 28;
    static final int LOCAL_OFFSET = 42;

    static final long UINT_MASK = 0xffffffffL;
    static final int USHORT_MASK = 0xffff;

    /**
     * @Private constructor to prevent instantiation.
     */
    private ZipFormat() {

    }
}


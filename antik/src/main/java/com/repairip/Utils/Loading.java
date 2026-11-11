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

import com.reandroid.apk.ApkModule;
import java.io.File;

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

public class Loading {

    /**
     * Buffer Grow smooth progress bar
     * @BufferGrow
     * @param mModule the APK module to write
     * @param mOut the output APK file
     * @throws LoadingException if APK writing fails
     */
    public static void progress(final ApkModule mModule, final File mOut)
            throws LoadingException {



        final Throwable[] mError = new Throwable[1];

        Thread mWriter = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mModule.writeApk(mOut);
                } catch (Exception e) {
                    mError[0] = e;
                }
            }
        });

        mWriter.start();

        double mBuffer = 0;

        try {
            for ( ; mWriter.isAlive() || mBuffer < 100; ) {

                double mTarget = mWriter.isAlive() ? 95 : 100;

                if (mBuffer < mTarget) {
                    mBuffer += Math.max(0.2, (mTarget - mBuffer) / 12);
                    mBuffer = Math.min(mBuffer, mTarget);
                }

                int mPercent = (int) mBuffer;

                StringBuilder mBar = new StringBuilder("\r" + SpannableBanner.CYAN + "[" + SpannableBanner.RESET);

                int filledCount = mPercent * 36 / 100;

                mBar.append(SpannableBanner.GREEN);
                int i = 0;
                for (; i < filledCount; i++) {
                    mBar.append("#");
                }
                mBar.append(SpannableBanner.RESET);

                for (; i < 36; i++) {
                    mBar.append("-");
                }

                mBar.append(SpannableBanner.CYAN + "] " + SpannableBanner.RESET)
                    .append(SpannableBanner.YELLOW).append(mPercent).append("%" + SpannableBanner.RESET).append("   ");

                System.out.print(mBar.toString());
                System.out.flush();

                Thread.sleep(50);
            }

            mWriter.join();

        } catch (InterruptedException e) {

            mWriter.interrupt();

            Thread.currentThread().interrupt();
            throw new LoadingException("Oops's Progress interrupted", e);
        }

        if (mError[0] != null) {
            throw new LoadingException("Oops's APK writing failed", mError[0]);
        }

        System.out.println();
    }
}

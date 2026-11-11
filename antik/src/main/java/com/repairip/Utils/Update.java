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

package com.repairip.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/*
 * Created by aantik
 * 11/12/2026
 *
 *   ⋆   ႔ ႔
 *     ᠸ^ ^ ⸝⸝
 *      |、˜〵
 *      じしˍ,)⁐̤ᐷ
 *
 * Fox Mode 🍺
 */

public class Update {
    public static final String CURRENT_VERSION = "1.8.30";

    /**
     * @apiNote This method fetches the latest release information from the
     * official RePairip GitHub repository and compares it with the current version.
     * If a newer or different version is available it displays an update
     * notification release link and changelog
     *
     * This method only notifies users about available updates and does not
     * modify or update the tool automatically An internet connection is
     * required to check for updates
     */
    public static void mUpdate() {

        HttpURLConnection mConnection = null;

        try {

            URL mUrl = new URL("https://github.com/ispointer/RePairip/releases/latest");

            mConnection = (HttpURLConnection) mUrl.openConnection();
            mConnection.setInstanceFollowRedirects(false);


            String mLink = mConnection.getHeaderField("Location");

            if (mLink != null && mLink.contains("/tag/")) {

                String mVersion = mLink.substring(mLink.lastIndexOf("/") + 1);

                if (mVersion.startsWith("v")) {
                    mVersion = mVersion.substring(1);
                }

                if (!mVersion.equals(CURRENT_VERSION)) {

                    mConnection.disconnect();
                    mConnection = (HttpURLConnection) new URL(mLink).openConnection();
                    BufferedReader mReader = new BufferedReader(new InputStreamReader(mConnection.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder mHtml = new StringBuilder();

                    String mLine;

                    for ( ; (mLine = mReader.readLine()) != null; )
                    {
                        mHtml.append(mLine);
                    }

                    mReader.close();

                    String mText = mHtml.toString();
                    String mBody = "";

                    int mStart = mText.indexOf("<p>", mText.indexOf("markdown-body"));
                    int mEnd = mText.indexOf("</p>", mStart);

                    if (mStart != -1 && mEnd > mStart) {
                        mBody = mText.substring(mStart + 3, mEnd).trim();
                    }

                    System.out.println();
                    System.out.println(SpannableBanner.YELLOW + "[UPDATE] New version: " + mVersion + SpannableBanner.RESET);
                    System.out.println("Link       : " + SpannableBanner.CYAN + mLink + SpannableBanner.RESET);

                    if (!mBody.isEmpty()) {
                        System.out.println("What's New : " + SpannableBanner.GREEN + mBody + SpannableBanner.RESET);
                    }
                }
            }

        } catch (Exception ignored) {
        } finally {
            if (mConnection != null) {
                mConnection.disconnect();
            }
        }
    }
}

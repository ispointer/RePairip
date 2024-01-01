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

package com.repairip.DexPatch.Translation;

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


/**
 * Enumeration of key Pairip target framework classes and their DEX descriptors.
 */
public enum PairipClass {
    VM_RUNNER("Lcom/pairip/VMRunner;"),
    APPLICATION("Lcom/pairip/application/Application;"),
    STARTUP_LAUNCHER("Lcom/pairip/StartupLauncher;"),
    LICENSE_CLIENT_V3("Lcom/pairip/licensecheck3/LicenseClientV3;");

    public final String type;

    PairipClass(String type) {
        this.type = type;
    }

    /**
     * Checks if the given class descriptor matches any target Pairip class.
     *
     * @param type class descriptor (e.g. {@code Lcom/pairip/StartupLauncher;})
     * @return {@code true} if matching, {@code false} otherwise
     */
    public static boolean contains(String type) {
        for (PairipClass pairipClass : values()) {
            if (pairipClass.type.equals(type)) {
                return true;
            }
        }
        return false;
    }
}

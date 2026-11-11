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

public class SpannableBanner {
    private static final char ESC = (char) 27;

    public static final String RESET   = ESC + "[0m";
    public static final String BOLD    = ESC + "[1m";
    public static final String RED     = ESC + "[31m";
    public static final String GREEN   = ESC + "[32m";
    public static final String YELLOW  = ESC + "[33m";
    public static final String CYAN    = ESC + "[36m";

    public static void banner() {
        System.out.println("░░░░░█▀▄░█▀▀░█░█░█▀▄░█▀▀░█░█░░░░░");
        System.out.println("░░░░░█▀▄░█▀▀░▀▄▀░█░█░█▀▀░▄▀▄░░░░░");
        System.out.println("░░░░░▀░▀░▀▀▀░░▀░░▀▀░░▀▀▀░▀░▀░░░░░");
        System.out.println("░█▀▄░█▀▀░█▀█░█▀█░▀█▀░█▀▄░▀█▀░█▀█░");
        System.out.println("░█▀▄░█▀▀░█▀▀░█▀█░░█░░█▀▄░░█░░█▀▀░");
        System.out.println("░▀░▀░▀▀▀░▀░░░▀░▀░▀▀▀░▀░▀░▀▀▀░▀░░░");
        System.out.println(YELLOW + "Version : " + Update.CURRENT_VERSION + RESET);
        System.out.println(CYAN + "--------------------------------------" + RESET);
        System.out.println("Dev     : " + GREEN + "Antik" + RESET);
        System.out.println("Channel : " + YELLOW + "https://t.me/RevDex" + RESET);
        System.out.println("GitHub  : " + YELLOW + "https://github.com/ispointer/RePairip" + RESET);
        System.out.println(CYAN + "--------------------------------------" + RESET);
    }

    public static void usage() {
        System.out.println("Usage: Pairip.jar [" + CYAN + "<options>" + RESET + "]");
    }

    public static void help() {
        usage();
        System.out.println();
        System.out.println(YELLOW + "Options:" + RESET);
        System.out.println("  " + CYAN + "--version               " + RESET + "Show the version and exit");
        System.out.println("  " + CYAN + "-i" + RESET + ", " + CYAN + "--input" + RESET + "=" + YELLOW + "<path>      " + RESET + "input apk");
        System.out.println("  " + CYAN + "-o" + RESET + ", " + CYAN + "--output" + RESET + "=" + YELLOW + "<path>     " + RESET + "output dir");
        System.out.println("  " + CYAN + "-t" + RESET + ", " + CYAN + "--translate" + RESET + "=" + YELLOW + "<path>  " + RESET + "translate file");
        System.out.println("  " + CYAN + "-h" + RESET + ", " + CYAN + "--help              " + RESET + "Show this message and exit");
    }

    public static void printError(String message) {
        System.out.println(RED + "Error: " + RESET + message);
    }
}

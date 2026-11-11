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

package com.repairip.DexPatch.Logging;

import com.repairip.Utils.SpannableBanner;

import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.immutable.ImmutableClassDef;

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


/**
 * Handles patching of {@code StartupLauncher} class to incorporate pairip logging hook.
 */
public class LauncherPatcher {

    /**
     * Patches the {@code StartupLauncher} class definition.
     *
     * @param classDef original {@code StartupLauncher} class definition
     * @param junkClasses list of junk class types found in the APK
     * @return updated class definition with modified launch method and generated pairip method
     */
    public static ClassDef patchStartupLauncher(ClassDef classDef, List<String> junkClasses) {
        System.out.println(SpannableBanner.CYAN + "[INFO] " + SpannableBanner.RESET + "Patching StartupLauncher");
        List<Method> directMethods = new ArrayList<>();
        for (Method method : classDef.getDirectMethods()) {
            if ("launch".equals(method.getName())) {
                directMethods.add(LaunchMethodPatcher.patchLaunchMethod(method));
            } else if (!"pairip".equals(method.getName())) {
                directMethods.add(method);
            }
        }
        directMethods.add(PairipMethodGenerator.createPairipMethod(classDef.getType(), junkClasses));

        return new ImmutableClassDef(
                classDef.getType(),
                classDef.getAccessFlags(),
                classDef.getSuperclass(),
                classDef.getInterfaces(),
                classDef.getSourceFile(),
                classDef.getAnnotations(),
                classDef.getStaticFields(),
                classDef.getInstanceFields(),
                directMethods,
                classDef.getVirtualMethods()
        );
    }
}

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

import com.reandroid.json.JSONObject;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

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
public class TranslationData {
    public static class Entry {
        public final String type;
        public final Map<String, String> fields;

        public Entry(String type, Map<String, String> fields) {
            this.type = type;
            this.fields = fields;
        }
    }

    public final Map<String, Entry> entries = new HashMap<>();
    public boolean hasMethods = false;

    public TranslationData(File jsonFile) throws Exception {
        String content = new String(Files.readAllBytes(jsonFile.toPath()));
        JSONObject json = new JSONObject(content);
        for (String className : json.keySet()) {
            String rawClassName = className.trim();
            JSONObject classObj = json.getJSONObject(className);
            String type = normalizeFieldType(classObj.getString("type"));
            if ("java.lang.reflect.Method".equals(type)) {
                hasMethods = true;
            }
            JSONObject fieldsObj = classObj.getJSONObject("fields");
            Map<String, String> fields = new HashMap<>();
            for (String fieldName : fieldsObj.keySet()) {
                fields.put(fieldName, fieldsObj.getString(fieldName));
            }
            /**
             * normalize class descriptor key so containsKey(type) matches DEX class descriptors
             */
            entries.put(normalizeClassName(rawClassName), new Entry(type, fields));
        }
    }

    public static String normalizeClassName(String className) {
        String normalized = className.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        if (normalized.startsWith("L") && normalized.endsWith(";")) {
            return "L" + normalized.substring(1, normalized.length() - 1).replace('.', '/').replace('\\', '/') + ";";
        }
        if (normalized.startsWith("[")) {
            return normalized.replace('.', '/').replace('\\', '/');
        }
        return "L" + normalized.replace('.', '/').replace('\\', '/') + ";";
    }

    private static String normalizeFieldType(String type) {
        if ("Ljava/lang/String;".equals(type)) {
            return "java.lang.String";
        }
        if ("Ljava/lang/reflect/Method;".equals(type)) {
            return "java.lang.reflect.Method";
        }
        return type;
    }
}

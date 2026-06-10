package com.antik.DexPatcher.Translation;

import com.reandroid.json.JSONObject;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

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
            entries.put(rawClassName, new Entry(type, fields));
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

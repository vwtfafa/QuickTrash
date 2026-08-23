package org.vwtfafa.quicktrash.util;

import java.util.Map;

public final class Placeholders {
    private Placeholders() { }

    public static String apply(String value, Map<String, String> replacements) {
        String result = value;
        for (var entry : replacements.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}

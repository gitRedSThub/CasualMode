package me.redst.casualMode.config;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class YamlText {

    private static final Pattern PLAIN_SAFE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private static final Set<String> RESERVED = Set.of("true", "false", "yes", "no", "on", "off", "y", "n", "null");

    private YamlText() {
    }

    static String string(String text) {
        if (PLAIN_SAFE.matcher(text).matches() && !RESERVED.contains(text.toLowerCase(Locale.ROOT))) {
            return text;
        }
        return quoted(text);
    }

    static String quoted(String text) {
        return "'" + text.replace("'", "''") + "'";
    }
}

package me.redst.casualMode.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigRenderer {

    private static final Pattern KEY_LINE = Pattern.compile("( *)([A-Za-z0-9_-]+):(.*)");

    private ConfigRenderer() {
    }

    public static String render(String template, SettingsSnapshot settings, List<String> playerNames) {
        StringBuilder out = new StringBuilder(template.length() + 16 * playerNames.size());
        List<Integer> sectionIndents = new ArrayList<>();
        List<String> sectionKeys = new ArrayList<>();
        boolean skippingTemplateNames = false;

        for (String line : template.lines().toList()) {
            String trimmed = line.strip();
            if (skippingTemplateNames) {
                if (trimmed.startsWith("- ")) {
                    continue;
                }
                skippingTemplateNames = false;
            }
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                out.append(line).append('\n');
                continue;
            }

            Matcher matcher = KEY_LINE.matcher(line);
            if (!matcher.matches()) {
                throw new IllegalStateException("Unexpected line in the config template: " + line);
            }
            String indent = matcher.group(1);
            String key = matcher.group(2);
            String value = matcher.group(3).strip();

            while (!sectionIndents.isEmpty() && sectionIndents.getLast() >= indent.length()) {
                sectionIndents.removeLast();
                sectionKeys.removeLast();
            }
            String path = sectionKeys.isEmpty() ? key : String.join(".", sectionKeys) + "." + key;

            if (path.equals(Settings.PLAYER_LIST_PATH)) {
                appendPlayerList(out, indent, key, playerNames);
                skippingTemplateNames = true;
            } else if (value.isEmpty()) {
                sectionIndents.add(indent.length());
                sectionKeys.add(key);
                out.append(line).append('\n');
            } else {
                Setting<?> setting = Settings.byPath(path);
                if (setting == null) {
                    throw new IllegalStateException("Unknown setting in the config template: " + path);
                }
                out.append(indent).append(key).append(": ").append(settings.toYaml(setting)).append('\n');
            }
        }
        return out.toString();
    }

    private static void appendPlayerList(StringBuilder out, String indent, String key, List<String> names) {
        if (names.isEmpty()) {
            out.append(indent).append(key).append(": []\n");
            return;
        }
        out.append(indent).append(key).append(":\n");
        for (String name : names) {
            out.append(indent).append("  - ").append(YamlText.string(name)).append('\n');
        }
    }
}

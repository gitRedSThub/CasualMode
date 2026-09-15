package me.redst.casualMode.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.redst.casualMode.player.PlayerNames;
import org.bukkit.configuration.ConfigurationSection;

public final class ConfigLoader {

    private ConfigLoader() {
    }

    public record Result(SettingsSnapshot settings, List<String> playerNames, List<String> problems) {
    }

    public static Result read(ConfigurationSection yaml) {
        List<String> problems = new ArrayList<>();
        SettingsSnapshot.Builder settings = new SettingsSnapshot.Builder();
        for (Setting<?> setting : Settings.ALL) {
            readSetting(yaml, setting, settings, problems);
        }
        reportUnknownKeys(yaml, problems);
        List<String> names = readPlayerList(yaml, problems);
        return new Result(settings.build(), List.copyOf(names), List.copyOf(problems));
    }

    private static <T> void readSetting(ConfigurationSection yaml, Setting<T> setting,
                                        SettingsSnapshot.Builder settings, List<String> problems) {
        String fallback = setting.toYaml(setting.defaultValue());
        if (!yaml.contains(setting.path(), true)) {
            problems.add(setting.path() + ": Missing. Using the default value " + fallback + ".");
            return;
        }
        ParseResult<T> result = setting.type().readYaml(yaml.get(setting.path()));
        if (result.isOk()) {
            settings.set(setting, result.value());
        } else {
            problems.add(setting.path() + ": " + result.problem() + " Using the default value " + fallback + ".");
        }
    }

    private static void reportUnknownKeys(ConfigurationSection yaml, List<String> problems) {
        Set<String> reported = new HashSet<>();
        for (String path : yaml.getKeys(true)) {
            String parent = path.contains(".") ? path.substring(0, path.lastIndexOf('.')) : null;
            boolean insideReported = parent != null && (reported.contains(parent) || Settings.byPath(parent) != null);
            boolean known = Settings.byPath(path) != null || Settings.isSection(path)
                    || path.equals(Settings.PLAYER_LIST_PATH);
            if (insideReported) {
                reported.add(path);
            } else if (!known) {
                reported.add(path);
                problems.add(path + ": Not a CasualMode setting, so it is ignored. Check the spelling.");
            }
        }
    }

    private static List<String> readPlayerList(ConfigurationSection yaml, List<String> problems) {
        String path = Settings.PLAYER_LIST_PATH;
        if (!yaml.contains(path, true)) {
            problems.add(path + ": Missing. No players are listed.");
            return List.of();
        }
        if (!(yaml.get(path) instanceof List<?> entries)) {
            problems.add(path + ": Must be a list of player names. No players are listed.");
            return List.of();
        }

        List<String> names = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Object entry : entries) {
            if (!(entry instanceof String raw)) {
                problems.add(path + ": An entry was read as " + entry + " instead of a name. "
                        + "Put names that look like numbers or true/false/yes/no in quotes, like 'No'.");
                continue;
            }
            String name = raw.strip();
            if (!PlayerNames.isValid(name)) {
                problems.add(path + ": '" + raw + "' is not a valid player name. " + PlayerNames.RULES);
            } else if (!seen.add(PlayerNames.key(name))) {
                problems.add(path + ": " + name + " is listed more than once. Only the first entry is used.");
            } else {
                names.add(name);
            }
        }
        return names;
    }
}

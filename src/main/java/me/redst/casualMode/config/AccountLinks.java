package me.redst.casualMode.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.redst.casualMode.player.PlayerNames;
import org.bukkit.configuration.ConfigurationSection;

public final class AccountLinks {

    public static final String FILE_NAME = "player-accounts.yml";

    private static final String SECTION = "accounts";

    private static final String HEADER = """
            # CasualMode keeps this file up to date by itself.
            # It remembers which Minecraft account each name in the casual playerlist belongs to,
            # so a different player who later uses the same name does not receive CasualMode.
            # To change the playerlist, use /casualmode playerlist or edit player-list in config.yml.
            """;

    private AccountLinks() {
    }

    public static String render(Map<UUID, String> links) {
        StringBuilder out = new StringBuilder(HEADER);
        if (links.isEmpty()) {
            return out.append(SECTION).append(": {}\n").toString();
        }
        out.append(SECTION).append(":\n");
        links.forEach((account, name) -> out.append("  ")
                .append(YamlText.quoted(account.toString())).append(": ")
                .append(YamlText.quoted(name)).append('\n'));
        return out.toString();
    }

    public static Map<UUID, String> read(ConfigurationSection yaml, List<String> problems) {
        Map<UUID, String> links = new LinkedHashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection(SECTION);
        if (section == null) {
            return links;
        }
        for (String key : section.getKeys(false)) {
            UUID account;
            try {
                account = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                problems.add(FILE_NAME + ": '" + key + "' is not an account id and was ignored.");
                continue;
            }
            if (section.get(key) instanceof String name && PlayerNames.isValid(name)) {
                links.put(account, name);
            } else {
                problems.add(FILE_NAME + ": The name for account " + key + " is not valid and was ignored.");
            }
        }
        return links;
    }
}

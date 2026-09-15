package me.redst.casualMode.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

public final class ConfigFiles {

    public static final String CONFIG_FILE_NAME = "config.yml";

    private static final Pattern YAML_ERROR_POSITION = Pattern.compile("line (\\d+), column (\\d+)");

    private final Path configPath;
    private final Path accountsPath;
    private final String template;
    private final AsyncFileWriter writer;

    private boolean configReadable = true;

    public ConfigFiles(Path dataFolder, String template, AsyncFileWriter writer) {
        this.configPath = dataFolder.resolve(CONFIG_FILE_NAME);
        this.accountsPath = dataFolder.resolve(AccountLinks.FILE_NAME);
        this.template = template;
        this.writer = writer;
    }

    public record ConfigRead(@Nullable ConfigLoader.Result config, @Nullable String error, @Nullable String details) {
    }

    public ConfigRead readConfig() {
        writer.flush();
        try {
            if (Files.notExists(configPath)) {
                AsyncFileWriter.writeNow(configPath, template);
            }
            YamlConfiguration yaml = parse(configPath);
            configReadable = true;
            return new ConfigRead(ConfigLoader.read(yaml), null, null);
        } catch (InvalidConfigurationException e) {
            configReadable = false;
            String details = e.getMessage() != null ? e.getMessage() : e.toString();
            Matcher position = YAML_ERROR_POSITION.matcher(details);
            String where = position.find() ? " near line " + position.group(1) + ", column " + position.group(2) : "";
            return new ConfigRead(null, CONFIG_FILE_NAME + " has a formatting mistake" + where + ".", details);
        } catch (IOException e) {
            configReadable = false;
            return new ConfigRead(null, CONFIG_FILE_NAME + " could not be read: " + e.getMessage(), e.toString());
        }
    }

    public boolean canSaveConfig() {
        return configReadable;
    }

    public void saveConfig(SettingsSnapshot settings, List<String> playerNames) {
        if (configReadable) {
            writer.write(configPath, ConfigRenderer.render(template, settings, playerNames));
        }
    }

    public Map<UUID, String> readAccountLinks(List<String> problems) {
        writer.flush();
        if (Files.notExists(accountsPath)) {
            return Map.of();
        }
        try {
            return AccountLinks.read(parse(accountsPath), problems);
        } catch (IOException | InvalidConfigurationException e) {
            problems.add(AccountLinks.FILE_NAME + " could not be read, so account links start fresh: " + e.getMessage());
            return Map.of();
        }
    }

    public void saveAccountLinks(Map<UUID, String> links) {
        writer.write(accountsPath, AccountLinks.render(links));
    }

    private static YamlConfiguration parse(Path path) throws IOException, InvalidConfigurationException {
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(text);
        return yaml;
    }
}

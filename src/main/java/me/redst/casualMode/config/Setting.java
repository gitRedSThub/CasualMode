package me.redst.casualMode.config;

import java.util.List;
import java.util.stream.Collectors;

public final class Setting<T> {

    private final String path;
    private final List<String> segments;
    private final SettingType<T> type;
    private final T defaultValue;
    private final boolean mainToggle;

    Setting(String path, SettingType<T> type, T defaultValue, boolean mainToggle) {
        this.path = path;
        this.segments = List.of(path.split("\\."));
        this.type = type;
        this.defaultValue = defaultValue;
        this.mainToggle = mainToggle;
    }

    public String path() {
        return path;
    }

    public List<String> segments() {
        return segments;
    }

    public String key() {
        return segments.getLast();
    }

    public SettingType<T> type() {
        return type;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public boolean isMainToggle() {
        return mainToggle;
    }

    public String label() {
        return segments.stream()
                .filter(segment -> !segment.equals("settings"))
                .map(Settings::displayName)
                .collect(Collectors.joining(" › "));
    }

    public ParseResult<T> parseInput(String input) {
        return type.parseInput(input);
    }

    public String describe(T value) {
        return type.describe(value);
    }

    public String toYaml(T value) {
        return type.toYaml(value);
    }
}

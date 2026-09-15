package me.redst.casualMode.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SettingsSnapshot {

    private static final SettingsSnapshot DEFAULTS;

    static {
        Builder builder = new Builder();
        for (Setting<?> setting : Settings.ALL) {
            builder.values.put(setting, setting.defaultValue());
        }
        DEFAULTS = builder.build();
    }

    private final Map<Setting<?>, Object> values;

    private SettingsSnapshot(Map<Setting<?>, Object> values) {
        this.values = values;
    }

    public static SettingsSnapshot defaults() {
        return DEFAULTS;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Setting<T> setting) {
        return (T) values.get(setting);
    }

    public <T> SettingsSnapshot with(Setting<T> setting, T value) {
        Map<Setting<?>, Object> copy = new HashMap<>(values);
        copy.put(setting, Objects.requireNonNull(value));
        return new SettingsSnapshot(Collections.unmodifiableMap(copy));
    }

    public String toYaml(Setting<?> setting) {
        return yaml(setting);
    }

    public String describe(Setting<?> setting) {
        return description(setting);
    }

    private <T> String yaml(Setting<T> setting) {
        return setting.toYaml(get(setting));
    }

    private <T> String description(Setting<T> setting) {
        return setting.describe(get(setting));
    }

    public static final class Builder {

        private final Map<Setting<?>, Object> values = new HashMap<>();

        public Builder() {
            if (DEFAULTS != null) {
                values.putAll(DEFAULTS.values);
            }
        }

        public <T> Builder set(Setting<T> setting, T value) {
            values.put(setting, Objects.requireNonNull(value));
            return this;
        }

        public SettingsSnapshot build() {
            return new SettingsSnapshot(Collections.unmodifiableMap(new HashMap<>(values)));
        }
    }
}

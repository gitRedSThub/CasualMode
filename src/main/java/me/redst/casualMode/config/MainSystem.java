package me.redst.casualMode.config;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum MainSystem {
    DAMAGE_REDUCTION("damage-reduction", "Damage Reduction"),
    AUTO_HEAL("auto-heal", "AutoHeal"),
    PETS("pets", "Pets"),
    COLOR_NAME("color-name", "Color Name");

    private final String key;
    private final String displayName;

    MainSystem(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public Setting<Boolean> toggle() {
        return switch (this) {
            case DAMAGE_REDUCTION -> Settings.PLAYER.damageReductionEnabled();
            case AUTO_HEAL -> Settings.PLAYER.autoHealEnabled();
            case PETS -> Settings.PETS_ENABLED;
            case COLOR_NAME -> Settings.COLOR_NAME_ENABLED;
        };
    }

    public static @Nullable MainSystem byKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        for (MainSystem system : values()) {
            if (system.key.equals(normalized)) {
                return system;
            }
        }
        return null;
    }
}

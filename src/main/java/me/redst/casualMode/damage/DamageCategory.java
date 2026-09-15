package me.redst.casualMode.damage;

public enum DamageCategory {
    MOBS("mobs", "Mobs", false, true),
    PLAYERS("players", "Players", true, false),
    FALL("fall", "Fall", true, false),
    PROJECTILES("projectiles", "Projectiles", true, true),
    EXPLOSIVES("explosives", "Explosives", true, true),
    MAGIC("magic", "Magic", true, true);

    private final String key;
    private final String displayName;
    private final boolean hasEnabledSetting;
    private final boolean enabledByDefault;

    DamageCategory(String key, String displayName, boolean hasEnabledSetting, boolean enabledByDefault) {
        this.key = key;
        this.displayName = displayName;
        this.hasEnabledSetting = hasEnabledSetting;
        this.enabledByDefault = enabledByDefault;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public boolean hasEnabledSetting() {
        return hasEnabledSetting;
    }

    public boolean enabledByDefault() {
        return enabledByDefault;
    }
}

package me.redst.casualMode.protection;

import me.redst.casualMode.config.ProfileSettings;
import me.redst.casualMode.config.SettingsSnapshot;
import me.redst.casualMode.damage.DamageCategory;

public final class DamageProfile {

    private static final double FULL_DAMAGE = 100.0;

    private final boolean enabled;
    private final double[] receivedPercent = new double[DamageCategory.values().length];

    private DamageProfile(boolean enabled) {
        this.enabled = enabled;
    }

    public static DamageProfile from(SettingsSnapshot settings, ProfileSettings keys) {
        DamageProfile profile = new DamageProfile(settings.get(keys.damageReductionEnabled()));
        for (DamageCategory category : DamageCategory.values()) {
            boolean categoryOn = keys.categoryEnabled(category).map(settings::get).orElse(true);
            profile.receivedPercent[category.ordinal()] =
                    categoryOn ? settings.get(keys.categoryPercent(category)) : FULL_DAMAGE;
        }
        return profile;
    }

    public boolean enabled() {
        return enabled;
    }

    public double receivedPercent(DamageCategory category) {
        return enabled ? receivedPercent[category.ordinal()] : FULL_DAMAGE;
    }
}

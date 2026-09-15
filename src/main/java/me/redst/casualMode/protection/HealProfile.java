package me.redst.casualMode.protection;

import me.redst.casualMode.config.ProfileSettings;
import me.redst.casualMode.config.SettingsSnapshot;

public record HealProfile(boolean enabled, double healthThreshold, double healAmount, int intervalTicks) {

    private static final int TICKS_PER_SECOND = 20;

    public static HealProfile from(SettingsSnapshot settings, ProfileSettings keys) {
        return new HealProfile(
                settings.get(keys.autoHealEnabled()),
                settings.get(keys.healthThreshold()),
                settings.get(keys.healAmount()),
                toTicks(settings.get(keys.interval())));
    }

    private static int toTicks(double seconds) {
        return (int) Math.max(1, Math.round(seconds * TICKS_PER_SECOND));
    }
}

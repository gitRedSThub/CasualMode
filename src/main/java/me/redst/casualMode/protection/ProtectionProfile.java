package me.redst.casualMode.protection;

import me.redst.casualMode.config.ProfileSettings;
import me.redst.casualMode.config.SettingsSnapshot;

public record ProtectionProfile(DamageProfile damage, HealProfile heal) {

    public static ProtectionProfile from(SettingsSnapshot settings, ProfileSettings keys) {
        return new ProtectionProfile(DamageProfile.from(settings, keys), HealProfile.from(settings, keys));
    }
}

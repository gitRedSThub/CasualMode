package me.redst.casualMode.protection;

import me.redst.casualMode.config.Settings;
import me.redst.casualMode.config.SettingsSnapshot;
import net.kyori.adventure.text.format.NamedTextColor;

public record ActiveSettings(ProtectionProfile player, boolean petsEnabled, ProtectionProfile pet,
                             boolean colorNameEnabled, NamedTextColor nameColor) {

    public static ActiveSettings from(SettingsSnapshot settings) {
        ProtectionProfile player = ProtectionProfile.from(settings, Settings.PLAYER);
        ProtectionProfile pet = settings.get(Settings.PETS_USE_OWN_SETTINGS)
                ? ProtectionProfile.from(settings, Settings.PET)
                : player;
        return new ActiveSettings(
                player,
                settings.get(Settings.PETS_ENABLED),
                pet,
                settings.get(Settings.COLOR_NAME_ENABLED),
                settings.get(Settings.COLOR_NAME_COLOR));
    }
}

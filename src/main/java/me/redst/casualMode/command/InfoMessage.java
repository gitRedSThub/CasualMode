package me.redst.casualMode.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.redst.casualMode.command.Text.TreeNode;
import me.redst.casualMode.config.ColorType;
import me.redst.casualMode.config.ProfileSettings;
import me.redst.casualMode.config.Setting;
import me.redst.casualMode.config.Settings;
import me.redst.casualMode.config.SettingsSnapshot;
import me.redst.casualMode.damage.DamageCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class InfoMessage {

    private InfoMessage() {
    }

    static Component create(SettingsSnapshot settings) {
        List<TreeNode> systems = List.of(
                damageReduction("Damage Reduction", settings, Settings.PLAYER),
                autoHeal("AutoHeal", settings, Settings.PLAYER),
                pets(settings),
                colorName(settings));

        List<Component> lines = new ArrayList<>();
        lines.add(Text.title("CasualMode"));
        lines.add(Component.empty());
        lines.addAll(Text.tree(systems, true));
        return Text.lines(lines);
    }

    static TreeNode damageReduction(String title, SettingsSnapshot settings, ProfileSettings keys) {
        List<TreeNode> categories = new ArrayList<>();
        for (DamageCategory category : DamageCategory.values()) {
            Optional<Setting<Boolean>> enabled = keys.categoryEnabled(category);
            Component value = enabled.isPresent() && !settings.get(enabled.get())
                    ? Text.onOff(false)
                    : Text.value(settings.describe(keys.categoryPercent(category)) + " damage received");
            categories.add(TreeNode.leaf(Text.labeled(category.displayName(), value)));
        }
        return new TreeNode(Text.labeled(title, Text.onOff(settings.get(keys.damageReductionEnabled()))), categories);
    }

    static TreeNode autoHeal(String title, SettingsSnapshot settings, ProfileSettings keys) {
        return new TreeNode(Text.labeled(title, Text.onOff(settings.get(keys.autoHealEnabled()))), List.of(
                detail("Health threshold", settings, keys.healthThreshold()),
                detail("Heal amount", settings, keys.healAmount()),
                detail("Interval", settings, keys.interval())));
    }

    private static TreeNode pets(SettingsSnapshot settings) {
        List<TreeNode> children = new ArrayList<>();
        children.add(detail("Use own settings", settings, Settings.PETS_USE_OWN_SETTINGS));
        if (settings.get(Settings.PETS_USE_OWN_SETTINGS)) {
            children.add(damageReduction("Damage Reduction", settings, Settings.PET));
            children.add(autoHeal("AutoHeal", settings, Settings.PET));
        }
        return new TreeNode(Text.labeled("Pets", Text.onOff(settings.get(Settings.PETS_ENABLED))), children);
    }

    private static TreeNode colorName(SettingsSnapshot settings) {
        NamedTextColor color = settings.get(Settings.COLOR_NAME_COLOR);
        return new TreeNode(Text.labeled("Color Name", Text.onOff(settings.get(Settings.COLOR_NAME_ENABLED))), List.of(
                TreeNode.leaf(Text.labeled("Color", Component.text(ColorType.name(color), color)))));
    }

    private static TreeNode detail(String label, SettingsSnapshot settings, Setting<?> setting) {
        return TreeNode.leaf(Text.labeled(label, Text.value(settings.describe(setting))));
    }
}

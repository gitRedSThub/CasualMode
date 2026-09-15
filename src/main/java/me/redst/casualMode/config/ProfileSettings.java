package me.redst.casualMode.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.redst.casualMode.damage.DamageCategory;

public final class ProfileSettings {

    private final Setting<Boolean> damageReductionEnabled;
    private final Map<DamageCategory, Setting<Boolean>> categoryEnabled = new EnumMap<>(DamageCategory.class);
    private final Map<DamageCategory, Setting<Double>> categoryPercent = new EnumMap<>(DamageCategory.class);
    private final Setting<Boolean> autoHealEnabled;
    private final Setting<Double> healthThreshold;
    private final Setting<Double> healAmount;
    private final Setting<Double> interval;
    private final List<Setting<?>> all;

    ProfileSettings(String prefix, boolean mainSystem) {
        List<Setting<?>> settings = new ArrayList<>();

        damageReductionEnabled = add(settings, new Setting<>(
                prefix + "damage-reduction.enabled", BooleanType.ON_OFF, true, mainSystem));
        for (DamageCategory category : DamageCategory.values()) {
            String section = prefix + "damage-reduction." + category.key() + ".";
            if (category.hasEnabledSetting()) {
                categoryEnabled.put(category, add(settings, new Setting<>(
                        section + "enabled", BooleanType.ON_OFF, category.enabledByDefault(), false)));
            }
            categoryPercent.put(category, add(settings, new Setting<>(
                    section + "damage-received-percent", DecimalType.PERCENT, 50.0, false)));
        }

        autoHealEnabled = add(settings, new Setting<>(
                prefix + "auto-heal.enabled", BooleanType.ON_OFF, true, mainSystem));
        healthThreshold = add(settings, new Setting<>(
                prefix + "auto-heal.health-threshold", DecimalType.HEALTH_THRESHOLD, 10.0, false));
        healAmount = add(settings, new Setting<>(
                prefix + "auto-heal.heal-amount", DecimalType.HEAL_AMOUNT, 2.0, false));
        interval = add(settings, new Setting<>(
                prefix + "auto-heal.interval", DecimalType.INTERVAL, 4.0, false));

        all = Collections.unmodifiableList(settings);
    }

    private static <T> Setting<T> add(List<Setting<?>> settings, Setting<T> setting) {
        settings.add(setting);
        return setting;
    }

    public Setting<Boolean> damageReductionEnabled() {
        return damageReductionEnabled;
    }

    public Optional<Setting<Boolean>> categoryEnabled(DamageCategory category) {
        return Optional.ofNullable(categoryEnabled.get(category));
    }

    public Setting<Double> categoryPercent(DamageCategory category) {
        return categoryPercent.get(category);
    }

    public Setting<Boolean> autoHealEnabled() {
        return autoHealEnabled;
    }

    public Setting<Double> healthThreshold() {
        return healthThreshold;
    }

    public Setting<Double> healAmount() {
        return healAmount;
    }

    public Setting<Double> interval() {
        return interval;
    }

    public List<Setting<?>> all() {
        return all;
    }

    public boolean contains(Setting<?> setting) {
        return all.contains(setting);
    }

    public Optional<DamageCategory> categoryOf(Setting<?> setting) {
        for (DamageCategory category : DamageCategory.values()) {
            if (setting == categoryPercent.get(category) || setting == categoryEnabled.get(category)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }

    public boolean isDamageReductionSetting(Setting<?> setting) {
        return setting == damageReductionEnabled || categoryOf(setting).isPresent();
    }
}

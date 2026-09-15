package me.redst.casualMode.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.redst.casualMode.damage.DamageCategory;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public final class Settings {

    public static final ProfileSettings PLAYER = new ProfileSettings("", true);

    public static final Setting<Boolean> PETS_ENABLED =
            new Setting<>("pets.enabled", BooleanType.ON_OFF, true, true);
    public static final Setting<Boolean> PETS_USE_OWN_SETTINGS =
            new Setting<>("pets.use-own-settings", BooleanType.YES_NO, false, false);
    public static final ProfileSettings PET = new ProfileSettings("pets.settings.", false);

    public static final Setting<Boolean> COLOR_NAME_ENABLED =
            new Setting<>("color-name.enabled", BooleanType.ON_OFF, true, true);
    public static final Setting<NamedTextColor> COLOR_NAME_COLOR =
            new Setting<>("color-name.color", ColorType.INSTANCE, NamedTextColor.GREEN, false);

    public static final String PLAYER_LIST_PATH = "player-list";

    public static final List<Setting<?>> ALL;

    private static final Map<String, Setting<?>> BY_PATH;
    private static final Set<String> SECTION_PATHS;
    private static final Map<String, String> DISPLAY_NAMES;

    static {
        List<Setting<?>> all = new ArrayList<>(PLAYER.all());
        all.add(PETS_ENABLED);
        all.add(PETS_USE_OWN_SETTINGS);
        all.addAll(PET.all());
        all.add(COLOR_NAME_ENABLED);
        all.add(COLOR_NAME_COLOR);
        ALL = Collections.unmodifiableList(all);

        Map<String, Setting<?>> byPath = new LinkedHashMap<>();
        Set<String> sections = new HashSet<>();
        for (Setting<?> setting : ALL) {
            byPath.put(setting.path(), setting);
            String path = setting.path();
            for (int dot = path.indexOf('.'); dot >= 0; dot = path.indexOf('.', dot + 1)) {
                sections.add(path.substring(0, dot));
            }
        }
        BY_PATH = Collections.unmodifiableMap(byPath);
        SECTION_PATHS = Collections.unmodifiableSet(sections);

        Map<String, String> names = new LinkedHashMap<>();
        names.put("damage-reduction", "Damage Reduction");
        names.put("auto-heal", "AutoHeal");
        names.put("pets", "Pets");
        names.put("color-name", "Color Name");
        names.put("settings", "Settings");
        for (DamageCategory category : DamageCategory.values()) {
            names.put(category.key(), category.displayName());
        }
        names.put("enabled", "Enabled");
        names.put("damage-received-percent", "Damage received");
        names.put("health-threshold", "Health threshold");
        names.put("heal-amount", "Heal amount");
        names.put("interval", "Interval");
        names.put("use-own-settings", "Use own settings");
        names.put("color", "Color");
        DISPLAY_NAMES = Collections.unmodifiableMap(names);
    }

    private Settings() {
    }

    public static @Nullable Setting<?> byPath(String path) {
        return BY_PATH.get(path);
    }

    public static boolean isSection(String path) {
        return SECTION_PATHS.contains(path);
    }

    public static String displayName(String segment) {
        return DISPLAY_NAMES.getOrDefault(segment, segment);
    }
}

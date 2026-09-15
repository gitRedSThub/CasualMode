package me.redst.casualMode.config;

import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public enum ColorType implements SettingType<NamedTextColor> {
    INSTANCE;

    public static final List<NamedTextColor> COLORS = List.of(
            NamedTextColor.BLACK, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN, NamedTextColor.DARK_AQUA,
            NamedTextColor.DARK_RED, NamedTextColor.DARK_PURPLE, NamedTextColor.GOLD, NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY, NamedTextColor.BLUE, NamedTextColor.GREEN, NamedTextColor.AQUA,
            NamedTextColor.RED, NamedTextColor.LIGHT_PURPLE, NamedTextColor.YELLOW, NamedTextColor.WHITE);

    private static final List<ValueSuggestion> SUGGESTIONS = COLORS.stream()
            .map(color -> new ValueSuggestion(name(color), null))
            .toList();

    public static String name(NamedTextColor color) {
        return NamedTextColor.NAMES.keyOrThrow(color).toUpperCase(Locale.ROOT);
    }

    @Override
    public String argumentName() {
        return "color";
    }

    @Override
    public String expectation() {
        return "Use a Minecraft color name such as GREEN, AQUA or GOLD.";
    }

    @Override
    public ParseResult<NamedTextColor> parseInput(String input) {
        String key = input.strip().replace(' ', '_').replace('-', '_').toLowerCase(Locale.ROOT);
        NamedTextColor color = NamedTextColor.NAMES.value(key);
        return color != null ? ParseResult.ok(color) : ParseResult.error(expectation());
    }

    @Override
    public ParseResult<NamedTextColor> readYaml(@Nullable Object raw) {
        return raw instanceof String text ? parseInput(text) : ParseResult.error(expectation());
    }

    @Override
    public String toYaml(NamedTextColor value) {
        return name(value);
    }

    @Override
    public String describe(NamedTextColor value) {
        return name(value);
    }

    @Override
    public List<ValueSuggestion> suggestions() {
        return SUGGESTIONS;
    }
}

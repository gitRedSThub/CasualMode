package me.redst.casualMode.config;

import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public enum BooleanType implements SettingType<Boolean> {
    ON_OFF("ON", "OFF"),
    YES_NO("YES", "NO");

    private static final List<ValueSuggestion> SUGGESTIONS = List.of(
            new ValueSuggestion("true", null),
            new ValueSuggestion("false", null));

    private final String trueText;
    private final String falseText;

    BooleanType(String trueText, String falseText) {
        this.trueText = trueText;
        this.falseText = falseText;
    }

    @Override
    public String argumentName() {
        return "true|false";
    }

    @Override
    public String expectation() {
        return "Use true or false.";
    }

    @Override
    public ParseResult<Boolean> parseInput(String input) {
        return switch (input.strip().toLowerCase(Locale.ROOT)) {
            case "true" -> ParseResult.ok(true);
            case "false" -> ParseResult.ok(false);
            default -> ParseResult.error(expectation());
        };
    }

    @Override
    public ParseResult<Boolean> readYaml(@Nullable Object raw) {
        if (raw instanceof Boolean value) {
            return ParseResult.ok(value);
        }
        if (raw instanceof String text) {
            return parseInput(text);
        }
        return ParseResult.error(expectation());
    }

    @Override
    public String toYaml(Boolean value) {
        return value.toString();
    }

    @Override
    public String describe(Boolean value) {
        return value ? trueText : falseText;
    }

    @Override
    public List<ValueSuggestion> suggestions() {
        return SUGGESTIONS;
    }
}

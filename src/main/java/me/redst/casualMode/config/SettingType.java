package me.redst.casualMode.config;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public sealed interface SettingType<T> permits BooleanType, DecimalType, ColorType {

    String argumentName();

    String expectation();

    ParseResult<T> parseInput(String input);

    ParseResult<T> readYaml(@Nullable Object raw);

    String toYaml(T value);

    String describe(T value);

    default String toInput(T value) {
        return toYaml(value);
    }

    List<ValueSuggestion> suggestions();

    record ValueSuggestion(String text, @Nullable String tooltip) {
    }
}

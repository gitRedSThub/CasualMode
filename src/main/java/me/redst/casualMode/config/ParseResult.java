package me.redst.casualMode.config;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public record ParseResult<T>(@Nullable T value, @Nullable String problem) {

    public static <T> ParseResult<T> ok(T value) {
        return new ParseResult<>(Objects.requireNonNull(value), null);
    }

    public static <T> ParseResult<T> error(String problem) {
        return new ParseResult<>(null, Objects.requireNonNull(problem));
    }

    public boolean isOk() {
        return problem == null;
    }
}

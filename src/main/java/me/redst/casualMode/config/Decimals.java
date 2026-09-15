package me.redst.casualMode.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public final class Decimals {

    public static final int MAX_DECIMAL_PLACES = 2;

    private static final Pattern PLAIN_NUMBER = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)");

    private Decimals() {
    }

    public static @Nullable BigDecimal parse(String text) {
        String trimmed = text.strip();
        if (!PLAIN_NUMBER.matcher(trimmed).matches()) {
            return null;
        }
        return new BigDecimal(trimmed);
    }

    public static @Nullable BigDecimal fromYaml(@Nullable Object raw) {
        if (raw instanceof Double || raw instanceof Float) {
            double value = ((Number) raw).doubleValue();
            return Double.isFinite(value) ? new BigDecimal(Double.toString(value)) : null;
        }
        if (raw instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (raw instanceof String text) {
            return parse(text);
        }
        return null;
    }

    public static boolean hasAtMostTwoDecimalPlaces(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= MAX_DECIMAL_PLACES;
    }

    public static String fixed(double value) {
        return BigDecimal.valueOf(value).setScale(MAX_DECIMAL_PLACES, RoundingMode.HALF_UP).toPlainString();
    }

    public static String compact(double value) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(MAX_DECIMAL_PLACES, RoundingMode.HALF_UP);
        return rounded.stripTrailingZeros().toPlainString();
    }
}

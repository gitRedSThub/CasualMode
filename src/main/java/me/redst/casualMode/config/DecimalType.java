package me.redst.casualMode.config;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public final class DecimalType implements SettingType<Double> {

    public static final DecimalType PERCENT = new DecimalType("percent", "0.00", "100.00", Unit.PERCENT, List.of(
            new ValueSuggestion("0", "No damage"),
            new ValueSuggestion("25", "Quarter damage"),
            new ValueSuggestion("50", "Half damage"),
            new ValueSuggestion("75", "Three quarters of the damage"),
            new ValueSuggestion("100", "Full damage")));

    public static final DecimalType HEALTH_THRESHOLD = new DecimalType("health", "0.01", "1000.00", Unit.HEALTH,
            healthSuggestions(4, 6, 8, 10, 14, 20));

    public static final DecimalType HEAL_AMOUNT = new DecimalType("health", "0.01", "1000.00", Unit.HEALTH,
            healthSuggestions(1, 2, 4, 6, 10));

    public static final DecimalType INTERVAL = new DecimalType("seconds", "1.00", "3600.00", Unit.SECONDS, List.of(
            new ValueSuggestion("1", "1 second"),
            new ValueSuggestion("2", "2 seconds"),
            new ValueSuggestion("4", "4 seconds"),
            new ValueSuggestion("5", "5 seconds"),
            new ValueSuggestion("10", "10 seconds"),
            new ValueSuggestion("30", "30 seconds"),
            new ValueSuggestion("60", "1 minute")));

    private static final Pattern COMMA_DECIMAL = Pattern.compile("[+-]?\\d*,\\d+");

    private final String argumentName;
    private final BigDecimal min;
    private final BigDecimal max;
    private final Unit unit;
    private final List<ValueSuggestion> suggestions;
    private final String expectation;

    private DecimalType(String argumentName, String min, String max, Unit unit, List<ValueSuggestion> suggestions) {
        this.argumentName = argumentName;
        this.min = new BigDecimal(min);
        this.max = new BigDecimal(max);
        this.unit = unit;
        this.suggestions = suggestions;
        this.expectation = "Use a number between " + min + " and " + max + unit.rangeSuffix + ".";
    }

    @Override
    public String argumentName() {
        return argumentName;
    }

    @Override
    public String expectation() {
        return expectation;
    }

    public double max() {
        return max.doubleValue();
    }

    @Override
    public ParseResult<Double> parseInput(String input) {
        String text = input.strip();
        BigDecimal value = Decimals.parse(text);
        if (value == null) {
            if (COMMA_DECIMAL.matcher(text).matches()) {
                return ParseResult.error("Use a dot for decimals, like " + text.replace(',', '.') + ". " + expectation);
            }
            return ParseResult.error(expectation);
        }
        return validate(value, text);
    }

    @Override
    public ParseResult<Double> readYaml(@Nullable Object raw) {
        BigDecimal value = Decimals.fromYaml(raw);
        if (value == null) {
            return ParseResult.error(expectation);
        }
        return validate(value, value.toPlainString());
    }

    private ParseResult<Double> validate(BigDecimal value, String shownValue) {
        if (!Decimals.hasAtMostTwoDecimalPlaces(value)) {
            return ParseResult.error("Numbers can have at most 2 decimal places. " + expectation);
        }
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            return ParseResult.error(shownValue + " is outside the allowed range. " + expectation);
        }
        return ParseResult.ok(value.doubleValue());
    }

    @Override
    public String toYaml(Double value) {
        return Decimals.fixed(value);
    }

    @Override
    public String describe(Double value) {
        return unit.describe(value);
    }

    @Override
    public String toInput(Double value) {
        return Decimals.compact(value);
    }

    @Override
    public List<ValueSuggestion> suggestions() {
        return suggestions;
    }

    private static List<ValueSuggestion> healthSuggestions(int... healthPoints) {
        return Arrays.stream(healthPoints)
                .mapToObj(points -> new ValueSuggestion(Integer.toString(points), hearts(points)))
                .toList();
    }

    private static String hearts(int healthPoints) {
        if (healthPoints == 1) {
            return "Half a heart";
        }
        String hearts = Decimals.compact(healthPoints / 2.0);
        return hearts + (healthPoints == 2 ? " heart" : " hearts");
    }

    private enum Unit {
        PERCENT(""),
        HEALTH(" health points"),
        SECONDS(" seconds");

        private final String rangeSuffix;

        Unit(String rangeSuffix) {
            this.rangeSuffix = rangeSuffix;
        }

        String describe(double value) {
            String number = Decimals.compact(value);
            return switch (this) {
                case PERCENT -> number + "%";
                case HEALTH -> number + " HP";
                case SECONDS -> number + (number.equals("1") ? " second" : " seconds");
            };
        }
    }
}

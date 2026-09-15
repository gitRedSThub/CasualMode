package me.redst.casualMode.player;

import java.util.Locale;
import java.util.regex.Pattern;

public final class PlayerNames {

    public static final String RULES = "Player names are 1 to 16 characters long and cannot contain spaces.";

    private static final Pattern VALID = Pattern.compile("[\\x21-\\x7E]{1,16}");

    private PlayerNames() {
    }

    public static boolean isValid(String name) {
        return VALID.matcher(name).matches();
    }

    public static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}

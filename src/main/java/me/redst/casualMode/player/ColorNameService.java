package me.redst.casualMode.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class ColorNameService {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final Map<UUID, ColoredName> coloredNames = new HashMap<>();

    public void update(Player player, @Nullable NamedTextColor color) {
        UUID id = player.getUniqueId();
        ColoredName colored = coloredNames.get(id);
        Component shown = player.playerListName();

        if (color == null) {
            if (colored != null) {
                coloredNames.remove(id);
                if (looksLike(shown, colored.applied())) {
                    player.playerListName(isPlainName(colored.previous(), player.getName()) ? null : colored.previous());
                }
            }
            return;
        }

        Component wanted = Component.text(player.getName(), color);
        if (colored != null && looksLike(shown, colored.applied())) {
            if (!looksLike(shown, wanted)) {
                player.playerListName(wanted);
                coloredNames.put(id, new ColoredName(colored.previous(), wanted));
            }
            return;
        }
        Component previous = looksLike(shown, wanted) ? Component.text(player.getName()) : shown;
        player.playerListName(wanted);
        coloredNames.put(id, new ColoredName(previous, wanted));
    }

    public void forget(UUID playerId) {
        coloredNames.remove(playerId);
    }

    public void restoreAll(Iterable<? extends Player> players) {
        for (Player player : players) {
            update(player, null);
        }
        coloredNames.clear();
    }

    private static boolean looksLike(Component shown, Component expected) {
        return PLAIN_TEXT.serialize(shown).equals(PLAIN_TEXT.serialize(expected))
                && Objects.equals(colorValue(shown), colorValue(expected));
    }

    private static boolean isPlainName(Component name, String username) {
        return name.children().isEmpty() && !name.hasStyling() && PLAIN_TEXT.serialize(name).equals(username);
    }

    private static @Nullable Integer colorValue(Component component) {
        TextColor color = component.color();
        return color == null ? null : color.value();
    }

    private record ColoredName(Component previous, Component applied) {
    }
}

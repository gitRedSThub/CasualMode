package me.redst.casualMode.command;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

final class Text {

    static final TextColor TITLE = NamedTextColor.GREEN;
    static final TextColor LABEL = NamedTextColor.WHITE;
    static final TextColor VALUE = NamedTextColor.YELLOW;
    static final TextColor MUTED = NamedTextColor.GRAY;
    static final TextColor LINES = NamedTextColor.DARK_GRAY;
    static final TextColor SUCCESS = NamedTextColor.GREEN;
    static final TextColor WARNING = NamedTextColor.GOLD;
    static final TextColor ERROR = NamedTextColor.RED;

    private Text() {
    }

    static Component title(String text) {
        return Component.text(text, TITLE, TextDecoration.BOLD);
    }

    static Component muted(String text) {
        return Component.text(text, MUTED);
    }

    static Component value(String text) {
        return Component.text(text, VALUE);
    }

    static Component success(String text) {
        return Component.text(text, SUCCESS);
    }

    static Component onOff(boolean on) {
        return Component.text(on ? "ON" : "OFF", on ? NamedTextColor.GREEN : NamedTextColor.RED);
    }

    static Component labeled(String label, Component value) {
        return Component.textOfChildren(Component.text(label + ": ", LABEL), value);
    }

    static Component error(String headline, String explanation) {
        return lines(List.of(Component.text(headline, ERROR), muted(explanation)));
    }

    static Component suggestCommand(Component text, String command) {
        return text.clickEvent(ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(muted("Click to type " + command.strip())));
    }

    static Component command(String command) {
        return suggestCommand(value(command), command);
    }

    static Component lines(List<? extends Component> lines) {
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    static List<Component> tree(List<TreeNode> roots, boolean spaced) {
        List<Component> lines = new ArrayList<>();
        for (int i = 0; i < roots.size(); i++) {
            boolean last = i == roots.size() - 1;
            addTreeLines(lines, roots.get(i), "", last);
            if (spaced && !last) {
                lines.add(Component.text("│", LINES));
            }
        }
        return lines;
    }

    private static void addTreeLines(List<Component> lines, TreeNode node, String indent, boolean last) {
        lines.add(Component.textOfChildren(Component.text(indent + (last ? "└─ " : "├─ "), LINES), node.label()));
        String childIndent = indent + (last ? "   " : "│  ");
        List<TreeNode> children = node.children();
        for (int i = 0; i < children.size(); i++) {
            addTreeLines(lines, children.get(i), childIndent, i == children.size() - 1);
        }
    }

    record TreeNode(Component label, List<TreeNode> children) {

        static TreeNode leaf(Component label) {
            return new TreeNode(label, List.of());
        }
    }
}

package me.redst.casualMode.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import me.redst.casualMode.CasualModeManager;
import me.redst.casualMode.CasualModeManager.ChangeResult;
import me.redst.casualMode.CasualModeManager.ReloadResult;
import me.redst.casualMode.config.BooleanType;
import me.redst.casualMode.config.ColorType;
import me.redst.casualMode.config.MainSystem;
import me.redst.casualMode.config.ParseResult;
import me.redst.casualMode.config.ProfileSettings;
import me.redst.casualMode.config.Setting;
import me.redst.casualMode.config.SettingType.ValueSuggestion;
import me.redst.casualMode.config.Settings;
import me.redst.casualMode.config.SettingsSnapshot;
import me.redst.casualMode.damage.DamageCategory;
import me.redst.casualMode.player.CasualPlayerList.AddResult;
import me.redst.casualMode.player.CasualPlayerList.RemovedEntry;
import me.redst.casualMode.player.PlayerNames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class CasualModeCommand {

    public static final String ADMIN_PERMISSION = "casualmode.admin";

    private static final Map<String, String> GROUP_DESCRIPTIONS = Map.of(
            "damage-reduction", "How much damage casual players receive",
            "auto-heal", "Healing when health gets low",
            "pets", "Protection for tamed animals",
            "color-name", "Name color in the player list");

    private final CasualModeManager manager;
    private final SettingNode settingTree = SettingNode.buildTree();

    public CasualModeCommand(CasualModeManager manager) {
        this.manager = manager;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set")
                .requires(this::isAdmin)
                .executes(context -> showSettingGroups(context.getSource()));
        for (SettingNode group : settingTree.children.values()) {
            set.then(settingBranch(group));
        }

        return Commands.literal("casualmode")
                .executes(context -> help(context.getSource()))
                .then(set)
                .then(toggleBranch())
                .then(playerListBranch())
                .then(Commands.literal("info")
                        .executes(context -> info(context.getSource())))
                .then(Commands.literal("reload")
                        .requires(this::isAdmin)
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("help")
                        .executes(context -> help(context.getSource())))
                .build();
    }

    private boolean isAdmin(CommandSourceStack source) {
        return source.getSender().hasPermission(ADMIN_PERMISSION);
    }

    private static void send(CommandSourceStack source, Component message) {
        source.getSender().sendMessage(message);
    }

    private static Component configUnreadable() {
        return Text.error("Nothing was changed.",
                "config.yml has a mistake and could not be read, so CasualMode will not overwrite it."
                        + " Fix the file, then use /casualmode reload.");
    }

    private int help(CommandSourceStack source) {
        boolean admin = isAdmin(source);
        List<Component> lines = new ArrayList<>();
        lines.add(Text.title("CasualMode Commands"));
        lines.add(Component.empty());
        if (admin) {
            addHelpEntry(lines, "/casualmode set ...", "/casualmode set ", "Change a configuration value.");
            addHelpEntry(lines, "/casualmode toggle ...", "/casualmode toggle ", "Turn a main system on or off.");
            addHelpEntry(lines, "/casualmode playerlist add <player>", "/casualmode playerlist add ",
                    "Add a player to CasualMode.");
            addHelpEntry(lines, "/casualmode playerlist remove <player>", "/casualmode playerlist remove ",
                    "Remove a player from CasualMode.");
        }
        addHelpEntry(lines, "/casualmode playerlist get", "/casualmode playerlist get", "Show the CasualMode playerlist.");
        addHelpEntry(lines, "/casualmode info", "/casualmode info", "Show current settings.");
        if (admin) {
            addHelpEntry(lines, "/casualmode reload", "/casualmode reload", "Reload config.yml.");
        }
        addHelpEntry(lines, "/casualmode help", "/casualmode help", "Show this help.");
        send(source, Text.lines(lines));
        return Command.SINGLE_SUCCESS;
    }

    private static void addHelpEntry(List<Component> lines, String usage, String command, String description) {
        lines.add(Text.suggestCommand(Text.value(usage), command));
        lines.add(Text.muted("  " + description));
    }

    private int info(CommandSourceStack source) {
        send(source, InfoMessage.create(manager.settings()));
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandSourceStack source) {
        ReloadResult result = manager.reload();
        if (!result.success()) {
            send(source, Text.error("Could not reload config.yml.", result.error()
                    + " The previous settings are still active. The console shows the full error."));
            return 0;
        }
        if (result.problems().isEmpty()) {
            send(source, Text.success("Reloaded config.yml."));
            return Command.SINGLE_SUCCESS;
        }
        String summary = "Reloaded config.yml, but found " + result.problems().size()
                + (result.problems().size() == 1 ? " problem" : " problems");
        if (source.getSender() instanceof ConsoleCommandSender) {
            send(source, Component.text(summary + " (listed above).", Text.WARNING));
            return Command.SINGLE_SUCCESS;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text(summary + ":", Text.WARNING));
        for (String problem : result.problems()) {
            lines.add(Text.muted("- " + problem));
        }
        send(source, Text.lines(lines));
        return Command.SINGLE_SUCCESS;
    }

    private LiteralArgumentBuilder<CommandSourceStack> toggleBranch() {
        LiteralArgumentBuilder<CommandSourceStack> toggle = Commands.literal("toggle")
                .requires(this::isAdmin)
                .executes(context -> showSystems(context.getSource()));
        for (MainSystem system : MainSystem.values()) {
            toggle.then(Commands.literal(system.key())
                    .executes(context -> toggle(context.getSource(), system)));
        }
        return toggle;
    }

    private int showSystems(CommandSourceStack source) {
        SettingsSnapshot settings = manager.settings();
        List<Component> lines = new ArrayList<>();
        lines.add(Text.title("CasualMode Systems"));
        lines.add(Component.empty());
        for (MainSystem system : MainSystem.values()) {
            Component state = Text.labeled(system.displayName(), Text.onOff(settings.get(system.toggle())));
            lines.add(Text.suggestCommand(state, "/casualmode toggle " + system.key()));
        }
        lines.add(Component.empty());
        lines.add(Text.muted("Click a system or use /casualmode toggle <system> to turn it on or off."));
        send(source, Text.lines(lines));
        return Command.SINGLE_SUCCESS;
    }

    private int toggle(CommandSourceStack source, MainSystem system) {
        boolean wasOn = manager.settings().get(system.toggle());
        ChangeResult<Boolean> result = manager.change(system.toggle(), !wasOn);
        if (result.status() == CasualModeManager.ChangeStatus.CONFIG_UNREADABLE) {
            send(source, configUnreadable());
            return 0;
        }
        send(source, changeLine(system.displayName(), Text.onOff(wasOn), Text.onOff(!wasOn)));
        return Command.SINGLE_SUCCESS;
    }

    private LiteralArgumentBuilder<CommandSourceStack> settingBranch(SettingNode node) {
        LiteralArgumentBuilder<CommandSourceStack> literal = Commands.literal(node.name());
        Setting<?> setting = node.setting;
        if (setting == null) {
            literal.executes(context -> showSection(context.getSource(), node));
            for (SettingNode child : node.children.values()) {
                literal.then(settingBranch(child));
            }
            return literal;
        }

        String argument = setting.type().argumentName();
        return literal
                .executes(context -> showSetting(context.getSource(), setting))
                .then(Commands.argument(argument, StringArgumentType.greedyString())
                        .suggests((context, builder) -> suggestValues(setting, builder))
                        .executes(context -> setValue(context.getSource(), setting,
                                StringArgumentType.getString(context, argument))));
    }

    private int showSettingGroups(CommandSourceStack source) {
        List<Component> lines = new ArrayList<>();
        lines.add(Text.title("CasualMode Settings"));
        lines.add(Component.empty());
        for (SettingNode group : settingTree.children.values()) {
            lines.add(Component.textOfChildren(
                    Text.suggestCommand(Text.value(group.name()), group.command() + " "),
                    Text.muted(" - " + GROUP_DESCRIPTIONS.getOrDefault(group.name(), ""))));
        }
        lines.add(Component.empty());
        lines.add(Text.muted("Use /casualmode set <group> <option> <value>. Press Tab to see the options."));
        lines.add(Text.muted("To turn a main system on or off, use /casualmode toggle."));
        send(source, Text.lines(lines));
        return Command.SINGLE_SUCCESS;
    }

    private int showSection(CommandSourceStack source, SettingNode section) {
        SettingsSnapshot settings = manager.settings();
        List<Component> lines = new ArrayList<>();
        lines.add(Text.title(section.label()));

        MainSystem system = section.path.size() == 1 ? MainSystem.byKey(section.name()) : null;
        if (system != null) {
            lines.add(Component.textOfChildren(
                    Text.labeled(system.displayName(), Text.onOff(settings.get(system.toggle()))),
                    Text.muted("  (switch with /casualmode toggle " + system.key() + ")")));
        }
        lines.add(Component.empty());

        for (SettingNode child : section.children.values()) {
            Component line;
            if (child.setting != null) {
                line = Text.labeled(child.name(), currentValueText(child.setting, settings));
            } else if (child.children.values().stream().allMatch(grandchild -> grandchild.setting != null)) {
                String summary = child.children.values().stream()
                        .map(grandchild -> grandchild.name() + " " + settings.describe(grandchild.setting))
                        .collect(Collectors.joining(", "));
                line = Component.textOfChildren(Component.text(child.name() + " › ", Text.LABEL), Text.muted(summary));
            } else {
                String options = String.join(", ", child.children.keySet());
                line = Component.textOfChildren(Component.text(child.name() + " › ", Text.LABEL), Text.muted(options));
            }
            lines.add(Text.suggestCommand(line, child.command() + " "));
        }

        lines.add(Component.empty());
        lines.add(Text.muted("Click an option to change it, or press Tab to see the options."));
        send(source, Text.lines(lines));
        return Command.SINGLE_SUCCESS;
    }

    private <T> int showSetting(CommandSourceStack source, Setting<T> setting) {
        SettingsSnapshot settings = manager.settings();
        String usage = "/casualmode set " + String.join(" ", setting.segments()) + " <" + setting.type().argumentName() + ">";
        List<Component> lines = new ArrayList<>();
        lines.add(Text.labeled(setting.label(), currentValueText(setting, settings)));
        lines.add(Component.textOfChildren(Text.muted("Usage: "), Text.suggestCommand(Text.value(usage),
                "/casualmode set " + String.join(" ", setting.segments()) + " ")));
        lines.add(Text.muted(setting.type().expectation()));
        hint(setting, settings).ifPresent(lines::add);
        send(source, Text.lines(lines));
        return Command.SINGLE_SUCCESS;
    }

    private <T> int setValue(CommandSourceStack source, Setting<T> setting, String input) {
        ParseResult<T> parsed = setting.parseInput(input);
        if (!parsed.isOk()) {
            String headline = setting.type() instanceof BooleanType
                    ? "Invalid value for " + setting.key() + "."
                    : "Invalid " + setting.key() + ".";
            send(source, Text.error(headline, parsed.problem()));
            return 0;
        }

        ChangeResult<T> result = manager.change(setting, parsed.value());
        switch (result.status()) {
            case CONFIG_UNREADABLE -> {
                send(source, configUnreadable());
                return 0;
            }
            case UNCHANGED -> send(source, Component.textOfChildren(
                    Text.muted(setting.label() + " is already "), valueText(setting, result.previous()), Text.muted(".")));
            case CHANGED -> send(source, changeLine(setting.label(),
                    valueText(setting, result.previous()), valueText(setting, result.requested())));
        }
        hint(setting, manager.settings()).ifPresent(message -> send(source, message));
        return Command.SINGLE_SUCCESS;
    }

    private static Component changeLine(String label, Component before, Component after) {
        return Component.textOfChildren(
                Component.text(label + ": ", Text.LABEL), before, Component.text(" → ", Text.MUTED), after);
    }

    private static <T> Component currentValueText(Setting<T> setting, SettingsSnapshot settings) {
        return valueText(setting, settings.get(setting));
    }

    private static <T> Component valueText(Setting<T> setting, T value) {
        if (value instanceof Boolean on && setting.type() == BooleanType.ON_OFF) {
            return Text.onOff(on);
        }
        if (value instanceof NamedTextColor color) {
            return Component.text(ColorType.name(color), color);
        }
        return Text.value(setting.describe(value));
    }

    private static Optional<Component> hint(Setting<?> setting, SettingsSnapshot settings) {
        boolean petSetting = setting == Settings.PETS_USE_OWN_SETTINGS || Settings.PET.contains(setting);
        if (petSetting && !settings.get(Settings.PETS_ENABLED)) {
            return note("Pets is OFF, so this has no effect until it is turned on:", "/casualmode toggle pets");
        }
        if (Settings.PET.contains(setting) && !settings.get(Settings.PETS_USE_OWN_SETTINGS)) {
            return note("Pets use the player settings right now, so this has no effect until they use their own:",
                    "/casualmode set pets use-own-settings true");
        }
        if (setting == Settings.COLOR_NAME_COLOR && !settings.get(Settings.COLOR_NAME_ENABLED)) {
            return note("Color Name is OFF, so names are not colored until it is turned on:",
                    "/casualmode toggle color-name");
        }

        ProfileSettings profile = Settings.PLAYER.contains(setting) ? Settings.PLAYER
                : Settings.PET.contains(setting) ? Settings.PET : null;
        if (profile == null) {
            return Optional.empty();
        }
        boolean forPets = profile == Settings.PET;

        boolean damageSetting = profile.isDamageReductionSetting(setting) && setting != profile.damageReductionEnabled();
        if (damageSetting && !settings.get(profile.damageReductionEnabled())) {
            return forPets
                    ? note("Pet damage reduction is OFF, so this has no effect until it is turned on:",
                    "/casualmode set pets settings damage-reduction enabled true")
                    : note("Damage Reduction is OFF, so this has no effect until it is turned on:",
                    "/casualmode toggle damage-reduction");
        }
        boolean healSetting = setting == profile.healthThreshold() || setting == profile.healAmount()
                || setting == profile.interval();
        if (healSetting && !settings.get(profile.autoHealEnabled())) {
            return forPets
                    ? note("Pet AutoHeal is OFF, so this has no effect until it is turned on:",
                    "/casualmode set pets settings auto-heal enabled true")
                    : note("AutoHeal is OFF, so this has no effect until it is turned on:",
                    "/casualmode toggle auto-heal");
        }
        Optional<DamageCategory> category = profile.categoryOf(setting);
        if (category.isPresent() && setting == profile.categoryPercent(category.get())) {
            Optional<Setting<Boolean>> enabled = profile.categoryEnabled(category.get());
            if (enabled.isPresent() && !settings.get(enabled.get())) {
                return note(category.get().displayName() + " is OFF, so this percentage is not used until it is turned on:",
                        "/casualmode set " + String.join(" ", enabled.get().segments()) + " true");
            }
        }
        return Optional.empty();
    }

    private static Optional<Component> note(String explanation, String command) {
        return Optional.of(Component.textOfChildren(Text.muted(explanation + " "), Text.command(command)));
    }

    private <T> CompletableFuture<Suggestions> suggestValues(Setting<T> setting, SuggestionsBuilder builder) {
        String typed = builder.getRemainingLowerCase();
        String current = setting.type().toInput(manager.settings().get(setting));
        boolean currentSuggested = false;
        for (ValueSuggestion suggestion : setting.type().suggestions()) {
            if (!suggestion.text().toLowerCase(Locale.ROOT).startsWith(typed)) {
                continue;
            }
            boolean isCurrent = suggestion.text().equalsIgnoreCase(current);
            currentSuggested |= isCurrent;
            addSuggestion(builder, suggestion.text(), tooltip(setting, suggestion, isCurrent));
        }
        if (!currentSuggested && current.toLowerCase(Locale.ROOT).startsWith(typed)) {
            addSuggestion(builder, current, Text.muted("Current value"));
        }
        return builder.buildFuture();
    }

    private static @Nullable Component tooltip(Setting<?> setting, ValueSuggestion suggestion, boolean isCurrent) {
        Component tooltip = null;
        if (setting.type() instanceof ColorType) {
            NamedTextColor color = NamedTextColor.NAMES.value(suggestion.text().toLowerCase(Locale.ROOT));
            tooltip = Component.text(suggestion.text(), color);
        } else if (suggestion.tooltip() != null) {
            tooltip = Text.muted(suggestion.tooltip());
        }
        if (isCurrent) {
            tooltip = tooltip == null ? Text.muted("Current value") : tooltip.append(Text.muted(" (current value)"));
        }
        return tooltip;
    }

    private static void addSuggestion(SuggestionsBuilder builder, String text, @Nullable Component tooltip) {
        Message message = tooltip == null ? null : MessageComponentSerializer.message().serialize(tooltip);
        if (text.matches("\\d{1,9}")) {
            builder.suggest(Integer.parseInt(text), message);
        } else {
            builder.suggest(text, message);
        }
    }

    private LiteralArgumentBuilder<CommandSourceStack> playerListBranch() {
        return Commands.literal("playerlist")
                .executes(context -> showPlayerListUsage(context.getSource()))
                .then(Commands.literal("add")
                        .requires(this::isAdmin)
                        .executes(context -> showUsage(context.getSource(),
                                "/casualmode playerlist add <player>", "Adds a player to CasualMode."))
                        .then(Commands.argument("player", StringArgumentType.greedyString())
                                .suggests(this::suggestPlayersToAdd)
                                .executes(context -> addPlayer(context.getSource(),
                                        StringArgumentType.getString(context, "player")))))
                .then(Commands.literal("remove")
                        .requires(this::isAdmin)
                        .executes(context -> showUsage(context.getSource(),
                                "/casualmode playerlist remove <player>", "Removes a player from CasualMode."))
                        .then(Commands.argument("player", StringArgumentType.greedyString())
                                .suggests(this::suggestListedPlayers)
                                .executes(context -> removePlayer(context.getSource(),
                                        StringArgumentType.getString(context, "player")))))
                .then(Commands.literal("get")
                        .executes(context -> showPlayerList(context.getSource())));
    }

    private int showPlayerListUsage(CommandSourceStack source) {
        List<Component> lines = new ArrayList<>();
        lines.add(Text.title("CasualMode Playerlist"));
        lines.add(Component.empty());
        if (isAdmin(source)) {
            addHelpEntry(lines, "/casualmode playerlist add <player>", "/casualmode playerlist add ",
                    "Add a player to CasualMode.");
            addHelpEntry(lines, "/casualmode playerlist remove <player>", "/casualmode playerlist remove ",
                    "Remove a player from CasualMode.");
        }
        addHelpEntry(lines, "/casualmode playerlist get", "/casualmode playerlist get", "Show the CasualMode playerlist.");
        send(source, Text.lines(lines));
        return Command.SINGLE_SUCCESS;
    }

    private static int showUsage(CommandSourceStack source, String usage, String description) {
        String command = usage.substring(0, usage.indexOf('<'));
        send(source, Text.lines(List.of(
                Component.textOfChildren(Text.muted("Usage: "), Text.suggestCommand(Text.value(usage), command)),
                Text.muted(description))));
        return Command.SINGLE_SUCCESS;
    }

    private int showPlayerList(CommandSourceStack source) {
        List<String> names = manager.playerList().names();
        List<Component> lines = new ArrayList<>();
        lines.add(Text.title("CasualMode Playerlist"));
        lines.add(Component.empty());
        if (names.isEmpty()) {
            lines.add(Text.muted("No players are currently listed."));
        } else {
            for (int i = 0; i < names.size(); i++) {
                lines.add(Component.textOfChildren(Text.muted((i + 1) + ". "), Component.text(names.get(i), Text.LABEL)));
            }
            lines.add(Component.empty());
            lines.add(Text.muted("Total: " + names.size() + (names.size() == 1 ? " player" : " players")));
        }
        send(source, Text.lines(lines));
        return Command.SINGLE_SUCCESS;
    }

    private int addPlayer(CommandSourceStack source, String input) {
        String name = input.strip();
        if (!PlayerNames.isValid(name)) {
            send(source, Text.error("Invalid player name.", PlayerNames.RULES));
            return 0;
        }
        if (!manager.canSaveConfig()) {
            send(source, configUnreadable());
            return 0;
        }

        AddResult result = manager.addPlayer(name);
        switch (result.status()) {
            case ADDED -> {
                send(source, Text.success("Added " + result.name() + " to the CasualMode playerlist."));
                if (Bukkit.getPlayerExact(result.name()) == null) {
                    send(source, Text.muted("CasualMode applies as soon as they join."));
                }
            }
            case ALREADY_LISTED -> send(source, Text.muted(result.name() + " is already in the CasualMode playerlist."));
            case RELINKED -> send(source, Text.lines(List.of(
                    Text.success(result.name() + " now receives CasualMode."),
                    Text.muted("This name was linked to a different account that used it before."
                            + " That account no longer receives CasualMode from this entry."))));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int removePlayer(CommandSourceStack source, String input) {
        String name = input.strip();
        if (!manager.canSaveConfig()) {
            send(source, configUnreadable());
            return 0;
        }
        RemovedEntry removed = manager.removePlayer(name);
        if (removed == null) {
            send(source, Text.error(name + " is not in the CasualMode playerlist.",
                    "Use /casualmode playerlist get to see who is listed."));
            return 0;
        }
        send(source, Text.success("Removed " + removed.name() + " from the CasualMode playerlist."));
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggestPlayersToAdd(CommandContext<CommandSourceStack> context,
                                                               SuggestionsBuilder builder) {
        String typed = builder.getRemainingLowerCase();
        CommandSender sender = context.getSource().getSender();
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean hidden = sender instanceof Player viewer && !viewer.canSee(player);
            if (!hidden && !manager.playerList().isCasual(player.getUniqueId())
                    && player.getName().toLowerCase(Locale.ROOT).startsWith(typed)) {
                builder.suggest(player.getName());
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestListedPlayers(CommandContext<CommandSourceStack> context,
                                                                SuggestionsBuilder builder) {
        String typed = builder.getRemainingLowerCase();
        for (String name : manager.playerList().names()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(typed)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    private static final class SettingNode {
        private final List<String> path;
        private final Map<String, SettingNode> children = new LinkedHashMap<>();
        private @Nullable Setting<?> setting;

        private SettingNode(List<String> path) {
            this.path = path;
        }

        static SettingNode buildTree() {
            SettingNode root = new SettingNode(List.of());
            for (Setting<?> setting : Settings.ALL) {
                if (setting.isMainToggle()) {
                    continue;
                }
                SettingNode node = root;
                for (String segment : setting.segments()) {
                    List<String> childPath = new ArrayList<>(node.path);
                    childPath.add(segment);
                    node = node.children.computeIfAbsent(segment, key -> new SettingNode(List.copyOf(childPath)));
                }
                node.setting = setting;
            }
            return root;
        }

        String name() {
            return path.getLast();
        }

        String command() {
            return "/casualmode set " + String.join(" ", path);
        }

        String label() {
            return path.stream().map(Settings::displayName).collect(Collectors.joining(" › "));
        }
    }
}

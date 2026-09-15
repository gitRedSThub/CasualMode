package me.redst.casualMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import me.redst.casualMode.config.AsyncFileWriter;
import me.redst.casualMode.config.ConfigFiles;
import me.redst.casualMode.config.Setting;
import me.redst.casualMode.config.SettingsSnapshot;
import me.redst.casualMode.heal.AutoHealService;
import me.redst.casualMode.player.CasualPlayerList;
import me.redst.casualMode.player.CasualPlayerList.AddResult;
import me.redst.casualMode.player.CasualPlayerList.Changes;
import me.redst.casualMode.player.CasualPlayerList.JoinResult;
import me.redst.casualMode.player.CasualPlayerList.JoinStatus;
import me.redst.casualMode.player.CasualPlayerList.RemovedEntry;
import me.redst.casualMode.player.ColorNameService;
import me.redst.casualMode.protection.ActiveSettings;
import me.redst.casualMode.protection.ProtectionProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public final class CasualModeManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final AsyncFileWriter fileWriter;
    private final ConfigFiles files;
    private final CasualPlayerList playerList = new CasualPlayerList();
    private final ColorNameService colorNames = new ColorNameService();
    private final AutoHealService autoHeal = new AutoHealService(this::protectionFor);

    private volatile SettingsSnapshot settings = SettingsSnapshot.defaults();
    private volatile ActiveSettings active = ActiveSettings.from(settings);

    public CasualModeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.fileWriter = new AsyncFileWriter(logger);
        this.files = new ConfigFiles(plugin.getDataFolder().toPath(), readBundledConfig(plugin), fileWriter);
    }

    public void enable() {
        ReloadResult result = loadFromDisk();
        if (result.success()) {
            logProblems(result.problems());
            logger.info("Loaded config.yml with " + playerList.size() + " player(s) in the casual playerlist.");
        } else {
            logger.severe(result.error() + " CasualMode starts with the default settings and an empty playerlist."
                    + " Fix the file, then use /casualmode reload.");
            logger.severe(result.details());
        }
        autoHeal.start(plugin);
    }

    public void disable() {
        autoHeal.stop();
        colorNames.restoreAll(Bukkit.getOnlinePlayers());
        fileWriter.close();
    }

    public SettingsSnapshot settings() {
        return settings;
    }

    public CasualPlayerList playerList() {
        return playerList;
    }

    public boolean canSaveConfig() {
        return files.canSaveConfig();
    }

    public @Nullable ProtectionProfile protectionFor(LivingEntity entity) {
        ActiveSettings current = active;
        if (entity instanceof Player player) {
            return playerList.isCasual(player.getUniqueId()) ? current.player() : null;
        }
        if (entity instanceof Tameable pet && current.petsEnabled() && pet.isTamed()) {
            UUID owner = pet.getOwnerUniqueId();
            return owner != null && playerList.isCasual(owner) ? current.pet() : null;
        }
        return null;
    }

    public AutoHealService autoHeal() {
        return autoHeal;
    }

    public <T> ChangeResult<T> change(Setting<T> setting, T value) {
        T previous = settings.get(setting);
        if (!files.canSaveConfig()) {
            return new ChangeResult<>(ChangeStatus.CONFIG_UNREADABLE, previous, value);
        }
        if (previous.equals(value)) {
            files.saveConfig(settings, playerList.names());
            return new ChangeResult<>(ChangeStatus.UNCHANGED, previous, value);
        }
        applySettings(settings.with(setting, value));
        files.saveConfig(settings, playerList.names());
        refreshOnlinePlayers();
        return new ChangeResult<>(ChangeStatus.CHANGED, previous, value);
    }

    public AddResult addPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        AddResult result = online != null
                ? playerList.add(online.getName(), online.getUniqueId())
                : playerList.add(name, null);
        save(result.changes());
        if (online != null) {
            refreshPlayer(online);
        }
        return result;
    }

    public @Nullable RemovedEntry removePlayer(String name) {
        RemovedEntry removed = playerList.remove(name);
        if (removed == null) {
            return null;
        }
        save(new Changes(true, removed.accountId() != null));
        Player player = removed.accountId() != null
                ? Bukkit.getPlayer(removed.accountId())
                : Bukkit.getPlayerExact(removed.name());
        if (player != null) {
            refreshPlayer(player);
        }
        return removed;
    }

    public ReloadResult reload() {
        ReloadResult result = loadFromDisk();
        if (result.success()) {
            logProblems(result.problems());
        } else {
            logger.severe(result.error() + " The previous settings stay active until the file is fixed.");
            logger.severe(result.details());
        }
        return result;
    }

    public void handleJoin(Player player) {
        JoinResult result = playerList.join(player.getUniqueId(), player.getName());
        logJoin(player, result);
        save(result.changes());
        refreshPlayer(player);
    }

    public void handleQuit(Player player) {
        colorNames.forget(player.getUniqueId());
        autoHeal.forget(player.getUniqueId());
    }

    private ReloadResult loadFromDisk() {
        ConfigFiles.ConfigRead read = files.readConfig();
        if (read.config() == null) {
            return new ReloadResult(false, read.error(), read.details(), List.of());
        }

        List<String> problems = new ArrayList<>(read.config().problems());
        applySettings(read.config().settings());

        Map<UUID, String> knownLinks = files.readAccountLinks(problems);
        boolean linksChanged = playerList.load(read.config().playerNames(), knownLinks);
        boolean namesChanged = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            JoinResult join = playerList.join(player.getUniqueId(), player.getName());
            logJoin(player, join);
            namesChanged |= join.changes().names();
            linksChanged |= join.changes().links();
        }
        save(new Changes(namesChanged, linksChanged));

        refreshOnlinePlayers();
        return new ReloadResult(true, null, null, List.copyOf(problems));
    }

    private void applySettings(SettingsSnapshot snapshot) {
        settings = snapshot;
        active = ActiveSettings.from(snapshot);
        autoHeal.settingsChanged();
    }

    private void save(Changes changes) {
        if (changes.names()) {
            files.saveConfig(settings, playerList.names());
        }
        if (changes.links()) {
            files.saveAccountLinks(playerList.links());
        }
    }

    private void refreshOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayer(player);
        }
    }

    private void refreshPlayer(Player player) {
        ActiveSettings current = active;
        boolean casual = playerList.isCasual(player.getUniqueId());
        colorNames.update(player, casual && current.colorNameEnabled() ? current.nameColor() : null);
        autoHeal.updatePlayer(player, casual ? current.player().heal() : null);
    }

    private void logJoin(Player player, JoinResult result) {
        if (result.status() == JoinStatus.NAME_LINKED_TO_OTHER_ACCOUNT) {
            logger.warning(player.getName() + " is in the casual playerlist as \"" + result.conflictingEntry()
                    + "\", but that entry belongs to the account that used this name before, so CasualMode was not"
                    + " applied. If this is the right player, use /casualmode playerlist add " + player.getName()
                    + " while they are online.");
        } else if (result.previousName() != null && !result.previousName().equalsIgnoreCase(player.getName())) {
            logger.info("Casual player " + result.previousName() + " changed their name to " + player.getName()
                    + ". The casual playerlist was updated.");
        }
    }

    private void logProblems(List<String> problems) {
        if (problems.isEmpty()) {
            return;
        }
        logger.warning("Found " + problems.size() + " problem(s) in the CasualMode configuration:");
        for (String problem : problems) {
            logger.warning(" - " + problem);
        }
    }

    private static String readBundledConfig(JavaPlugin plugin) {
        try (InputStream stream = plugin.getResource(ConfigFiles.CONFIG_FILE_NAME)) {
            if (stream == null) {
                throw new IllegalStateException("config.yml is missing from the plugin jar");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public enum ChangeStatus {
        CHANGED,
        UNCHANGED,
        CONFIG_UNREADABLE
    }

    public record ChangeResult<T>(ChangeStatus status, T previous, T requested) {
    }

    public record ReloadResult(boolean success, @Nullable String error, @Nullable String details,
                               List<String> problems) {
    }
}

package me.redst.casualMode;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.redst.casualMode.command.CasualModeCommand;
import me.redst.casualMode.damage.DamageListener;
import me.redst.casualMode.player.PlayerConnectionListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CasualMode extends JavaPlugin {

    private CasualModeManager manager;

    @Override
    public void onEnable() {
        manager = new CasualModeManager(this);
        manager.enable();

        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(new DamageListener(manager), this);
        plugins.registerEvents(new PlayerConnectionListener(manager), this);

        CasualModeCommand command = new CasualModeCommand(manager);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(command.build(), getPluginMeta().getDescription()));
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.disable();
        }
    }
}

package me.redst.casualMode.heal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import me.redst.casualMode.config.DecimalType;
import me.redst.casualMode.protection.HealProfile;
import me.redst.casualMode.protection.ProtectionProfile;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public final class AutoHealService implements Runnable {

    private static final long NEVER = Long.MIN_VALUE / 4;
    private static final long LONGEST_INTERVAL_TICKS = Math.round(DecimalType.INTERVAL.max() * 20);
    private static final long FORGET_OLD_HEALS_EVERY_TICKS = 1200;

    private final Function<LivingEntity, @Nullable ProtectionProfile> protectionLookup;
    private final Map<UUID, Watch> watches = new HashMap<>();
    private final Map<UUID, Long> lastHealTicks = new HashMap<>();
    private long nextDueTick = Long.MAX_VALUE;
    private long nextCleanupTick;
    private @Nullable BukkitTask task;

    public AutoHealService(Function<LivingEntity, @Nullable ProtectionProfile> protectionLookup) {
        this.protectionLookup = protectionLookup;
    }

    public void start(Plugin plugin) {
        if (task == null) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, 1L, 1L);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        watches.clear();
        lastHealTicks.clear();
        nextDueTick = Long.MAX_VALUE;
    }

    public void updatePlayer(Player player, @Nullable HealProfile heal) {
        UUID id = player.getUniqueId();
        if (heal == null || !heal.enabled()) {
            watches.remove(id);
            return;
        }
        Watch watch = watches.computeIfAbsent(id, key -> new Watch(key, true));
        requestCheck(watch, heal);
    }

    public void forget(UUID entityId) {
        watches.remove(entityId);
    }

    public void onDamaged(LivingEntity entity) {
        Watch watch = watches.get(entity.getUniqueId());
        if (watch == null && entity instanceof Player) {
            return;
        }
        ProtectionProfile protection = protectionLookup.apply(entity);
        if (protection == null || !protection.heal().enabled()) {
            return;
        }
        if (watch == null) {
            watch = new Watch(entity.getUniqueId(), false);
            watches.put(watch.entityId, watch);
        }
        requestCheck(watch, protection.heal());
    }

    public void settingsChanged() {
        long soon = Bukkit.getCurrentTick() + 1L;
        for (Watch watch : watches.values()) {
            watch.nextCheckTick = Math.min(watch.nextCheckTick, soon);
        }
        nextDueTick = Math.min(nextDueTick, soon);
    }

    @Override
    public void run() {
        long now = Bukkit.getCurrentTick();
        if (now >= nextCleanupTick) {
            nextCleanupTick = now + FORGET_OLD_HEALS_EVERY_TICKS;
            lastHealTicks.values().removeIf(tick -> now - tick >= LONGEST_INTERVAL_TICKS);
        }
        if (now < nextDueTick) {
            return;
        }

        List<Watch> due = new ArrayList<>();
        for (Watch watch : watches.values()) {
            if (watch.nextCheckTick <= now) {
                due.add(watch);
            }
        }
        for (Watch watch : due) {
            if (!check(watch, now)) {
                watches.remove(watch.entityId, watch);
            }
        }

        long nextDue = Long.MAX_VALUE;
        for (Watch watch : watches.values()) {
            nextDue = Math.min(nextDue, watch.nextCheckTick);
        }
        nextDueTick = nextDue;
    }

    private boolean check(Watch watch, long now) {
        LivingEntity entity = find(watch);
        if (entity == null) {
            return false;
        }
        ProtectionProfile protection = protectionLookup.apply(entity);
        if (protection == null || !protection.heal().enabled()) {
            return false;
        }
        HealProfile heal = protection.heal();

        long allowedAt = lastHealTick(watch.entityId) + heal.intervalTicks();
        if (now < allowedAt) {
            watch.nextCheckTick = allowedAt;
            return true;
        }

        watch.nextCheckTick = now + heal.intervalTicks();
        if (needsHealing(entity, heal)) {
            lastHealTicks.put(watch.entityId, now);
            entity.heal(heal.healAmount(), EntityRegainHealthEvent.RegainReason.CUSTOM);
            return true;
        }
        return watch.player;
    }

    private void requestCheck(Watch watch, HealProfile heal) {
        long earliest = Math.max(Bukkit.getCurrentTick() + 1L, lastHealTick(watch.entityId) + heal.intervalTicks());
        if (earliest < watch.nextCheckTick) {
            watch.nextCheckTick = earliest;
            nextDueTick = Math.min(nextDueTick, earliest);
        }
    }

    private long lastHealTick(UUID entityId) {
        return lastHealTicks.getOrDefault(entityId, NEVER);
    }

    private static @Nullable LivingEntity find(Watch watch) {
        if (watch.player) {
            return Bukkit.getPlayer(watch.entityId);
        }
        Entity entity = Bukkit.getEntity(watch.entityId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static boolean needsHealing(LivingEntity entity, HealProfile heal) {
        if (entity.isDead() || entity.getHealth() <= 0) {
            return false;
        }
        if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        double health = entity.getHealth();
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        boolean belowMaximum = maxHealth == null || health < maxHealth.getValue();
        return belowMaximum && health < heal.healthThreshold();
    }

    private static final class Watch {
        private final UUID entityId;
        private final boolean player;
        private long nextCheckTick = Long.MAX_VALUE;

        private Watch(UUID entityId, boolean player) {
            this.entityId = entityId;
            this.player = player;
        }
    }
}

package me.redst.casualMode.damage;

import me.redst.casualMode.CasualModeManager;
import me.redst.casualMode.protection.DamageProfile;
import me.redst.casualMode.protection.ProtectionProfile;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class DamageListener implements Listener {

    private final CasualModeManager manager;

    public DamageListener(CasualModeManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void reduceDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        ProtectionProfile protection = manager.protectionFor(victim);
        if (protection == null || !protection.damage().enabled()) {
            return;
        }
        double damage = event.getDamage();
        if (!(damage > 0)) {
            return;
        }
        DamageCategory category = DamageClassifier.classify(DamageFactsReader.read(victim, event));
        if (category == null) {
            return;
        }
        DamageProfile damageProfile = protection.damage();
        double receivedPercent = damageProfile.receivedPercent(category);
        if (receivedPercent >= 100.0) {
            return;
        }
        event.setDamage(Math.max(0.0, damage * receivedPercent / 100.0));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void notifyAutoHeal(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity victim && event.getFinalDamage() > 0) {
            manager.autoHeal().onDamaged(victim);
        }
    }
}

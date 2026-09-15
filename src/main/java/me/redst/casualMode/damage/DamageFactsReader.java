package me.redst.casualMode.damage;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.tag.DamageTypeTags;
import org.jetbrains.annotations.Nullable;

public final class DamageFactsReader {

    private static final Set<NamespacedKey> MAGIC_TYPES = Set.of(
            NamespacedKey.minecraft("magic"),
            NamespacedKey.minecraft("indirect_magic"),
            NamespacedKey.minecraft("wither"),
            NamespacedKey.minecraft("dragon_breath"));

    private DamageFactsReader() {
    }

    public static DamageFacts read(LivingEntity victim, EntityDamageEvent event) {
        DamageSource source = event.getDamageSource();
        DamageType type = source.getDamageType();
        return new DamageFacts(
                DamageTypeTags.BYPASSES_INVULNERABILITY.isTagged(type),
                attacker(victim, source),
                typeCategories(type),
                causeCategory(event.getCause()));
    }

    private static DamageFacts.Attacker attacker(LivingEntity victim, DamageSource source) {
        Entity responsible = source.getCausingEntity() != null ? source.getCausingEntity() : source.getDirectEntity();
        if (responsible == null) {
            return DamageFacts.Attacker.NONE;
        }
        if (responsible instanceof Player) {
            return responsible.getUniqueId().equals(victim.getUniqueId())
                    ? DamageFacts.Attacker.SELF
                    : DamageFacts.Attacker.OTHER_PLAYER;
        }
        return responsible instanceof Mob ? DamageFacts.Attacker.MOB : DamageFacts.Attacker.OTHER;
    }

    private static Set<DamageCategory> typeCategories(DamageType type) {
        Set<DamageCategory> categories = EnumSet.noneOf(DamageCategory.class);
        if (DamageTypeTags.IS_PROJECTILE.isTagged(type)) {
            categories.add(DamageCategory.PROJECTILES);
        }
        if (DamageTypeTags.IS_EXPLOSION.isTagged(type)) {
            categories.add(DamageCategory.EXPLOSIVES);
        }
        if (MAGIC_TYPES.contains(type.getKey())) {
            categories.add(DamageCategory.MAGIC);
        }
        if (DamageTypeTags.IS_FALL.isTagged(type)) {
            categories.add(DamageCategory.FALL);
        }
        return categories;
    }

    private static @Nullable DamageCategory causeCategory(DamageCause cause) {
        return switch (cause) {
            case PROJECTILE -> DamageCategory.PROJECTILES;
            case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> DamageCategory.EXPLOSIVES;
            case MAGIC, POISON, WITHER -> DamageCategory.MAGIC;
            case FALL -> DamageCategory.FALL;
            default -> null;
        };
    }
}

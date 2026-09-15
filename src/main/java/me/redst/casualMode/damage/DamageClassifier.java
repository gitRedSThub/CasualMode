package me.redst.casualMode.damage;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class DamageClassifier {

    private static final List<DamageCategory> TYPE_PRIORITY = List.of(
            DamageCategory.PROJECTILES,
            DamageCategory.EXPLOSIVES,
            DamageCategory.MAGIC,
            DamageCategory.FALL);

    private DamageClassifier() {
    }

    public static @Nullable DamageCategory classify(DamageFacts facts) {
        if (facts.ignoresProtection()) {
            return null;
        }
        if (facts.attacker() == DamageFacts.Attacker.OTHER_PLAYER) {
            return DamageCategory.PLAYERS;
        }
        for (DamageCategory category : TYPE_PRIORITY) {
            if (facts.typeCategories().contains(category)) {
                return category;
            }
        }
        if (facts.typeCategories().isEmpty() && facts.causeCategory() != null) {
            return facts.causeCategory();
        }
        if (facts.attacker() == DamageFacts.Attacker.MOB) {
            return DamageCategory.MOBS;
        }
        return null;
    }
}

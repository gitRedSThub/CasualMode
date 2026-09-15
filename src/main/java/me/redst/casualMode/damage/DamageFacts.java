package me.redst.casualMode.damage;

import java.util.Set;
import org.jetbrains.annotations.Nullable;

public record DamageFacts(boolean ignoresProtection, Attacker attacker, Set<DamageCategory> typeCategories,
                          @Nullable DamageCategory causeCategory) {

    public DamageFacts {
        typeCategories = Set.copyOf(typeCategories);
    }

    public enum Attacker {
        NONE,
        SELF,
        OTHER_PLAYER,
        MOB,
        OTHER
    }
}

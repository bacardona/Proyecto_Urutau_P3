package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;
import com.pokemon.battle.service.TypeChartService;

/**
 * How much damage the shield stops. It never stops everything: part of the hit
 * always gets through, so nobody can win by hiding behind the shield.
 *
 * - normal hit: stops 80%
 * - super effective hit: stops only 65%
 * - not very effective hit: stops 90%
 * - ultimate: stops only 50%
 */
final class ShieldRules {

    static final double NORMAL_BLOCK = 0.80;
    static final double SUPER_EFFECTIVE_BLOCK = 0.65;
    static final double NOT_EFFECTIVE_BLOCK = 0.90;
    static final double ULTIMATE_BLOCK = 0.50;

    private static final TypeChartService TYPE_CHART = new TypeChartService();

    private ShieldRules() {
    }

    /**
     * True when the defender holds the shield. The shield is a bubble around the whole
     * Pokemon, so it covers every side: front, back and from above.
     */
    static boolean blocks(Fighter defender, Fighter attacker) {
        return defender != null && attacker != null && defender.isBlocking();
    }

    /** Fraction of the damage that the shield stops (0.5 to 0.9). */
    static double blockedFraction(String moveId, Pokemon target, boolean ultimate) {
        if (ultimate) return ULTIMATE_BLOCK;
        double multiplier = target == null ? 1 : TYPE_CHART.multiplier(typeOf(moveId), target);
        if (multiplier > 1) return SUPER_EFFECTIVE_BLOCK;
        if (multiplier < 1) return NOT_EFFECTIVE_BLOCK;
        return NORMAL_BLOCK;
    }

    /** Damage that still gets through the shield (at least 1). */
    static int chipDamage(int fullDamage, double blockedFraction) {
        if (fullDamage <= 0) return 0;
        return Math.max(1, (int) Math.round(fullDamage * (1 - blockedFraction)));
    }

    /** Elemental type of each free-fight move (the physical ones are Normal). */
    static Type typeOf(String moveId) {
        if (moveId == null) return Type.NORMAL;
        return switch (moveId) {
            case "EMBER", "FIRE_BLAST", "INFERNO_BURST" -> Type.FIRE;
            case "WATER_GUN", "HYDRO_PUMP" -> Type.WATER;
            case "VINE_WHIP", "LEAF_STORM" -> Type.GRASS;
            case "THUNDER_SHOCK" -> Type.ELECTRIC;
            case "BITE" -> Type.DARK;
            case "FLY" -> Type.FLYING;
            default -> Type.NORMAL;
        };
    }
}

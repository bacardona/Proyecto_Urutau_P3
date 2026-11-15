package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;

/** Checks the partial shield numbers. Run with main. */
public class ShieldRulesCheck {
    public static void main(String[] args) {
        Pokemon grass = new Pokemon(1, "Bulbasaur", 45, 49, 49, 65, 65, 45, Type.GRASS, Type.POISON, java.util.List.of());
        check(ShieldRules.blockedFraction("TACKLE", grass, false) == 0.80, "Normal hit must stop 80%");
        check(ShieldRules.blockedFraction("EMBER", grass, false) == 0.65, "Super effective must stop 65%");
        check(ShieldRules.blockedFraction("WATER_GUN", grass, false) == 0.90, "Not very effective must stop 90%");
        check(ShieldRules.blockedFraction("EMBER", grass, true) == 0.50, "Ultimate must stop 50%");
        check(ShieldRules.chipDamage(10, 0.80) == 2, "20% of 10 is 2");
        check(ShieldRules.chipDamage(3, 0.90) == 1, "At least 1 damage gets through");
        check(ShieldRules.chipDamage(0, 0.80) == 0, "No damage stays no damage");
        System.out.println("PASS shield rules");
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}

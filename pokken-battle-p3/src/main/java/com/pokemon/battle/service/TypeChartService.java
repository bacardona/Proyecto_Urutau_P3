package com.pokemon.battle.service;

import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;
import com.pokemon.battle.model.TypeEffectiveness;

import java.util.EnumMap;
import java.util.Map;

/**
 * Type effectiveness table. Only the rules that differ from 1.0 are stored.
 */
public class TypeChartService implements TypeEffectiveness {

    private final Map<Type, Map<Type, Double>> chart = new EnumMap<>(Type.class);

    /**
     * Builds the standard type chart.
     */
    public TypeChartService() {
        rules(Type.NORMAL, 0.5, Type.ROCK, Type.STEEL);
        rules(Type.NORMAL, 0, Type.GHOST);

        rules(Type.FIRE, 2, Type.GRASS, Type.ICE, Type.BUG, Type.STEEL);
        rules(Type.FIRE, 0.5, Type.FIRE, Type.WATER, Type.ROCK, Type.DRAGON);

        rules(Type.WATER, 2, Type.FIRE, Type.GROUND, Type.ROCK);
        rules(Type.WATER, 0.5, Type.WATER, Type.GRASS, Type.DRAGON);

        rules(Type.ELECTRIC, 2, Type.WATER, Type.FLYING);
        rules(Type.ELECTRIC, 0.5, Type.ELECTRIC, Type.GRASS, Type.DRAGON);
        rules(Type.ELECTRIC, 0, Type.GROUND);

        rules(Type.GRASS, 2, Type.WATER, Type.GROUND, Type.ROCK);
        rules(Type.GRASS, 0.5, Type.FIRE, Type.GRASS, Type.POISON, Type.FLYING, Type.BUG,
                Type.DRAGON, Type.STEEL);

        rules(Type.ICE, 2, Type.GRASS, Type.GROUND, Type.FLYING, Type.DRAGON);
        rules(Type.ICE, 0.5, Type.FIRE, Type.WATER, Type.ICE, Type.STEEL);

        rules(Type.FIGHTING, 2, Type.NORMAL, Type.ICE, Type.ROCK, Type.DARK, Type.STEEL);
        rules(Type.FIGHTING, 0.5, Type.POISON, Type.FLYING, Type.PSYCHIC, Type.BUG, Type.FAIRY);
        rules(Type.FIGHTING, 0, Type.GHOST);

        rules(Type.POISON, 2, Type.GRASS, Type.FAIRY);
        rules(Type.POISON, 0.5, Type.POISON, Type.GROUND, Type.ROCK, Type.GHOST);
        rules(Type.POISON, 0, Type.STEEL);

        rules(Type.GROUND, 2, Type.FIRE, Type.ELECTRIC, Type.POISON, Type.ROCK, Type.STEEL);
        rules(Type.GROUND, 0.5, Type.GRASS, Type.BUG);
        rules(Type.GROUND, 0, Type.FLYING);

        rules(Type.FLYING, 2, Type.GRASS, Type.FIGHTING, Type.BUG);
        rules(Type.FLYING, 0.5, Type.ELECTRIC, Type.ROCK, Type.STEEL);

        rules(Type.PSYCHIC, 2, Type.FIGHTING, Type.POISON);
        rules(Type.PSYCHIC, 0.5, Type.PSYCHIC, Type.STEEL);
        rules(Type.PSYCHIC, 0, Type.DARK);

        rules(Type.BUG, 2, Type.GRASS, Type.PSYCHIC, Type.DARK);
        rules(Type.BUG, 0.5, Type.FIRE, Type.FIGHTING, Type.POISON, Type.FLYING, Type.GHOST,
                Type.STEEL, Type.FAIRY);

        rules(Type.ROCK, 2, Type.FIRE, Type.ICE, Type.FLYING, Type.BUG);
        rules(Type.ROCK, 0.5, Type.FIGHTING, Type.GROUND, Type.STEEL);

        rules(Type.GHOST, 2, Type.PSYCHIC, Type.GHOST);
        rules(Type.GHOST, 0.5, Type.DARK);
        rules(Type.GHOST, 0, Type.NORMAL);

        rules(Type.DRAGON, 2, Type.DRAGON);
        rules(Type.DRAGON, 0.5, Type.STEEL);
        rules(Type.DRAGON, 0, Type.FAIRY);

        rules(Type.DARK, 2, Type.PSYCHIC, Type.GHOST);
        rules(Type.DARK, 0.5, Type.FIGHTING, Type.DARK, Type.FAIRY);

        rules(Type.STEEL, 2, Type.ICE, Type.ROCK, Type.FAIRY);
        rules(Type.STEEL, 0.5, Type.FIRE, Type.WATER, Type.ELECTRIC, Type.STEEL);

        rules(Type.FAIRY, 2, Type.FIGHTING, Type.DRAGON, Type.DARK);
        rules(Type.FAIRY, 0.5, Type.FIRE, Type.POISON, Type.STEEL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double multiplier(Type attackType, Pokemon target) {
        return single(attackType, target.getPrimaryType()) * single(attackType, target.getSecondaryType());
    }

    private void rules(Type attacker, double multiplier, Type... defenders) {
        Map<Type, Double> row = chart.computeIfAbsent(attacker, t -> new EnumMap<>(Type.class));
        for (Type defender : defenders) {
            row.put(defender, multiplier);
        }
    }

    private double single(Type attacker, Type defender) {
        if (defender == null || defender == Type.NONE) {
            return 1.0;
        }
        return chart.getOrDefault(attacker, Map.of()).getOrDefault(defender, 1.0);
    }
}

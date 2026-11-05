package com.pokemon.battle.model;

/**
 * Knows how effective a move type is against a Pokemon.
 */
public interface TypeEffectiveness {

    /**
     * @param attackType type of the move
     * @param target     Pokemon that receives the move
     * @return damage multiplier (0, 0.25, 0.5, 1, 2, 4)
     */
    double multiplier(Type attackType, Pokemon target);
}

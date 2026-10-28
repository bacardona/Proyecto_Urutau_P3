package com.pokemon.battle.model;

/**
 * Source of randomness used by the battle rules.
 * Being an abstraction, tests can replace it with a predictable one.
 */
public interface RandomProvider {

    /**
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return a random integer between min and max
     */
    int nextInt(int min, int max);

    /**
     * @param min minimum value (inclusive)
     * @param max maximum value (exclusive)
     * @return a random decimal between min and max
     */
    double nextDouble(double min, double max);
}

package com.pokemon.battle.service;

import com.pokemon.battle.model.RandomProvider;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Default {@link RandomProvider} backed by {@link ThreadLocalRandom}.
 */
public class ThreadLocalRandomProvider implements RandomProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public int nextInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double nextDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }
}

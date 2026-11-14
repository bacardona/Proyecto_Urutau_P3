package com.pokemon.battle.persistence;

/**
 * Full storage of trainers: reading plus writing. Clients that only read should
 * depend on {@link TrainerReader}.
 */
public interface TrainerRepository extends TrainerReader, TrainerWriter {
}
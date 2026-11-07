package com.pokemon.battle.persistence;

/**
 * Full storage of combats: reading plus writing. Clients that only read should
 * depend on {@link CombatReader}.
 */
public interface CombatRepository extends CombatReader, CombatWriter {
}
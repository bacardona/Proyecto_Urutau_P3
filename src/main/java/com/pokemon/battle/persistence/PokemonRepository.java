package com.pokemon.battle.persistence;

/**
 * Full storage of Pokemon: reading plus writing. Clients that only read should
 * depend on {@link PokemonReader}.
 */
public interface PokemonRepository extends PokemonReader, PokemonWriter {
}

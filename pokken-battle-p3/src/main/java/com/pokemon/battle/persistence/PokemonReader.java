package com.pokemon.battle.persistence;

import com.pokemon.battle.model.Pokemon;

import java.util.List;

/**
 * Read-only access to stored Pokemon.
 */
public interface PokemonReader {

    /**
     * @return every stored Pokemon, or an empty list when there is no data
     */
    List<Pokemon> loadAll();
}

package com.pokemon.battle.persistence;

import com.pokemon.battle.model.Pokemon;

import java.util.List;

/**
 * Write access to stored Pokemon.
 */
public interface PokemonWriter {

    /**
     * Replaces the stored Pokemon.
     *
     * @param pokemon Pokemon to store
     */
    void saveAll(List<Pokemon> pokemon);
}

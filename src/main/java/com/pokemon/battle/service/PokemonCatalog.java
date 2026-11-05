package com.pokemon.battle.service;

import com.pokemon.battle.model.Pokemon;

import java.util.List;

/**
 * What the rest of the system needs from the Pokemon catalog.
 */
public interface PokemonCatalog {

    /**
     * @return every Pokemon in the catalog
     */
    List<Pokemon> listAll();

    /**
     * Finds a Pokemon by name, ignoring case.
     *
     * @param name name to search
     * @return the matching Pokemon
     */
    Pokemon findByName(String name);
}

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
     * Every Pokemon in the database, including the ones that are only for Combate Libre.
     * By default it is the same list as {@link #listAll()}.
     */
    default List<Pokemon> listAllWithFreeFight() {
        return listAll();
    }

    /**
     * Finds a Pokemon by name, ignoring case.
     *
     * @param name name to search
     * @return the matching Pokemon
     */
    Pokemon findByName(String name);
}

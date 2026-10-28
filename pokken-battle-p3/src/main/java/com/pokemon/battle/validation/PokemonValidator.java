package com.pokemon.battle.validation;

import com.pokemon.battle.exceptions.BusinessRuleException;
import com.pokemon.battle.exceptions.InvalidDataException;
import com.pokemon.battle.exceptions.ResourceNotFoundException;
import com.pokemon.battle.model.Pokemon;

/**
 * Validation rules related to Pokemon.
 */
public final class PokemonValidator {

    private PokemonValidator() {
    }

    /**
     * Checks that a name is not null or blank.
     *
     * @param name name to validate
     * @throws InvalidDataException when the name is null or blank
     */
    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidDataException("nombre", "no puede estar vacio");
        }
    }

    /**
     * Checks that a search found a Pokemon.
     *
     * @param pokemon result of the search
     * @param name    name that was searched
     * @throws ResourceNotFoundException when the Pokemon is null
     */
    public static void validatePokemonExists(Pokemon pokemon, String name) {
        if (pokemon == null) {
            throw new ResourceNotFoundException("Pokemon", name);
        }
    }

    /**
     * Checks that a Pokemon knows at least one move.
     *
     * @param pokemon Pokemon to validate
     * @throws BusinessRuleException when it has no moves
     */
    public static void validateHasMoves(Pokemon pokemon) {
        if (pokemon.getMoves().isEmpty()) {
            throw new BusinessRuleException("El Pokemon " + pokemon.getName() + " no tiene ataques");
        }
    }
}

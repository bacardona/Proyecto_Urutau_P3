package com.pokemon.battle.validation;

import com.pokemon.battle.exceptions.BusinessRuleException;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Team;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validation rules for the teams of a battle.
 */
public final class TeamValidator {

    private TeamValidator() {
    }

    /**
     * Checks that there are enough Pokemon to build a team.
     *
     * @param catalog the Pokemon stored in the database
     * @throws BusinessRuleException when there are fewer than a full team
     */
    public static void validateCatalogSize(List<Pokemon> catalog) {
        if (catalog.size() < Team.SIZE) {
            throw new BusinessRuleException("Se necesitan al menos " + Team.SIZE
                    + " Pokemon en la base de datos para armar un equipo");
        }
    }

    /**
     * Checks that a team has exactly the required number of Pokemon, without
     * repeating any, and that all of them have moves.
     *
     * @param pokemon the Pokemon of the team
     * @throws BusinessRuleException when the team is not valid
     */
    public static void validateTeam(List<Pokemon> pokemon) {
        if (pokemon == null || pokemon.size() != Team.SIZE) {
            throw new BusinessRuleException("Un equipo debe tener exactamente " + Team.SIZE + " Pokemon");
        }
        Set<String> names = new HashSet<>();
        for (Pokemon p : pokemon) {
            if (!names.add(p.getName())) {
                throw new BusinessRuleException("El equipo no puede repetir el Pokemon " + p.getName());
            }
            PokemonValidator.validateHasMoves(p);
        }
    }
}
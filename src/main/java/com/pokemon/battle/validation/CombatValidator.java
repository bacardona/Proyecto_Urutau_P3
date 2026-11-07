package com.pokemon.battle.validation;

import com.pokemon.battle.exceptions.BusinessRuleException;
import com.pokemon.battle.exceptions.InvalidDataException;
import com.pokemon.battle.model.Trainer;

import java.util.List;

/**
 * Validation rules related to trainers and combats.
 */
public final class CombatValidator {

    private CombatValidator() {
    }

    /**
     * Checks that a combat has two different trainers.
     *
     * @param winner trainer that won
     * @param loser  trainer that lost
     * @throws InvalidDataException when a trainer is missing
     * @throws BusinessRuleException when both are the same trainer
     */
    public static void validateParticipants(Trainer winner, Trainer loser) {
        if (winner == null || loser == null) {
            throw new InvalidDataException("entrenador", "no puede ser nulo");
        }
        if (winner.getId() == loser.getId()) {
            throw new BusinessRuleException("Un entrenador no puede combatir contra si mismo");
        }
    }

    /**
     * Checks that there is at least one trainer to choose from.
     *
     * @param trainers trainers loaded from the database
     * @param kind     kind of trainer, used in the message ("jugadores", "maquinas")
     * @throws BusinessRuleException when the list is empty
     */
    public static void validateNotEmpty(List<? extends Trainer> trainers, String kind) {
        if (trainers.isEmpty()) {
            throw new BusinessRuleException("No hay " + kind + " registrados en la base de datos");
        }
    }
}

package com.pokemon.battle.validation;

import com.pokemon.battle.exceptions.BusinessRuleException;
import com.pokemon.battle.exceptions.InvalidDataException;
import com.pokemon.battle.model.Trainer;

import java.util.List;

/**
 * Validation rules for creating a trainer.
 */
public final class TrainerValidator {

    private static final int MAX_NAME_LENGTH = 50;

    private TrainerValidator() {
    }

    /**
     * Checks the name of a new trainer.
     *
     * @param name the name typed by the user
     * @throws InvalidDataException when it is empty or too long
     */
    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidDataException("nombre", "no puede estar vacio");
        }
        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new InvalidDataException("nombre", "no puede tener mas de " + MAX_NAME_LENGTH + " caracteres");
        }
    }

    /**
     * Checks that no existing trainer already uses the name (ignoring case).
     *
     * @param name     the name of the new trainer
     * @param existing the trainers already stored
     * @throws BusinessRuleException when the name is already used
     */
    public static void validateNameIsFree(String name, List<? extends Trainer> existing) {
        for (Trainer trainer : existing) {
            if (trainer.getName().equalsIgnoreCase(name.trim())) {
                throw new BusinessRuleException("Ya existe un entrenador con el nombre " + name.trim());
            }
        }
    }
}
package com.pokemon.battle.service;

import com.pokemon.battle.model.CombatHistoryEntry;
import com.pokemon.battle.model.Trainer;
import com.pokemon.battle.persistence.CombatRepository;
import com.pokemon.battle.validation.CombatValidator;

import java.util.List;

/**
 * Use cases related to the record of combats (COMBATE and HISTORIAL_COMBATES).
 */
public class CombatService implements CombatRegistry {

    private final CombatRepository repository;

    /**
     * @param repository storage of the combats (injected)
     */
    public CombatService(CombatRepository repository) {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     *
     * @throws com.pokemon.battle.exceptions.InvalidDataException  when a trainer is missing
     * @throws com.pokemon.battle.exceptions.BusinessRuleException when both trainers are the same
     */
    @Override
    public int register(Trainer winner, Trainer loser) {
        CombatValidator.validateParticipants(winner, loser);
        return repository.registerCombat(winner.getId(), loser.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CombatHistoryEntry> historyOf(Trainer trainer) {
        return repository.loadHistory(trainer.getId());
    }
}

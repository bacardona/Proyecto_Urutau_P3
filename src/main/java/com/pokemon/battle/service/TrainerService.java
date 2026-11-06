package com.pokemon.battle.service;

import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.persistence.TrainerReader;
import com.pokemon.battle.validation.CombatValidator;

import java.util.List;

/**
 * Use cases related to the trainers stored in the database.
 */
public class TrainerService implements TrainerCatalog {

    private final TrainerReader reader;

    /**
     * @param reader read access to the stored trainers (injected)
     */
    public TrainerService(TrainerReader reader) {
        this.reader = reader;
    }

    /**
     * {@inheritDoc}
     *
     * @throws com.pokemon.battle.exceptions.BusinessRuleException when there are no players
     */
    @Override
    public List<Player> listPlayers() {
        List<Player> players = reader.loadPlayers();
        CombatValidator.validateNotEmpty(players, "jugadores");
        return players;
    }

    /**
     * {@inheritDoc}
     *
     * @throws com.pokemon.battle.exceptions.BusinessRuleException when there are no machines
     */
    @Override
    public List<Machine> listMachines() {
        List<Machine> machines = reader.loadMachines();
        CombatValidator.validateNotEmpty(machines, "maquinas");
        return machines;
    }
}

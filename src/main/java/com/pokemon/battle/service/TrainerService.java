package com.pokemon.battle.service;

import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Trainer;
import com.pokemon.battle.persistence.TrainerRepository;
import com.pokemon.battle.validation.CombatValidator;
import com.pokemon.battle.validation.TrainerValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Use cases related to the trainers stored in the database.
 */
public class TrainerService implements TrainerCatalog {

    private final TrainerRepository repository;

    /**
     * @param repository storage of the trainers (injected)
     */
    public TrainerService(TrainerRepository repository) {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Player> listPlayers() {
        return repository.loadPlayers();
    }

    /**
     * {@inheritDoc}
     *
     * @throws com.pokemon.battle.exceptions.BusinessRuleException when there are no machines
     */
    @Override
    public List<Machine> listMachines() {
        List<Machine> machines = repository.loadMachines();
        CombatValidator.validateNotEmpty(machines, "maquinas");
        return machines;
    }

    /**
     * {@inheritDoc}
     *
     * @throws com.pokemon.battle.exceptions.InvalidDataException  when the name is empty or too long
     * @throws com.pokemon.battle.exceptions.BusinessRuleException when the name is already used
     */
    @Override
    public Player createPlayer(String name) {
        TrainerValidator.validateName(name);
        // ENTRENADOR guarda jugadores y maquinas: el nombre no debe repetirse en ninguno
        List<Trainer> all = new ArrayList<>();
        all.addAll(repository.loadPlayers());
        all.addAll(repository.loadMachines());
        TrainerValidator.validateNameIsFree(name, all);
        return repository.createPlayer(name.trim());
    }
}
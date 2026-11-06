package com.pokemon.battle.service;

import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Player;

import java.util.List;

/**
 * What the rest of the system needs from the trainers.
 */
public interface TrainerCatalog {

    /**
     * @return every human player
     */
    List<Player> listPlayers();

    /**
     * @return every machine trainer
     */
    List<Machine> listMachines();
}

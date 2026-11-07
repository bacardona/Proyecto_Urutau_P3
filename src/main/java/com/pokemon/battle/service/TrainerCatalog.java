package com.pokemon.battle.service;

import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Player;

import java.util.List;

/**
 * What the rest of the system needs from the trainers: list them and create players.
 */
public interface TrainerCatalog {

    /**
     * @return every human player (the list can be empty)
     */
    List<Player> listPlayers();

    /**
     * @return every machine trainer
     */
    List<Machine> listMachines();

    /**
     * Creates and stores a new human player.
     *
     * @param name name of the new player
     * @return the player created
     */
    Player createPlayer(String name);
}
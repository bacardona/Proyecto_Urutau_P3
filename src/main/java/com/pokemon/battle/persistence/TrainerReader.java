package com.pokemon.battle.persistence;

import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Player;

import java.util.List;

/**
 * Read-only access to the stored trainers (ENTRENADOR, JUGADOR and MAQUINA).
 */
public interface TrainerReader {

    /**
     * @return every human player, or an empty list when there is no data
     */
    List<Player> loadPlayers();

    /**
     * @return every machine trainer, or an empty list when there is no data
     */
    List<Machine> loadMachines();
}
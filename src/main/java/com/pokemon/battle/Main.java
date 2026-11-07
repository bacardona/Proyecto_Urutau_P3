package com.pokemon.battle;

import com.pokemon.battle.model.RandomProvider;
import com.pokemon.battle.model.TypeEffectiveness;
import com.pokemon.battle.persistence.*;
import com.pokemon.battle.service.BattleController;
import com.pokemon.battle.service.BattleService;
import com.pokemon.battle.service.CombatService;
import com.pokemon.battle.service.MachineStrategy;
import com.pokemon.battle.service.PokemonService;
import com.pokemon.battle.service.RandomMachineStrategy;
import com.pokemon.battle.service.ThreadLocalRandomProvider;
import com.pokemon.battle.service.TrainerService;
import com.pokemon.battle.service.TypeChartService;
import com.pokemon.battle.ui.BattleConsoleUI;

import java.util.Scanner;

/**
 * Entry point. It only builds the objects and injects their dependencies.
 */
public class Main {

    /**
     * Starts the game: a player against a machine, with the data read from
     * and saved to the Oracle database.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        MoveRepository moveRepository = new JdbcMoveRepository();
        PokemonRepository pokemonRepository = new JdbcPokemonRepository(moveRepository);
        PokemonService pokemonService = new PokemonService(pokemonRepository);

        TrainerService trainerService = new TrainerService(new JdbcTrainerRepository());
        CombatService combatService = new CombatService(new JdbcCombatRepository());

        RandomProvider random = new ThreadLocalRandomProvider();
        MachineStrategy machineStrategy = new RandomMachineStrategy(random);
        TypeEffectiveness effectiveness = new TypeChartService();
        BattleService battleService = new BattleService(random, effectiveness);
        BattleController controller = new BattleController(
                pokemonService, battleService, trainerService, combatService, machineStrategy);

        BattleConsoleUI ui = new BattleConsoleUI(controller, scanner);
        ui.start();
    }
}

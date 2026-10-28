package com.pokemon.battle;

import com.pokemon.battle.model.RandomProvider;
import com.pokemon.battle.model.TypeEffectiveness;
import com.pokemon.battle.persistence.CsvMoveRepository;
import com.pokemon.battle.persistence.CsvPokemonRepository;
import com.pokemon.battle.persistence.MoveRepository;
import com.pokemon.battle.persistence.PokemonRepository;
import com.pokemon.battle.service.BattleController;
import com.pokemon.battle.service.BattleService;
import com.pokemon.battle.service.PokemonService;
import com.pokemon.battle.service.ThreadLocalRandomProvider;
import com.pokemon.battle.service.TypeChartService;
import com.pokemon.battle.ui.BattleConsoleUI;

import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Entry point. It only builds the objects and injects their dependencies.
 */
public class Main {

    /**
     * Starts the game: Eevee against Pikachu.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        MoveRepository moveRepository = new CsvMoveRepository(Paths.get("data", "moves.csv"));
        PokemonRepository pokemonRepository =
                new CsvPokemonRepository(Paths.get("data", "pokemon.csv"), moveRepository);
        PokemonService pokemonService = new PokemonService(pokemonRepository);

        RandomProvider random = new ThreadLocalRandomProvider();
        TypeEffectiveness effectiveness = new TypeChartService();
        BattleService battleService = new BattleService(random, effectiveness);
        BattleController controller = new BattleController(pokemonService, battleService);

        BattleConsoleUI ui = new BattleConsoleUI(controller, scanner);
        ui.start("EEVEE", "PIKACHU");

        System.out.print("Presione ENTER para cerrar el programa...");
        scanner.nextLine();
    }
}

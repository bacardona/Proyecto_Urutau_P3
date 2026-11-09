package com.pokemon.battle;

import com.pokemon.battle.model.RandomProvider;
import com.pokemon.battle.model.TypeEffectiveness;
import com.pokemon.battle.persistence.*;
import com.pokemon.battle.service.BattleController;
import com.pokemon.battle.service.BattleService;
import com.pokemon.battle.service.CombatRegistry;
import com.pokemon.battle.service.CombatService;
import com.pokemon.battle.service.MachineStrategy;
import com.pokemon.battle.service.PokemonCatalog;
import com.pokemon.battle.service.PokemonService;
import com.pokemon.battle.service.RandomMachineStrategy;
import com.pokemon.battle.service.ThreadLocalRandomProvider;
import com.pokemon.battle.service.TrainerCatalog;
import com.pokemon.battle.service.TrainerService;
import com.pokemon.battle.service.TypeChartService;

/**
 * Builds the controller and the services with their dependencies,
 * shared by the console and the window.
 */
public final class AppFactory {

    /** Everything the window needs, built once. */
    public static final class Services {
        public final BattleController controller;
        public final PokemonCatalog pokemonCatalog;
        public final TrainerCatalog trainerCatalog;
        public final CombatRegistry combatRegistry;

        private Services(BattleController controller, PokemonCatalog pokemonCatalog,
                         TrainerCatalog trainerCatalog, CombatRegistry combatRegistry) {
            this.controller = controller;
            this.pokemonCatalog = pokemonCatalog;
            this.trainerCatalog = trainerCatalog;
            this.combatRegistry = combatRegistry;
        }
    }

    private AppFactory() {
    }

    public static Services create() {
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

        return new Services(controller, pokemonService, trainerService, combatService);
    }

    public static BattleController createController() {
        return create().controller;
    }
}
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

public final class AppFactory {

    private AppFactory() {
    }

    public static BattleController createController() {
        MoveRepository moveRepository = new JdbcMoveRepository();
        PokemonRepository pokemonRepository = new JdbcPokemonRepository(moveRepository);
        PokemonService pokemonService = new PokemonService(pokemonRepository);

        TrainerService trainerService = new TrainerService(new JdbcTrainerRepository());
        CombatService combatService = new CombatService(new JdbcCombatRepository());

        RandomProvider random = new ThreadLocalRandomProvider();
        MachineStrategy machineStrategy = new RandomMachineStrategy(random);
        TypeEffectiveness effectiveness = new TypeChartService();
        BattleService battleService = new BattleService(random, effectiveness);
        return new BattleController(
                pokemonService, battleService, trainerService, combatService, machineStrategy);
    }
}
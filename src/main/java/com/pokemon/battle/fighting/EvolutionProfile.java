package com.pokemon.battle.fighting;

import java.util.Map;

/** Reusable free-fight evolution data; it does not alter shared Pokemon records. */
public record EvolutionProfile(String basePokemonId, int baseOracleId,
                               String evolvedPokemonId, int evolvedOracleId,
                               String evolvedName, int evolvedRenderHeight) {

    public static final double EVOLVED_DAMAGE_BONUS = 0.12;
    public static final double EVOLVED_MOVEMENT_SPEED_BONUS = 0.07;

    private static final Map<String, EvolutionProfile> BY_BASE_ID = Map.of(
            "BULBASAUR", new EvolutionProfile("BULBASAUR", 1,
                    "IVYSAUR", 2, "Ivysaur", 148),
            "CHARMANDER", new EvolutionProfile("CHARMANDER", 4,
                    "CHARMELEON", 5, "Charmeleon", 155),
            "SQUIRTLE", new EvolutionProfile("SQUIRTLE", 7,
                    "WARTORTLE", 8, "Wartortle", 148),
            "EEVEE", new EvolutionProfile("EEVEE", 133,
                    "FLAREON", 35, "Flareon", 145)
    );

    public static EvolutionProfile forBasePokemon(String pokemonId) {
        return BY_BASE_ID.get(pokemonId);
    }
}

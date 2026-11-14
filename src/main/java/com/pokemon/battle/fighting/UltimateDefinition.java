package com.pokemon.battle.fighting;

import java.awt.Color;
import java.util.Map;

/** Tuning and visuals for one of the four free-fight ultimates. */
public record UltimateDefinition(String moveId, String displayName, Shape shape,
                                 int totalDamage, int pulseCount,
                                 double chargeSeconds, double pulseIntervalSeconds,
                                 Color primaryColor, Color secondaryColor) {

    public enum Shape { BEAM, RINGS }

    private static final Map<String, UltimateDefinition> BY_EVOLVED_FORM = Map.of(
            "IVYSAUR", new UltimateDefinition("LEAF_STORM", "TORMENTA DE HOJAS",
                    Shape.BEAM, 80, 4, 1.0, 0.30,
                    new Color(0x4BCE55), new Color(0xB5FF79)),
            "CHARMELEON", new UltimateDefinition("FIRE_BLAST", "LLAMARADA",
                    Shape.BEAM, 85, 5, 1.0, 0.27,
                    new Color(0xF0441C), new Color(0xFFD447)),
            "WARTORTLE", new UltimateDefinition("HYDRO_PUMP", "HIDROTORRENTE",
                    Shape.BEAM, 80, 4, 1.0, 0.30,
                    new Color(0x208BEE), new Color(0xA8F4FF)),
            "FLAREON", new UltimateDefinition("INFERNO_BURST", "ESTALLIDO IGNEO",
                    Shape.RINGS, 90, 5, 1.0, 0.28,
                    new Color(0xF04418), new Color(0xFFD84A))
    );

    public int damagePerPulse() {
        return totalDamage / pulseCount;
    }

    public static UltimateDefinition forEvolvedForm(String pokemonId) {
        return BY_EVOLVED_FORM.get(pokemonId);
    }
}

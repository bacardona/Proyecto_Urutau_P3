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
            "NINETALES", new UltimateDefinition("FLAMETHROWER", "LANZALLAMAS",
                    Shape.BEAM, 85, 5, 1.0, 0.27, new Color(0xF0601C), new Color(0xFFD447)),
            "SANDSLASH", new UltimateDefinition("EARTHQUAKE", "TERREMOTO",
                    Shape.BEAM, 85, 5, 1.0, 0.28, new Color(0xA0703C), new Color(0xE8C890)),
            "HAUNTER", new UltimateDefinition("SHADOW_STORM", "BOLA SOMBRA",
                    Shape.BEAM, 85, 5, 1.0, 0.27, new Color(0x6A3FB5), new Color(0xD0A8FF)),
            "MACHOKE", new UltimateDefinition("DYNAMIC_PUNCH", "PUNO DINAMICO",
                    Shape.BEAM, 90, 4, 1.0, 0.32, new Color(0xE8503A), new Color(0xFFC0A0)),
            "KADABRA", new UltimateDefinition("PSYSTRIKE", "PSICOATAQUE",
                    Shape.BEAM, 85, 5, 1.0, 0.27, new Color(0xE05AB0), new Color(0xFFC8EE)),
            "RAICHU", new UltimateDefinition("THUNDER", "TRUENO",
                    Shape.BEAM, 85, 5, 1.0, 0.27,
                    new Color(0xFFD21F), new Color(0xFFF7A8)),
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

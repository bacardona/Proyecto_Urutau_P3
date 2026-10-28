package com.pokemon.battle.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Stat stages of a combatant (from -6 to +6 in the multiplier table).
 */
public class StatStages {

    // Multiplicadores de etapa: indice 6 = etapa 0
    private static final float[] MULTIPLIERS = {
            2.0f / 8, 2.0f / 7, 2.0f / 6, 2.0f / 5, 2.0f / 4, 2.0f / 3,
            1.0f, 3 / 2.0f, 4 / 2.0f, 5 / 2.0f, 6 / 2.0f, 7 / 2.0f, 8 / 2.0f
    };

    private final Map<StatType, Integer> stages = new EnumMap<>(StatType.class);

    /**
     * Returns the multiplier associated with a stage.
     *
     * @param stage stage value; values outside [-6, 6] are clamped
     * @return the multiplier to apply to the stat
     */
    public static float multiplier(int stage) {
        int index = Math.max(0, Math.min(12, stage + 6));
        return MULTIPLIERS[index];
    }

    /**
     * @param stat the stat to query
     * @return the current stage of the stat (0 when never modified)
     */
    public int get(StatType stat) {
        return stages.getOrDefault(stat, 0);
    }

    /**
     * Adds a delta to the stage of a stat. Package-private: only the model
     * rules (moves) can change the stages.
     *
     * @param stat  the stat to modify
     * @param delta amount to add (negative lowers the stat)
     * @return the new stage
     */
    int change(StatType stat, int delta) {
        int value = get(stat) + delta;
        stages.put(stat, value);
        return value;
    }
}

package com.pokemon.battle.fighting;

import java.awt.Color;
import java.util.Objects;

/** Real-time tuning for a move; independent from turn-based Move rules. */
public record AttackDefinition(
        String moveId,
        Kind kind,
        int damage,
        double startupSeconds,
        double activeSeconds,
        double recoverySeconds,
        double hitboxWidth,
        double hitboxHeight,
        double dashSpeed,
        double projectileSpeed,
        double projectileLifetime,
        double projectileSize,
        Color effectColor) {

    public enum Kind { DASH, PROJECTILE }

    public AttackDefinition {
        Objects.requireNonNull(moveId);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(effectColor);
        if (damage < 0 || startupSeconds < 0 || activeSeconds <= 0 || recoverySeconds < 0) {
            throw new IllegalArgumentException("Parámetros de ataque inválidos");
        }
    }
}

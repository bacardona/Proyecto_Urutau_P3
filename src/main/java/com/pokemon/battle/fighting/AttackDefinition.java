package com.pokemon.battle.fighting;

import java.awt.Color;
import java.util.Objects;

/** Real-time tuning for a move; independent from turn-based Move rules. */
public record AttackDefinition(
        String moveId,
        int oracleMoveId,
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

    public enum Kind { HITBOX, DASH, LUNGE, PROJECTILE, STATUS, SELF_BUFF, GRUNT, FLY }

    /** Reuse delay keeps the original tuning, independently of the action lock. */
    public double cooldownSeconds() {
        return startupSeconds + activeSeconds + recoverySeconds;
    }

    /** Status moves used recoverySeconds to space out casts, not to keep the caster busy. */
    public double actionRecoverySeconds() {
        return switch (moveId) {
            case "GROWTH", "GROWL", "SLEEP_POWDER", "THUNDER_WAVE" -> Math.min(recoverySeconds, 0.20);
            default -> recoverySeconds;
        };
    }

    public AttackDefinition(String moveId, Kind kind, int damage,
                            double startupSeconds, double activeSeconds, double recoverySeconds,
                            double hitboxWidth, double hitboxHeight, double dashSpeed,
                            double projectileSpeed, double projectileLifetime, double projectileSize,
                            Color effectColor) {
        this(moveId, -1, kind, damage, startupSeconds, activeSeconds, recoverySeconds,
                hitboxWidth, hitboxHeight, dashSpeed, projectileSpeed,
                projectileLifetime, projectileSize, effectColor);
    }

    public AttackDefinition {
        Objects.requireNonNull(moveId);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(effectColor);
        if (damage < 0 || startupSeconds < 0 || activeSeconds <= 0 || recoverySeconds < 0) {
            throw new IllegalArgumentException("Parámetros de ataque inválidos");
        }
    }
}

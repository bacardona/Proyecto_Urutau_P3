package com.pokemon.battle.fighting;

/**
 * Something that happened in combat during one simulation step. The combat system
 * only records it; FightingFx reads it to draw effects and play sounds.
 *
 * @param type          what happened
 * @param moveId        move that caused it (for example "EMBER")
 * @param attackerIndex 0 = player one, 1 = player two
 * @param x             where it happened (arena coordinates)
 * @param y             where it happened (arena coordinates)
 * @param direction     -1 = towards the left, 1 = towards the right
 * @param damage        damage dealt (0 when it does not apply)
 */
public record CombatEvent(Type type, String moveId, int attackerIndex,
                          double x, double y, double direction, int damage) {

    public enum Type {
        /** The attacker starts preparing a move. */
        ATTACK_START,
        /** A hit or a projectile was dodged just in time. */
        DODGE,
        /** The move reaches its active part (the hit or the shot happens now). */
        ATTACK_ACTIVE,
        /** A projectile leaves the attacker's mouth. */
        PROJECTILE_FIRED,
        /** A move or projectile deals damage. */
        HIT,
        /** The defender blocks a physical move. */
        BLOCK,
        /** The defender blocked right on time (parry): no damage and the attacker is stunned. */
        PARRY,
        /** La barrera de Mewtwo devolvio un proyectil. */
        REFLECT,
        /** A projectile disappears without hitting anyone (for example against a platform). */
        FIZZLE
    }
}

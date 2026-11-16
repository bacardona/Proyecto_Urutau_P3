package com.pokemon.battle.fighting;

/**
 * Where the mouth of a Pokemon is in the arena, so the effects (energy, fire,
 * lightning) come out of it. It uses the same points Angel defined in
 * FightingSpriteProfile for the projectiles; if the Pokemon has no animated
 * sprites it uses an approximate point.
 */
final class MouthPoint {

    private MouthPoint() {
    }

    /** @return {x, y} of the mouth for that move */
    static double[] of(Fighter fighter, String moveId) {
        FightingSpriteProfile profile = FightingSpriteProfile.forFighter(fighter);
        if (profile != null) {
            try {
                FightingSpriteProfile.Origin origin = profile.attacks.containsKey(moveId)
                        ? profile.projectileOrigin(fighter, moveId)
                        : profile.mouthOrigin(fighter, "Idle");
                return new double[]{origin.x(), origin.y()};
            } catch (RuntimeException e) {
                // Si falta alguna hoja, se usa el punto aproximado de abajo
            }
        }
        double direction = fighter.isFacingLeft() ? -1 : 1;
        return new double[]{fighter.getX() + direction * fighter.getRenderHeight() * 0.3,
                fighter.getY() - fighter.getRenderHeight() * 0.6};
    }
}

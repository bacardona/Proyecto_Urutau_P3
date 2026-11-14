package com.pokemon.battle.fighting;

import java.util.Map;

/** Species choices and fixed geometry, independent of transparent sheet dimensions. */
final class FightingSpriteProfile {
    private static final Map<String, FightingSpriteProfile> PROFILES = Map.of(
            "SQUIRTLE", new FightingSpriteProfile("SQUIRTLE", 4, 75, 30, "Withdraw", Map.of(
                    "TACKLE", "Attack", "WATER_GUN", "Shoot", "TAIL_WHIP", "Swing", "BITE", "Nod")),
            "BULBASAUR", new FightingSpriteProfile("BULBASAUR", 4, 72, 34, "Idle", Map.of(
                    "TACKLE", "Swing", "VINE_WHIP", "Shoot", "SLEEP_POWDER", "Shake", "GROWTH", "DeepBreath")),
            "CHARMANDER", new FightingSpriteProfile("CHARMANDER", 4, 84, 26, "Idle", Map.of(
                    "TACKLE", "Strike", "EMBER", "Shoot", "GROWL", "DeepBreath", "FLY", "Hop")),
            "EEVEE", new FightingSpriteProfile("EEVEE", 4, 80, 30, "Idle", Map.of(
                    "TACKLE", "Swing", "QUICK_ATTACK", "Charge", "GROWL", "Shoot", "TAIL_WHIP", "TailWhip")),
            "IVYSAUR", new FightingSpriteProfile("IVYSAUR", "BULBASAUR", 4, 80, 38, "Idle", "Hurt", Map.of(
                    "TACKLE", "Swing", "VINE_WHIP", "Shoot", "SLEEP_POWDER", "Shake", "GROWTH", "Charge")),
            "CHARMELEON", new FightingSpriteProfile("CHARMELEON", "CHARMANDER", 4, 104, 32, "Idle", "Faint", Map.of(
                    "TACKLE", "Strike", "EMBER", "Shoot", "GROWL", "DeepBreath", "FLY", "Hop")),
            "WARTORTLE", new FightingSpriteProfile("WARTORTLE", "SQUIRTLE", 4, 96, 32, "Withdraw", "Hurt", Map.of(
                    "TACKLE", "Attack", "WATER_GUN", "Shoot", "TAIL_WHIP", "Swing")),
            "FLAREON", new FightingSpriteProfile("FLAREON", "EEVEE", 4, 92, 36, "Idle", "Faint", Map.of(
                    "TACKLE", "Swing", "QUICK_ATTACK", "Charge", "GROWL", "Shoot", "TAIL_WHIP", "TailWhip")));

    final String species;
    final int scale;
    final int bodyHeight;
    final double bodyHalfWidth;
    final String blockAnimation;
    final String defeatAnimation;
    final String resourceSpecies;
    final String packageDirectory;
    final Map<String, String> attacks;
    private SpriteCollabAnimationSet sprites;

    private FightingSpriteProfile(String species, int scale, int bodyHeight, double bodyHalfWidth,
                                  String blockAnimation, Map<String, String> attacks) {
        this(species, species, "Sprites_nuevos", scale, bodyHeight, bodyHalfWidth, blockAnimation, "Faint", attacks);
    }

    private FightingSpriteProfile(String species, String resourceSpecies, int scale, int bodyHeight,
                                  double bodyHalfWidth, String blockAnimation, String defeatAnimation,
                                  Map<String, String> attacks) {
        this(species, resourceSpecies, "Evolucion", scale, bodyHeight, bodyHalfWidth,
                blockAnimation, defeatAnimation, attacks);
    }

    private FightingSpriteProfile(String species, String resourceSpecies, String packageDirectory,
                                  int scale, int bodyHeight, double bodyHalfWidth, String blockAnimation,
                                  String defeatAnimation, Map<String, String> attacks) {
        this.species = species;
        this.resourceSpecies = resourceSpecies;
        this.packageDirectory = packageDirectory;
        this.scale = scale;
        this.bodyHeight = bodyHeight;
        this.bodyHalfWidth = bodyHalfWidth;
        this.blockAnimation = blockAnimation;
        this.defeatAnimation = defeatAnimation;
        this.attacks = attacks;
    }

    static FightingSpriteProfile forSpecies(String species) { return PROFILES.get(species); }

    static FightingSpriteProfile forFighter(Fighter fighter) {
        return forSpecies(fighter.getPokemonId());
    }

    boolean isEvolution() { return "Evolucion".equals(packageDirectory); }

    synchronized SpriteCollabAnimationSet sprites() {
        if (sprites == null) sprites = new SpriteCollabAnimationSet(resourceSpecies, packageDirectory);
        return sprites;
    }

    double faintDuration() {
        var faint = sprites().get(defeatAnimation);
        if (faint == null) return 0;
        // Reduced Ivysaur/Wartortle packs only have Hurt. Play it, then hold before replacement.
        return "Hurt".equals(defeatAnimation) ? Math.max(0.55, faint.durationSeconds()) : faint.durationSeconds();
    }

    /** Forward reach, area height and top above the feet; tuned per move, not per sheet. */
    record Contact(double reach, double height, double top) { }

    Contact contact(String move) {
        return switch (species) {
            case "BULBASAUR" -> switch (move) {
                case "TACKLE" -> new Contact(44, 48, 56);
                case "VINE_WHIP" -> new Contact(132, 44, 60);
                default -> null;
            };
            case "CHARMANDER" -> switch (move) {
                case "TACKLE" -> new Contact(44, 56, 64);
                case "FLY" -> new Contact(66, 64, 76);
                default -> null;
            };
            case "EEVEE" -> switch (move) {
                case "TACKLE" -> new Contact(42, 48, 60);
                case "QUICK_ATTACK" -> new Contact(56, 44, 60);
                case "TAIL_WHIP" -> new Contact(72, 44, 48);
                default -> null;
            };
            case "IVYSAUR" -> switch (move) {
                case "TACKLE" -> new Contact(48, 52, 62);
                case "VINE_WHIP" -> new Contact(148, 48, 66);
                default -> null;
            };
            case "CHARMELEON" -> switch (move) {
                case "TACKLE" -> new Contact(48, 64, 76);
                case "FLY" -> new Contact(72, 76, 88);
                default -> null;
            };
            case "WARTORTLE" -> switch (move) {
                case "TACKLE" -> new Contact(48, 60, 72);
                case "BITE" -> new Contact(34, 48, 72);
                case "TAIL_WHIP" -> new Contact(84, 64, 72);
                default -> null;
            };
            case "FLAREON" -> switch (move) {
                case "TACKLE" -> new Contact(48, 56, 68);
                case "QUICK_ATTACK" -> new Contact(60, 52, 72);
                case "TAIL_WHIP" -> new Contact(84, 52, 60);
                default -> null;
            };
            default -> null; // Squirtle retains its existing contact geometry.
        };
    }

    record Origin(double x, double y) { }

    Origin projectileOrigin(Fighter fighter, String move) {
        double direction = fighter.isFacingLeft() ? -1 : 1;
        if ("SQUIRTLE".equals(species)) {
            return new Origin(fighter.getX() + direction * 36, fighter.getY() - 24);
        }
        if ("SLEEP_POWDER".equals(move)) {
            return isEvolution()
                    ? new Origin(fighter.getX() - direction * 8, fighter.getY() - 72)
                    : new Origin(fighter.getX() - direction * 12, fighter.getY() - 64);
        }
        return mouthOrigin(fighter, attacks.get(move));
    }

    Origin mouthOrigin(Fighter fighter, String animationName) {
        double direction = fighter.isFacingLeft() ? -1 : 1;
        var animation = sprites().get(animationName);
        var frame = animation.frame(fighter.isFacingLeft(), Math.max(0, animation.hitFrame()));
        return new Origin(fighter.getX() + (frame.headX() - frame.bodyX()) * scale + direction * 8,
                fighter.getY() + (frame.headY() - frame.shadowY()) * scale);
    }
}

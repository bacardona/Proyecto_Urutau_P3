package com.pokemon.battle.fighting;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Species choices and fixed geometry, independent of transparent sheet dimensions. */
final class FightingSpriteProfile {
    private static final Map<String, FightingSpriteProfile> PROFILES = Map.ofEntries(

            Map.entry("SQUIRTLE", new FightingSpriteProfile("SQUIRTLE", 4, 75, 30, "Withdraw", Map.of(
                    "TACKLE", "Attack", "WATER_GUN", "Shoot", "TAIL_WHIP", "Swing", "BITE", "Nod"))),
            Map.entry("BULBASAUR", new FightingSpriteProfile("BULBASAUR", 4, 72, 34, "Idle", Map.of(
                    "TACKLE", "Swing", "VINE_WHIP", "Shoot", "SLEEP_POWDER", "Shake", "GROWTH", "DeepBreath"))),
            Map.entry("CHARMANDER", new FightingSpriteProfile("CHARMANDER", 4, 84, 26, "Idle", Map.of(
                    "TACKLE", "Strike", "EMBER", "Shoot", "GROWL", "DeepBreath", "FLY", "Hop"))),
            Map.entry("EEVEE", new FightingSpriteProfile("EEVEE", 4, 80, 30, "Idle", Map.of(
                    "TACKLE", "Swing", "QUICK_ATTACK", "Charge", "GROWL", "Shoot", "TAIL_WHIP", "TailWhip"))),
            Map.entry("IVYSAUR", new FightingSpriteProfile("IVYSAUR", "BULBASAUR", 4, 80, 38, "Idle", "Hurt", Map.of(
                    "TACKLE", "Swing", "VINE_WHIP", "Shoot", "SLEEP_POWDER", "Shake", "GROWTH", "Charge"))),
            Map.entry("CHARMELEON", new FightingSpriteProfile("CHARMELEON", "CHARMANDER", 4, 104, 32, "Idle", "Faint", Map.of(
                    "TACKLE", "Strike", "EMBER", "Shoot", "GROWL", "DeepBreath", "FLY", "Hop"))),
            Map.entry("WARTORTLE", new FightingSpriteProfile("WARTORTLE", "SQUIRTLE", 4, 96, 32, "Withdraw", "Hurt", Map.of(
                    "TACKLE", "Attack", "WATER_GUN", "Shoot", "TAIL_WHIP", "Swing"))),
            Map.entry("FLAREON", new FightingSpriteProfile("FLAREON", "EEVEE", 4, 92, 36, "Idle", "Faint", Map.of(
                    "TACKLE", "Swing", "QUICK_ATTACK", "Charge", "GROWL", "Shoot", "TAIL_WHIP", "TailWhip"))),
            Map.entry("VULPIX", new FightingSpriteProfile("VULPIX", 4, 72, 30, "Idle", Map.of(
                    "TACKLE", "Attack", "EMBER", "Shoot", "GROWL", "Charge", "FIRE_FANG", "Strike"))),
            Map.entry("NINETALES", new FightingSpriteProfile("NINETALES", "VULPIX", 4, 96, 38, "Idle", "Faint", Map.of(
                    "TACKLE", "Attack", "EMBER", "Shoot", "GROWL", "Charge", "FIRE_FANG", "Strike"))),
            Map.entry("SANDSHREW", new FightingSpriteProfile("SANDSHREW", 4, 72, 30, "Idle", Map.of(
                    "TACKLE", "Attack", "DIG", "Strike", "TAIL_WHIP", "Swing", "ROCK_SLIDE", "Shoot"))),
            Map.entry("SANDSLASH", new FightingSpriteProfile("SANDSLASH", "SANDSHREW", 4, 92, 36, "Idle", "Faint", Map.of(
                    "TACKLE", "Attack", "DIG", "Strike", "TAIL_WHIP", "Swing", "ROCK_SLIDE", "Shoot"))),
            Map.entry("GASTLY", new FightingSpriteProfile("GASTLY", "GASTLY", "Sprites_nuevos", 4, 92, 36, "Idle", "Hurt",
                    Map.of("LICK", "Attack", "SHADOW_BALL", "Shoot", "SLEEP_POWDER", "Charge", "GROWL", "Swing"))),
            Map.entry("HAUNTER", new FightingSpriteProfile("HAUNTER", "GASTLY", 4, 106, 38, "Idle", "Hurt", Map.of(
                    "LICK", "Attack", "SHADOW_BALL", "Shoot", "SLEEP_POWDER", "Charge", "GROWL", "Swing"))),
            Map.entry("MACHOP", new FightingSpriteProfile("MACHOP", 4, 80, 30, "Idle", Map.of(
                    "TACKLE", "Attack", "CLOSE_COMBAT", "Strike", "GROWL", "Charge", "BODY_SLAM", "Swing"))),
            Map.entry("MACHOKE", new FightingSpriteProfile("MACHOKE", "MACHOP", 4, 104, 38, "Idle", "Hurt", Map.of(
                    "TACKLE", "Attack", "CLOSE_COMBAT", "Attack", "GROWL", "Charge", "BODY_SLAM", "Swing"))),
            Map.entry("ABRA", new FightingSpriteProfile("ABRA", 4, 80, 30, "Idle", Map.of(
                    "ZEN_HEADBUTT", "Attack", "PSYCHIC", "Shoot", "GROWTH", "Charge", "TELEPORT", "Strike"))),
            Map.entry("KADABRA", new FightingSpriteProfile("KADABRA", "ABRA", 4, 106, 38, "Idle", "Hurt", Map.of(
                    "ZEN_HEADBUTT", "Attack", "PSYCHIC", "Shoot", "GROWTH", "Charge", "TELEPORT", "Strike"))),
            Map.entry("PIKACHU", new FightingSpriteProfile("PIKACHU", 4, 78, 30, "Idle", Map.of(
                    "QUICK_ATTACK", "QuickStrike", "THUNDER_SHOCK", "Shoot", "TAIL_WHIP", "Swing", "THUNDER_WAVE", "Charge"))),
            Map.entry("RAICHU", new FightingSpriteProfile("RAICHU", "PIKACHU", 4, 96, 34, "Idle", "Faint", Map.of(
                    "QUICK_ATTACK", "QuickStrike", "THUNDER_SHOCK", "Shoot", "TAIL_WHIP", "Swing", "THUNDER_WAVE", "Charge"))),
            // Jefe secreto
            Map.entry("MEWTWO", new FightingSpriteProfile("MEWTWO", 6, 200, 52, "Idle", Map.of(
                    "PSYCHO_CUT", "Attack", "PSYCHIC", "Shoot", "SHADOW_BALL", "Charge", "TELEPORT", "Rotate"))),
            // Fase 3 del modo dificil (paquete Evolucion de MEWTWO = forma 0002 de SpriteCollab)
            Map.entry("MEGA_MEWTWO_Y", new FightingSpriteProfile("MEGA_MEWTWO_Y", "MEWTWO", 6, 210, 54, "Idle",
                    "Hurt", Map.of("PSYCHO_CUT", "Attack", "PSYCHIC", "Shoot", "SHADOW_BALL", "Charge",
                            "TELEPORT", "Rotate"))));

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
    private ShieldEnvelope[] shields;

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

    /** Stable screen-pixel geometry relative to the fighter's center X and support Y. */
    record ShieldEnvelope(double centerX, double centerY, double radiusX, double radiusY) { }

    synchronized ShieldEnvelope shieldEnvelope(boolean left) {
        if (shields == null) {
            var right = measureShield(false);
            var west = measureShield(true);
            // A turn may move the tail's center, but must not resize the bubble.
            double rx = Math.max(right.radiusX(), west.radiusX());
            double ry = Math.max(right.radiusY(), west.radiusY());
            shields = new ShieldEnvelope[]{
                    new ShieldEnvelope(right.centerX(), right.centerY(), rx, ry),
                    new ShieldEnvelope(west.centerX(), west.centerY(), rx, ry)};
        }
        return shields[left ? 1 : 0];
    }

    private ShieldEnvelope measureShield(boolean left) {
        var idle = sprites().get("Idle");
        var guard = sprites().get(blockAnimation);
        var idleAnchor = idle.frame(left, 0);
        List<Point2D.Double> edges = new ArrayList<>();
        // Include every guard/neutral pose, including activation and release. Transparent
        // sheet padding is ignored; no per-frame measurements happen while drawing.
        for (var animation : guard == idle ? List.of(idle) : List.of(idle, guard)) {
            for (int i = 0; i < animation.frameCount(); i++) {
                var frame = animation.frame(left, i);
                double anchorY = "SQUIRTLE".equals(species)
                        ? frame.bodyY() + idleAnchor.shadowY() - idleAnchor.bodyY() : frame.shadowY();
                var image = frame.image();
                double margin = 8; // Two original pixels at the current 4x scale.
                for (int y = 0; y < image.getHeight(); y++) {
                    int first = image.getWidth(), last = -1;
                    for (int x = 0; x < image.getWidth(); x++) {
                        if ((image.getRGB(x, y) >>> 24) != 0) {
                            first = Math.min(first, x);
                            last = x;
                        }
                    }
                    if (last < 0) continue;
                    double x1 = (first - frame.bodyX()) * scale - margin;
                    double x2 = (last + 1 - frame.bodyX()) * scale + margin;
                    double y1 = (y - anchorY) * scale - margin;
                    double y2 = (y + 1 - anchorY) * scale + margin;
                    edges.add(new Point2D.Double(x1, y1));
                    edges.add(new Point2D.Double(x2, y1));
                    edges.add(new Point2D.Double(x1, y2));
                    edges.add(new Point2D.Double(x2, y2));
                }
            }
        }
        if (edges.isEmpty()) return new ShieldEnvelope(0, -bodyHeight * 0.5,
                bodyHalfWidth + 12, bodyHeight * 0.5 + 12);
        double minX = Double.POSITIVE_INFINITY, minY = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX;
        for (var p : edges) {
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
        }
        double cx = (minX + maxX) * 0.5, cy = (minY + maxY) * 0.5;
        double rx = (maxX - minX) * 0.5, ry = (maxY - minY) * 0.5;
        double expansion = 1;
        // A bounding rectangle's corners need not fit its inscribed ellipse. Expand
        // until all visible row edges AND their margin are inside the oval.
        for (var p : edges) expansion = Math.max(expansion,
                Math.hypot((p.x - cx) / rx, (p.y - cy) / ry));
        return new ShieldEnvelope(cx, cy, rx * expansion, ry * expansion);
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
        var animation = sprites().get(animationName);
        var frame = animation.frame(fighter.isFacingLeft(), Math.max(0, animation.hitFrame()));
        return mouthOrigin(fighter, frame);
    }

    Origin mouthOrigin(Fighter fighter, SpriteCollabAnimationSet.Frame frame) {
        double direction = fighter.isFacingLeft() ? -1 : 1;
        return new Origin(fighter.getX() + (frame.headX() - frame.bodyX()) * scale + direction * 8,
                fighter.getY() + (frame.headY() - frame.shadowY()) * scale);
    }
}

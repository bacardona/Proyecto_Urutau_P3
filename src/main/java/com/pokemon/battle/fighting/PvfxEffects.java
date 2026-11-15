package com.pokemon.battle.fighting;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Big pixel art effects from "PVFX Foundry" (CC0, resources/efectos/pvfx).
 * Each sheet is a grid of 8 columns; every effect has its own size, speed and
 * anchor point (pivot), usually the feet of the effect.
 */
final class PvfxEffects {

    enum Effect {
        FIRE_FIREBALL_IMPACT("fire-fireball-impact", 96, 64, 15, 70, 72, 38, false),
        FIRE_FLAMETHROWER("fire-flamethrower", 128, 48, 16, 60, 6, 32, true),
        FIRE_FIRE_PILLAR("fire-fire-pillar", 64, 112, 24, 70, 32, 102, false),
        FIRE_FIRE_TORNADO("fire-fire-tornado", 48, 80, 16, 70, 24, 74, true),
        FIRE_LAVA_BURST("fire-lava-burst", 64, 72, 23, 70, 32, 62, false),
        EXPLOSIONS_FIRE_NOVA("explosions-fire-nova", 96, 64, 19, 60, 48, 44, false),
        EXPLOSIONS_BIG_EXPLOSION("explosions-big-explosion", 128, 128, 18, 70, 64, 113, false),
        EXPLOSIONS_SMALL_EXPLOSION("explosions-small-explosion", 64, 64, 14, 60, 32, 57, false),
        WATER_WATER_SPLASH("water-water-splash", 112, 80, 22, 70, 56, 66, false),
        WATER_WATER_GEYSER("water-water-geyser", 48, 88, 21, 65, 24, 80, false),
        WATER_TIDAL_WAVE("water-tidal-wave", 96, 56, 20, 70, 48, 47, false),
        WATER_BUBBLE_BURST("water-bubble-burst", 48, 48, 12, 60, 24, 24, false),
        LIGHTNING_LIGHTNING_STRIKE("lightning-lightning-strike", 96, 128, 18, 80, 38, 118, false),
        LIGHTNING_STATIC_BURST("lightning-static-burst", 112, 112, 13, 60, 56, 58, false),
        STATUS_ELECTRIFIED("status-electrified", 48, 56, 14, 65, 24, 47, true),
        NATURE_VINE_SNARE("nature-vine-snare", 56, 56, 19, 70, 28, 46, false),
        NATURE_SPORE_BURST("nature-spore-burst", 64, 56, 20, 70, 32, 52, false),
        NATURE_LEAF_STORM("nature-leaf-storm", 80, 56, 18, 65, 0, 28, false),
        HOLY_HOLY_SMITE("holy-holy-smite", 80, 120, 14, 80, 40, 108, false),
        HOLY_DIVINE_BEAM("holy-divine-beam", 48, 96, 19, 70, 24, 88, false),
        IMPACTS_PUNCH_HIT("impacts-punch-hit", 48, 40, 11, 60, 17, 21, false),
        IMPACTS_CRIT_HIT("impacts-crit-hit", 96, 80, 11, 70, 48, 40, false),
        IMPACTS_HIT_SPARK("impacts-hit-spark", 64, 64, 8, 80, 32, 32, false),
        IMPACTS_SHOCKWAVE_RING("impacts-shockwave-ring", 120, 52, 16, 70, 60, 30, false),
        IMPACTS_PARRY("impacts-parry", 96, 80, 13, 60, 48, 38, false),
        SMOKE_DUST_PUFF("smoke-dust-puff", 72, 40, 14, 70, 36, 33, false),
        STATUS_POWER_UP_AURA("status-power-up-aura", 56, 56, 14, 70, 28, 50, true),
        UI_LEVEL_UP("ui-level-up", 64, 96, 20, 70, 32, 88, false),
        UI_TREASURE_GLINT("ui-treasure-glint", 48, 48, 16, 75, 24, 24, true),
        SHADOW_SHADOW_BOLT("shadow-shadow-bolt", 112, 56, 19, 70, 86, 30, false),
        SHADOW_VOID_IMPLOSION("shadow-void-implosion", 96, 96, 21, 70, 48, 48, false),
        ARCANE_ARCANE_BEAM("arcane-arcane-beam", 168, 64, 8, 75, 18, 32, true),
        EXPLOSIONS_IMPLOSION("explosions-implosion", 96, 96, 28, 70, 48, 48, false),
        IMPACTS_GROUND_SLAM("impacts-ground-slam", 128, 64, 19, 70, 64, 44, false),
        IMPACTS_ROCK_SHATTER("impacts-rock-shatter", 64, 56, 18, 60, 30, 44, false),
        NATURE_EARTH_SPIKE("nature-earth-spike", 128, 64, 22, 70, 62, 54, false),
        ARCANE_MAGIC_CIRCLE("arcane-magic-circle", 96, 64, 20, 80, 48, 47, false),
        EXPLOSIONS_ARCANE_BURST("explosions-arcane-burst", 72, 72, 20, 65, 36, 38, false);

        final String file;
        final int width;
        final int height;
        final int frames;
        final double frameSeconds;
        final int pivotX;
        final int pivotY;
        final boolean loop;

        Effect(String file, int width, int height, int frames, int frameMs,
               int pivotX, int pivotY, boolean loop) {
            this.file = file;
            this.width = width;
            this.height = height;
            this.frames = frames;
            this.frameSeconds = frameMs / 1000.0;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.loop = loop;
        }

        double seconds() {
            return frames * frameSeconds;
        }
    }

    private static final Map<Effect, BufferedImage[]> FRAMES = new EnumMap<>(Effect.class);

    private static final class Play {
        Effect effect;
        double x;
        double y;
        int scale;
        double age;
        double delay;
        double duration;
        boolean flip;
    }

    private final List<Play> playing = new ArrayList<>();

    /** Plays once (a loop effect repeats for {@code seconds}). (x, y) is where the pivot goes. */
    void play(Effect effect, double x, double y, int scale, boolean flip) {
        play(effect, x, y, scale, flip, 0, effect.loop ? effect.seconds() : effect.seconds());
    }

    void play(Effect effect, double x, double y, int scale, boolean flip, double delay, double seconds) {
        if (frames(effect).length == 0 || playing.size() > 60) return;
        Play p = new Play();
        p.effect = effect;
        p.x = x;
        p.y = y;
        p.scale = scale;
        p.flip = flip;
        p.delay = delay;
        p.duration = seconds;
        playing.add(p);
    }

    void tick(double dt) {
        for (Iterator<Play> it = playing.iterator(); it.hasNext(); ) {
            Play p = it.next();
            if (p.delay > 0) {
                p.delay -= dt;
                continue;
            }
            p.age += dt;
            if (p.age >= p.duration) it.remove();
        }
    }

    void clear() {
        playing.clear();
    }

    void draw(Graphics2D g) {
        if (playing.isEmpty()) return;
        Object old = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (Play p : playing) {
            if (p.delay > 0) continue;
            BufferedImage[] frames = frames(p.effect);
            int index = (int) (p.age / p.effect.frameSeconds);
            index = p.effect.loop ? index % frames.length : Math.min(frames.length - 1, index);
            BufferedImage frame = frames[index];
            int w = p.effect.width * p.scale;
            int h = p.effect.height * p.scale;
            int pivotX = p.flip ? p.effect.width - p.effect.pivotX : p.effect.pivotX;
            int left = (int) Math.round(p.x - pivotX * p.scale);
            int top = (int) Math.round(p.y - p.effect.pivotY * p.scale);
            if (p.flip) g.drawImage(frame, left + w, top, -w, h, null);
            else g.drawImage(frame, left, top, w, h, null);
        }
        if (old != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
    }

    private static synchronized BufferedImage[] frames(Effect effect) {
        BufferedImage[] cached = FRAMES.get(effect);
        if (cached != null) return cached;
        BufferedImage[] result = new BufferedImage[0];
        try (InputStream in = PvfxEffects.class.getResourceAsStream("/efectos/pvfx/" + effect.file + ".png")) {
            BufferedImage sheet = in == null ? null : ImageIO.read(in);
            if (sheet != null) {
                result = new BufferedImage[effect.frames];
                for (int n = 0; n < effect.frames; n++) {
                    result[n] = sheet.getSubimage((n % 8) * effect.width, (n / 8) * effect.height,
                            effect.width, effect.height);
                }
            }
        } catch (Exception e) {
            result = new BufferedImage[0];
        }
        FRAMES.put(effect, result);
        return result;
    }
}

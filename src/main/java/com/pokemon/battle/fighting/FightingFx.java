package com.pokemon.battle.fighting;

import com.pokemon.battle.ui.RetroStyle;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Visual and sound effects of the free fight: particles, lightning, explosions,
 * screen shake, hit-stop (the game freezes a moment when a hit lands), white flash
 * on the hit Pokemon, damage numbers and the little jump when a Pokemon attacks.
 *
 * It never changes the combat rules: it only reads what happened (CombatEvent and
 * the state of the fighters) and draws on top of the arena.
 */
public final class FightingFx {

    // ------------------------------------------------------------------ piezas

    private enum Shape { PIXEL, GLOW, SPARK, RING, SMOKE, FIRE, WATER, LEAF, STAR, SLASH, ZZZ, DEBRIS }

    private static final class Particle {
        private Shape shape;
        private double x;
        private double y;
        private double vx;
        private double vy;
        private double gravity;
        private double drag;
        private double size;
        private double grow;
        private double life;
        private double maxLife;
        private double delay;
        private double angle;
        private double spin;
        private Color from;
        private Color to;
        private boolean behind;
    }

    private static final class Bolt {
        private double x1;
        private double y1;
        private double x2;
        private double y2;
        private double life;
        private double maxLife;
        private float width;
        private Color glow;
    }

    private static final class Popup {
        private String text;
        private double x;
        private double y;
        private double life;
        private double maxLife;
        private int size;
        private Color color;
    }

    /** Mark left on the floor (burn, puddle, crack) that fades away. */
    private static final class Decal {
        private String kind;
        private double x;
        private double y;
        private double size;
        private double life;
        private double maxLife;
        private long seed;
    }

    /** Little jump / crouch when a Pokemon attacks (only visual). */
    private static final class Hop {
        private double elapsed;
        private boolean ranged;
    }

    private static final double HOP_CROUCH = 0.12;
    private static final double HOP_AIR = 0.28;
    private static final double HOP_LAND = 0.08;

    private final Random random = new Random();
    private final FightingSound sound = FightingSound.get();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Bolt> bolts = new ArrayList<>();
    private final List<Popup> popups = new ArrayList<>();
    private final Map<Fighter, Double> whiteFlash = new IdentityHashMap<>();
    private final Map<Fighter, double[]> knock = new IdentityHashMap<>();
    private final Map<Fighter, Hop> hops = new IdentityHashMap<>();
    private final Map<Fighter, Integer> lastHealth = new IdentityHashMap<>();
    private final Map<Fighter, Boolean> lastOnGround = new IdentityHashMap<>();
    private final Map<Fighter, Double> lastVelocityY = new IdentityHashMap<>();
    private final Map<Fighter, Boolean> lastDefeated = new IdentityHashMap<>();
    private final Map<Fighter, Boolean> lastEvolved = new IdentityHashMap<>();
    private final Fighter[] lastActive = new Fighter[2];
    private final Map<FightingProjectile, double[]> projectileOrigins = new IdentityHashMap<>();
    private final UltimateController.Phase[] lastUltimatePhase = new UltimateController.Phase[2];

    private final List<Decal> decals = new ArrayList<>();
    // Efectos dibujados a mano (Pixel FX Pack, CC0)
    private final PixelFxSheets sheets = new PixelFxSheets();
    // Intro VS, cuenta 3-2-1, cinematica de la ulti, K.O. en camara lenta y ganador
    private final FightingDirector director = new FightingDirector();
    // Efectos grandes de PVFX Foundry (CC0): llamas, geiseres, rayos, explosiones
    private final PvfxEffects big = new PvfxEffects();
    private double groundY = 677;
    // Zoom de camara en los golpes fuertes
    private double zoom;
    private double zoomFocusX = 640;
    private double zoomFocusY = 400;
    // Lineas de velocidad del K.O. / golpes muy fuertes
    private double speedLines;
    private double speedLinesX;
    private double speedLinesY;
    private double impactFrame;
    private double time;
    // Tiempo que avanza solo cuando el juego no esta congelado (para las animaciones)
    private double simTime;
    private double hitStop;
    private double shakeRemaining;
    private double shakeDuration;
    private double shakeMagnitude;
    private double shakeX;
    private double shakeY;
    private double flashRemaining;
    private double flashDuration;
    private float flashAlpha;
    private Color flashColor = Color.WHITE;

    // ------------------------------------------------------------------ ciclo

    /** Forget everything (new match or rematch). */
    public void clear() {
        director.reset();
        big.clear();
        cameraZoom = 0;
        sheets.clear();
        particles.clear();
        bolts.clear();
        popups.clear();
        whiteFlash.clear();
        knock.clear();
        hops.clear();
        lastHealth.clear();
        lastOnGround.clear();
        lastVelocityY.clear();
        lastDefeated.clear();
        lastEvolved.clear();
        lastActive[0] = null;
        lastActive[1] = null;
        projectileOrigins.clear();
        decals.clear();
        fallingColumns.clear();
        zoom = 0;
        speedLines = 0;
        impactFrame = 0;
        lastUltimatePhase[0] = null;
        lastUltimatePhase[1] = null;
        hitStop = 0;
        shakeRemaining = 0;
        flashRemaining = 0;
    }

    /**
     * One simulation step. During a hit-stop the fighters stay frozen, but the
     * particles and the shake keep moving.
     */
    public void step(FightingGameSession session, FightingInput input, double dt) {
        time += dt;
        if (session.isBossFight()) director.useBossIntro();
        director.tick(dt);
        tick(dt);
        if (director.freezesSimulation()) {
            // Durante la intro y la cinematica de la ulti los pokemon esperan
            director.discardTransientInputs(input);
            return;
        }
        if (hitStop > 0) {
            hitStop -= dt;
            return;
        }
        double sdt = dt * director.timeScale();
        session.update(input, sdt);
        simTime += sdt;
        react(session, sdt);
        followCamera(session, dt);
        if (session.getState() == FightingGameSession.State.FINISHED) director.onFinished(session);
    }

    /** Pantalla de resultados: solo avanzan las animaciones (texto, confeti, risa). */
    public void tickResults(double dt) {
        time += dt;
        director.tickResults(dt);
    }

    FightingDirector director() {
        return director;
    }

    private void tick(double dt) {
        sheets.tick(dt);
        big.tick(dt);
        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle p = it.next();
            if (p.delay > 0) {
                p.delay -= dt;
                continue;
            }
            p.life -= dt;
            if (p.life <= 0) {
                it.remove();
                continue;
            }
            p.vy += p.gravity * dt;
            if (p.shape == Shape.DEBRIS && p.y > groundY - 2 && p.vy > 0) {
                // Los escombros rebotan en el piso
                p.y = groundY - 2;
                p.vy *= -0.35;
                p.vx *= 0.55;
                p.spin *= 0.5;
            }
            double damping = Math.max(0, 1 - p.drag * dt);
            p.vx *= damping;
            p.vy *= damping;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.size = Math.max(0, p.size + p.grow * dt);
            p.angle += p.spin * dt;
        }
        bolts.removeIf(b -> (b.life -= dt) <= 0);
        decals.removeIf(d -> (d.life -= dt) <= 0);
        fallingColumns.removeIf(c -> (c[1] += dt) > COLUMN_FALL_SECONDS + 0.3);
        zoom = Math.max(0, zoom - dt * 0.35);
        speedLines = Math.max(0, speedLines - dt);
        impactFrame = Math.max(0, impactFrame - dt);
        popups.removeIf(p -> (p.life -= dt) <= 0);
        whiteFlash.replaceAll((f, v) -> v - dt);
        whiteFlash.values().removeIf(v -> v <= 0);
        for (double[] k : knock.values()) {
            // Resorte: el golpeado sale empujado y vuelve a su sitio
            k[1] += (-k[0] * 260 - k[1] * 22) * dt;
            k[0] += k[1] * dt;
        }
        for (Iterator<Map.Entry<Fighter, Hop>> it = hops.entrySet().iterator(); it.hasNext(); ) {
            Hop hop = it.next().getValue();
            hop.elapsed += dt;
            if (hop.elapsed > HOP_CROUCH + HOP_AIR + HOP_LAND) it.remove();
        }
        if (shakeRemaining > 0) {
            shakeRemaining -= dt;
            double strength = shakeMagnitude * Math.max(0, shakeRemaining / shakeDuration);
            shakeX = (random.nextDouble() * 2 - 1) * strength;
            shakeY = (random.nextDouble() * 2 - 1) * strength * 0.7;
        } else {
            shakeX = 0;
            shakeY = 0;
        }
        if (flashRemaining > 0) flashRemaining -= dt;
    }

    // ------------------------------------------------------------------ reaccionar a lo que paso

    private void react(FightingGameSession session, double dt) {
        groundY = session.getGroundY();
        List<Fighter> active = session.getFighters();
        List<CombatEvent> events = session.getCombatSystem().drainEvents();
        // Cambio de Pokemon en la pared: humo y aviso
        for (int i = 0; i < active.size() && i < 2; i++) {
            Fighter now = active.get(i);
            if (lastActive[i] != null && lastActive[i] != now && !lastActive[i].isDefeated()) {
                switchEffect(now);
            }
            lastActive[i] = now;
            if (session.consumeEntranceLanding(i)) entranceLanding(now);
        }
        // Jefe secreto: teletransporte y furia
        double blinkFrom = session.consumeBossBlink();
        if (!Double.isNaN(blinkFrom) && session.getBoss() != null) bossBlink(session.getBoss(), blinkFrom);
        if (session.consumeBossEnraged() && session.getBoss() != null) {
            director.startEnrage(session.getBoss());
            shake(10, 0.5);
        }
        for (FightingGameSession.BossEvent e : session.consumeBossEvents()) bossEvent(session, e);

        // Quien perdio vida en este paso (puede que ya lo hayan cambiado por otro del equipo)
        Map<Fighter, Integer> damaged = new IdentityHashMap<>();
        List<Fighter> all = new ArrayList<>(session.getPlayerOneTeam().getMembers());
        all.addAll(session.getPlayerTwoTeam().getMembers());
        for (Fighter f : all) {
            int before = lastHealth.getOrDefault(f, f.getHealth());
            if (f.getHealth() < before) damaged.put(f, before - f.getHealth());
            lastHealth.put(f, f.getHealth());
        }

        for (CombatEvent e : events) {
            Fighter attacker = active.get(e.attackerIndex());
            switch (e.type()) {
                case ATTACK_START -> startHop(attacker, e.moveId());
                case ATTACK_ACTIVE -> onAttackActive(attacker, e);
                case PROJECTILE_FIRED -> muzzle(e.moveId(), e.x(), e.y(), e.direction());
                case HIT -> {
                    Fighter target = damagedOf(1 - e.attackerIndex(), session, damaged, active);
                    if (target != null) damaged.remove(target);
                    impact(e, target);
                }
                case BLOCK -> {
                    // El dano que paso el escudo sale pequeno y celeste, sin golpe completo
                    Fighter target = damagedOf(1 - e.attackerIndex(), session, damaged, active);
                    if (target != null) damaged.remove(target);
                    block(e.x(), e.y(), e.direction());
                    chipPopup(e.x(), e.y() - 40, e.damage());
                }
                case PARRY -> parry(e.x(), e.y(), e.direction());
                case REFLECT -> {
                    sound.play(FightingSound.Sfx.BLOCK, 1f);
                    ring(e.x(), e.y(), 16, 110, 0.25, new Color(0xFF8FCB), 0, 4);
                    sparks(e.x(), e.y(), 12, 360, Color.WHITE, new Color(0xFF8FCB));
                    bossPopup("¡REFLEJADO!", e.x(), e.y() - 40, new Color(0xFF8FCB), 26);
                }
                case FIZZLE -> fizzle(e.x(), e.y());
                default -> { }
            }
        }
        // Dano sin evento (por ejemplo los pulsos de la ulti)
        List<UltimateController.Visual> ultimateVisuals = session.getCombatSystem().getUltimateVisuals();
        for (Map.Entry<Fighter, Integer> entry : damaged.entrySet()) {
            Fighter f = entry.getKey();
            genericHit(f, entry.getValue());
            for (UltimateController.Visual v : ultimateVisuals) {
                if (v.target() == f && v.phase() == UltimateController.Phase.RELEASING) ultimatePulse(v.definition(), f);
            }
        }
        for (Fighter f : all) {
            boolean wasDefeated = lastDefeated.getOrDefault(f, false);
            if (f.isDefeated() && !wasDefeated) {
                knockOut(f);
                FightingTeam team = session.getPlayerOneTeam().getMembers().contains(f)
                        ? session.getPlayerOneTeam() : session.getPlayerTwoTeam();
                director.startKnockOut(!team.hasAvailableFighter());
            }
            lastDefeated.put(f, f.isDefeated());
        }
        for (Fighter f : active) {
            boolean wasEvolved = lastEvolved.getOrDefault(f, f.isEvolved());
            if (f.isEvolved() && !wasEvolved) director.startEvolution(f);
            lastEvolved.put(f, f.isEvolved());
        }
        for (Fighter f : active) {
            jumpAndLanding(f);
            continuousEffects(session, f, dt);
        }
        projectileTrails(session, dt);
        ultimates(session, dt);
    }

    private Fighter damagedOf(int teamIndex, FightingGameSession session,
                              Map<Fighter, Integer> damaged, List<Fighter> active) {
        FightingTeam team = teamIndex == 0 ? session.getPlayerOneTeam() : session.getPlayerTwoTeam();
        for (Fighter f : team.getMembers()) {
            if (damaged.containsKey(f)) return f;
        }
        return active.get(teamIndex);
    }

    // ------------------------------------------------------------------ salto al atacar

    private void startHop(Fighter attacker, String moveId) {
        if ("FLY".equals(moveId)) return;   // Vuelo ya se mueve de verdad
        Hop hop = new Hop();
        hop.ranged = switch (moveId) {
            case "EMBER", "WATER_GUN", "THUNDER_SHOCK", "SLEEP_POWDER", "GROWL", "GROWTH", "THUNDER_WAVE",
                 "ROCK_SLIDE", "SHADOW_BALL", "PSYCHIC" -> true;
            default -> false;
        };
        hops.put(attacker, hop);
    }

    /** How high the sprite is drawn above its real position (negative = up). */
    public double hopOffsetY(Fighter f) {
        Hop hop = hops.get(f);
        if (hop == null) return 0;
        double t = hop.elapsed - HOP_CROUCH;
        if (t <= 0 || t >= HOP_AIR) return 0;
        double height = f.getRenderHeight() * (hop.ranged ? 0.20 : 0.07);
        return -height * Math.sin(Math.PI * t / HOP_AIR);
    }

    /** Horizontal stretch: wider when it crouches or lands, thinner when it jumps. */
    public double hopScaleX(Fighter f) {
        double squash = squash(f);
        return 1 + squash * 0.75;
    }

    public double hopScaleY(Fighter f) {
        return 1 - squash(f);
    }

    // >0 aplastado, <0 estirado
    private double squash(Fighter f) {
        Hop hop = hops.get(f);
        if (hop == null) return 0;
        double t = hop.elapsed;
        double amount = hop.ranged ? 1 : 0.8;
        if (t < HOP_CROUCH) return 0.17 * amount * Math.sin(Math.PI / 2 * t / HOP_CROUCH);
        t -= HOP_CROUCH;
        if (t < 0.08) return -0.12 * amount * (1 - t / 0.08);
        if (t < HOP_AIR) return 0;
        t -= HOP_AIR;
        return 0.13 * amount * Math.sin(Math.PI * Math.min(1, t / HOP_LAND));
    }

    /** Push back of a hit Pokemon (only visual). */
    public double knockOffsetX(Fighter f) {
        double[] k = knock.get(f);
        return k == null ? 0 : k[0];
    }

    /** 0..1: how white the sprite is drawn this frame. */
    public float whiteFlash(Fighter f) {
        Double v = whiteFlash.get(f);
        if (v == null) return 0;
        return (float) Math.min(1, v / 0.07);
    }

    /** Seconds since the match started (keeps going during hit-stop). */
    public double time() {
        return time;
    }

    /** Seconds of play without counting the hit-stop freezes. */
    public double simTime() {
        return simTime;
    }

    public double shakeX() {
        return shakeX;
    }

    public double shakeY() {
        return shakeY;
    }

    // ------------------------------------------------------------------ ataques

    private void onAttackActive(Fighter attacker, CombatEvent e) {
        double dir = e.direction();
        double feetY = attacker.getY();
        double backX = attacker.getX() - dir * attacker.getBodyHalfWidth() * 0.6;
        switch (e.moveId()) {
            case "TACKLE", "QUICK_ATTACK", "TAIL_WHIP", "BITE", "FIRE_FANG", "DIG", "LICK",
                 "CLOSE_COMBAT", "BODY_SLAM", "ZEN_HEADBUTT", "PSYCHO_CUT", "TELEPORT" -> {
                sound.play(FightingSound.Sfx.WHOOSH, 0.8f);
                dust(backX, feetY, -dir, 7, 1.0);
            }
            case "VINE_WHIP" -> sound.play(FightingSound.Sfx.VINE, 0.9f);
            case "FLY" -> {
                sound.play(FightingSound.Sfx.WHOOSH, 1f);
                dust(attacker.getX(), feetY, 0, 14, 1.5);
                ring(attacker.getX(), feetY - 6, 10, 260, 0.35, new Color(0xE8F7FF), 0, 4);
            }
            case "GROWL" -> {
                sound.play(FightingSound.Sfx.GROWL, 1f);
                double[] mouth = MouthPoint.of(attacker, "GROWL");
                double mx = mouth[0];
                double my = mouth[1];
                for (int i = 0; i < 4; i++) {
                    ring(mx, my, 8, 420, 0.55, new Color(0xFFE15A), i * 0.12, 7);
                }
                shake(5, 0.45);
            }
            case "THUNDER_WAVE" -> {
                // Onda Trueno: chispazo alrededor de Pikachu y ondas amarillas hacia el rival
                sound.play(FightingSound.Sfx.ELECTRIC_SHOT, 1f);
                double[] mouth = MouthPoint.of(attacker, "GROWL");
                big.play(PvfxEffects.Effect.LIGHTNING_STATIC_BURST, attacker.getX(),
                        feetY - attacker.getRenderHeight() * 0.5, 2, false);
                for (int i = 0; i < 3; i++) {
                    ring(mouth[0], mouth[1], 8, 380, 0.5, new Color(0xFFE45A), i * 0.12, 6);
                }
                shake(4, 0.3);
            }
            case "GROWTH" -> {
                sound.play(FightingSound.Sfx.BUFF, 1f);
                double cy = feetY - attacker.getRenderHeight() * 0.5;
                glow(attacker.getX(), cy, attacker.getRenderHeight() * 1.1, 0.45,
                        new Color(0x9CFF7A), false);
                ring(attacker.getX(), feetY - 4, 12, 300, 0.5, new Color(0xB6FF8E), 0, 5);
                for (int i = 0; i < 34; i++) {
                    double x = attacker.getX() + (random.nextDouble() * 2 - 1) * attacker.getBodyHalfWidth() * 1.3;
                    Particle p = pixel(x, feetY - random.nextDouble() * 20, 0,
                            -120 - random.nextDouble() * 220, 4 + random.nextDouble() * 5,
                            0.6 + random.nextDouble() * 0.5, new Color(0xE7FFB8), new Color(0x3FAE45));
                    p.delay = random.nextDouble() * 0.25;
                    if (i % 3 == 0) p.shape = Shape.LEAF;
                    p.spin = 6;
                }
            }
            default -> { }
        }
    }

    private void muzzle(String moveId, double x, double y, double dir) {
        switch (moveId) {
            case "EMBER" -> {
                sound.play(FightingSound.Sfx.FIRE_SHOT);
                glow(x, y, 70, 0.16, new Color(0xFFB347), false);
                for (int i = 0; i < 18; i++) {
                    double a = coneAngle(dir, 0.45);
                    double speed = 140 + random.nextDouble() * 260;
                    Particle p = pixel(x, y, Math.cos(a) * speed, Math.sin(a) * speed - 30,
                            5 + random.nextDouble() * 6, 0.22 + random.nextDouble() * 0.2,
                            new Color(0xFFF2A8), new Color(0xD8301A));
                    p.drag = 4;
                    p.gravity = -160;
                    p.grow = -10;
                }
                for (int i = 0; i < 6; i++) {
            double a = coneAngle(dir, 0.35);
            double speed = 120 + random.nextDouble() * 160;
            firePuff(x, y, Math.cos(a) * speed, Math.sin(a) * speed, 14 + random.nextDouble() * 10,
                    0.22 + random.nextDouble() * 0.12);
        }
            }
            case "WATER_GUN" -> {
                sound.play(FightingSound.Sfx.WATER_SHOT);
                glow(x, y, 56, 0.14, new Color(0x7FE4FF), false);
                for (int i = 0; i < 20; i++) {
                    double a = coneAngle(dir, 0.30);
                    double speed = 220 + random.nextDouble() * 320;
                    Particle p = make(Shape.WATER, x, y, Math.cos(a) * speed, Math.sin(a) * speed - 40,
                            4 + random.nextDouble() * 5, 0.25 + random.nextDouble() * 0.2,
                            Color.WHITE, Color.WHITE);
                    p.gravity = 700;
                    p.drag = 2;
                }
            }
            case "THUNDER_SHOCK" -> {
                sound.play(FightingSound.Sfx.ELECTRIC_SHOT);
                glow(x, y, 80, 0.12, new Color(0xFFF26B), false);
                for (int i = 0; i < 3; i++) {
                    double a = coneAngle(dir, 0.6);
                    double len = 50 + random.nextDouble() * 50;
                    bolt(x, y, x + Math.cos(a) * len, y + Math.sin(a) * len, 0.10, 2.5f,
                            new Color(0xFFF26B));
                }
                sparks(x, y, 12, 380, new Color(0xFFFBD1), new Color(0xFFD21F));
                screenFlash(new Color(0xFFF7B0), 0.05, 0.18f);
            }
            case "SLEEP_POWDER" -> {
                sound.play(FightingSound.Sfx.POWDER);
                for (int i = 0; i < 16; i++) {
                    double a = coneAngle(dir, 0.8);
                    double speed = 40 + random.nextDouble() * 110;
                    Particle p = pixel(x, y, Math.cos(a) * speed, Math.sin(a) * speed,
                            3 + random.nextDouble() * 4, 0.5 + random.nextDouble() * 0.4,
                            new Color(0xFBE7FF), new Color(0x8E4BC4));
                    p.drag = 1.5;
                    p.gravity = -20;
                }
                glow(x, y, 50, 0.3, new Color(0xD7A6FF), false);
            }
            case "ROCK_SLIDE" -> {
                sound.play(FightingSound.Sfx.HIT_HEAVY, 0.6f);
                debris(x, y, 6, 0.8, new Color(0x9C7B55));
            }
            case "SHADOW_BALL" -> {
                sound.play(FightingSound.Sfx.ELECTRIC_SHOT, 0.7f);
                glow(x, y, 60, 0.18, new Color(0x9D6BE8), false);
            }
            case "PSYCHIC" -> {
                sound.play(FightingSound.Sfx.ELECTRIC_SHOT, 0.7f);
                glow(x, y, 60, 0.18, new Color(0xFF9AD8), false);
            }
            default -> glow(x, y, 40, 0.12, Color.WHITE, false);
        }
    }

    // ------------------------------------------------------------------ impactos

    private void impact(CombatEvent e, Fighter target) {
        double x = e.x();
        double y = e.y();
        double dir = e.direction();
        int damage = e.damage();
        if (target != null) {
            whiteFlash.put(target, 0.11);
            knock.computeIfAbsent(target, k -> new double[2])[1] += dir * (180 + damage * 22);
        }
        switch (e.moveId()) {
            case "EMBER" -> fireExplosion(x, y, dir, damage);
            case "WATER_GUN" -> waterSplash(x, y, dir, damage);
            case "THUNDER_SHOCK" -> electricBurst(x, y, damage, target);
            case "SLEEP_POWDER" -> powderPuff(x, y);
            default -> physicalHit(e.moveId(), x, y, dir, damage, target);
        }
        sheetHit(e.moveId(), x, y, dir, damage);
        bigHit(e.moveId(), x, y, target == null ? y + 60 : target.getY(), dir, damage);
        damagePopup(x, y - 30, damage);
    }

    /** Efecto grande de PVFX segun el ataque: es lo que mas se ve en el golpe. */
    private void bigHit(String moveId, double x, double y, double feetY, double dir, int damage) {
        boolean flip = dir < 0;
        boolean heavy = damage >= 12;
        switch (moveId) {
            case "EMBER" -> {
                big.play(PvfxEffects.Effect.FIRE_FIREBALL_IMPACT, x + dir * 30, y, 2, flip);
                if (heavy) big.play(PvfxEffects.Effect.EXPLOSIONS_SMALL_EXPLOSION, x, feetY, 2, flip);
            }
            case "WATER_GUN" -> big.play(PvfxEffects.Effect.WATER_WATER_SPLASH, x, feetY, 2, flip);
            case "THUNDER_SHOCK" -> big.play(PvfxEffects.Effect.LIGHTNING_LIGHTNING_STRIKE, x, feetY, 2, flip);
            case "VINE_WHIP" -> big.play(PvfxEffects.Effect.NATURE_VINE_SNARE, x, feetY, 2, flip);
            case "SLEEP_POWDER" -> big.play(PvfxEffects.Effect.NATURE_SPORE_BURST, x, feetY, 2, flip);
            case "FIRE_FANG" -> big.play(PvfxEffects.Effect.FIRE_FIREBALL_IMPACT, x + dir * 30, y, 2, flip);
            case "DIG" -> {
                big.play(PvfxEffects.Effect.IMPACTS_GROUND_SLAM, x, feetY, 2, flip);
                big.play(PvfxEffects.Effect.IMPACTS_ROCK_SHATTER, x, y + 20, 2, flip);
            }
            case "ROCK_SLIDE" -> big.play(PvfxEffects.Effect.IMPACTS_ROCK_SHATTER, x, y + 20, 3, flip);
            case "LICK", "SHADOW_BALL" -> big.play(PvfxEffects.Effect.SHADOW_VOID_IMPLOSION, x, y, 2, flip);
            case "PSYCHIC", "ZEN_HEADBUTT", "PSYCHO_CUT" -> big.play(PvfxEffects.Effect.EXPLOSIONS_ARCANE_BURST, x, y, 2, flip);
            case "CLOSE_COMBAT", "BODY_SLAM" -> big.play(PvfxEffects.Effect.IMPACTS_CRIT_HIT, x, y, 2, flip);
            default -> big.play(heavy ? PvfxEffects.Effect.IMPACTS_CRIT_HIT : PvfxEffects.Effect.IMPACTS_PUNCH_HIT,
                    x, y, heavy ? 2 : 3, flip);
        }
    }

    /** Animacion pixel art encima del golpe, segun el tipo de ataque. */
    private void sheetHit(String moveId, double x, double y, double dir, int damage) {
        boolean flip = dir < 0;
        double big = damage >= 12 ? 1.35 : 1.0;
        switch (moveId) {
            case "EMBER" -> {
                sheets.play(PixelFxSheets.Sheet.BURST, x, y - 10, 3 * big, 0.55, flip);
                sheets.play(PixelFxSheets.Sheet.FIRE_RING, x, y, 3 * big, 0.45, flip);
            }
            case "WATER_GUN" -> {
                sheets.play(PixelFxSheets.Sheet.SPLASH, x, y - 30, 4 * big, 0.5, flip);
                sheets.play(PixelFxSheets.Sheet.DROPS, x, y, 3, 0.45, flip);
            }
            case "THUNDER_SHOCK" -> {
                sheets.play(PixelFxSheets.Sheet.SPARK, x, y, 4 * big, 0.45, flip);
                sheets.play(PixelFxSheets.Sheet.SHOCKWAVE, x, y, 3, 0.35, flip);
            }
            case "SLEEP_POWDER" -> sheets.play(PixelFxSheets.Sheet.TWIRL, x, y - 20, 3, 0.7, flip);
            case "VINE_WHIP", "GROWTH" -> sheets.play(PixelFxSheets.Sheet.RING, x, y, 3, 0.4, flip);
            default -> {
                sheets.play(PixelFxSheets.Sheet.SPARK, x, y, 3 * big, 0.35, flip);
                sheets.play(PixelFxSheets.Sheet.SHOCKWAVE, x, y, 2.5 * big, 0.3, flip);
            }
        }
    }

    private void fireExplosion(double x, double y, double dir, int damage) {
        sound.play(FightingSound.Sfx.FIRE_HIT);
        glow(x, y, 120, 0.22, new Color(0xFFE08A), false);
        glow(x, y, 70, 0.12, Color.WHITE, false);
        ring(x, y, 10, 220, 0.32, new Color(0xFF9A3C), 0, 6);
        for (int i = 0; i < 30; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double speed = 90 + random.nextDouble() * 320;
            Particle p = pixel(x, y, Math.cos(a) * speed + dir * 60, Math.sin(a) * speed - 40,
                    6 + random.nextDouble() * 8, 0.3 + random.nextDouble() * 0.3,
                    new Color(0xFFF6B0), new Color(0xB4241A));
            p.drag = 3.5;
            p.gravity = -180;
            p.grow = -6;
        }
        for (int i = 0; i < 14; i++) {
            double a = -Math.PI / 2 + (random.nextDouble() - 0.5) * 2.4;
            double speed = 260 + random.nextDouble() * 360;
            Particle p = spark(x, y, Math.cos(a) * speed, Math.sin(a) * speed,
                    0.35 + random.nextDouble() * 0.3, new Color(0xFFE27A), new Color(0xFF5A1F));
            p.gravity = 900;
        }
        for (int i = 0; i < 16; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double speed = 40 + random.nextDouble() * 170;
            Particle p = firePuff(x + Math.cos(a) * 8, y + Math.sin(a) * 8,
                    Math.cos(a) * speed + dir * 40, Math.sin(a) * speed * 0.8 - 30,
                    26 + random.nextDouble() * 22, 0.4 + random.nextDouble() * 0.25);
            p.delay = random.nextDouble() * 0.05;
        }
        smoke(x, y - 20, 4, new Color(170, 160, 160));
        // Explosiones secundarias un instante despues
        for (int k = 0; k < 3; k++) {
            double ox = x + (random.nextDouble() - 0.5) * 70;
            double oy = y + (random.nextDouble() - 0.5) * 50;
            for (int i = 0; i < 5; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                Particle p = firePuff(ox, oy, Math.cos(a) * 80, Math.sin(a) * 60 - 20,
                        18 + random.nextDouble() * 12, 0.35);
                p.delay = 0.08 + k * 0.07;
            }
        }
        ring(x, y, 20, 420, 0.4, new Color(0xFFD0A0), 0.03, 3);
        debris(x, groundY - 4, 8, 0.9, new Color(0x6E7F6A));
        decal("burn", x, 70 + damage * 3, 4.5);
        punch(x, y, 0.035);
        shake(7 + damage * 0.25, 0.25);
        hitStop(0.075);
        screenFlash(new Color(0xFF9A3C), 0.08, 0.12f);
    }

    private void waterSplash(double x, double y, double dir, int damage) {
        sound.play(FightingSound.Sfx.WATER_HIT);
        glow(x, y, 90, 0.18, new Color(0x9BEBFF), false);
        ring(x, y, 8, 180, 0.30, new Color(0xBDF3FF), 0, 5);
        ring(x, y, 8, 140, 0.30, new Color(0x55C6FF), 0.08, 4);
        for (int i = 0; i < 30; i++) {
            double a = -Math.PI / 2 + (random.nextDouble() - 0.5) * 2.6 + dir * 0.35;
            double speed = 160 + random.nextDouble() * 380;
            Particle p = make(Shape.WATER, x, y, Math.cos(a) * speed, Math.sin(a) * speed,
                    6 + random.nextDouble() * 7, 0.5 + random.nextDouble() * 0.35,
                    Color.WHITE, Color.WHITE);
            p.gravity = 1100;
            p.drag = 0.8;
        }
        for (int i = 0; i < 10; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double speed = 40 + random.nextDouble() * 130;
            Particle p = make(Shape.WATER, x, y, Math.cos(a) * speed + dir * 50,
                    Math.sin(a) * speed - 40, 20 + random.nextDouble() * 16, 0.35 + random.nextDouble() * 0.15,
                    Color.WHITE, Color.WHITE);
            p.grow = 30;
            p.drag = 4;
        }
        smoke(x, y, 5, new Color(190, 235, 255));
        // Columna de agua que sube
        for (int i = 0; i < 14; i++) {
            Particle p = make(Shape.WATER, x + (random.nextDouble() - 0.5) * 24, y,
                    (random.nextDouble() - 0.5) * 60, -380 - random.nextDouble() * 320,
                    10 + random.nextDouble() * 10, 0.7, Color.WHITE, Color.WHITE);
            p.gravity = 1100;
        }
        decal("wet", x, 90, 5);
        punch(x, y, 0.025);
        shake(5 + damage * 0.2, 0.2);
        hitStop(0.06);
    }

    private void electricBurst(double x, double y, int damage, Fighter target) {
        sound.play(FightingSound.Sfx.ELECTRIC_HIT);
        glow(x, y, 130, 0.18, new Color(0xFFF46B), false);
        for (int i = 0; i < 7; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double len = 60 + random.nextDouble() * 80;
            bolt(x, y, x + Math.cos(a) * len, y + Math.sin(a) * len, 0.16, 3f, new Color(0xFFF26B));
        }
        sparks(x, y, 22, 560, new Color(0xFFFFFF), new Color(0xFFD21F));
        ring(x, y, 6, 240, 0.22, new Color(0xFFFBC2), 0, 4);
        // Rayo que cae del cielo sobre el golpeado
        bolt(x + (random.nextDouble() - 0.5) * 80, -30, x, groundY - 10, 0.22, 7f, new Color(0xFFF26B));
        bolt(x + (random.nextDouble() - 0.5) * 120, -30, x, groundY - 10, 0.16, 4f, new Color(0xFFF7B0));
        sparks(x, groundY - 6, 14, 420, Color.WHITE, new Color(0xFFE74A));
        debris(x, groundY - 4, 6, 0.8, new Color(0x6E7F6A));
        decal("scorch", x, 80, 4);
        punch(x, y, 0.04);
        if (target != null) whiteFlash.put(target, 0.22);
        shake(7 + damage * 0.25, 0.25);
        hitStop(0.09);
        screenFlash(new Color(0xFFF7B0), 0.12, 0.5f);
    }

    private void powderPuff(double x, double y) {
        sound.play(FightingSound.Sfx.POWDER);
        glow(x, y, 90, 0.4, new Color(0xD7A6FF), false);
        for (int i = 0; i < 24; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double speed = 30 + random.nextDouble() * 140;
            Particle p = pixel(x, y, Math.cos(a) * speed, Math.sin(a) * speed,
                    3 + random.nextDouble() * 5, 0.6 + random.nextDouble() * 0.5,
                    new Color(0xFFF0FF), new Color(0x7B3FB0));
            p.drag = 2;
            p.gravity = -30;
        }
        Particle z = make(Shape.ZZZ, x + 20, y - 30, 20, -50, 26, 1.2,
                new Color(0xF4E3FF), new Color(0xB98AE0));
        z.drag = 0.5;
        hitStop(0.04);
    }

    private void physicalHit(String moveId, double x, double y, double dir, int damage, Fighter target) {
        boolean heavy = damage >= 11 || "FLY".equals(moveId);
        sound.play(heavy ? FightingSound.Sfx.HIT_HEAVY : FightingSound.Sfx.HIT_LIGHT);
        if ("BITE".equals(moveId)) sound.play(FightingSound.Sfx.BITE, 0.8f);
        Color accent = switch (moveId) {
            case "VINE_WHIP" -> new Color(0x8BE36B);
            case "BITE" -> new Color(0xFF5A5A);
            case "QUICK_ATTACK" -> new Color(0xFFE36E);
            case "FLY" -> new Color(0xCDEFFF);
            default -> new Color(0xFFC46B);
        };
        Particle star = make(Shape.STAR, x, y, 0, 0, heavy ? 46 : 34, 0.16, Color.WHITE, accent);
        star.grow = heavy ? 260 : 190;
        star.angle = random.nextDouble();
        glow(x, y, heavy ? 110 : 80, 0.14, accent, false);
        ring(x, y, 6, heavy ? 230 : 170, 0.22, Color.WHITE, 0, heavy ? 5 : 4);
        sparks(x, y, heavy ? 18 : 12, heavy ? 560 : 420, Color.WHITE, accent);
        if ("BITE".equals(moveId)) {
            for (int i = -1; i <= 1; i += 2) {
                Particle slash = make(Shape.SLASH, x + i * 10, y, 0, 0, 52, 0.18,
                        Color.WHITE, new Color(0xFF4040));
                slash.angle = i * 0.5 + (dir > 0 ? 0 : Math.PI);
            }
        }
        if ("VINE_WHIP".equals(moveId)) leaves(x, y, 12);
        if (target != null) {
            dust(target.getX(), target.getY(), dir, heavy ? 10 : 6, heavy ? 1.3 : 1);
            debris(target.getX(), groundY - 4, heavy ? 7 : 3, heavy ? 1 : 0.7, new Color(0x6E7F6A));
            if (heavy) decal("crack", target.getX(), 80, 4);
        }
        punch(x, y, heavy ? 0.04 : 0.02);
        if ("FLY".equals(moveId)) speedLines(x, y, 0.16);
        shake(3 + damage * (heavy ? 0.55 : 0.4), heavy ? 0.24 : 0.16);
        hitStop(heavy ? 0.085 : 0.055);
    }

    private void genericHit(Fighter f, int damage) {
        double x = f.getX();
        double y = f.getY() - f.getRenderHeight() * 0.55;
        whiteFlash.put(f, 0.09);
        knock.computeIfAbsent(f, k -> new double[2])[1] += (f.isFacingLeft() ? 1 : -1) * 160;
        sparks(x, y, 10, 380, Color.WHITE, new Color(0xFFE59A));
        glow(x, y, 70, 0.12, new Color(0xFFF2C2), false);
        sound.play(FightingSound.Sfx.HIT_LIGHT, 0.7f);
        damagePopup(x, y - 30, damage);
        shake(4, 0.12);
    }

    private void block(double x, double y, double dir) {
        sound.play(FightingSound.Sfx.BLOCK);
        sheets.play(PixelFxSheets.Sheet.TWINKLE, x, y, 3, 0.35, dir < 0);
        big.play(PvfxEffects.Effect.IMPACTS_PARRY, x, y, 2, dir < 0);
        ring(x, y, 14, 90, 0.2, new Color(0xA7E8FF), 0, 5);
        glow(x, y, 70, 0.15, new Color(0x8FDFFF), false);
        sparks(x, y, 10, 320, Color.WHITE, new Color(0x6CCBFF));
        shake(3, 0.1);
        hitStop(0.04);
    }

    /** Parry: destello blanco, congelado un instante y texto grande. */
    private void parry(double x, double y, double dir) {
        sound.play(FightingSound.Sfx.BLOCK, 1f);
        sound.play(FightingSound.Sfx.CUT_IN, 0.7f);
        big.play(PvfxEffects.Effect.IMPACTS_PARRY, x, y, 3, dir < 0);
        ring(x, y, 20, 170, 0.28, Color.WHITE, 0, 6);
        ring(x, y, 10, 110, 0.22, new Color(0xFFE45A), 0, 4);
        glow(x, y, 120, 0.2, new Color(0xFFF6C8), false);
        sparks(x, y, 22, 520, Color.WHITE, new Color(0xFFE45A));
        screenFlash(Color.WHITE, 0.12, 0.45f);
        shake(6, 0.15);
        hitStop(0.16);
        Popup p = new Popup();
        p.text = "¡PARRY!";
        p.x = Math.max(110, Math.min(1170, x));
        p.y = y - 60;
        p.life = 0.9;
        p.maxLife = 0.9;
        p.size = 38;
        p.color = new Color(0xFFE45A);
        addPopup(p);
    }

    /** Tecnicas del modo dificil de Mewtwo. */
    private void bossEvent(FightingGameSession session, FightingGameSession.BossEvent e) {
        Fighter boss = session.getBoss();
        Color pink = new Color(0xFF8FCB);
        Color purple = new Color(0xB78CFF);
        switch (e.kind()) {
            case MEGA -> director.startMega(session);
            case BARRIER -> {
                sound.play(FightingSound.Sfx.BUFF, 0.8f);
                bossPopup("¡BARRERA!", e.x(), e.y() - 230, pink, 26);
            }
            case TELEKINESIS -> {
                sound.play(FightingSound.Sfx.CUT_IN, 0.6f);
                sound.play(FightingSound.Sfx.WHOOSH, 1f);
                ring(e.x(), e.y() - 40, 20, 90, 0.35, purple, 0, 5);
                glow(e.x(), e.y() - 40, 90, 0.4, purple, false);
                bossPopup("¡TELEQUINESIS!", e.x(), e.y() - 130, purple, 26);
            }
            case FUTURE_SIGHT_MARK -> {
                sound.play(FightingSound.Sfx.ULTI_CHARGE, 0.4f);
                bossPopup("¡PREMONICION!", e.x(), e.y() - 150, pink, 24);
            }
            case FUTURE_SIGHT_FALL -> {
                // La columna empieza a caer desde arriba de la pantalla
                fallingColumns.add(new double[]{e.x(), 0, e.y()});
                sound.play(FightingSound.Sfx.WHOOSH, 0.9f);
            }
            case FUTURE_SIGHT_STRIKE -> {
                sound.play(FightingSound.Sfx.THUNDER, 0.7f);
                sound.play(FightingSound.Sfx.HIT_HEAVY, 0.6f);
                ring(e.x(), e.y(), 20, 130, 0.3, pink, 0, 5);
                ring(e.x(), e.y(), 10, 80, 0.22, Color.WHITE, 0, 3);
                sparks(e.x(), e.y() - 10, 12, 420, Color.WHITE, pink);
                if (Math.abs(e.y() - groundY) < 2) decal("crack", e.x(), 110, 3.5);
                shake(7, 0.22);
            }
            case WAVE_CHARGE -> {
                sound.play(FightingSound.Sfx.ULTI_CHARGE, 0.6f);
                ring(e.x(), e.y(), 10, 120, 0.45, pink, 0, 4);
                glow(e.x(), e.y() - 40, 110, 0.45, pink, false);
                bossPopup("¡ONDA MENTAL!", e.x(), e.y() - 260, pink, 26);
            }
            case WAVE -> {
                sound.play(FightingSound.Sfx.THUNDER, 0.5f);
                sound.play(FightingSound.Sfx.WHOOSH, 1f);
                shake(5, 0.25);
            }
            case TRIPLE_BLINK -> bossPopup("¡TELETRANSPORTE TRIPLE!", e.x(), e.y() - 260, purple, 24);
            case VOLLEY -> bossPopup("¡TRIPLE BOLA SOMBRA!", e.x(), e.y() - 240, purple, 22);
        }
    }

    // Columnas de Premonicion cayendo: {x, edad}
    static final double COLUMN_FALL_SECONDS = 0.15;
    private final List<double[]> fallingColumns = new ArrayList<>();

    /** Columna de energia que baja del cielo hasta el suelo y luego se desvanece. */
    private void drawFallingColumns(Graphics2D g) {
        for (double[] column : fallingColumns) {
            double age = column[1];
            int x = (int) column[0];
            double fall = Math.min(1, age / COLUMN_FALL_SECONDS);
            int bottom = (int) (column[2] * fall);
            float alpha = (float) (age <= COLUMN_FALL_SECONDS ? 1 : Math.max(0, 1 - (age - COLUMN_FALL_SECONDS) / 0.3));
            var old = g.getComposite();
            g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
            g.setColor(new Color(255, 111, 208, 150));
            g.fillRect(x - 30, 0, 60, bottom);
            g.setColor(new Color(0xFFD6F0));
            g.fillRect(x - 12, 0, 24, bottom);
            g.setColor(Color.WHITE);
            g.fillRect(x - 4, 0, 8, bottom);
            // Punta brillante mientras cae
            if (fall < 1) g.fillOval(x - 22, bottom - 22, 44, 44);
            g.setComposite(old);
        }
    }

    // Solo un efecto grande de Mewtwo a la vez: si hay otro en curso, este sale en tamano pequeno
    private double bossBigBusyUntil;

    private void bigLimited(PvfxEffects.Effect effect, double x, double y, boolean flip) {
        boolean busy = time < bossBigBusyUntil;
        big.play(effect, x, y, busy ? 1 : 2, flip);
        if (!busy) bossBigBusyUntil = time + effect.seconds();
    }

    /** Agrega un texto; si hay otro reciente en el mismo lugar, este sube para no encimarse. */
    private void addPopup(Popup p) {
        for (int tries = 0; tries < 5; tries++) {
            boolean moved = false;
            for (Popup other : popups) {
                boolean recent = other.life > other.maxLife - 0.6;
                if (recent && Math.abs(other.x - p.x) < 150 && Math.abs(other.y - p.y) < (other.size + p.size) * 0.6) {
                    p.y = other.y - (other.size + p.size) * 0.7;
                    moved = true;
                }
            }
            if (!moved) break;
        }
        popups.add(p);
    }

    private void bossPopup(String text, double x, double y, Color color, int size) {
        Popup p = new Popup();
        p.text = text;
        p.x = Math.max(150, Math.min(1130, x));
        p.y = y;
        p.life = 1.0;
        p.maxLife = 1.0;
        p.size = size;
        p.color = color;
        addPopup(p);
    }

    /** Mewtwo desaparece en un destello morado y aparece en otro lado. */
    private void bossBlink(Fighter boss, double fromX) {
        Color purple = new Color(0xB78CFF);
        double midY = boss.getY() - boss.getRenderHeight() * 0.5;
        sound.play(FightingSound.Sfx.WHOOSH, 1f);
        sound.play(FightingSound.Sfx.BUFF, 0.5f);
        // Efecto corto: un anillo donde desaparece y un destello pequeno donde aparece
        ring(fromX, midY, 14, 70, 0.2, purple, 0, 3);
        bigLimited(PvfxEffects.Effect.EXPLOSIONS_ARCANE_BURST, boss.getX(), midY, boss.isFacingLeft());
        sparks(boss.getX(), midY, 8, 300, Color.WHITE, purple);
    }

    /** El Pokemon que entra cae al suelo: onda de choque y polvo. */
    private void entranceLanding(Fighter f) {
        sound.play(FightingSound.Sfx.LAND, 1f);
        sound.play(FightingSound.Sfx.HIT_HEAVY, 0.5f);
        big.play(PvfxEffects.Effect.IMPACTS_GROUND_SLAM, f.getX(), f.getY() + 4, 2, f.isFacingLeft());
        ring(f.getX(), f.getY(), 10, 140, 0.25, new Color(0xFFF2C2), 0, 4);
        smoke(f.getX() - 30, f.getY(), 4, new Color(220, 205, 180));
        smoke(f.getX() + 30, f.getY(), 4, new Color(220, 205, 180));
        shake(5, 0.18);
    }

    private void fizzle(double x, double y) {
        smoke(x, y, 4, new Color(200, 170, 230));
        sparks(x, y, 6, 160, Color.WHITE, new Color(0xB987D8));
    }

    private void knockOut(Fighter f) {
        double x = f.getX();
        double y = f.getY() - f.getRenderHeight() * 0.5;
        sound.play(FightingSound.Sfx.KO);
        sheets.play(PixelFxSheets.Sheet.SHOCKWAVE, x, y, 5, 0.6, false);
        big.play(PvfxEffects.Effect.EXPLOSIONS_BIG_EXPLOSION, x, f.getY() + 10, 3, false);
        glow(x, y, 220, 0.35, Color.WHITE, false);
        for (int i = 0; i < 3; i++) ring(x, y, 10, 420, 0.5, Color.WHITE, i * 0.1, 6);
        sparks(x, y, 40, 700, Color.WHITE, new Color(0xFFE36E));
        smoke(x, f.getY() - 20, 10, new Color(120, 120, 120));
        debris(x, groundY - 4, 14, 1.2, new Color(0x6E7F6A));
        decal("crack", x, 130, 6);
        punch(x, y, 0.08);
        speedLines(x, y, 0.45);
        impactFrame = 0.08;
        shake(14, 0.5);
        hitStop(0.22);
        screenFlash(Color.WHITE, 0.16, 0.55f);
        // El texto grande de K.O. lo dibuja FightingDirector
    }

    // ------------------------------------------------------------------ efectos continuos

    private void jumpAndLanding(Fighter f) {
        boolean wasOnGround = lastOnGround.getOrDefault(f, f.isOnGround());
        double lastVy = lastVelocityY.getOrDefault(f, 0.0);
        if (wasOnGround && !f.isOnGround() && f.getVelocityY() < -400) {
            dust(f.getX(), f.getY(), 0, 6, 0.8);
            sound.play(FightingSound.Sfx.JUMP, 0.45f);
        } else if (!wasOnGround && f.isOnGround() && lastVy > 350) {
            dust(f.getX(), f.getY(), 0, lastVy > 700 ? 12 : 7, lastVy > 700 ? 1.4 : 0.9);
            big.play(PvfxEffects.Effect.SMOKE_DUST_PUFF, f.getX(), f.getY() + 4, 2, false);
            sound.play(FightingSound.Sfx.LAND, 0.6f);
            if (lastVy > 700) shake(4, 0.12);
        }
        lastOnGround.put(f, f.isOnGround());
        lastVelocityY.put(f, f.getVelocityY());
    }

    private void continuousEffects(FightingGameSession session, Fighter f, double dt) {
        FightingCombatSystem.AttackVisual visual = session.getCombatSystem()
                .getAttackVisual(f, session.getFighters());
        if (visual != null && "STARTUP".equals(visual.phase())) {
            String move = visual.definition().moveId();
            Color charge = switch (move) {
                case "EMBER" -> new Color(0xFF8A2A);
                case "WATER_GUN" -> new Color(0x5BC8FF);
                case "THUNDER_SHOCK" -> new Color(0xFFE74A);
                case "SLEEP_POWDER" -> new Color(0xC88BFF);
                default -> null;
            };
            if (charge != null) {
                // Energia que se junta en la boca antes de lanzar
                double[] mouth = MouthPoint.of(f, move);
                double mx = mouth[0];
                double my = mouth[1];
                for (int i = 0; i < 3; i++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    double r = 40 + random.nextDouble() * 40;
                    Particle p = pixel(mx + Math.cos(a) * r, my + Math.sin(a) * r,
                            -Math.cos(a) * r * 7, -Math.sin(a) * r * 7, 4 + random.nextDouble() * 3,
                            0.13, Color.WHITE, charge);
                }
                if (random.nextDouble() < dt * 30) glow(mx, my, 34, 0.08, charge, false);
            }
        }
        if (visual != null && "ACTIVE".equals(visual.phase())) {
            String move = visual.definition().moveId();
            double dir = visual.direction();
            double midY = f.getY() - f.getRenderHeight() * 0.5;
            switch (move) {
                case "TACKLE", "QUICK_ATTACK", "FLY" -> {
                    // Lineas de velocidad detras del pokemon
                    if (random.nextDouble() < dt * 60) {
                        double y = midY + (random.nextDouble() - 0.5) * f.getRenderHeight() * 0.8;
                        double x = f.getX() - dir * f.getBodyHalfWidth();
                        Particle p = spark(x, y, -dir * (500 + random.nextDouble() * 300), 0,
                                0.14, Color.WHITE, new Color(0xD8F1FF));
                        p.size = 3;
                    }
                }
                case "VINE_WHIP" -> {
                    if (random.nextDouble() < dt * 25) {
                        double reach = visual.definition().hitboxWidth() * Math.sin(Math.PI * visual.activeProgress());
                        leaves(f.getX() + dir * (f.getBodyHalfWidth() + reach),
                                f.getY() - visual.definition().hitboxHeight() * 0.6, 1);
                    }
                }
                default -> { }
            }
        }
        if (f.isAsleep() && random.nextDouble() < dt * 1.6) {
            Particle z = make(Shape.ZZZ, f.getX() + f.getBodyHalfWidth() * 0.4,
                    f.getY() - f.getRenderHeight() * 0.9, 25, -45, 18, 1.1,
                    new Color(0xF4E3FF), new Color(0xB98AE0));
            z.drag = 0.4;
        }
        if (f.hasAttackBoost() && random.nextDouble() < dt * 10) {
            double x = f.getX() + (random.nextDouble() * 2 - 1) * f.getBodyHalfWidth();
            Particle p = pixel(x, f.getY() - random.nextDouble() * f.getRenderHeight() * 0.5, 0,
                    -90 - random.nextDouble() * 60, 3 + random.nextDouble() * 3, 0.6,
                    new Color(0xE7FFB8), new Color(0x4FC24F));
            p.behind = true;
        }
        if (f.isEvolutionAnimating() && random.nextDouble() < dt * 45) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = f.getRenderHeight() * 0.7;
            double cy = f.getY() - f.getRenderHeight() * 0.5;
            Particle p = pixel(f.getX() + Math.cos(a) * r, cy + Math.sin(a) * r,
                    -Math.cos(a) * r * 2.2, -Math.sin(a) * r * 2.2, 4 + random.nextDouble() * 3,
                    0.4, Color.WHITE, new Color(0xFFD45A));
            p.drag = 1;
        }
    }

    private void projectileTrails(FightingGameSession session, double dt) {
        List<FightingProjectile> projectiles = session.getProjectiles();
        projectileOrigins.keySet().retainAll(projectiles);
        for (FightingProjectile p : projectiles) {
            projectileOrigins.putIfAbsent(p, new double[]{p.getX(), p.getY(), time});
            double dir = Math.signum(p.getVelocityX());
            double x = p.getX();
            double y = p.getY();
            switch (p.getMoveId()) {
                case "EMBER" -> {
                    Particle puff = firePuff(x - dir * 6, y + (random.nextDouble() - 0.5) * 8,
                            -dir * (30 + random.nextDouble() * 50), -20 - random.nextDouble() * 30,
                            12 + random.nextDouble() * 8, 0.22 + random.nextDouble() * 0.1);
                    puff.grow = 10;
                    for (int i = 0; i < 2; i++) {
                        Particle f = pixel(x + (random.nextDouble() - 0.5) * 10,
                                y + (random.nextDouble() - 0.5) * 12,
                                -dir * (40 + random.nextDouble() * 80), -30 - random.nextDouble() * 60,
                                4 + random.nextDouble() * 4, 0.3 + random.nextDouble() * 0.2,
                                new Color(0xFFE88A), new Color(0xE8401E));
                        f.grow = -8;
                    }
                }
                case "WATER_GUN" -> {
                    for (int i = 0; i < 2; i++) {
                        Particle d = pixel(x - dir * random.nextDouble() * 14,
                                y + (random.nextDouble() - 0.5) * 12,
                                -dir * (30 + random.nextDouble() * 60), (random.nextDouble() - 0.6) * 90,
                                2 + random.nextDouble() * 4, 0.3 + random.nextDouble() * 0.2,
                                Color.WHITE, new Color(0x2C8BE0));
                        d.gravity = 600;
                    }
                }
                case "THUNDER_SHOCK" -> {
                    if (random.nextDouble() < dt * 40) {
                        sparks(x, y, 1, 220, Color.WHITE, new Color(0xFFD21F));
                    }
                }
                case "SLEEP_POWDER" -> {
                    if (random.nextDouble() < dt * 30) {
                        Particle s = pixel(x + (random.nextDouble() - 0.5) * 20,
                                y + (random.nextDouble() - 0.5) * 20, -dir * 20,
                                -15, 2 + random.nextDouble() * 3, 0.6,
                                new Color(0xFFF0FF), new Color(0x8E4BC4));
                        s.drag = 1;
                    }
                }
                default -> { }
            }
        }
    }

    private void ultimates(FightingGameSession session, double dt) {
        UltimateController.Phase[] now = new UltimateController.Phase[2];
        UltimateController.Visual[] byOwner = new UltimateController.Visual[2];
        for (UltimateController.Visual v : session.getCombatSystem().getUltimateVisuals()) {
            now[v.ownerIndex()] = v.phase();
            byOwner[v.ownerIndex()] = v;
            Fighter owner = v.owner();
            Color primary = v.definition().primaryColor();
            double cx = owner.getX();
            double cy = owner.getY() - owner.getRenderHeight() * 0.55;
            if (v.phase() == UltimateController.Phase.CHARGING && random.nextDouble() < dt * 70) {
                // Energia que se junta en el pokemon
                double a = random.nextDouble() * Math.PI * 2;
                double r = 150 + random.nextDouble() * 70;
                Particle p = pixel(cx + Math.cos(a) * r, cy + Math.sin(a) * r,
                        -Math.cos(a) * r * 2.6, -Math.sin(a) * r * 2.6, 4 + random.nextDouble() * 4,
                        0.38, Color.WHITE, primary);
                p.drag = 0.5;
            } else if (v.phase() == UltimateController.Phase.RELEASING) {
                if (random.nextDouble() < dt * 20) shake(4, 0.1);
            }
        }
        for (int i = 0; i < 2; i++) {
            if (now[i] != lastUltimatePhase[i]) {
                if (now[i] == UltimateController.Phase.CHARGING) {
                    sound.play(FightingSound.Sfx.ULTI_CHARGE);
                    // Franja con el pokemon y el nombre del ataque antes de soltarlo
                    director.startCutIn(i, byOwner[i].owner(), byOwner[i].definition());
                    Fighter o = byOwner[i].owner();
                    big.play(PvfxEffects.Effect.STATUS_POWER_UP_AURA, o.getX(), o.getY() + 6, 3, false,
                            0, byOwner[i].definition().chargeSeconds() + 0.3);
                } else if (now[i] == UltimateController.Phase.RELEASING) {
                    ultimateRelease(byOwner[i]);
                    sound.play(FightingSound.Sfx.ULTI_RELEASE);
                    shake(12, 0.5);
                    screenFlash(Color.WHITE, 0.14, 0.5f);
                    hitStop(0.12);
                }
            }
            lastUltimatePhase[i] = now[i];
        }
    }

    /** Lo grande de cada ulti al soltarla: llamarada, ola, tormenta de hojas o tornado. */
    private void ultimateRelease(UltimateController.Visual v) {
        if (v == null) return;
        Fighter owner = v.owner();
        Fighter target = v.target();
        UltimateDefinition d = v.definition();
        double dir = target.getX() >= owner.getX() ? 1 : -1;
        boolean flip = dir < 0;
        double seconds = d.pulseCount() * d.pulseIntervalSeconds() + 0.4;
        double startX = owner.getX() + dir * owner.getBodyHalfWidth() * 0.8;
        double mouthY = owner.getY() - owner.getRenderHeight() * 0.45;
        double distance = Math.max(80, Math.abs(target.getX() - startX));
        // Los rayos decorativos son horizontales: si el rival esta en una plataforma (mas alto
        // o mas bajo) no se dibujan, para que solo se vea el rayo real que apunta hacia el
        boolean level = Math.abs(owner.getY() - target.getY()) < 40;
        switch (d.moveId()) {
            case "FIRE_BLAST" -> {
                if (!level) break;
                double step = 128 * 3 * 0.8;
                for (int k = 0; k * step < distance; k++) {
                    big.play(PvfxEffects.Effect.FIRE_FLAMETHROWER, startX + dir * k * step, mouthY, 3, flip,
                            k * 0.05, seconds);
                }
            }
            case "HYDRO_PUMP" -> {
                double step = 96 * 3 * 0.7;
                for (int k = 0; k * step < distance; k++) {
                    big.play(PvfxEffects.Effect.WATER_TIDAL_WAVE, startX + dir * (k * step + 140), owner.getY() + 6,
                            3, flip, k * 0.08, seconds);
                }
            }
            case "LEAF_STORM" -> {
                if (!level) break;
                double step = 80 * 3 * 0.8;
                for (int k = 0; k * step < distance; k++) {
                    big.play(PvfxEffects.Effect.NATURE_LEAF_STORM, startX + dir * k * step, mouthY, 3, flip,
                            k * 0.06, seconds);
                }
            }
            case "INFERNO_BURST" -> big.play(PvfxEffects.Effect.FIRE_FIRE_TORNADO, owner.getX(), owner.getY() + 6,
                    4, false, 0, seconds);
            case "THUNDER" -> big.play(PvfxEffects.Effect.STATUS_ELECTRIFIED, owner.getX(), owner.getY() + 6,
                    4, false, 0, seconds);
            case "FLAMETHROWER" -> {
                double step = 128 * 3 * 0.8;
                for (int k = 0; k * step < distance; k++) {
                    big.play(PvfxEffects.Effect.FIRE_FLAMETHROWER, startX + dir * k * step, mouthY, 3, flip,
                            k * 0.05, seconds);
                }
            }
            case "EARTHQUAKE" -> big.play(PvfxEffects.Effect.IMPACTS_GROUND_SLAM, owner.getX(), owner.getY() + 6,
                    3, false);
            case "SHADOW_STORM" -> big.play(PvfxEffects.Effect.SHADOW_SHADOW_BOLT, startX + dir * 120, mouthY, 3, flip,
                    0, seconds);
            case "DYNAMIC_PUNCH" -> big.play(PvfxEffects.Effect.STATUS_POWER_UP_AURA, owner.getX(), owner.getY() + 6,
                    3, false, 0, seconds);
            case "PSYSTRIKE" -> {
                big.play(PvfxEffects.Effect.ARCANE_MAGIC_CIRCLE, owner.getX(), owner.getY() + 6, 3, false);
                if (!level) break;
                double step = 168 * 3 * 0.85;
                for (int k = 0; k * step < distance; k++) {
                    big.play(PvfxEffects.Effect.ARCANE_ARCANE_BEAM, startX + dir * k * step, mouthY, 3, flip,
                            k * 0.05, seconds);
                }
            }
            default -> { }
        }
    }

    /** Cada golpe de la ulti en el rival. */
    private void ultimatePulse(UltimateDefinition d, Fighter target) {
        double x = target.getX();
        double feet = target.getY() + 6;
        switch (d.moveId()) {
            case "FIRE_BLAST" -> {
                big.play(PvfxEffects.Effect.FIRE_FIRE_PILLAR, x, feet, 3, false);
                big.play(PvfxEffects.Effect.EXPLOSIONS_FIRE_NOVA, x, feet - 10, 3, false);
            }
            case "HYDRO_PUMP" -> {
                big.play(PvfxEffects.Effect.WATER_WATER_GEYSER, x, feet, 3, false);
                big.play(PvfxEffects.Effect.WATER_WATER_SPLASH, x, feet, 3, false);
            }
            case "LEAF_STORM" -> {
                big.play(PvfxEffects.Effect.HOLY_HOLY_SMITE, x, feet, 3, false);
                big.play(PvfxEffects.Effect.NATURE_SPORE_BURST, x, feet, 3, false);
            }
            case "FLAMETHROWER" -> {
                big.play(PvfxEffects.Effect.FIRE_FIRE_PILLAR, x, feet, 3, false);
                big.play(PvfxEffects.Effect.FIRE_LAVA_BURST, x, feet, 3, false);
            }
            case "EARTHQUAKE" -> {
                big.play(PvfxEffects.Effect.NATURE_EARTH_SPIKE, x, feet, 3, false);
                big.play(PvfxEffects.Effect.IMPACTS_ROCK_SHATTER, x, feet - 20, 3, false);
                shake(10, 0.25);
            }
            case "SHADOW_STORM" -> {
                big.play(PvfxEffects.Effect.SHADOW_VOID_IMPLOSION, x, target.getY() - target.getRenderHeight() * 0.5, 3, false);
            }
            case "DYNAMIC_PUNCH" -> {
                big.play(PvfxEffects.Effect.IMPACTS_CRIT_HIT, x, target.getY() - target.getRenderHeight() * 0.5, 3, false);
                big.play(PvfxEffects.Effect.EXPLOSIONS_BIG_EXPLOSION, x, feet, 2, false);
            }
            case "PSYSTRIKE" -> {
                big.play(PvfxEffects.Effect.EXPLOSIONS_IMPLOSION, x, target.getY() - target.getRenderHeight() * 0.5, 3, false);
                big.play(PvfxEffects.Effect.EXPLOSIONS_ARCANE_BURST, x, target.getY() - target.getRenderHeight() * 0.5, 3, false);
            }
            case "THUNDER" -> {
                big.play(PvfxEffects.Effect.LIGHTNING_LIGHTNING_STRIKE, x, feet, 3, false);
                big.play(PvfxEffects.Effect.IMPACTS_SHOCKWAVE_RING, x, feet - 6, 3, false);
            }
            case "INFERNO_BURST" -> {
                big.play(PvfxEffects.Effect.EXPLOSIONS_FIRE_NOVA, x, feet - 10, 3, false);
                big.play(PvfxEffects.Effect.FIRE_LAVA_BURST, x, feet, 3, false);
            }
            default -> big.play(PvfxEffects.Effect.IMPACTS_CRIT_HIT, x, target.getY() - target.getRenderHeight() * 0.5, 2, false);
        }
    }

    /** Draws the PVFX effects at full resolution (outside the posterized layer). */
    public void drawBig(Graphics2D g) {
        big.draw(g);
        drawFallingColumns(g);
    }

    // ------------------------------------------------------------------ ayudantes para crear efectos

    private Particle make(Shape shape, double x, double y, double vx, double vy,
                          double size, double life, Color from, Color to) {
        Particle p = new Particle();
        p.shape = shape;
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.size = size;
        p.life = life;
        p.maxLife = life;
        p.from = from;
        p.to = to;
        if (particles.size() < 1400) particles.add(p);
        return p;
    }

    private Particle pixel(double x, double y, double vx, double vy, double size,
                           double life, Color from, Color to) {
        return make(Shape.PIXEL, x, y, vx, vy, size, life, from, to);
    }

    private Particle spark(double x, double y, double vx, double vy, double life,
                           Color from, Color to) {
        Particle p = make(Shape.SPARK, x, y, vx, vy, 3, life, from, to);
        p.drag = 3;
        return p;
    }

    private void sparks(double x, double y, int count, double maxSpeed, Color from, Color to) {
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double speed = maxSpeed * (0.35 + random.nextDouble() * 0.65);
            spark(x, y, Math.cos(a) * speed, Math.sin(a) * speed, 0.18 + random.nextDouble() * 0.2,
                    from, to);
        }
    }

    private void glow(double x, double y, double size, double life, Color color, boolean behind) {
        Particle p = make(Shape.GLOW, x, y, 0, 0, size, life, color, color);
        p.grow = size * 1.5;
        p.behind = behind;
    }

    private void ring(double x, double y, double size, double grow, double life,
                      Color color, double delay, double width) {
        Particle p = make(Shape.RING, x, y, 0, 0, size, life, color, color);
        p.grow = grow;
        p.delay = delay;
        p.angle = width;
    }

    private void smoke(double x, double y, int count, Color color) {
        for (int i = 0; i < count; i++) {
            Particle p = make(Shape.SMOKE, x + (random.nextDouble() - 0.5) * 24,
                    y + (random.nextDouble() - 0.5) * 16, (random.nextDouble() - 0.5) * 50,
                    -40 - random.nextDouble() * 50, 10 + random.nextDouble() * 10,
                    0.6 + random.nextDouble() * 0.4, color, color);
            p.grow = 40;
            p.drag = 1.2;
        }
    }

    /** Bola de fuego que crece y pasa de amarillo a rojo y luego a humo. */
    private Particle firePuff(double x, double y, double vx, double vy, double size, double life) {
        Particle p = make(Shape.FIRE, x, y, vx, vy, size, life, Color.WHITE, Color.WHITE);
        p.grow = size * 1.4;
        p.drag = 3;
        p.gravity = -140;
        return p;
    }

    private void dust(double x, double feetY, double dir, int count, double strength) {
        for (int i = 0; i < count; i++) {
            double side = dir == 0 ? (random.nextBoolean() ? 1 : -1) : dir;
            Particle p = make(Shape.SMOKE, x + (random.nextDouble() - 0.5) * 30, feetY - 4,
                    side * (60 + random.nextDouble() * 140) * strength, -20 - random.nextDouble() * 50,
                    6 + random.nextDouble() * 6, 0.35 + random.nextDouble() * 0.25,
                    new Color(0xE8E2CF), new Color(0xB9AE8E));
            p.grow = 30;
            p.drag = 3;
        }
    }

    private void leaves(double x, double y, int count) {
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double speed = 60 + random.nextDouble() * 220;
            Particle p = make(Shape.LEAF, x, y, Math.cos(a) * speed, Math.sin(a) * speed - 40,
                    7 + random.nextDouble() * 4, 0.5 + random.nextDouble() * 0.4,
                    new Color(0xA9F07A), new Color(0x2F8C3A));
            p.gravity = 260;
            p.drag = 2.5;
            p.spin = (random.nextDouble() - 0.5) * 16;
        }
    }

    private void debris(double x, double y, int count, double strength, Color color) {
        for (int i = 0; i < count; i++) {
            double a = -Math.PI / 2 + (random.nextDouble() - 0.5) * 2.4;
            double speed = (180 + random.nextDouble() * 380) * strength;
            Particle p = make(Shape.DEBRIS, x + (random.nextDouble() - 0.5) * 20, y,
                    Math.cos(a) * speed, Math.sin(a) * speed, 5 + random.nextDouble() * 7,
                    1.1 + random.nextDouble() * 0.6, color, color.darker());
            p.gravity = 1500;
            p.drag = 0.3;
            p.spin = (random.nextDouble() - 0.5) * 18;
        }
    }

    private void decal(String kind, double x, double size, double life) {
        Decal d = new Decal();
        d.kind = kind;
        d.x = x;
        d.y = groundY;
        d.size = size;
        d.life = life;
        d.maxLife = life;
        d.seed = random.nextLong();
        if (decals.size() > 30) decals.remove(0);
        decals.add(d);
    }

    private void punch(double x, double y, double amount) {
        if (amount >= zoom) {
            zoom = amount;
            zoomFocusX = x;
            zoomFocusY = y;
        }
    }

    private void speedLines(double x, double y, double seconds) {
        speedLines = seconds;
        speedLinesX = x;
        speedLinesY = y;
    }

    /** Extra zoom of the camera (0 = normal): the bigger of the hit "punch" and the follow camera. */
    public double zoom() {
        return Math.max(zoom, cameraZoom);
    }

    public double zoomFocusX() {
        return zoom >= cameraZoom ? zoomFocusX : cameraX;
    }

    public double zoomFocusY() {
        return zoom >= cameraZoom ? zoomFocusY : cameraY;
    }

    // ------------------------------------------------------------------ camara dinamica

    /** Distance between fighters below which the camera starts to get closer. */
    static final double CAMERA_NEAR_DISTANCE = 520;
    static final double CAMERA_MAX_ZOOM = 0.16;
    private double cameraZoom;
    private double cameraX = 640;
    private double cameraY = 420;

    /**
     * Follow camera: when both Pokemon are close it zooms in a little on the middle point;
     * when they separate it goes back to the full arena. Zooming in around a point inside
     * the arena never shows anything outside it, so nobody can leave the screen.
     */
    private void followCamera(FightingGameSession session, double dt) {
        List<Fighter> fighters = session.getFighters();
        if (fighters.size() < 2) return;
        Fighter a = fighters.get(0);
        Fighter b = fighters.get(1);
        double gap = Math.abs(a.getX() - b.getX());
        double target = gap >= CAMERA_NEAR_DISTANCE ? 0
                : CAMERA_MAX_ZOOM * (1 - gap / CAMERA_NEAR_DISTANCE);
        double follow = Math.min(1, dt * 2.5);
        cameraZoom += (target - cameraZoom) * follow;
        double midX = (a.getX() + b.getX()) / 2;
        double midY = Math.min(a.getY(), b.getY()) - 150;
        cameraX += (Math.max(0, Math.min(session.getWidth(), midX)) - cameraX) * follow;
        cameraY += (Math.max(0, Math.min(session.getHeight(), midY)) - cameraY) * follow;
    }

    private void bolt(double x1, double y1, double x2, double y2, double life, float width, Color glow) {
        Bolt b = new Bolt();
        b.x1 = x1;
        b.y1 = y1;
        b.x2 = x2;
        b.y2 = y2;
        b.life = life;
        b.maxLife = life;
        b.width = width;
        b.glow = glow;
        bolts.add(b);
    }

    private double coneAngle(double dir, double spread) {
        double base = dir < 0 ? Math.PI : 0;
        return base + (random.nextDouble() - 0.5) * 2 * spread;
    }

    private void damagePopup(double x, double y, int damage) {
        if (damage <= 0) return;
        Popup p = new Popup();
        p.text = "-" + damage;
        p.x = x + (random.nextDouble() - 0.5) * 20;
        p.y = y;
        p.life = 0.7;
        p.maxLife = 0.7;
        p.size = damage >= 11 ? 30 : 24;
        p.color = damage >= 11 ? new Color(0xFFB13B) : Color.WHITE;
        addPopup(p);
    }

    private void switchEffect(Fighter incoming) {
        sound.play(FightingSound.Sfx.WHOOSH, 1f);
        sound.play(FightingSound.Sfx.BUFF, 0.6f);
        big.play(PvfxEffects.Effect.SMOKE_DUST_PUFF, incoming.getX(), incoming.getY() + 6, 3, false);
        big.play(PvfxEffects.Effect.UI_LEVEL_UP, incoming.getX(), incoming.getY() + 6, 2, false);
        Popup p = new Popup();
        p.text = "¡CAMBIO!";
        // Cerca de la pared: el texto se corre hacia adentro para que se lea completo
        p.x = Math.max(110, Math.min(1170, incoming.getX()));
        p.y = incoming.getY() - incoming.getRenderHeight() - 10;
        p.life = 0.9;
        p.maxLife = 0.9;
        p.size = 30;
        p.color = new Color(0xFFE45A);
        addPopup(p);
    }

    private void chipPopup(double x, double y, int damage) {
        if (damage <= 0) return;
        Popup p = new Popup();
        p.text = "-" + damage;
        p.x = x;
        p.y = y;
        p.life = 0.6;
        p.maxLife = 0.6;
        p.size = 20;
        p.color = new Color(0xA7E8FF);
        addPopup(p);
    }

    private void shake(double magnitude, double duration) {
        double current = shakeRemaining > 0 ? shakeMagnitude * shakeRemaining / shakeDuration : 0;
        if (magnitude >= current) {
            shakeMagnitude = magnitude;
            shakeDuration = duration;
            shakeRemaining = duration;
        }
    }

    private void hitStop(double seconds) {
        hitStop = Math.max(hitStop, seconds);
    }

    private void screenFlash(Color color, double duration, float alpha) {
        flashColor = color;
        flashDuration = duration;
        flashRemaining = duration;
        flashAlpha = alpha;
    }

    // ------------------------------------------------------------------ dibujo

    /** Marks on the floor (drawn on the background, behind the Pokemon). */
    public void drawDecals(Graphics2D g) {
        Composite old = g.getComposite();
        for (Decal d : decals) {
            float alpha = (float) Math.min(1, d.life / d.maxLife * 2);
            Random r = new Random(d.seed);
            int x = (int) d.x;
            int y = (int) d.y;
            int w = (int) d.size;
            switch (d.kind) {
                case "burn", "scorch" -> {
                    setAlpha(g, alpha * 0.55f);
                    g.setColor(new Color(0x1A1410));
                    g.fillOval(x - w / 2, y - 9, w, 18);
                    setAlpha(g, alpha * 0.35f);
                    g.fillOval(x - w / 3, y - 6, w * 2 / 3, 12);
                    if ("burn".equals(d.kind)) {
                        // Brasas que siguen prendidas un rato
                        for (int i = 0; i < 9; i++) {
                            int ex = x + r.nextInt(w) - w / 2;
                            int ey = y + r.nextInt(12) - 6;
                            double flick = 0.5 + 0.5 * Math.sin(time * 9 + i);
                            setAlpha(g, (float) (alpha * flick));
                            g.setColor(i % 2 == 0 ? new Color(0xFF8A2A) : new Color(0xFFD25A));
                            g.fillRect(ex, ey, 4, 4);
                        }
                    }
                }
                case "wet" -> {
                    setAlpha(g, alpha * 0.45f);
                    g.setColor(new Color(0x2C6FB0));
                    g.fillOval(x - w / 2, y - 8, w, 16);
                    setAlpha(g, alpha * 0.5f);
                    g.setColor(new Color(0xBDF3FF));
                    g.fillOval(x - w / 4, y - 5, w / 4, 4);
                }
                case "crack" -> {
                    setAlpha(g, alpha * 0.8f);
                    g.setColor(new Color(0x141A16));
                    g.setStroke(new BasicStroke(3));
                    for (int i = 0; i < 6; i++) {
                        double a = (i / 6.0) * Math.PI + r.nextDouble() * 0.4;
                        int len = w / 3 + r.nextInt(Math.max(1, w / 3));
                        int mx = x + (int) (Math.cos(a) * len * 0.5);
                        int ex = x + (int) (Math.cos(a) * len);
                        g.drawLine(x, y, mx, y + (int) (Math.sin(a) * 4) - 2);
                        g.drawLine(mx, y + (int) (Math.sin(a) * 4) - 2, ex, y + (int) (Math.sin(a) * 8) - 4);
                    }
                    g.setStroke(new BasicStroke(1));
                }
                default -> { }
            }
        }
        g.setComposite(old);
    }

    /** Speed lines and the impact frame of very strong hits (over the whole arena). */
    public void drawOverlay(Graphics2D g, int width, int height) {
        Composite old = g.getComposite();
        if (impactFrame > 0) {
            setAlpha(g, 0.55f);
            g.setColor(new Color(0x0B0D14));
            g.fillRect(-40, -40, width + 80, height + 80);
        }
        if (speedLines > 0) {
            Random r = new Random((long) (time * 40));
            float alpha = (float) Math.min(1, speedLines / 0.12);
            g.setColor(Color.WHITE);
            for (int i = 0; i < 46; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                double inner = 160 + r.nextDouble() * 120;
                double outer = 900;
                setAlpha(g, alpha * (0.35f + r.nextFloat() * 0.4f));
                g.setStroke(new BasicStroke(2 + r.nextInt(4)));
                g.drawLine((int) (speedLinesX + Math.cos(a) * inner), (int) (speedLinesY + Math.sin(a) * inner),
                        (int) (speedLinesX + Math.cos(a) * outer), (int) (speedLinesY + Math.sin(a) * outer));
            }
            g.setStroke(new BasicStroke(1));
        }
        g.setComposite(old);
    }

    /** Glows behind the fighters (auras). */
    public void drawBehind(Graphics2D g) {
        for (Particle p : particles) {
            if (p.behind && p.delay <= 0) drawParticle(g, p);
        }
    }

    /** Projectiles, particles and lightning on top of the fighters. */
    public void drawFront(Graphics2D g, FightingGameSession session) {
        Composite old = g.getComposite();
        for (FightingProjectile projectile : session.getProjectiles()) {
            drawProjectile(g, session, projectile);
        }
        for (Particle p : particles) {
            if (!p.behind && p.delay <= 0) drawParticle(g, p);
        }
        for (Bolt b : bolts) {
            drawBolt(g, b.x1, b.y1, b.x2, b.y2, b.width, b.glow,
                    (float) Math.min(1, b.life / b.maxLife * 1.5));
        }
        g.setComposite(old);
        sheets.draw(g);
    }

    /** Damage numbers and "K.O.!". */
    public void drawPopups(Graphics2D g) {
        Composite old = g.getComposite();
        for (Popup p : popups) {
            double t = 1 - p.life / p.maxLife;
            double pop = t < 0.15 ? 0.6 + t / 0.15 * 0.6 : 1.2 - Math.min(0.2, (t - 0.15));
            int size = (int) Math.round(p.size * pop);
            int y = (int) Math.round(p.y - t * 40);
            float alpha = (float) Math.min(1, p.life / p.maxLife * 2.5);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setFont(RetroStyle.font(size));
            FontMetrics metrics = g.getFontMetrics();
            int x = (int) Math.round(p.x) - metrics.stringWidth(p.text) / 2;
            g.setColor(new Color(0x1B2233));
            for (int dx = -2; dx <= 2; dx += 2) {
                for (int dy = -2; dy <= 2; dy += 2) {
                    if (dx != 0 || dy != 0) g.drawString(p.text, x + dx, y + dy);
                }
            }
            g.setColor(p.color);
            g.drawString(p.text, x, y);
        }
        g.setComposite(old);
    }

    /** Colored flash over the whole arena (explosions, lightning, K.O.). */
    public void drawScreenFlash(Graphics2D g, int width, int height) {
        if (flashRemaining <= 0) return;
        Composite old = g.getComposite();
        float alpha = (float) (flashAlpha * flashRemaining / flashDuration);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alpha))));
        g.setColor(flashColor);
        g.fillRect(-40, -40, width + 80, height + 80);
        g.setComposite(old);
    }

    private void drawParticle(Graphics2D g, Particle p) {
        double t = 1 - p.life / p.maxLife;
        Color color = lerp(p.from, p.to, t);
        float alpha = (float) Math.max(0, Math.min(1, p.life / p.maxLife * 1.6));
        int x = (int) Math.round(p.x);
        int y = (int) Math.round(p.y);
        int s = (int) Math.round(p.size);
        switch (p.shape) {
            case PIXEL -> {
                setAlpha(g, alpha);
                g.setColor(color);
                int half = Math.max(1, s / 2);
                g.fillRect(x - half, y - half, Math.max(2, s), Math.max(2, s));
            }
            case GLOW -> {
                setAlpha(g, alpha * 0.85f);
                float radius = (float) Math.max(2, p.size);
                Color center = new Color(color.getRed(), color.getGreen(), color.getBlue(), 220);
                Color edge = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
                g.setPaint(new RadialGradientPaint(new Point2D.Double(p.x, p.y), radius,
                        new float[]{0f, 0.35f, 1f},
                        new Color[]{center, new Color(color.getRed(), color.getGreen(), color.getBlue(), 120), edge},
                        MultipleGradientPaint.CycleMethod.NO_CYCLE));
                g.fillOval((int) (p.x - radius), (int) (p.y - radius), (int) (radius * 2), (int) (radius * 2));
            }
            case SPARK -> {
                setAlpha(g, alpha);
                g.setColor(color);
                double speed = Math.hypot(p.vx, p.vy);
                double len = Math.min(34, 4 + speed * 0.04);
                double nx = speed == 0 ? 0 : p.vx / speed;
                double ny = speed == 0 ? 0 : p.vy / speed;
                g.setStroke(new BasicStroke((float) Math.max(2, p.size), BasicStroke.CAP_SQUARE,
                        BasicStroke.JOIN_MITER));
                g.drawLine(x, y, (int) Math.round(p.x - nx * len), (int) Math.round(p.y - ny * len));
            }
            case RING -> {
                // Borde oscuro debajo para que se vea sobre el cielo claro
                float width = (float) Math.max(1, p.angle * (1 - t));
                setAlpha(g, alpha * 0.55f);
                g.setColor(new Color(0x2B3A55));
                g.setStroke(new BasicStroke(width + 3));
                g.drawOval(x - s, y - s, s * 2, s * 2);
                setAlpha(g, alpha);
                g.setColor(color);
                g.setStroke(new BasicStroke(width));
                g.drawOval(x - s, y - s, s * 2, s * 2);
            }
            case FIRE -> {
                Color fire = t < 0.15 ? lerp(new Color(0xFFFBE0), new Color(0xFFD25A), t / 0.15)
                        : t < 0.45 ? lerp(new Color(0xFFD25A), new Color(0xF2601E), (t - 0.15) / 0.3)
                        : t < 0.75 ? lerp(new Color(0xF2601E), new Color(0xA8231A), (t - 0.45) / 0.3)
                        : lerp(new Color(0xA8231A), new Color(0x8A3A30), (t - 0.75) / 0.25);
                float fireAlpha = (float) (t < 0.55 ? 0.95 : 0.95 * Math.pow(1 - (t - 0.55) / 0.45, 2));
                setAlpha(g, fireAlpha);
                g.setColor(fire);
                g.fillOval(x - s / 2, y - s / 2, s, s);
                if (t < 0.5) {
                    g.setColor(lerp(Color.WHITE, fire, t * 2));
                    g.fillOval(x - s / 4, y - s / 4 - s / 10, s / 2, s / 2);
                }
            }
            case WATER -> {
                Color water = t < 0.2 ? lerp(Color.WHITE, new Color(0x8FE3FF), t / 0.2)
                        : lerp(new Color(0x8FE3FF), new Color(0x2479D0), (t - 0.2) / 0.8);
                setAlpha(g, (float) (0.9 * Math.pow(1 - t, 0.7)));
                g.setColor(new Color(0x1C5FA8));
                g.fillOval(x - s / 2 - 1, y - s / 2 - 1, s + 2, s + 2);
                g.setColor(water);
                g.fillOval(x - s / 2, y - s / 2, s, s);
                g.setColor(Color.WHITE);
                g.fillOval(x - s / 5, y - s / 3, Math.max(2, s / 4), Math.max(2, s / 5));
            }
            case SMOKE -> {
                setAlpha(g, alpha * 0.3f);
                g.setColor(color);
                g.fillOval(x - s / 2, y - s / 2, s, s);
            }
            case LEAF -> {
                setAlpha(g, alpha);
                Graphics2D leaf = (Graphics2D) g.create();
                leaf.translate(p.x, p.y);
                leaf.rotate(p.angle);
                leaf.setColor(color);
                leaf.fillRect(-s / 2, -s / 4, s, Math.max(2, s / 2));
                leaf.setColor(color.darker());
                leaf.fillRect(-s / 2, -1, s, 2);
                leaf.dispose();
            }
            case STAR -> {
                setAlpha(g, alpha);
                drawStar(g, p.x, p.y, p.size, p.angle, p.to);
            }
            case SLASH -> {
                setAlpha(g, alpha);
                Graphics2D slash = (Graphics2D) g.create();
                slash.translate(p.x, p.y);
                slash.rotate(p.angle);
                double len = p.size * (0.4 + 0.6 * Math.min(1, t * 4));
                slash.setColor(p.to);
                slash.setStroke(new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                slash.drawLine((int) (-len / 2), 0, (int) (len / 2), 0);
                slash.setColor(Color.WHITE);
                slash.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                slash.drawLine((int) (-len / 2), 0, (int) (len / 2), 0);
                slash.dispose();
            }
            case DEBRIS -> {
                setAlpha(g, (float) Math.min(1, p.life / 0.3));
                Graphics2D rock = (Graphics2D) g.create();
                rock.translate(p.x, p.y);
                rock.rotate(p.angle);
                rock.setColor(p.from);
                rock.fillRect(-s / 2, -s / 2, s, s);
                rock.setColor(p.from.brighter());
                rock.fillRect(-s / 2, -s / 2, s, Math.max(1, s / 3));
                rock.setColor(p.to);
                rock.fillRect(-s / 2, s / 4, s, Math.max(1, s / 4));
                rock.dispose();
            }
            case ZZZ -> {
                setAlpha(g, alpha);
                g.setFont(RetroStyle.font(Math.max(10, s)));
                g.setColor(new Color(0x40285C));
                g.drawString("z", x + 2, y + 2);
                g.setColor(color);
                g.drawString("z", x, y);
            }
            default -> { }
        }
        g.setStroke(new BasicStroke(1));
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void drawProjectile(Graphics2D g, FightingGameSession session, FightingProjectile projectile) {
        double x = projectile.getX();
        double y = projectile.getY();
        double dir = Math.signum(projectile.getVelocityX());
        double size = projectile.getSize();
        double flicker = 1 + 0.15 * Math.sin(time * 45 + x * 0.05);
        double[] origin = projectileOrigins.get(projectile);
        switch (projectile.getMoveId()) {
            case "EMBER" -> {
                double s = size * 1.9 * flicker;
                softCircle(g, x, y, s * 1.6, new Color(0xFF8A2A), 0.55f);
                // Cola de fuego
                Path2D.Double tail = new Path2D.Double();
                tail.moveTo(x, y - s * 0.45);
                tail.quadTo(x - dir * s * 1.2, y - s * 0.55, x - dir * s * 2.3, y + Math.sin(time * 30) * 4);
                tail.quadTo(x - dir * s * 1.2, y + s * 0.5, x, y + s * 0.45);
                tail.closePath();
                setAlpha(g, 0.85f);
                g.setColor(new Color(0xE8401E));
                g.fill(tail);
                g.setColor(new Color(0xFF9A2E));
                g.fill(scaled(tail, x, y, 0.65));
                setAlpha(g, 1f);
                fillCircle(g, x, y, s * 0.55, new Color(0xD8301A));
                fillCircle(g, x + dir * s * 0.06, y, s * 0.42, new Color(0xFF9A2E));
                fillCircle(g, x + dir * s * 0.12, y - s * 0.03, s * 0.28, new Color(0xFFE27A));
                fillCircle(g, x + dir * s * 0.16, y - s * 0.05, s * 0.13, Color.WHITE);
            }
            case "WATER_GUN" -> {
                double s = size * 2.2;
                // Chorro: una franja que sale de la boca y se adelgaza
                if (origin != null) {
                    double startX = x - dir * Math.min(Math.abs(x - origin[0]), 220);
                    double wobble = Math.sin(time * 40) * 3;
                    Path2D.Double jet = new Path2D.Double();
                    jet.moveTo(startX, y - 3 + wobble);
                    jet.quadTo((startX + x) / 2, y - s * 0.30 + wobble, x, y - s * 0.42);
                    jet.lineTo(x, y + s * 0.42);
                    jet.quadTo((startX + x) / 2, y + s * 0.30 - wobble, startX, y + 3 + wobble);
                    jet.closePath();
                    setAlpha(g, 0.7f);
                    g.setColor(new Color(0x1C5FA8));
                    g.setStroke(new BasicStroke(3));
                    g.draw(jet);
                    g.setColor(new Color(0x2F9BEA));
                    g.fill(jet);
                    setAlpha(g, 0.9f);
                    g.setColor(new Color(0xBFF2FF));
                    g.fill(scaled(jet, x, y, 0.45));
                    g.setStroke(new BasicStroke(1));
                    setAlpha(g, 1f);
                }
                softCircle(g, x, y, s * 1.4, new Color(0x7FE4FF), 0.5f);
                fillEllipse(g, x, y, s * 1.25, s * 0.85, new Color(0x1E6FC4));
                fillEllipse(g, x + dir * s * 0.08, y, s * 1.0, s * 0.62, new Color(0x45B4F5));
                fillEllipse(g, x + dir * s * 0.18, y - s * 0.1, s * 0.5, s * 0.3, new Color(0xBDF3FF));
                fillCircle(g, x + dir * s * 0.28, y - s * 0.16, s * 0.1, Color.WHITE);
            }
            case "THUNDER_SHOCK" -> {
                double s = size * 1.5 * flicker;
                softCircle(g, x, y, s * 2.2, new Color(0xFFF26B), 0.6f);
                // Rayo que une la boca con la bola en el primer instante
                if (origin != null && time - origin[2] < 0.18) {
                    drawBolt(g, origin[0], origin[1], x, y, 3.5f,
                            new Color(0xFFF26B), 1f);
                }
                Random r = new Random((long) (time * 30) + (long) x);
                for (int i = 0; i < 4; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double len = s * (0.9 + r.nextDouble() * 0.9);
                    drawBolt(g, x, y, x + Math.cos(a) * len, y + Math.sin(a) * len, 2f,
                            new Color(0xFFE74A), 1f);
                }
                fillCircle(g, x, y, s * 0.5, new Color(0xFFD21F));
                fillCircle(g, x, y, s * 0.32, new Color(0xFFF7B8));
                fillCircle(g, x, y, s * 0.16, Color.WHITE);
            }
            case "SLEEP_POWDER" -> {
                double s = size * 1.6;
                softCircle(g, x, y, s * 1.5, new Color(0xC88BFF), 0.55f);
                for (int i = 0; i < 7; i++) {
                    double a = time * 4 + i * Math.PI * 2 / 7;
                    double r = s * (0.45 + 0.15 * Math.sin(time * 6 + i));
                    fillCircle(g, x + Math.cos(a) * r, y + Math.sin(a) * r * 0.8, s * 0.22,
                            new Color(0x9A5BD0));
                    g.setColor(Color.WHITE);
                    g.fillRect((int) (x + Math.cos(a) * r) - 1, (int) (y + Math.sin(a) * r * 0.8) - 1, 3, 3);
                }
                fillCircle(g, x, y, s * 0.3, new Color(0xF2D9FF));
            }
            default -> {
                softCircle(g, x, y, size * 2, projectile.getColor(), 0.5f);
                fillCircle(g, x, y, size * 0.6, projectile.getColor());
                fillCircle(g, x, y, size * 0.3, Color.WHITE);
            }
        }
        setAlpha(g, 1f);
    }

    private void drawBolt(Graphics2D g, double x1, double y1, double x2, double y2,
                          float width, Color glow, float alpha) {
        double len = Math.hypot(x2 - x1, y2 - y1);
        int segments = Math.max(3, (int) (len / 14));
        double nx = -(y2 - y1) / Math.max(1, len);
        double ny = (x2 - x1) / Math.max(1, len);
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x1, y1);
        for (int i = 1; i < segments; i++) {
            double t = i / (double) segments;
            double offset = (random.nextDouble() - 0.5) * Math.min(26, len * 0.25);
            path.lineTo(x1 + (x2 - x1) * t + nx * offset, y1 + (y2 - y1) * t + ny * offset);
        }
        path.lineTo(x2, y2);
        setAlpha(g, alpha * 0.45f);
        g.setColor(glow);
        g.setStroke(new BasicStroke(width * 3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
        setAlpha(g, alpha);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
        g.setStroke(new BasicStroke(1));
        setAlpha(g, 1f);
    }

    private void drawStar(Graphics2D g, double x, double y, double size, double angle, Color accent) {
        Path2D.Double star = new Path2D.Double();
        int points = 8;
        for (int i = 0; i < points * 2; i++) {
            double r = i % 2 == 0 ? size : size * 0.28;
            if (i % 4 == 2) r *= 0.6;
            double a = angle + i * Math.PI / points;
            double px = x + Math.cos(a) * r;
            double py = y + Math.sin(a) * r;
            if (i == 0) star.moveTo(px, py);
            else star.lineTo(px, py);
        }
        star.closePath();
        g.setColor(accent);
        g.fill(star);
        g.setColor(Color.WHITE);
        g.fill(scaled(star, x, y, 0.6));
    }

    private static java.awt.Shape scaled(java.awt.Shape shape, double cx, double cy, double factor) {
        java.awt.geom.AffineTransform t = new java.awt.geom.AffineTransform();
        t.translate(cx, cy);
        t.scale(factor, factor);
        t.translate(-cx, -cy);
        return t.createTransformedShape(shape);
    }

    private static void softCircle(Graphics2D g, double x, double y, double radius, Color color, float alpha) {
        if (radius <= 1) return;
        setAlpha(g, alpha);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) radius,
                new float[]{0f, 1f},
                new Color[]{new Color(color.getRed(), color.getGreen(), color.getBlue(), 200),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)}));
        g.fillOval((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2));
        setAlpha(g, 1f);
    }

    private static void fillCircle(Graphics2D g, double x, double y, double radius, Color color) {
        g.setColor(color);
        int r = (int) Math.round(radius);
        g.fillOval((int) Math.round(x) - r, (int) Math.round(y) - r, r * 2, r * 2);
    }

    private static void fillEllipse(Graphics2D g, double x, double y, double w, double h, Color color) {
        g.setColor(color);
        g.fillOval((int) Math.round(x - w / 2), (int) Math.round(y - h / 2), (int) Math.round(w), (int) Math.round(h));
    }

    private static void setAlpha(Graphics2D g, float alpha) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
    }

    private static Color lerp(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}

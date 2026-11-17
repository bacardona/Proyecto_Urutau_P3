package com.pokemon.battle.fighting;

import com.pokemon.battle.ui.RetroStyle;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * The "cinematic" layer of the free fight. It does not change the fighting rules:
 * it only decides when the simulation waits or goes slower, and draws on top.
 *
 * - Intro: VS screen and countdown 3, 2, 1, FIGHT (the fight waits).
 * - Ultimate: a short banner with the Pokemon and the name of the attack (the fight waits).
 * - K.O.: a moment of slow motion and a big "K.O." text.
 * - Winner: rays, the winning Pokemon and its name behind the results menu.
 */
final class FightingDirector {

    static final double VS_SECONDS = 2.2;
    static final double COUNT_STEP = 0.6;
    static final double INTRO_SECONDS = VS_SECONDS + COUNT_STEP * 4;
    static final double CUT_IN_SECONDS = 1.5;
    static final double KO_SLOW_SECONDS = 0.9;
    static final double KO_TEXT_SECONDS = 1.6;
    static final double KO_TIME_SCALE = 0.3;
    static final double EVOLUTION_SECONDS = 2.4;
    static final double EVOLUTION_FLASH_AT = 1.5;

    private static final Color P1_COLOR = new Color(0xF2C230);
    private static final Color P2_COLOR = new Color(0x3C86EA);
    private static final Color OUTLINE = new Color(0x1B2233);

    private final FightingSound sound = FightingSound.get();

    private double time;
    // Sin reset() no hay intro (por ejemplo al dibujar vistas previas)
    private double vsSeconds = VS_SECONDS;
    private double introTime = INTRO_SECONDS;
    private int lastCountStep = 4;
    private int cutOwner = -1;
    private double cutTime;
    private Fighter cutFighter;
    private String cutName = "";
    private Color cutColor = Color.WHITE;
    private double koTime = -1;
    private boolean finalKo;
    private boolean finishedHandled;
    private double evolutionTime = -1;
    private FightingSpriteProfile evolutionFrom;
    private FightingSpriteProfile evolutionTo;
    private String evolutionFromName = "";
    private String evolutionToName = "";
    private final java.util.Map<BufferedImage, BufferedImage> silhouettes = new java.util.IdentityHashMap<>();

    /** New match or rematch: the intro starts again. */
    void reset() {
        boolean introEnabled = !Boolean.getBoolean("pokemon.fighting.skipIntro");
        vsSeconds = VS_SECONDS;
        bossIntro = false;
        enrageTime = -1;
        introTime = introEnabled ? 0 : introSeconds();
        lastCountStep = introEnabled ? -1 : 4;
        cutOwner = -1;
        cutFighter = null;
        koTime = -1;
        evolutionTime = -1;
        pendingEvolutions.clear();
        finishedHandled = false;
        if (!introEnabled) sound.startBattleMusic();
    }

    void tick(double dt) {
        time += dt;
        sound.keepAlive();
        if (introTime < introSeconds()) {
            if (introTime == 0 && !bossIntro) sound.play(FightingSound.Sfx.VS);
            double before = introTime;
            introTime += dt;
            if (bossIntro) tickBossArrival(before, introTime);
            int step = introTime < vsSeconds ? -1 : (int) ((introTime - vsSeconds) / COUNT_STEP);
            if (step != lastCountStep && step >= 0 && step <= 3) {
                sound.play(step == 3 ? FightingSound.Sfx.GO : FightingSound.Sfx.COUNT);
                if (step == 3) sound.startBattleMusic();
            }
            lastCountStep = step;
        }
        if (cutOwner >= 0) {
            cutTime += dt;
            if (cutTime >= CUT_IN_SECONDS) {
                cutOwner = -1;
                cutFighter = null;
            }
        }
        if (evolutionTime >= 0) {
            double before = evolutionTime;
            evolutionTime += dt;
            if (before < EVOLUTION_FLASH_AT && evolutionTime >= EVOLUTION_FLASH_AT) {
                sound.play(FightingSound.Sfx.ULTI_RELEASE, 0.5f);
                sound.play(FightingSound.Sfx.BUFF);
            }
            if (evolutionTime >= EVOLUTION_SECONDS) {
                evolutionTime = -1;
                if (!pendingEvolutions.isEmpty()) startEvolution(pendingEvolutions.poll());
            }
        }
        if (koTime >= 0) {
            koTime += dt;
            if (koTime > KO_TEXT_SECONDS) koTime = -1;
        }
        if (enrageTime >= 0) {
            double before = enrageTime;
            enrageTime += dt;
            if (before < 0.9 && enrageTime >= 0.9) sound.play(FightingSound.Sfx.ULTI_RELEASE, 0.8f);
            if (enrageTime >= ENRAGE_SECONDS) {
                enrageTime = -1;
                enragedBoss = null;
            }
        }
    }

    /** While true the fighters do not move (intro and ultimate banner). */
    boolean freezesSimulation() {
        return introTime < introSeconds() || cutOwner >= 0 || evolutionTime >= 0 || enrageTime >= 0;
    }

    private double introSeconds() {
        return vsSeconds + COUNT_STEP * 4;
    }

    // ------------------------------------------------------------ jefe secreto
    static final double BOSS_WARNING_SECONDS = 5.0;
    static final double ENRAGE_SECONDS = 2.6;
    private static final Color BOSS_COLOR = new Color(0x9B4DFF);
    private static final Color ALERT_RED = new Color(0xE8342E);
    private boolean bossIntro;
    private double enrageTime = -1;
    private Fighter enragedBoss;

    /** Pelea de jefe: la intro es la alerta de "un nuevo retador" (mas larga que el VS). */
    void useBossIntro() {
        if (bossIntro || introTime >= VS_SECONDS) return;
        bossIntro = true;
        vsSeconds = BOSS_WARNING_SECONDS;
        sound.play(FightingSound.Sfx.COUNT, 0.8f);
    }

    /** Mewtwo llega a la mitad de vida: escena corta de furia. */
    void startEnrage(Fighter boss) {
        enragedBoss = boss;
        enrageTime = 0;
        sound.play(FightingSound.Sfx.CUT_IN);
        sound.play(FightingSound.Sfx.ULTI_CHARGE, 0.8f);
    }

    /** 1 = normal speed; less than 1 during the slow motion of a K.O. */
    double timeScale() {
        return koTime >= 0 && koTime < KO_SLOW_SECONDS ? KO_TIME_SCALE : 1;
    }

    boolean isIntroRunning() {
        return introTime < introSeconds();
    }

    void startCutIn(int ownerIndex, Fighter owner, UltimateDefinition definition) {
        if (owner == null || definition == null) return;
        cutOwner = ownerIndex;
        cutTime = 0;
        cutFighter = owner;
        cutName = "¡" + definition.displayName() + "!";
        cutColor = definition.primaryColor();
        sound.play(FightingSound.Sfx.CUT_IN);
    }

    /** Short evolution cinematic: rays, white silhouette that changes shape, flash and the new name. */
    private final java.util.ArrayDeque<Fighter> pendingEvolutions = new java.util.ArrayDeque<>();

    void startEvolution(Fighter fighter) {
        if (evolutionTime >= 0) {
            // Si los dos evolucionan a la vez, se muestra uno y luego el otro
            pendingEvolutions.add(fighter);
            return;
        }
        FightingSpriteProfile to = FightingSpriteProfile.forFighter(fighter);
        FightingSpriteProfile from = FightingSpriteProfile.forSpecies(fighter.getBasePokemonId());
        if (to == null || from == null) return;
        evolutionFrom = from;
        evolutionTo = to;
        evolutionFromName = fighter.getBasePokemonId();
        evolutionToName = fighter.getName().toUpperCase();
        evolutionTime = 0;
        sound.play(FightingSound.Sfx.ULTI_CHARGE, 0.7f);
    }

    void startKnockOut(boolean lastOfTeam) {
        koTime = 0;
        finalKo = lastOfTeam;
    }

    void onFinished() {
        onFinished(null);
    }

    /** Fin del combate: fanfarria, o la risa de Mewtwo si fue el que gano. */
    void onFinished(FightingGameSession session) {
        if (finishedHandled) return;
        finishedHandled = true;
        sound.stopMusic();
        if (session != null && session.isBossFight() && !session.challengerWon()) {
            sound.play(FightingSound.Sfx.LAUGH);
            sound.play(FightingSound.Sfx.ULTI_CHARGE, 0.4f);
        } else {
            sound.playFanfare();
        }
    }

    /** Throw away one-shot keys pressed while the fight waits. Held directions stay. */
    void discardTransientInputs(FightingInput input) {
        input.consumePlayerOneJump();
        input.consumePlayerTwoJump();
        input.consumePlayerOneAttackRequest();
        input.consumePlayerTwoAttackRequest();
        input.consumePlayerOneEvolution();
        input.consumePlayerTwoEvolution();
        input.consumePlayerOneUltimate();
        input.consumePlayerTwoUltimate();
    }

    // ------------------------------------------------------------------ dibujo

    /** Drawn after the HUD, over the whole arena. */
    void draw(Graphics2D g, FightingGameSession session) {
        int w = session.getWidth();
        int h = session.getHeight();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (introTime < vsSeconds && bossIntro) {
            drawBossWarning(g, session, w, h, introTime);
        } else if (introTime < vsSeconds) {
            drawVersus(g, session, w, h, introTime);
        } else if (introTime < introSeconds()) {
            drawCountdown(g, w, h, introTime - vsSeconds);
        }
        if (sound.isMuted() && introTime >= introSeconds()) {
            outlined(g, "SONIDO APAGADO (F1)", w / 2, 108, 22, new Color(0xFF8A80), OUTLINE, 2);
        }
        if (session.getState() == FightingGameSession.State.FINISHED) return;
        if (evolutionTime >= 0) drawEvolution(g, w, h);
        if (cutOwner >= 0 && cutFighter != null) drawCutIn(g, w, h);
        if (koTime >= 0) drawKnockOut(g, w, h);
        if (enrageTime >= 0 && enragedBoss != null) drawEnrage(g, w, h);
    }

    // Momentos de la llegada de Mewtwo (segundos desde que empieza la intro)
    static final double ARRIVAL_STRIKE = 1.5;     // cae el rayo
    static final double ARRIVAL_PILLAR = 1.85;    // circulo y columna de energia
    static final double ARRIVAL_REVEAL = 2.25;    // Mewtwo empieza a aparecer
    static final double ARRIVAL_SHOWN = 2.85;     // ya se ve entero
    static final double ARRIVAL_BANNER = 3.0;     // franja con su nombre

    /** 0 = Mewtwo invisible, 1 = visible. Durante su llegada aparece dentro de la columna. */
    double bossReveal() {
        if (!bossIntro || introTime >= vsSeconds) return 1;
        if (introTime < ARRIVAL_REVEAL) return 0;
        return Math.min(1, (introTime - ARRIVAL_REVEAL) / (ARRIVAL_SHOWN - ARRIVAL_REVEAL));
    }

    /** Sonidos de la llegada, al pasar por cada momento. */
    private void tickBossArrival(double before, double now) {
        for (double beep : new double[]{0.0, 0.5, 1.0}) {
            if (before <= beep && now > beep) sound.play(FightingSound.Sfx.COUNT, 0.8f);
        }
        if (before < ARRIVAL_STRIKE && now >= ARRIVAL_STRIKE) sound.play(FightingSound.Sfx.THUNDER);
        if (before < ARRIVAL_PILLAR && now >= ARRIVAL_PILLAR) sound.play(FightingSound.Sfx.ULTI_CHARGE, 0.8f);
        if (before < ARRIVAL_BANNER && now >= ARRIVAL_BANNER) sound.play(FightingSound.Sfx.CUT_IN);
    }

    /**
     * Llegada de Mewtwo dentro del escenario (como en el trailer): alerta con franjas,
     * un rayo cae, aparece un circulo magico con una columna de energia y de ahi sale Mewtwo;
     * luego una franja con su nombre.
     */
    private void drawBossWarning(Graphics2D g, FightingGameSession session, int w, int h, double t) {
        Composite old = g.getComposite();
        Fighter boss = session.getBoss();
        int ground = session.getGroundY();
        int bx = boss == null ? w * 3 / 4 : (int) boss.getX();

        // Escenario oscurecido con tono morado
        float tint = (float) Math.min(0.55, t * 1.2);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, tint));
        g.setColor(new Color(0x1A0630));
        g.fillRect(0, 0, w, h);
        g.setComposite(old);

        // Franjas de alerta arriba y abajo con el texto corriendo
        if (t < ARRIVAL_BANNER) {
            drawTicker(g, w, 0, t);
            drawTicker(g, w, h - 34, -t);
        }

        // Rayo
        if (t >= ARRIVAL_STRIKE && t < ARRIVAL_STRIKE + 0.45) {
            double q = (t - ARRIVAL_STRIKE) / 0.45;
            if (q < 0.12) flash(g, w, h, 0.75f);
            drawLightning(g, bx, ground, (int) (t * 30), (float) (1 - q * 0.8));
        }

        // Circulo magico y columna de energia
        if (t >= ARRIVAL_PILLAR && t < vsSeconds) {
            double q = t - ARRIVAL_PILLAR;
            float alpha = (float) Math.min(1, q / 0.2) * (float) Math.min(1, (vsSeconds - t) / 0.4);
            drawMagicCircle(g, bx, ground, q, alpha);
            if (t < ARRIVAL_SHOWN + 0.25) {
                float pillar = (float) Math.min(1, q / 0.15) * (float) Math.min(1, (ARRIVAL_SHOWN + 0.25 - t) / 0.3);
                drawPillar(g, bx, ground, pillar);
            }
        }

        // Franja con el nombre
        if (t >= ARRIVAL_BANNER) {
            double q = t - ARRIVAL_BANNER;
            double out = vsSeconds - t;
            int slide = (int) (w * (1 - Math.min(1, q / 0.22)));
            if (out < 0.25) slide = (int) (-w * (1 - out / 0.25));
            int bandY = h / 2 - 105;
            Polygon band = new Polygon(new int[]{slide, slide + w, slide + w, slide},
                    new int[]{bandY + 18, bandY - 6, bandY + 196, bandY + 220}, 4);
            g.setColor(new Color(0x14071F));
            g.fill(band);
            g.setColor(BOSS_COLOR);
            g.setStroke(new BasicStroke(8));
            g.drawLine(slide, bandY + 18, slide + w, bandY - 6);
            g.drawLine(slide, bandY + 220, slide + w, bandY + 196);
            g.setStroke(new BasicStroke(2));
            for (int i = 0; i < 14; i++) {
                int ly = bandY + 30 + (i * 37) % 170;
                int lx = slide + (int) ((i * 173 + time * 900) % (w + 200)) - 100;
                g.setColor(new Color(155, 77, 255, 110));
                g.drawLine(lx, ly, lx + 120, ly);
            }
            if (boss != null) {
                FightingSpriteProfile profile = FightingSpriteProfile.forFighter(boss);
                if (profile != null) drawProfile(g, profile, slide + 300, h / 2 + 5, 210, false);
            }
            outlined(g, "MEWTWO", slide + w / 2 + 110, h / 2 + 10, 76, BOSS_COLOR, Color.WHITE, 5);
            outlined(g, "JEFE SECRETO", slide + w / 2 + 110, h / 2 + 55, 26, Color.WHITE, OUTLINE, 2);
        }
        g.setComposite(old);
    }

    private void drawTicker(Graphics2D g, int w, int y, double t) {
        g.setColor(ALERT_RED);
        g.fillRect(0, y, w, 34);
        g.setColor(new Color(0x5A0E0E));
        g.fillRect(0, y + 30, w, 4);
        g.setFont(RetroStyle.font(20));
        String text = "¡ALERTA!   UN DESAFIO APARECE   ";
        int textWidth = g.getFontMetrics().stringWidth(text);
        int offset = (int) Math.floorMod((long) (t * 220), (long) textWidth);
        g.setColor(Color.WHITE);
        for (int x = -offset; x < w; x += textWidth) g.drawString(text, x, y + 24);
    }

    private static void drawLightning(Graphics2D g, int x, int ground, int seed, float alpha) {
        java.util.Random random = new java.util.Random(seed);
        int segments = 12;
        int[] xs = new int[segments + 1];
        int[] ys = new int[segments + 1];
        for (int i = 0; i <= segments; i++) {
            ys[i] = ground * i / segments;
            xs[i] = x + (i == 0 || i == segments ? 0 : random.nextInt(70) - 35);
        }
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, alpha)));
        g.setStroke(new BasicStroke(16, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(110, 190, 255, 120));
        g.drawPolyline(xs, ys, xs.length);
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0xBFE8FF));
        g.drawPolyline(xs, ys, xs.length);
        g.setStroke(new BasicStroke(2));
        g.setColor(Color.WHITE);
        g.drawPolyline(xs, ys, xs.length);
        // Chispazo en el suelo
        g.setColor(new Color(0xBFE8FF));
        for (int i = 0; i < 10; i++) {
            double a = Math.PI + i * Math.PI / 9;
            g.drawLine(x, ground, (int) (x + Math.cos(a) * 50), (int) (ground + Math.sin(a) * 30));
        }
        g.setComposite(old);
    }

    private void drawMagicCircle(Graphics2D g, int x, int ground, double q, float alpha) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, alpha)));
        double grow = Math.min(1, q / 0.35);
        int rx = (int) (110 * grow), ry = (int) (26 * grow);
        g.setStroke(new BasicStroke(4));
        g.setColor(BOSS_COLOR);
        g.drawOval(x - rx, ground - ry, rx * 2, ry * 2);
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(0xF0C8FF));
        g.drawOval(x - rx * 7 / 10, ground - ry * 7 / 10, rx * 14 / 10, ry * 14 / 10);
        // Runas que giran alrededor del circulo
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6 + time * 2.2;
            int px = (int) (x + Math.cos(a) * rx * 0.85);
            int py = (int) (ground + Math.sin(a) * ry * 0.85);
            g.fillRect(px - 3, py - 2, 6, 4);
        }
        g.setComposite(old);
    }

    private static void drawPillar(Graphics2D g, int x, int ground, float alpha) {
        if (alpha <= 0) return;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1, alpha)));
        int top = ground - 420;
        java.awt.GradientPaint outer = new java.awt.GradientPaint(0, top, new Color(155, 77, 255, 0),
                0, ground, new Color(155, 77, 255, 200));
        java.awt.Paint paint = g.getPaint();
        g.setPaint(outer);
        g.fillRect(x - 46, top, 92, ground - top);
        java.awt.GradientPaint inner = new java.awt.GradientPaint(0, top, new Color(255, 255, 255, 0),
                0, ground, new Color(255, 240, 255, 235));
        g.setPaint(inner);
        g.fillRect(x - 14, top, 28, ground - top);
        g.setPaint(paint);
        g.setComposite(old);
    }

    /** Escena de furia: pantalla oscura, Mewtwo con un brillo rojo y el texto. */
    private void drawEnrage(Graphics2D g, int w, int h) {
        double t = enrageTime;
        Composite old = g.getComposite();
        float dark = (float) Math.min(0.75, t * 2.5) * (float) Math.min(1, (ENRAGE_SECONDS - t) / 0.3);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, dark)));
        g.setColor(new Color(0x12021C));
        g.fillRect(0, 0, w, h);
        g.setComposite(old);
        double fade = Math.min(1, (ENRAGE_SECONDS - t) / 0.3);
        if (fade <= 0) return;
        int bandY = h / 2 - 110;
        int slide = (int) (w * (1 - Math.min(1, t / 0.25)));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) fade));
        g.setColor(new Color(0x2A0838));
        g.fillRect(-slide, bandY, w, 220);
        g.setColor(ALERT_RED);
        g.fillRect(-slide, bandY, w, 6);
        g.fillRect(-slide, bandY + 214, w, 6);
        FightingSpriteProfile profile = FightingSpriteProfile.forFighter(enragedBoss);
        if (profile != null) drawProfile(g, profile, w / 2 - 300 - slide, h / 2 + 5, 200, false);
        double shake = t < 1.2 ? Math.sin(t * 70) * 6 : 0;
        outlined(g, "¡MEWTWO SE ENFURECE!", w / 2 + 120 - slide + (int) shake, h / 2 + 10, 58,
                new Color(0xFF6A5A), Color.WHITE, 4);
        outlined(g, "MAS RAPIDO  ·  MAS FUERTE  ·  PSICOATAQUE", w / 2 + 120 - slide, h / 2 + 62, 22,
                Color.WHITE, OUTLINE, 2);
        g.setComposite(old);
        if (t >= 0.9 && t < 0.98) flash(g, w, h, 0.6f);
    }

    private void drawVersus(Graphics2D g, FightingGameSession session, int w, int h, double t) {
        Composite old = g.getComposite();
        g.setColor(new Color(0x100C1E));
        g.fillRect(0, 0, w, h);
        double left = Math.min(1, t / 0.3);
        double right = Math.min(1, Math.max(0, (t - 0.18) / 0.3));
        int lx = (int) (-w * (1 - ease(left)));
        int rx = (int) (w * (1 - ease(right)));
        Polygon leftBand = new Polygon(new int[]{lx, lx + w / 2 + 80, lx + w / 2 - 80, lx},
                new int[]{0, 0, h, h}, 4);
        Polygon rightBand = new Polygon(new int[]{rx + w / 2 + 80, rx + w, rx + w, rx + w / 2 - 80},
                new int[]{0, 0, h, h}, 4);
        g.setColor(P1_COLOR);
        g.fill(leftBand);
        g.setColor(P2_COLOR);
        g.fill(rightBand);
        g.setStroke(new BasicStroke(3));
        for (int i = 0; i < 26; i++) {
            int y1 = (int) ((i * 41 + time * 700) % h);
            int y2 = (int) ((i * 59 + time * 520) % h);
            g.setColor(new Color(255, 236, 140, 150));
            g.drawLine(lx, y1, lx + w / 2 - 110, y1);
            g.setColor(new Color(150, 200, 255, 150));
            g.drawLine(rx + w / 2 + 110, y2, rx + w, y2);
        }
        Fighter one = session.getFighters().get(0);
        Fighter two = session.getFighters().get(1);
        drawPortrait(g, one, lx + w / 4 - 20, h / 2 - 20, 300, false);
        drawPortrait(g, two, rx + 3 * w / 4 + 20, h / 2 - 20, 300, true);
        outlined(g, one.getName().toUpperCase(), lx + w / 4 - 20, h - 150, 64, Color.WHITE, OUTLINE, 4);
        outlined(g, session.getTrainerName(0).toUpperCase(), lx + w / 4 - 20, h - 95, 30,
                new Color(0x3A2A00), Color.WHITE, 2);
        outlined(g, two.getName().toUpperCase(), rx + 3 * w / 4 + 20, h - 150, 64, Color.WHITE, OUTLINE, 4);
        outlined(g, session.getTrainerName(1).toUpperCase(), rx + 3 * w / 4 + 20, h - 95, 30,
                new Color(0x00204A), Color.WHITE, 2);
        if (t >= 0.75) {
            double q = t - 0.75;
            double grow = Math.max(1, 2.6 - q * 10);
            outlined(g, "VS", w / 2, h / 2 + 50, (int) (170 * grow), new Color(0xFF4545), Color.WHITE, 6);
            if (q < 0.06) flash(g, w, h, 0.8f);
        }
        if (t > vsSeconds - 0.25) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    (float) Math.min(1, (t - (vsSeconds - 0.25)) / 0.25)));
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);
        }
        g.setComposite(old);
    }

    private void drawCountdown(Graphics2D g, int w, int h, double t) {
        int step = Math.min(3, (int) (t / COUNT_STEP));
        double local = t - step * COUNT_STEP;
        if (t < 0.25 && !bossIntro) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (1 - t / 0.25)));
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);
            g.setComposite(old);
        }
        String label = step < 3 ? String.valueOf(3 - step) : "¡PELEA!";
        double pop = 1 + Math.max(0, 0.22 - local) * 3;
        int size = (int) ((step < 3 ? 220 : 160) * pop);
        Color fill = step < 3 ? new Color(0xFFE45A) : new Color(0xFF5A3C);
        outlined(g, label, w / 2, h / 2 + size / 3, size, fill, OUTLINE, 6);
    }

    private void drawCutIn(Graphics2D g, int w, int h) {
        double u = cutTime;
        double in = Math.min(1, u / 0.18);
        double out = Math.max(0, (u - (CUT_IN_SECONDS - 0.18)) / 0.18);
        int off = (int) (w * (1 - ease(in)) - w * ease(out));
        boolean mirror = cutOwner == 1;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        g.setColor(new Color(0x0A0618));
        g.fillRect(0, 0, w, h);
        g.setComposite(old);
        int top = h / 2 - 190;
        int bottom = h / 2 + 190;
        Polygon outer = new Polygon(new int[]{off - 100, off + w + 100, off + w + 100, off - 100},
                new int[]{top + 55, top, bottom - 55, bottom}, 4);
        Polygon inner = new Polygon(new int[]{off - 100, off + w + 100, off + w + 100, off - 100},
                new int[]{top + 77, top + 22, bottom - 77, bottom - 22}, 4);
        g.setColor(cutColor);
        g.fill(outer);
        g.setColor(new Color(0x140F2A));
        g.fill(inner);
        g.setStroke(new BasicStroke(3));
        for (int i = 0; i < 40; i++) {
            double r = 0.5 + (i * 37 % 50) / 100.0;
            int y = top + 40 + (i * 53) % (bottom - top - 80);
            int x = (int) (w - ((u * 2600 * r) % (w + 400)) + 200) + off;
            g.setColor(new Color(cutColor.getRed(), cutColor.getGreen(), cutColor.getBlue(), (int) (150 * r)));
            if (mirror) g.drawLine(w - x, y, w - x + (int) (140 * r), y);
            else g.drawLine(x, y, x + (int) (140 * r), y);
        }
        int portraitX = mirror ? off + w - 260 : off + 260;
        drawPortrait(g, cutFighter, portraitX, h / 2 + 10, 300, mirror, "Charge");
        int textX = mirror ? off + w / 2 - 120 : off + w / 2 + 140;
        outlined(g, cutName, textX, h / 2 + 10, 74, cutColor.brighter(), OUTLINE, 5);
        outlined(g, cutFighter.getName().toUpperCase() + " - ATAQUE DEFINITIVO", textX, h / 2 + 62, 28,
                Color.WHITE, OUTLINE, 3);
        if (u < 0.07) flash(g, w, h, 0.7f);
    }

    private void drawEvolution(Graphics2D g, int w, int h) {
        double u = evolutionTime;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(0.88, u / 0.25 * 0.88)));
        g.setColor(new Color(0x08061A));
        g.fillRect(0, 0, w, h);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(0.35, u / 0.3 * 0.35)));
        g.setColor(new Color(0xFFF0C8));
        int cx = w / 2;
        int cy = h / 2 + 10;
        for (int i = 0; i < 16; i++) {
            double a = time * 0.6 + i * Math.PI * 2 / 16;
            g.fill(new Polygon(new int[]{cx, (int) (cx + Math.cos(a) * 1500), (int) (cx + Math.cos(a + 0.12) * 1500)},
                    new int[]{cy, (int) (cy + Math.sin(a) * 1500), (int) (cy + Math.sin(a + 0.12) * 1500)}, 3));
        }
        g.setComposite(old);
        if (u < EVOLUTION_FLASH_AT) {
            // La silueta blanca cambia entre las dos formas, cada vez mas rapido
            double q = u / EVOLUTION_FLASH_AT;
            double period = Math.max(0.05, 0.32 * Math.pow(1 - q, 1.5));
            boolean showNew = ((int) (u / period)) % 2 == 1;
            drawProfile(g, showNew ? evolutionTo : evolutionFrom, cx, cy, 300, true);
            g.setColor(new Color(0xFFE9A0));
            for (int i = 0; i < 8; i++) {
                double a = time * 2.2 + i * Math.PI / 4;
                double r = 190 - 70 * q;
                int sx = (int) (cx + Math.cos(a) * r);
                int sy = (int) (cy + Math.sin(a) * r);
                g.fillRect(sx - 3, sy - 12, 6, 24);
                g.fillRect(sx - 12, sy - 3, 24, 6);
            }
        } else {
            drawProfile(g, evolutionTo, cx, cy, 300, false);
            outlined(g, "¡" + evolutionFromName + " EVOLUCIONÓ", cx, 120, 56, new Color(0xFFE45A), OUTLINE, 4);
            outlined(g, "A " + evolutionToName + "!", cx, 180, 56, new Color(0xFFE45A), OUTLINE, 4);
            double f = u - EVOLUTION_FLASH_AT;
            if (f < 0.2) flash(g, w, h, (float) (0.9 * (1 - f / 0.2)));
        }
        if (u > EVOLUTION_SECONDS - 0.2) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    (float) Math.min(1, (u - (EVOLUTION_SECONDS - 0.2)) / 0.2 * 0.6)));
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setComposite(old);
        }
    }

    private void drawProfile(Graphics2D g, FightingSpriteProfile profile, int cx, int cy, int height, boolean white) {
        if (profile == null) return;
        var animation = profile.sprites().get(white ? "Idle" : "Pose");
        if (animation == null) animation = profile.sprites().get("Idle");
        if (animation == null) return;
        BufferedImage image = animation.frameAtTime(white ? 0 : time, false, true).image();
        if (white) image = silhouettes.computeIfAbsent(image, FightingDirector::whiteOf);
        int[] box = opaqueBounds(image);
        if (box == null) return;
        int bw = box[2] - box[0];
        int bh = box[3] - box[1];
        int scale = Math.max(2, Math.min(10, height / Math.max(1, bh)));
        g.drawImage(image, cx - bw * scale / 2, cy - bh * scale / 2, cx - bw * scale / 2 + bw * scale,
                cy - bh * scale / 2 + bh * scale, box[0], box[1], box[2], box[3], null);
    }

    /** Silueta negra con borde morado (Mewtwo antes de revelarse). */
    private void drawShadowProfile(Graphics2D g, FightingSpriteProfile profile, int cx, int cy, int height) {
        var animation = profile.sprites().get("Idle");
        if (animation == null) return;
        BufferedImage image = shadows.computeIfAbsent(animation.frameAtTime(0, false, true).image(),
                FightingDirector::shadowOf);
        int[] box = opaqueBounds(image);
        if (box == null) return;
        int bw = box[2] - box[0];
        int bh = box[3] - box[1];
        int scale = Math.max(2, Math.min(10, height / Math.max(1, bh)));
        g.drawImage(image, cx - bw * scale / 2, cy - bh * scale / 2, cx - bw * scale / 2 + bw * scale,
                cy - bh * scale / 2 + bh * scale, box[0], box[1], box[2], box[3], null);
    }

    private final java.util.Map<BufferedImage, BufferedImage> shadows = new java.util.IdentityHashMap<>();

    private static BufferedImage shadowOf(BufferedImage source) {
        int w = source.getWidth(), h = source.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((source.getRGB(x, y) >>> 24) <= 20) continue;
                boolean edge = false;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h || (source.getRGB(nx, ny) >>> 24) <= 20) edge = true;
                }
                out.setRGB(x, y, edge ? 0xFF9B4DFF : 0xFF0A0612);
            }
        }
        return out;
    }

    private static BufferedImage whiteOf(BufferedImage source) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int alpha = source.getRGB(x, y) >>> 24;
                if (alpha > 20) out.setRGB(x, y, (alpha << 24) | 0xFFFFFF);
            }
        }
        return out;
    }

    private void drawKnockOut(Graphics2D g, int w, int h) {
        double q = koTime;
        if (q < KO_SLOW_SECONDS) {
            // Lineas de camara lenta en los bordes
            g.setStroke(new BasicStroke(4));
            g.setColor(new Color(255, 255, 255, 170));
            for (int i = 0; i < 36; i++) {
                double a = i * Math.PI * 2 / 36 + 0.2;
                double r1 = 380 + (i * 71 + time * 900) % 200;
                g.drawLine((int) (w / 2 + Math.cos(a) * r1 * 1.5), (int) (h / 2 + Math.sin(a) * r1),
                        (int) (w / 2 + Math.cos(a) * (r1 + 150) * 1.5), (int) (h / 2 + Math.sin(a) * (r1 + 150)));
            }
        }
        if (q >= 0.35) {
            double t = q - 0.35;
            double pop = 1 + Math.max(0, 0.2 - t) * 4;
            outlined(g, "K.O.", w / 2, h / 2 + 70, (int) (230 * pop), new Color(0xFF3C32), Color.WHITE, 7);
            if (finalKo) outlined(g, "¡FIN DEL COMBATE!", w / 2, h / 2 + 150, 40, new Color(0xFFE45A), OUTLINE, 3);
            if (t < 0.06) flash(g, w, h, 0.6f);
        }
    }

    /** Rays and the winning Pokemon, drawn behind the results menu. */
    void drawWinner(Graphics2D g, FightingGameSession session) {
        if (session.getOutcome() == null) return;
        int w = session.getWidth();
        int h = session.getHeight();
        int winner = switch (session.getOutcome()) {
            case PLAYER_ONE_WINS -> 0;
            case PLAYER_TWO_WINS -> 1;
            default -> -1;
        };
        Color main = session.isBossFight() && winner == session.getBossIndex() ? BOSS_COLOR
                : winner == 1 ? P2_COLOR : P1_COLOR;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g.setColor(main);
        for (int i = 0; i < 18; i++) {
            double a = i * Math.PI / 9;
            g.fill(new Polygon(new int[]{w / 2, (int) (w / 2 + Math.cos(a) * 1400), (int) (w / 2 + Math.cos(a + 0.16) * 1400)},
                    new int[]{h / 2, (int) (h / 2 + Math.sin(a) * 1400), (int) (h / 2 + Math.sin(a + 0.16) * 1400)}, 3));
        }
        g.setComposite(old);
        if (winner < 0) {
            outlined(g, "¡EMPATE!", w / 2, 120, 84, Color.WHITE, OUTLINE, 5);
            return;
        }
        FightingTeam team = winner == 0 ? session.getPlayerOneTeam() : session.getPlayerTwoTeam();
        Fighter champion = team.getActive();
        if (session.isBossFight() && winner == session.getBossIndex()) {
            outlined(g, "MEWTWO ES INVENCIBLE...", w / 2, 125, 72, BOSS_COLOR, Color.WHITE, 5);
            drawTaunt(g, session, w, h);
        } else if (session.isBossFight()) {
            outlined(g, "¡DERROTASTE A MEWTWO!", w / 2, 125, 76, new Color(0xFFE45A), OUTLINE, 5);
        } else {
            outlined(g, "¡" + session.getTrainerName(winner).toUpperCase() + " GANA!", w / 2, 125, 84,
                    new Color(0xFFE45A), OUTLINE, 5);
        }
        drawPortrait(g, champion, 190, h / 2 + 60, 280, false, "Pose");
        drawPortrait(g, champion, w - 190, h / 2 + 60, 280, true, "Pose");
    }

    private static final String[] TAUNTS = {
            "¿ESO ES TODO LO QUE TIENES?",
            "VUELVE CUANDO SEAS MAS FUERTE",
            "NINGUN POKEMON PUEDE VENCERME",
            "QUE DECEPCION, HUMANO"
    };

    /** Mewtwo se rie de ti: globo con "¡JA JA JA!" que rebota y una frase para picarte. */
    private void drawTaunt(Graphics2D g, FightingGameSession session, int w, int h) {
        String taunt = TAUNTS[Math.floorMod(System.identityHashCode(session), TAUNTS.length)];
        int bx = 190, by = h / 2 - 150;
        int bw = 350, bh = 96;
        g.setColor(Color.WHITE);
        g.fillRoundRect(bx - bw / 2, by - 50, bw, bh, 26, 26);
        g.fill(new Polygon(new int[]{bx - 20, bx + 20, bx + 4}, new int[]{by + 44, by + 44, by + 74}, 3));
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(bx - bw / 2, by - 50, bw, bh, 26, 26);
        String[] has = {"¡JA", "JA", "JA!"};
        for (int i = 0; i < has.length; i++) {
            double hop = Math.abs(Math.sin(time * 9 + i * 0.9)) * 8;
            outlined(g, has[i], bx - 80 + i * 80, by - 6 - (int) hop, 36, BOSS_COLOR, OUTLINE, 2);
        }
        g.setFont(RetroStyle.font(17));
        g.setColor(RetroStyle.DARK);
        int tw = g.getFontMetrics().stringWidth(taunt);
        g.drawString(taunt, bx - tw / 2, by + 30);
    }

    // ------------------------------------------------------------------ ayudantes

    private void drawPortrait(Graphics2D g, Fighter fighter, int cx, int cy, int height, boolean facingLeft) {
        drawPortrait(g, fighter, cx, cy, height, facingLeft, "Idle");
    }

    private void drawPortrait(Graphics2D g, Fighter fighter, int cx, int cy, int height,
                              boolean facingLeft, String animationName) {
        if (fighter == null) return;
        FightingSpriteProfile profile = FightingSpriteProfile.forFighter(fighter);
        if (profile == null) return;
        var sprites = profile.sprites();
        var animation = sprites.get(animationName);
        if (animation == null) animation = sprites.get("Idle");
        if (animation == null) return;
        BufferedImage image = animation.frameAtTime(time, facingLeft, true).image();
        int[] box = opaqueBounds(image);
        if (box == null) return;
        int bw = box[2] - box[0];
        int bh = box[3] - box[1];
        int scale = Math.max(2, Math.min(10, height / Math.max(1, bh)));
        int dw = bw * scale;
        int dh = bh * scale;
        g.drawImage(image, cx - dw / 2, cy - dh / 2, cx - dw / 2 + dw, cy - dh / 2 + dh,
                box[0], box[1], box[2], box[3], null);
    }

    private static int[] opaqueBounds(BufferedImage image) {
        int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 20) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? null : new int[]{minX, minY, maxX + 1, maxY + 1};
    }

    private static void outlined(Graphics2D g, String text, int centerX, int baseline, int size,
                                 Color fill, Color outline, int thickness) {
        g.setFont(RetroStyle.font(size));
        FontMetrics metrics = g.getFontMetrics();
        int x = centerX - metrics.stringWidth(text) / 2;
        g.setColor(outline);
        for (int dx = -thickness; dx <= thickness; dx += thickness) {
            for (int dy = -thickness; dy <= thickness; dy += thickness) {
                if (dx != 0 || dy != 0) g.drawString(text, x + dx, baseline + dy);
            }
        }
        g.setColor(fill);
        g.drawString(text, x, baseline);
    }

    private static void flash(Graphics2D g, int w, int h, float alpha) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setComposite(old);
    }

    private static double ease(double t) {
        return 1 - Math.pow(1 - t, 3);
    }
}

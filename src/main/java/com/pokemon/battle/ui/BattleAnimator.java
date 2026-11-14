package com.pokemon.battle.ui;

import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.MoveCategory;
import com.pokemon.battle.model.Type;
import com.pokemon.battle.service.MoveEvent;

import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

/**
 * Plays a battle turn on the {@link BattleScreenForm} step by step: the
 * messages appear one at a time, each attack shows its animation over the
 * target, the HP bar goes down after the hit and the Pokemon that faint sink
 * into the ground.
 * <p>
 * Two-turn moves that make the user invulnerable hide it: with a Flying move
 * (Fly) it flies up out of the screen, with a Ground move (Dig) it goes under
 * the ground. It stays hidden until it attacks in the next turn.
 */
final class BattleAnimator {

    private static final int TICK_MS = 30;
    private static final int MESSAGE_MS = 850;
    private static final int FIRST_MESSAGE_MS = 450;
    private static final int HP_WAIT_MAX_MS = 1600;

    /**
     * How a Pokemon is moved while an animation runs.
     */
    static final class Pose {
        double dx;
        double dy;
        double alpha = 1;
        /** 0 = standing, 1 = completely under the ground. */
        double sink;
        boolean blink;
        long blinkStart;

        boolean isVisibleNow() {
            return !blink || ((System.currentTimeMillis() - blinkStart) / 80) % 2 == 0;
        }
    }

    // Un efecto (GIF de ataque) que se esta dibujando encima del campo
    private static final class Overlay {
        final GifAnimation animation;
        final long start;
        final int cx;
        final int cy;
        final int size;
        final boolean flip;

        Overlay(GifAnimation animation, int cx, int cy, int size, boolean flip) {
            this.animation = animation;
            this.start = System.currentTimeMillis();
            this.cx = cx;
            this.cy = cy;
            this.size = size;
            this.flip = flip;
        }

        int frameIndex() {
            return (int) ((System.currentTimeMillis() - start) * GifAnimation.FPS / 1000);
        }

        boolean finished() {
            return frameIndex() >= animation.frameCount();
        }
    }

    // Un paso del guion: dura un tiempo, o hasta que se cumpla "until"
    private static final class Step {
        final long duration;
        final Runnable onStart;
        final DoubleConsumer onProgress;
        final BooleanSupplier until;

        Step(long duration, Runnable onStart, DoubleConsumer onProgress, BooleanSupplier until) {
            this.duration = duration;
            this.onStart = onStart;
            this.onProgress = onProgress;
            this.until = until;
        }
    }

    private final BattleScreenForm form;
    private final Timer timer;
    private final Deque<Step> steps = new ArrayDeque<>();
    private final Map<Combatant, Pose> poses = new HashMap<>();
    private final Set<Combatant> hidden = new HashSet<>();
    private final Set<Combatant> standing = new HashSet<>();
    private final List<Overlay> overlays = new ArrayList<>();

    private Step current;
    private long currentStart;
    private boolean running;

    BattleAnimator(BattleScreenForm form) {
        this.form = form;
        this.timer = new Timer(TICK_MS, e -> tick());
    }

    // ------------------------------------------------------------------ lo que consulta la pantalla al dibujar

    boolean isRunning() {
        return running;
    }

    boolean isHidden(Combatant c) {
        return hidden.contains(c);
    }

    /**
     * @return true when a fainted Pokemon must still be drawn (its faint animation has not played yet)
     */
    boolean isStanding(Combatant c) {
        return standing.contains(c);
    }

    Pose pose(Combatant c) {
        return poses.get(c);
    }

    /**
     * Hides the Pokemon that are flying or underground, reading it from the
     * battle. Used when the turn ends and when the Pokemon on the field change.
     */
    void syncHidden(Combatant... combatants) {
        if (running) {
            return;
        }
        hidden.clear();
        for (Combatant c : combatants) {
            if (c != null && c.isCharging() && c.isInvulnerable()) {
                hidden.add(c);
            }
        }
    }

    void drawEffects(Graphics2D g) {
        Iterator<Overlay> it = overlays.iterator();
        while (it.hasNext()) {
            Overlay o = it.next();
            if (o.finished()) {
                it.remove();
                continue;
            }
            BufferedImage img = o.animation.frame(o.frameIndex());
            int half = o.size / 2;
            if (o.flip) {
                // Espejo horizontal: el efecto va del rival hacia el jugador
                RetroStyle.drawPixelImage(g, img, o.cx + half, o.cy - half, -o.size, o.size);
            } else {
                RetroStyle.drawPixelImage(g, img, o.cx - half, o.cy - half, o.size, o.size);
            }
        }
    }

    // ------------------------------------------------------------------ armar el guion del turno

    /**
     * Plays the turn. The messages appear one by one and each move is animated
     * just after its first message ("X uso Y.").
     *
     * @param messages all the messages of the turn
     * @param events   moves used in the turn, in order
     * @param onDone   what to do when everything has been shown
     */
    void play(List<String> messages, List<MoveEvent> events, Runnable onDone) {
        steps.clear();
        overlays.clear();
        poses.clear();
        running = true;
        standing.clear();
        standing.add(form.playerCombatant());
        standing.add(form.rivalCombatant());
        form.clearLines();
        form.freezeHp();

        int next = 0;
        for (int i = 0; i < events.size(); i++) {
            MoveEvent e = events.get(i);
            int start = Math.max(next, Math.min(e.getMessageIndex(), messages.size()));
            int end = i + 1 < events.size() ? events.get(i + 1).getMessageIndex() : messages.size();
            end = Math.max(start, Math.min(end, messages.size()));

            // Mensajes de antes del ataque (por ejemplo, dano por veneno al empezar)
            addHp(e.getAttacker(), e.getAttackerHpBefore(), e.getDefender(), e.getDefenderHpBefore());
            addMessages(messages, next, start);

            // "X uso Y." y despues la animacion
            int firstShown = addFirstMessage(messages, start, end);
            addMoveAnimation(e);
            addHp(e.getAttacker(), e.getAttackerHpAfter(), e.getDefender(), e.getDefenderHpAfter());
            addMessages(messages, firstShown, end);
            next = end;
        }
        // Lo que queda (por ejemplo, "se ha debilitado") con la vida ya real
        steps.add(new Step(0, form::releaseHp, null, null));
        addWaitHp();
        addMessages(messages, next, messages.size());
        addFaints();
        steps.add(new Step(0, () -> {
            running = false;
            standing.clear();
            poses.clear();
            syncHidden(form.playerCombatant(), form.rivalCombatant());
            timer.stop();
            onDone.run();
        }, null, null));

        startNext();
        timer.start();
    }

    private void addHp(Combatant a, int hpA, Combatant b, int hpB) {
        steps.add(new Step(0, () -> {
            form.setHpTarget(a, hpA);
            form.setHpTarget(b, hpB);
        }, null, null));
        addWaitHp();
    }

    private void addWaitHp() {
        steps.add(new Step(HP_WAIT_MAX_MS, null, null, form::hpSettled));
    }

    private void addMessages(List<String> messages, int from, int to) {
        for (int i = from; i < to; i++) {
            String m = messages.get(i);
            if (m.isBlank()) {
                continue;
            }
            if (m.toLowerCase().contains("debilitado")) {
                addFaints();
            }
            steps.add(new Step(MESSAGE_MS, () -> form.addLine(m), null, null));
        }
    }

    // Muestra el primer mensaje no vacio del ataque. Devuelve desde donde siguen los demas
    private int addFirstMessage(List<String> messages, int start, int end) {
        for (int i = start; i < end; i++) {
            String m = messages.get(i);
            if (!m.isBlank()) {
                steps.add(new Step(FIRST_MESSAGE_MS, () -> form.addLine(m), null, null));
                return i + 1;
            }
        }
        return start;
    }

    private void addFaints() {
        steps.add(new Step(0, () -> {
            for (Combatant c : new ArrayList<>(standing)) {
                if (c.isFainted()) {
                    queueFront(faintStep(c));
                }
            }
        }, null, null));
    }

    // Se mete al frente de la cola, para que se vea justo ahora
    private void queueFront(Step step) {
        steps.addFirst(step);
    }

    private Step faintStep(Combatant c) {
        return new Step(600, null, p -> {
            Pose pose = poseOf(c);
            pose.sink = p;
            pose.alpha = 1 - p * 0.6;
            if (p >= 1) {
                standing.remove(c);
            }
        }, null);
    }

    // ------------------------------------------------------------------ animacion de cada ataque

    private void addMoveAnimation(MoveEvent e) {
        Combatant attacker = e.getAttacker();
        Type type = e.getMove().getType();
        switch (e.getKind()) {
            case CHARGE:
                if (e.isInvulnerable()) {
                    addHide(attacker, type);
                } else {
                    // Se prepara sin esconderse: un brillo de su tipo encima de el
                    addEffect(attacker, AttackEffects.forMove(e.getMove()), false, true);
                }
                return;
            case RELEASE:
                if (hidden.contains(attacker)) {
                    addShow(attacker, type);
                }
                addHit(e);
                return;
            default:
                addHit(e);
        }
    }

    private void addHit(MoveEvent e) {
        Combatant attacker = e.getAttacker();
        Combatant target = e.isSelfTarget() ? attacker : e.getDefender();
        if (e.getMove().getCategory() != MoveCategory.STATUS) {
            addLunge(attacker);
        }
        if (e.isMissed()) {
            return;
        }
        GifAnimation effect = AttackEffects.forMove(e.getMove());
        boolean damaged = e.getDefenderHpAfter() < e.getDefenderHpBefore();
        if (effect != null) {
            addEffect(target, effect, !e.isSelfTarget(), true);
        }
        if (damaged) {
            Combatant defender = e.getDefender();
            steps.add(new Step(480, () -> {
                Pose pose = poseOf(defender);
                pose.blink = true;
                pose.blinkStart = System.currentTimeMillis();
            }, p -> {
                if (p >= 1) {
                    poseOf(defender).blink = false;
                }
            }, null));
        }
    }

    // El atacante se lanza un poco hacia el rival y vuelve
    private void addLunge(Combatant attacker) {
        boolean isPlayer = attacker == form.playerCombatant();
        double dirX = isPlayer ? 34 : -34;
        double dirY = isPlayer ? -16 : 16;
        steps.add(new Step(260, null, p -> {
            double f = Math.sin(Math.PI * p);
            Pose pose = poseOf(attacker);
            pose.dx = dirX * f;
            pose.dy = dirY * f;
        }, null));
    }

    /**
     * @param on     Pokemon over which the effect is drawn
     * @param mirror true to flip it when it goes towards the player
     * @param wait   true to wait until the GIF ends
     */
    private void addEffect(Combatant on, GifAnimation effect, boolean mirror, boolean wait) {
        if (effect == null) {
            return;
        }
        long duration = wait ? effect.frameCount() * 1000L / GifAnimation.FPS : 0;
        steps.add(new Step(duration, () -> startEffect(on, effect, mirror, 0.5), null, null));
    }

    // heightFactor: 0.5 = centro del pokemon, 0.35 = mas cerca de los pies
    private void startEffect(Combatant on, GifAnimation effect, boolean mirror, double heightFactor) {
        // El efecto se ajusta al tamano real del pokemon, con un minimo para que se note
        int size = Math.max(120, (int) (form.spriteSizeOf(on) * 1.3));
        int cx = form.feetXOf(on);
        int cy = (int) (form.feetYOf(on) - form.spriteHeightOf(on) * heightFactor);
        boolean flip = mirror && on == form.playerCombatant();
        overlays.add(new Overlay(effect, cx, cy, size, flip));
    }

    // Primer turno de Vuelo / Excavar: el pokemon se va
    private void addHide(Combatant c, Type type) {
        if (type == Type.FLYING) {
            steps.add(new Step(650, null, p -> {
                Pose pose = poseOf(c);
                double eased = p * p;
                pose.dy = -520 * eased;
                pose.dx = (c == form.playerCombatant() ? 60 : -60) * eased;
                pose.alpha = 1 - 0.5 * p;
            }, null));
        } else if (type == Type.GROUND) {
            GifAnimation dirt = AttackEffects.byName("EXCAVAR");
            steps.add(new Step(750, () -> {
                if (dirt != null) {
                    startEffect(c, dirt, false, 0.35);
                }
            }, p -> {
                Pose pose = poseOf(c);
                pose.sink = p;
                pose.dx = Math.sin(p * 40) * 3 * (1 - p);   // tiembla mientras se entierra
            }, null));
        } else {
            steps.add(new Step(450, null, p -> poseOf(c).alpha = 1 - p, null));
        }
        steps.add(new Step(0, () -> {
            hidden.add(c);
            poses.remove(c);
        }, null, null));
    }

    // Segundo turno: el pokemon vuelve a aparecer en su sitio antes de golpear
    private void addShow(Combatant c, Type type) {
        steps.add(new Step(0, () -> hidden.remove(c), null, null));
        if (type == Type.FLYING) {
            steps.add(new Step(380, null, p -> {
                Pose pose = poseOf(c);
                double rest = (1 - p) * (1 - p);
                pose.dy = -520 * rest;
                pose.dx = 0;
                pose.alpha = 1;
            }, null));
        } else if (type == Type.GROUND) {
            GifAnimation dirt = AttackEffects.byName("EXCAVAR");
            steps.add(new Step(450, () -> {
                if (dirt != null) {
                    startEffect(c, dirt, false, 0.35);
                }
            }, p -> poseOf(c).sink = 1 - p, null));
        } else {
            steps.add(new Step(300, null, p -> poseOf(c).alpha = p, null));
        }
        steps.add(new Step(0, () -> poses.remove(c), null, null));
    }

    private Pose poseOf(Combatant c) {
        return poses.computeIfAbsent(c, k -> new Pose());
    }

    // ------------------------------------------------------------------ reloj

    private void startNext() {
        current = steps.pollFirst();
        currentStart = System.currentTimeMillis();
        if (current != null && current.onStart != null) {
            current.onStart.run();
        }
    }

    private void tick() {
        // Varios pasos de duracion 0 se resuelven en el mismo tick
        int guard = 0;
        while (current != null && guard++ < 50) {
            long elapsed = System.currentTimeMillis() - currentStart;
            double progress = current.duration <= 0 ? 1 : Math.min(1, elapsed / (double) current.duration);
            if (current.onProgress != null) {
                current.onProgress.accept(progress);
            }
            boolean done = progress >= 1 || (current.until != null && current.until.getAsBoolean());
            if (!done) {
                break;
            }
            startNext();
        }
        form.repaint();
    }

    // ------------------------------------------------------------------ ayuda para dibujar

    /**
     * Applies the transparency of the pose and returns the previous composite.
     */
    static Composite applyAlpha(Graphics2D g, Pose pose) {
        Composite old = g.getComposite();
        if (pose != null && pose.alpha < 1) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    (float) Math.max(0, Math.min(1, pose.alpha))));
        }
        return old;
    }
}

package com.pokemon.battle.fighting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Shared simulation-time state machine for the four free-fight ultimates. */
public final class UltimateController {

    private static final double INTERRUPT_RECOVERY_SECONDS = 0.70;
    private static final double FIRST_BEAM_PULSE_SECONDS = 0.12;
    private static final double BEAM_END_LINGER_SECONDS = 0.18;
    private static final double RING_EXPANSION_SECONDS = 0.42;
    private static final double RING_END_LINGER_SECONDS = 0.18;
    private static final double FINISH_EFFECT_SECONDS = 0.22;

    public enum Phase { IDLE, CHARGING, RELEASING, INTERRUPTED, FINISHING }

    public record Visual(int ownerIndex, Fighter owner, Fighter target,
                         UltimateDefinition definition, Phase phase,
                         double progress, double elapsedSeconds, double pulseFlash,
                         double centerX, double centerY, List<Double> ringRadii) { }

    private static final class State {
        private final int ownerIndex;
        private final Fighter owner;
        private final Fighter target;
        private final UltimateDefinition definition;
        private final double centerX;
        private final double centerY;
        private final double[] ringRadii;
        private Phase phase = Phase.CHARGING;
        private double remaining;
        private double elapsed;
        private double pulseFlash;
        private int nextPulse;
        private boolean stunApplied;

        private State(int ownerIndex, Fighter owner, Fighter target,
                      UltimateDefinition definition) {
            this.ownerIndex = ownerIndex;
            this.owner = owner;
            this.target = target;
            this.definition = definition;
            this.centerX = owner.getX();
            this.centerY = owner.getY() - owner.getRenderHeight() * 0.55;
            this.remaining = definition.chargeSeconds();
            this.ringRadii = new double[definition.pulseCount()];
        }
    }

    private record Pulse(State state, Fighter target, int damage) { }

    private final State[] states = new State[2];

    public boolean begin(int playerIndex, List<Fighter> fighters, boolean requested,
                         boolean normalActionReady) {
        if (!requested || !normalActionReady || playerIndex < 0 || playerIndex >= states.length
                || fighters.size() < 2 || states[playerIndex] != null) return false;
        Fighter owner = fighters.get(playerIndex);
        Fighter target = fighters.get(1 - playerIndex);
        UltimateDefinition definition = UltimateDefinition.forEvolvedForm(owner.getPokemonId());
        if (definition == null || owner.isDefeated() || owner.isAsleep()
                || owner.isUltimateStunned() || target.isDefeated()
                || !owner.consumeUltimateEnergy()) return false;
        states[playerIndex] = new State(playerIndex, owner, target, definition);
        return true;
    }

    public boolean isPlayerInputLocked(int playerIndex, List<Fighter> fighters) {
        if (playerIndex < 0 || playerIndex >= fighters.size()) return false;
        return isFighterInputLocked(fighters.get(playerIndex));
    }

    public boolean isFighterInputLocked(Fighter fighter) {
        if (fighter.isAsleep() || fighter.isUltimateStunned()) return true;
        for (State state : states) {
            if (state != null && state.owner == fighter
                    && (state.phase == Phase.CHARGING || state.phase == Phase.RELEASING
                        || state.phase == Phase.INTERRUPTED)) return true;
        }
        return false;
    }

    public boolean hasActiveSequence(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= states.length) return false;
        State state = states[playerIndex];
        return state != null && state.phase != Phase.IDLE && state.phase != Phase.FINISHING;
    }

    /** A successful damaging hit is the only way to interrupt a charge. */
    public void onDamagingHit(Fighter damaged) {
        for (State state : states) {
            if (state == null || state.phase != Phase.CHARGING || state.owner != damaged) continue;
            if (damaged.isDefeated()) {
                reset(state.ownerIndex);
            } else {
                damaged.refundInterruptedUltimate();
                state.phase = Phase.INTERRUPTED;
                state.remaining = INTERRUPT_RECOVERY_SECONDS;
                state.pulseFlash = 0.18;
            }
        }
    }

    /** Clears sequences owned by, or targeting, the participant being replaced. */
    public void clearPlayerState(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= states.length) return;
        // In this two-player arena every active sequence belongs to one side
        // and targets the other, so replacement invalidates both sides' references.
        clearAll();
    }

    public void clearAll() {
        reset(0);
        reset(1);
    }

    /** Advances states and applies all pulses as one simulation-step batch. */
    public void update(List<Fighter> fighters, int arenaWidth, int arenaHeight,
                       double deltaSeconds) {
        List<Pulse> duePulses = new ArrayList<>(2);
        double maximumRadius = Math.hypot(arenaWidth, arenaHeight);

        for (int index = 0; index < states.length; index++) {
            State state = states[index];
            if (state == null) continue;
            if (!stillOwnsActivePair(state, fighters)) {
                boolean ownerStillActive = fighters.size() >= 2
                        && fighters.get(state.ownerIndex) == state.owner
                        && !state.owner.isDefeated();
                if (ownerStillActive && state.phase == Phase.RELEASING) {
                    finishSuccessfully(index, state, arenaWidth);
                } else if (state.phase == Phase.CHARGING && ownerStillActive) {
                    state.owner.refundInterruptedUltimate();
                    reset(index);
                } else {
                    reset(index);
                }
                continue;
            }

            state.pulseFlash = Math.max(0, state.pulseFlash - deltaSeconds);
            if (state.phase == Phase.CHARGING) {
                state.remaining -= deltaSeconds;
                if (state.remaining <= 0) {
                    state.phase = Phase.RELEASING;
                    state.elapsed = 0;
                    Arrays.fill(state.ringRadii, 0);
                }
            } else if (state.phase == Phase.INTERRUPTED) {
                state.remaining -= deltaSeconds;
                if (state.remaining <= 0) reset(index);
            } else if (state.phase == Phase.RELEASING) {
                state.elapsed += deltaSeconds;
                if (state.definition.shape() == UltimateDefinition.Shape.BEAM) {
                    advanceBeam(state, duePulses);
                } else {
                    advanceRings(state, duePulses, maximumRadius);
                }
            } else if (state.phase == Phase.FINISHING) {
                state.remaining -= deltaSeconds;
                if (state.remaining <= 0) reset(index);
            }
        }

        // Resolve simultaneous ultimates without player-order advantage.
        for (Pulse pulse : duePulses) {
            pulse.target().receiveDamage(pulse.damage());
        }
        for (Pulse pulse : duePulses) {
            onDamagingHit(pulse.target());
        }

        for (int index = 0; index < states.length; index++) {
            State state = states[index];
            if (state == null || state.phase != Phase.RELEASING) continue;
            if (state.target.isDefeated() && !state.owner.isDefeated()) {
                finishSuccessfully(index, state, arenaWidth);
                continue;
            }
            if (state.nextPulse < state.definition.pulseCount()) continue;
            if (state.elapsed >= finishAt(state)) finishSuccessfully(index, state, arenaWidth);
        }
    }

    private boolean stillOwnsActivePair(State state, List<Fighter> fighters) {
        return fighters.size() >= 2 && fighters.get(state.ownerIndex) == state.owner
                && fighters.get(1 - state.ownerIndex) == state.target
                && !state.owner.isDefeated() && !state.target.isDefeated();
    }

    private void advanceBeam(State state, List<Pulse> duePulses) {
        state.owner.setFacingLeft(state.target.getX() < state.owner.getX());
        while (state.nextPulse < state.definition.pulseCount()
                && state.elapsed >= FIRST_BEAM_PULSE_SECONDS
                + state.nextPulse * state.definition.pulseIntervalSeconds()) {
            issuePulse(state, duePulses);
        }
    }

    private void advanceRings(State state, List<Pulse> duePulses, double maximumRadius) {
        if (state.nextPulse >= state.definition.pulseCount()) return;
        int pulseIndex = state.nextPulse;
        double ringStart = pulseIndex * state.definition.pulseIntervalSeconds();
        double ringElapsed = state.elapsed - ringStart;
        if (ringElapsed < 0) return;
        double radius = maximumRadius * Math.min(1, ringElapsed / RING_EXPANSION_SECONDS);
        state.ringRadii[pulseIndex] = radius;
        double targetDistance = Math.hypot(state.target.getX() - state.centerX,
                state.target.getY() - state.centerY);
        if (radius >= targetDistance || ringElapsed >= RING_EXPANSION_SECONDS) {
            issuePulse(state, duePulses);
        }
    }

    private void issuePulse(State state, List<Pulse> duePulses) {
        if (!state.stunApplied) {
            state.target.addUltimateStun();
            state.stunApplied = true;
        }
        state.pulseFlash = 0.16;
        duePulses.add(new Pulse(state, state.target, state.definition.damagePerPulse()));
        state.nextPulse++;
    }

    private double finishAt(State state) {
        if (state.definition.shape() == UltimateDefinition.Shape.BEAM) {
            return FIRST_BEAM_PULSE_SECONDS
                    + (state.definition.pulseCount() - 1)
                    * state.definition.pulseIntervalSeconds() + BEAM_END_LINGER_SECONDS;
        }
        return (state.definition.pulseCount() - 1)
                * state.definition.pulseIntervalSeconds()
                + RING_EXPANSION_SECONDS + RING_END_LINGER_SECONDS;
    }

    private void finishSuccessfully(int playerIndex, State state, int arenaWidth) {
        removeStun(state);
        if ("HYDRO_PUMP".equals(state.definition.moveId()) && !state.target.isDefeated()) {
            double direction = state.target.getX() < state.owner.getX() ? -1 : 1;
            double halfWidth = PhysicsSystem.halfWidthFor(state.target);
            double recoilX = state.target.getX() + direction * 72;
            state.target.setX(Math.max(halfWidth, Math.min(arenaWidth - halfWidth, recoilX)));
        }
        state.owner.returnToBaseAfterUltimate();
        state.phase = Phase.FINISHING;
        state.remaining = FINISH_EFFECT_SECONDS;
    }

    private void removeStun(State state) {
        if (state.stunApplied) {
            state.target.removeUltimateStun();
            state.stunApplied = false;
        }
    }

    private void reset(int playerIndex) {
        State state = states[playerIndex];
        if (state != null) removeStun(state);
        states[playerIndex] = null;
    }

    public List<Visual> getVisuals() {
        List<Visual> visuals = new ArrayList<>(2);
        for (State state : states) {
            if (state == null) continue;
            List<Double> radii = new ArrayList<>(state.ringRadii.length);
            for (double radius : state.ringRadii) radii.add(radius);
            double progress = switch (state.phase) {
                case CHARGING -> Math.min(1, 1 - state.remaining / state.definition.chargeSeconds());
                case RELEASING -> Math.min(1, state.elapsed / finishAt(state));
                case INTERRUPTED, FINISHING -> 1;
                case IDLE -> 0;
            };
            visuals.add(new Visual(state.ownerIndex, state.owner, state.target,
                    state.definition, state.phase, progress, state.elapsed,
                    state.pulseFlash, state.centerX, state.centerY,
                    Collections.unmodifiableList(radii)));
        }
        return Collections.unmodifiableList(visuals);
    }

    public String getPhaseName(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= states.length || states[playerIndex] == null) return "IDLE";
        return states[playerIndex].phase.name();
    }

    public int getPulsesApplied(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= states.length || states[playerIndex] == null) return 0;
        return states[playerIndex].nextPulse;
    }
}

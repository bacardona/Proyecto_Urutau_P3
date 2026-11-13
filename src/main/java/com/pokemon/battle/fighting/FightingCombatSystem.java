package com.pokemon.battle.fighting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Runs free-fight attack phases, hitboxes, damage, and projectile collisions. */
public class FightingCombatSystem {

    private static final double EVOLUTION_ENERGY_PER_HIT = 15.0;

    private static final AttackDefinition QUICK_ATTACK = new AttackDefinition(
            "QUICK_ATTACK", AttackDefinition.Kind.DASH, 12,
            0.08, 0.18, 0.38,
            72, 86, 470, 0, 0, 0, new Color(0xFFE36E));
    private static final AttackDefinition THUNDER_SHOCK = new AttackDefinition(
            "THUNDER_SHOCK", AttackDefinition.Kind.PROJECTILE, 10,
            0.12, 0.08, 0.48,
            0, 0, 0, 560, 1.7, 22, new Color(0xFFE02E));
    private static final AttackDefinition EMBER = new AttackDefinition(
            "EMBER", AttackDefinition.Kind.PROJECTILE, 10,
            0.14, 0.08, 0.48,
            0, 0, 0, 490, 2.0, 24, new Color(0xFF722E));
    private static final AttackDefinition TACKLE = new AttackDefinition(
            "TACKLE", AttackDefinition.Kind.DASH, 12,
            0.08, 0.16, 0.40,
            64, 82, 360, 0, 0, 0, new Color(0xF2C28B));

    private enum Phase { READY, STARTUP, ACTIVE, RECOVERY }

    private static final class AttackState {
        private Phase phase = Phase.READY;
        private AttackDefinition definition;
        private double remaining;
        private boolean hasHit;
        private boolean activeEffectStarted;
    }

    private final AttackState[] states = {new AttackState(), new AttackState()};
    private final List<FightingProjectile> projectiles = new ArrayList<>();
    private final List<FightingProjectile> projectileView = Collections.unmodifiableList(projectiles);

    public boolean hasAttackProfile(String pokemonId) {
        return "PIKACHU".equals(pokemonId) || "CHARIZARD".equals(pokemonId);
    }

    public void clearPlayerState(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= states.length) return;
        AttackState state = states[playerIndex];
        state.phase = Phase.READY;
        state.definition = null;
        state.remaining = 0;
        state.hasHit = false;
        state.activeEffectStarted = false;
        projectiles.clear();
    }

    public void update(List<Fighter> fighters, int arenaWidth,
                       boolean playerOneQuickAttack, boolean playerOneThunderShock,
                       boolean playerTwoEmber, boolean playerTwoTackle,
                       double deltaSeconds) {
        Fighter playerOne = fighters.get(0);
        Fighter playerTwo = fighters.get(1);
        AttackDefinition playerOneAttack = playerOneQuickAttack ? QUICK_ATTACK
                : playerOneThunderShock ? THUNDER_SHOCK : null;
        AttackDefinition playerTwoAttack = playerTwoEmber ? EMBER
                : playerTwoTackle ? TACKLE : null;
        if ("PIKACHU".equals(playerOne.getPokemonId())) {
            startRequestedAttack(states[0], playerOneAttack);
        }
        if ("CHARIZARD".equals(playerTwo.getPokemonId())) {
            startRequestedAttack(states[1], playerTwoAttack);
        }

        for (int i = 0; i < states.length; i++) {
            updateAttackState(i, fighters, arenaWidth, deltaSeconds);
        }
        updateProjectiles(fighters, arenaWidth, deltaSeconds);
    }

    private void startRequestedAttack(AttackState state, AttackDefinition requested) {
        if (requested == null || state.phase != Phase.READY) return;
        state.definition = requested;
        state.phase = Phase.STARTUP;
        state.remaining = requested.startupSeconds();
        state.hasHit = false;
        state.activeEffectStarted = false;
    }

    private void updateAttackState(int attackerIndex, List<Fighter> fighters,
                                   int arenaWidth, double deltaSeconds) {
        AttackState state = states[attackerIndex];
        if (state.phase == Phase.READY) return;
        state.remaining -= deltaSeconds;

        if (state.phase == Phase.STARTUP && state.remaining <= 0) {
            state.phase = Phase.ACTIVE;
            state.remaining += state.definition.activeSeconds();
        } else if (state.phase == Phase.ACTIVE) {
            Fighter attacker = fighters.get(attackerIndex);
            Fighter target = fighters.get(1 - attackerIndex);
            if (!state.activeEffectStarted) {
                state.activeEffectStarted = true;
                if (state.definition.kind() == AttackDefinition.Kind.PROJECTILE) {
                    spawnProjectile(attacker, attackerIndex, state.definition);
                }
            }
            if (state.definition.kind() == AttackDefinition.Kind.DASH) {
                double direction = attacker.isFacingLeft() ? -1 : 1;
                double minX = PhysicsSystem.halfWidthFor(attacker);
                double maxX = arenaWidth - minX;
                attacker.setX(Math.max(minX, Math.min(maxX,
                        attacker.getX() + direction * state.definition.dashSpeed() * deltaSeconds)));
                if (!state.hasHit && attackHitbox(attacker, state.definition).intersects(target)) {
                    applyHit(attacker, target, state.definition);
                    state.hasHit = true;
                }
            }
            if (state.remaining <= 0) {
                state.phase = Phase.RECOVERY;
                state.remaining += state.definition.recoverySeconds();
            }
        } else if (state.phase == Phase.RECOVERY && state.remaining <= 0) {
            state.phase = Phase.READY;
            state.definition = null;
        }
    }

    private Hitbox attackHitbox(Fighter attacker, AttackDefinition definition) {
        double direction = attacker.isFacingLeft() ? -1 : 1;
        double halfWidth = PhysicsSystem.halfWidthFor(attacker);
        double left = direction > 0
                ? attacker.getX() + halfWidth
                : attacker.getX() - halfWidth - definition.hitboxWidth();
        return new Hitbox(left, attacker.getY() - definition.hitboxHeight(),
                definition.hitboxWidth(), definition.hitboxHeight());
    }

    private void spawnProjectile(Fighter attacker, int ownerIndex, AttackDefinition definition) {
        double direction = attacker.isFacingLeft() ? -1 : 1;
        double originX = attacker.getX() + direction * (PhysicsSystem.halfWidthFor(attacker) + 12);
        double originY = attacker.getY() - 82;
        projectiles.add(new FightingProjectile(originX, originY,
                direction * definition.projectileSpeed(), definition.projectileSize(),
                definition.damage(), definition.projectileLifetime(), definition.effectColor(), ownerIndex));
    }

    private void applyHit(Fighter attacker, Fighter target, AttackDefinition definition) {
        target.receiveDamage(definition.damage());
        attacker.addEvolutionEnergy(EVOLUTION_ENERGY_PER_HIT);
    }

    private void updateProjectiles(List<Fighter> fighters, int arenaWidth, double deltaSeconds) {
        Iterator<FightingProjectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            FightingProjectile projectile = iterator.next();
            projectile.update(deltaSeconds);
            Fighter target = fighters.get(1 - projectile.getOwnerIndex());
            if (projectile.hitbox().intersects(target)) {
                Fighter attacker = fighters.get(projectile.getOwnerIndex());
                target.receiveDamage(projectile.getDamage());
                attacker.addEvolutionEnergy(EVOLUTION_ENERGY_PER_HIT);
                iterator.remove();
            } else if (projectile.isExpired(arenaWidth)) {
                iterator.remove();
            }
        }
    }

    public List<FightingProjectile> getProjectiles() { return projectileView; }

    public boolean isDashing(Fighter fighter, List<Fighter> fighters) {
        int index = fighters.indexOf(fighter);
        return index >= 0 && states[index].phase == Phase.ACTIVE
                && states[index].definition.kind() == AttackDefinition.Kind.DASH;
    }

    public AttackDefinition getActiveDefinition(Fighter fighter, List<Fighter> fighters) {
        int index = fighters.indexOf(fighter);
        if (index < 0) return null;
        AttackState state = states[index];
        return state.phase == Phase.ACTIVE ? state.definition : null;
    }
}

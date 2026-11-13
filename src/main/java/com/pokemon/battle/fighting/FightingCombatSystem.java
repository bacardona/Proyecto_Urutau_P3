package com.pokemon.battle.fighting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Runs free-fight attacks, temporary status effects, and projectile collisions. */
public class FightingCombatSystem {

    private static final double EVOLUTION_ENERGY_PER_HIT = 15.0;
    private static final double SLEEP_DURATION = 1.25;
    private static final double GROWTH_DURATION = 6.0;
    private static final double ATTACK_REDUCTION_DURATION = 3.5;
    private static final double DEFENSE_REDUCTION_DURATION = 3.0;
    private static final double GRUNT_LOCK_SECONDS = 0.65;
    private static final double FLY_MIN_RANGE = 120;
    private static final double FLY_MAX_RANGE = 460;
    private static final double FLY_TRAVEL_SECONDS = 0.72;
    private static final double FLY_RETREAT_SECONDS = 0.18;
    private static final double FLY_RETREAT_SPEED = 125;
    private static final double TAIL_LUNGE_SECONDS = 0.16;

    private static final AttackDefinition QUICK_ATTACK = new AttackDefinition(
            "QUICK_ATTACK", 22, AttackDefinition.Kind.DASH, 11,
            0.08, 0.20, 0.52, 58, 76, 430, 0, 0, 0, new Color(0xFFE36E));
    private static final AttackDefinition THUNDER_SHOCK = new AttackDefinition(
            "THUNDER_SHOCK", -1, AttackDefinition.Kind.PROJECTILE, 10,
            0.12, 0.08, 0.48, 0, 0, 0, 560, 1.7, 22, new Color(0xFFE02E));
    private static final AttackDefinition EMBER = new AttackDefinition(
            "EMBER", 25, AttackDefinition.Kind.PROJECTILE, 9,
            0.16, 0.06, 0.62, 0, 0, 0, 440, 1.8, 20, new Color(0xFF722E));
    private static final AttackDefinition WATER_GUN = new AttackDefinition(
            "WATER_GUN", 27, AttackDefinition.Kind.PROJECTILE, 9,
            0.14, 0.06, 0.60, 0, 0, 0, 470, 1.7, 21, new Color(0x43C8FF));
    private static final AttackDefinition TACKLE = new AttackDefinition(
            "TACKLE", 18, AttackDefinition.Kind.HITBOX, 8,
            0.10, 0.10, 0.52, 54, 62, 0, 0, 0, 0, new Color(0xF2C28B));
    private static final AttackDefinition VINE_WHIP = new AttackDefinition(
            "VINE_WHIP", 19, AttackDefinition.Kind.HITBOX, 10,
            0.14, 0.12, 0.66, 148, 54, 0, 0, 0, 0, new Color(0x5BBF53));
    private static final AttackDefinition SLEEP_POWDER = new AttackDefinition(
            "SLEEP_POWDER", 31, AttackDefinition.Kind.PROJECTILE, 0,
            0.16, 0.06, 3.10, 0, 0, 0, 390, 1.5, 18, new Color(0xB987D8));
    private static final AttackDefinition GROWTH = new AttackDefinition(
            "GROWTH", 20, AttackDefinition.Kind.SELF_BUFF, 0,
            0.12, 0.06, 5.90, 0, 0, 0, 0, 0, 0, new Color(0x8CDB68));
    private static final AttackDefinition GROWL = new AttackDefinition(
            "GROWL", 21, AttackDefinition.Kind.GRUNT, 0,
            0.12, GRUNT_LOCK_SECONDS, 3.60, 0, 0, 0, 0, 0, 0,
            new Color(0xC6D47A));
    private static final AttackDefinition FLY = new AttackDefinition(
            "FLY", 32, AttackDefinition.Kind.FLY, 12,
            0.12, 0.78, 0.75, 84, 90, 0, 0, 0, 0, new Color(0xD8F1FF));
    private static final AttackDefinition TAIL_WHIP = new AttackDefinition(
            "TAIL_WHIP", 23, AttackDefinition.Kind.LUNGE, 8,
            0.12, 0.22, 0.68, 112, 82, 150, 0, 0, 0, new Color(0xD6D6C2));
    private static final AttackDefinition BITE = new AttackDefinition(
            "BITE", 28, AttackDefinition.Kind.HITBOX, 10,
            0.12, 0.10, 0.68, 62, 66, 0, 0, 0, 0, new Color(0xA5A5A5));

    // Original prototype profiles remain available in the fallback session.
    private static final AttackDefinition CHARIZARD_TACKLE = new AttackDefinition(
            "TACKLE", 18, AttackDefinition.Kind.DASH, 12,
            0.08, 0.16, 0.40, 64, 82, 360, 0, 0, 0, new Color(0xF2C28B));

    private enum Phase { READY, STARTUP, ACTIVE, RECOVERY }

    private static final class AttackState {
        private Phase phase = Phase.READY;
        private AttackDefinition definition;
        private double remaining;
        private double activeElapsed;
        private boolean hasHit;
        private boolean activeEffectStarted;
        private double flyTargetY;
        private double flyLandingX;
        private double flyDirection;
        private double flyVelocityX;
        private boolean flyLaunched;
        private boolean flyRetreatStarted;
        private double flyRetreatRemaining;
    }

    public static final class ImpactEffect {
        private final double x;
        private final double y;
        private double remainingSeconds;

        private ImpactEffect(double x, double y, double duration) {
            this.x = x;
            this.y = y;
            this.remainingSeconds = duration;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getRemainingSeconds() { return remainingSeconds; }

        private void update(double deltaSeconds) { remainingSeconds -= deltaSeconds; }
    }

    private final AttackState[] states = {new AttackState(), new AttackState()};
    private final List<FightingProjectile> projectiles = new ArrayList<>();
    private final List<FightingProjectile> projectileView = Collections.unmodifiableList(projectiles);
    private final List<ImpactEffect> impactEffects = new ArrayList<>();
    private final List<ImpactEffect> impactEffectView = Collections.unmodifiableList(impactEffects);

    public boolean hasAttackProfile(String pokemonId) {
        return switch (pokemonId) {
            case "PIKACHU", "CHARIZARD", "BULBASAUR", "CHARMANDER", "SQUIRTLE", "EEVEE" -> true;
            default -> false;
        };
    }

    public void clearPlayerState(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= states.length) return;
        resetAttackState(states[playerIndex]);
        AttackState other = states[1 - playerIndex];
        if (other.definition != null && other.definition.kind() == AttackDefinition.Kind.GRUNT) {
            resetAttackState(other);
        }
        projectiles.clear();
        impactEffects.clear();
    }

    public void update(List<Fighter> fighters, int arenaWidth,
                       boolean playerOneAttackOne, boolean playerOneAttackTwo,
                       boolean playerOneAttackThree, boolean playerOneAttackFour,
                       boolean playerTwoAttackOne, boolean playerTwoAttackTwo,
                       boolean playerTwoAttackThree, boolean playerTwoAttackFour,
                       double deltaSeconds) {
        update(fighters, arenaWidth, List.of(), playerOneAttackOne, playerOneAttackTwo,
                playerOneAttackThree, playerOneAttackFour, playerTwoAttackOne,
                playerTwoAttackTwo, playerTwoAttackThree, playerTwoAttackFour, deltaSeconds);
    }

    public void update(List<Fighter> fighters, int arenaWidth, List<Platform> platforms,
                       boolean playerOneAttackOne, boolean playerOneAttackTwo,
                       boolean playerOneAttackThree, boolean playerOneAttackFour,
                       boolean playerTwoAttackOne, boolean playerTwoAttackTwo,
                       boolean playerTwoAttackThree, boolean playerTwoAttackFour,
                       double deltaSeconds) {
        int playerOneRequest = requestedAttackIndex(playerOneAttackOne, playerOneAttackTwo,
                playerOneAttackThree, playerOneAttackFour);
        int playerTwoRequest = requestedAttackIndex(playerTwoAttackOne, playerTwoAttackTwo,
                playerTwoAttackThree, playerTwoAttackFour);
        startRequestedAttack(states[0], attackFor(fighters.get(0).getPokemonId(), playerOneRequest),
                fighters.get(0), fighters.get(1));
        startRequestedAttack(states[1], attackFor(fighters.get(1).getPokemonId(), playerTwoRequest),
                fighters.get(1), fighters.get(0));

        for (int i = 0; i < states.length; i++) {
            Fighter attacker = fighters.get(i);
            if (attacker.isDefeated() || attacker.isAsleep() || controlledByOtherGrowl(i)) {
                resetAttackState(states[i]);
                continue;
            }
            updateAttackState(i, fighters, arenaWidth, deltaSeconds);
        }
        updateProjectiles(fighters, arenaWidth, platforms, deltaSeconds);
        updateImpactEffects(deltaSeconds);
    }

    private int requestedAttackIndex(boolean attackOne, boolean attackTwo,
                                     boolean attackThree, boolean attackFour) {
        if (attackOne) return 0;
        if (attackTwo) return 1;
        if (attackThree) return 2;
        if (attackFour) return 3;
        return -1;
    }

    private AttackDefinition attackFor(String pokemonId, int requestedAttack) {
        if (requestedAttack < 0) return null;
        if (!isSelectableFreeFightPokemon(pokemonId)) {
            if ("PIKACHU".equals(pokemonId)) {
                if (requestedAttack == 0) return QUICK_ATTACK;
                if (requestedAttack == 1) return THUNDER_SHOCK;
            } else if ("CHARIZARD".equals(pokemonId)) {
                if (requestedAttack == 0) return EMBER;
                if (requestedAttack == 1) return CHARIZARD_TACKLE;
            }
            return null;
        }

        return switch (requestedAttack) {
            case 0 -> TACKLE;
            case 1 -> switch (pokemonId) {
                case "BULBASAUR" -> VINE_WHIP;
                case "CHARMANDER" -> EMBER;
                case "SQUIRTLE" -> WATER_GUN;
                case "EEVEE" -> QUICK_ATTACK;
                default -> null;
            };
            case 2 -> switch (pokemonId) {
                case "BULBASAUR" -> SLEEP_POWDER;
                case "CHARMANDER", "EEVEE" -> GROWL;
                case "SQUIRTLE" -> TAIL_WHIP;
                default -> null;
            };
            case 3 -> switch (pokemonId) {
                case "BULBASAUR" -> GROWTH;
                case "CHARMANDER" -> FLY;
                case "SQUIRTLE", "EEVEE" -> TAIL_WHIP;
                default -> null;
            };
            default -> null;
        };
    }

    private boolean isSelectableFreeFightPokemon(String pokemonId) {
        return "BULBASAUR".equals(pokemonId) || "CHARMANDER".equals(pokemonId)
                || "SQUIRTLE".equals(pokemonId) || "EEVEE".equals(pokemonId);
    }

    private void startRequestedAttack(AttackState state, AttackDefinition requested,
                                      Fighter attacker, Fighter target) {
        if (requested == null || state.phase != Phase.READY
                || attacker.isDefeated() || attacker.isAsleep()) return;
        if (requested.kind() == AttackDefinition.Kind.FLY
                && !prepareFlight(state, requested, attacker, target)) return;
        state.definition = requested;
        state.phase = Phase.STARTUP;
        state.remaining = requested.startupSeconds();
        state.activeElapsed = 0;
        state.hasHit = false;
        state.activeEffectStarted = false;
    }

    private boolean prepareFlight(AttackState state, AttackDefinition definition,
                                  Fighter attacker, Fighter target) {
        double deltaX = target.getX() - attacker.getX();
        double distance = Math.abs(deltaX);
        double direction = attacker.isFacingLeft() ? -1 : 1;
        if (distance < FLY_MIN_RANGE || distance > FLY_MAX_RANGE
                || deltaX * direction <= 0) return false;
        state.flyDirection = direction;
        double bodyGap = PhysicsSystem.halfWidthFor(attacker)
                + PhysicsSystem.halfWidthFor(target) + 18;
        state.flyLandingX = target.getX() - direction * bodyGap;
        double halfWidth = PhysicsSystem.halfWidthFor(attacker);
        state.flyLandingX = Math.max(halfWidth, Math.min(1280 - halfWidth, state.flyLandingX));
        state.flyTargetY = target.getY();
        state.flyLaunched = false;
        state.flyRetreatStarted = false;
        state.flyRetreatRemaining = 0;
        return true;
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
            if (attacker.isDefeated()) {
                resetAttackState(state);
                return;
            }
            if (!state.activeEffectStarted) {
                state.activeEffectStarted = true;
                switch (state.definition.kind()) {
                    case PROJECTILE -> spawnProjectile(attacker, attackerIndex, state.definition);
                    case SELF_BUFF -> attacker.applyAttackBoost(GROWTH_DURATION);
                    case GRUNT -> target.applyAttackReduction(ATTACK_REDUCTION_DURATION);
                    case FLY -> launchFlight(state, attacker);
                    default -> { }
                }
            }
            state.activeElapsed += deltaSeconds;

            if (state.definition.kind() == AttackDefinition.Kind.DASH
                    || (state.definition.kind() == AttackDefinition.Kind.LUNGE
                        && state.activeElapsed <= TAIL_LUNGE_SECONDS)) {
                moveTowardTarget(attacker, target, state.definition, arenaWidth, deltaSeconds);
            }

            boolean strikeReady = state.definition.kind() != AttackDefinition.Kind.FLY
                    || state.activeElapsed >= 0.12;
            if (strikeReady && !state.hasHit) {
                if (state.definition.kind() == AttackDefinition.Kind.STATUS) {
                    if (attackHitbox(attacker, state.definition).intersects(target)) {
                        applyStatusMove(state.definition.moveId(), target);
                        state.hasHit = true;
                    }
                } else if (state.definition.kind() == AttackDefinition.Kind.LUNGE
                        || state.definition.kind() == AttackDefinition.Kind.FLY
                        || state.definition.kind() == AttackDefinition.Kind.HITBOX
                        || state.definition.kind() == AttackDefinition.Kind.DASH) {
                    if (attackHitbox(attacker, state.definition).intersects(target)) {
                        applyHit(attacker, target, state.definition);
                        state.hasHit = true;
                    }
                }
            }

            if (state.remaining <= 0) {
                state.phase = Phase.RECOVERY;
                state.remaining += state.definition.recoverySeconds();
            }
        } else if (state.phase == Phase.RECOVERY) {
            updateRecovery(state, fighters.get(attackerIndex), deltaSeconds);
        }
    }

    private void launchFlight(AttackState state, Fighter attacker) {
        state.flyVelocityX = (state.flyLandingX - attacker.getX()) / FLY_TRAVEL_SECONDS;
        attacker.setVelocityY(PhysicsSystem.launchVelocityForDisplacement(
                attacker.getY(), state.flyTargetY, FLY_TRAVEL_SECONDS));
        attacker.setOnGround(false);
        state.flyLaunched = true;
    }

    private void updateRecovery(AttackState state, Fighter attacker, double deltaSeconds) {
        if (state.definition.kind() == AttackDefinition.Kind.FLY && state.flyLaunched) {
            if (!state.flyRetreatStarted && attacker.isOnGround()) {
                state.flyRetreatStarted = true;
                state.flyRetreatRemaining = FLY_RETREAT_SECONDS;
            } else if (state.flyRetreatRemaining > 0) {
                state.flyRetreatRemaining = Math.max(0, state.flyRetreatRemaining - deltaSeconds);
            }
        }
        if (state.remaining <= 0 && state.flyRetreatRemaining <= 0
                && (state.definition.kind() != AttackDefinition.Kind.FLY
                    || !state.flyLaunched || attacker.isOnGround())) {
            resetAttackState(state);
        }
    }

    private boolean controlledByOtherGrowl(int playerIndex) {
        int opponentIndex = 1 - playerIndex;
        AttackState opponent = states[opponentIndex];
        return opponent.definition != null
                && opponent.definition.kind() == AttackDefinition.Kind.GRUNT
                && (opponent.phase == Phase.STARTUP || opponent.phase == Phase.ACTIVE);
    }

    public boolean isPlayerInputLocked(int playerIndex, List<Fighter> fighters) {
        if (playerIndex < 0 || playerIndex >= states.length) return false;
        Fighter fighter = fighters.get(playerIndex);
        if (fighter.isAsleep()) return true;
        for (AttackState state : states) {
            if (state.definition != null && state.definition.kind() == AttackDefinition.Kind.GRUNT
                    && (state.phase == Phase.STARTUP || state.phase == Phase.ACTIVE)) return true;
        }
        AttackState own = states[playerIndex];
        if (own.definition != null && own.definition.kind() == AttackDefinition.Kind.FLY) {
            return own.phase == Phase.STARTUP || own.phase == Phase.ACTIVE
                    || (own.phase == Phase.RECOVERY
                        && (own.flyRetreatRemaining > 0 || !fighter.isOnGround()));
        }
        return false;
    }

    /** Horizontal displacement imposed by a locked Fly arc or its landing recoil. */
    public double getForcedHorizontalDelta(Fighter fighter, List<Fighter> fighters,
                                           double deltaSeconds, int arenaWidth) {
        int index = fighters.indexOf(fighter);
        if (index < 0) return 0;
        AttackState state = states[index];
        double delta = 0;
        if (state.definition != null && state.definition.kind() == AttackDefinition.Kind.FLY
                && state.phase == Phase.ACTIVE && state.flyLaunched
                && state.activeElapsed < FLY_TRAVEL_SECONDS) {
            double remaining = state.flyLandingX - fighter.getX();
            double proposed = state.flyVelocityX * deltaSeconds;
            if (Math.signum(proposed) == Math.signum(remaining)) {
                delta = Math.copySign(Math.min(Math.abs(proposed), Math.abs(remaining)), remaining);
            }
        } else if (state.definition != null && state.definition.kind() == AttackDefinition.Kind.FLY
                && state.phase == Phase.RECOVERY && state.flyRetreatRemaining > 0) {
            delta = -state.flyDirection * FLY_RETREAT_SPEED * deltaSeconds;
        }
        double halfWidth = PhysicsSystem.halfWidthFor(fighter);
        double nextX = Math.max(halfWidth, Math.min(arenaWidth - halfWidth, fighter.getX() + delta));
        return nextX - fighter.getX();
    }

    private void moveTowardTarget(Fighter attacker, Fighter target,
                                  AttackDefinition definition, int arenaWidth,
                                  double deltaSeconds) {
        double direction = attacker.isFacingLeft() ? -1 : 1;
        double halfWidth = PhysicsSystem.halfWidthFor(attacker);
        double targetHalfWidth = PhysicsSystem.halfWidthFor(target);
        double distanceToTarget = direction > 0
                ? target.getX() - targetHalfWidth - (attacker.getX() + halfWidth)
                : attacker.getX() - halfWidth - (target.getX() + targetHalfWidth);
        double movement = definition.dashSpeed() * deltaSeconds;
        if (distanceToTarget > 0) movement = Math.min(movement, distanceToTarget);
        else movement = 0;
        attacker.setX(Math.max(halfWidth, Math.min(arenaWidth - halfWidth,
                attacker.getX() + direction * movement)));
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
        projectiles.add(new FightingProjectile(definition.moveId(), originX, originY,
                direction * definition.projectileSpeed(), definition.projectileSize(),
                definition.damage(), definition.projectileLifetime(), definition.effectColor(), ownerIndex));
    }

    private void applyStatusMove(String moveId, Fighter target) {
        if ("TAIL_WHIP".equals(moveId)) target.applyDefenseReduction(DEFENSE_REDUCTION_DURATION);
    }

    private void applyHit(Fighter attacker, Fighter target, AttackDefinition definition) {
        if (attacker.isDefeated() || target.isDefeated()) return;
        int damage = Math.max(1, (int) Math.round(definition.damage()
                * attacker.getOutgoingDamageMultiplier()
                * target.getIncomingDamageMultiplier()));
        target.receiveDamage(damage);
        attacker.addEvolutionEnergy(EVOLUTION_ENERGY_PER_HIT);
        if ("TAIL_WHIP".equals(definition.moveId())) {
            target.applyDefenseReduction(DEFENSE_REDUCTION_DURATION);
        }
    }

    private void updateProjectiles(List<Fighter> fighters, int arenaWidth,
                                   List<Platform> platforms, double deltaSeconds) {
        Iterator<FightingProjectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            FightingProjectile projectile = iterator.next();
            projectile.update(deltaSeconds);
            Fighter target = fighters.get(1 - projectile.getOwnerIndex());
            if (target.isDefeated()) {
                iterator.remove();
            } else if ("SLEEP_POWDER".equals(projectile.getMoveId())
                    && isBlockedByPlatform(projectile.hitbox(), platforms)) {
                impactEffects.add(new ImpactEffect(projectile.getX(), projectile.getY(), 0.22));
                iterator.remove();
            } else if (projectile.hitbox().intersects(target)) {
                Fighter attacker = fighters.get(projectile.getOwnerIndex());
                if ("SLEEP_POWDER".equals(projectile.getMoveId())) {
                    target.applySleep(SLEEP_DURATION);
                    impactEffects.add(new ImpactEffect(projectile.getX(), projectile.getY(), 0.22));
                } else if (!attacker.isDefeated() && projectile.getDamage() > 0) {
                    int damage = Math.max(1, (int) Math.round(projectile.getDamage()
                            * attacker.getOutgoingDamageMultiplier()
                            * target.getIncomingDamageMultiplier()));
                    target.receiveDamage(damage);
                    attacker.addEvolutionEnergy(EVOLUTION_ENERGY_PER_HIT);
                }
                iterator.remove();
            } else if (projectile.isExpired(arenaWidth)) {
                iterator.remove();
            }
        }
    }

    private boolean isBlockedByPlatform(Hitbox projectile, List<Platform> platforms) {
        for (Platform platform : platforms) {
            if (platform.blocksProjectile(projectile)) return true;
        }
        return false;
    }

    private void updateImpactEffects(double deltaSeconds) {
        Iterator<ImpactEffect> iterator = impactEffects.iterator();
        while (iterator.hasNext()) {
            ImpactEffect effect = iterator.next();
            effect.update(deltaSeconds);
            if (effect.getRemainingSeconds() <= 0) iterator.remove();
        }
    }

    private void resetAttackState(AttackState state) {
        state.phase = Phase.READY;
        state.definition = null;
        state.remaining = 0;
        state.activeElapsed = 0;
        state.hasHit = false;
        state.activeEffectStarted = false;
        state.flyTargetY = 0;
        state.flyLandingX = 0;
        state.flyDirection = 0;
        state.flyVelocityX = 0;
        state.flyLaunched = false;
        state.flyRetreatStarted = false;
        state.flyRetreatRemaining = 0;
    }

    public List<FightingProjectile> getProjectiles() { return projectileView; }
    public List<ImpactEffect> getImpactEffects() { return impactEffectView; }

    public boolean isDashing(Fighter fighter, List<Fighter> fighters) {
        int index = fighters.indexOf(fighter);
        if (index < 0) return false;
        AttackState state = states[index];
        return state.phase == Phase.ACTIVE && state.definition != null
                && (state.definition.kind() == AttackDefinition.Kind.DASH
                    || state.definition.kind() == AttackDefinition.Kind.FLY);
    }

    public AttackDefinition getActiveDefinition(Fighter fighter, List<Fighter> fighters) {
        int index = fighters.indexOf(fighter);
        if (index < 0) return null;
        AttackState state = states[index];
        return state.phase == Phase.ACTIVE ? state.definition : null;
    }

    public double getActiveProgress(Fighter fighter, List<Fighter> fighters) {
        int index = fighters.indexOf(fighter);
        if (index < 0) return 0;
        AttackState state = states[index];
        if (state.phase != Phase.ACTIVE || state.definition == null) return 0;
        return Math.min(1, state.activeElapsed / state.definition.activeSeconds());
    }
}

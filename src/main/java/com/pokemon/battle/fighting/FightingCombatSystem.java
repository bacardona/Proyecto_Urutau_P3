package com.pokemon.battle.fighting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    private static final double EEVEE_QUICK_ATTACK_MAX_DISTANCE = 120;
    private static final double INPUT_BUFFER_SECONDS = 0.18;
    private static final double BLOCK_STUN_SECONDS = 0.35;
    private static final double COMBO_STUN_RESET_SECONDS = 0.90;
    private static final double COMBO_CANCEL_WINDOW_SECONDS = 0.18;
    private static final double HIT_RECOVERY_MULTIPLIER = 0.65;
    private static final double[] COMBO_HIT_STUN_SECONDS = {0.20, 0.15, 0.10};

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
            "SLEEP_POWDER", 31, AttackDefinition.Kind.PROJECTILE, 3,
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
            0.08, 0.08, 0.44, 30, 42, 0, 0, 0, 0, new Color(0xA5A5A5));

    // Original prototype profiles remain available in the fallback session.
    // Pikachu / Raichu: Onda Trueno baja el ataque del rival como Grunido, con su propio efecto
    private static final AttackDefinition THUNDER_WAVE = new AttackDefinition(
            "THUNDER_WAVE", 83, AttackDefinition.Kind.GRUNT, 0,
            0.12, GRUNT_LOCK_SECONDS, 3.60, 0, 0, 0, 0, 0, 0,
            new Color(0xFFE45A));

    // Ataques de los Pokemon nuevos (los que usan mecanicas propias reutilizan Somnifero, Desarrollo y Grunido)
    private static final AttackDefinition FIRE_FANG = new AttackDefinition(
            "FIRE_FANG", 80, AttackDefinition.Kind.HITBOX, 11,
            0.10, 0.10, 0.56, 50, 54, 0, 0, 0, 0, new Color(0xFF8A3D));
    private static final AttackDefinition DIG = new AttackDefinition(
            "DIG", 33, AttackDefinition.Kind.DASH, 12,
            0.14, 0.24, 0.70, 60, 70, 400, 0, 0, 0, new Color(0xB48A52));
    private static final AttackDefinition ROCK_SLIDE = new AttackDefinition(
            "ROCK_SLIDE", 112, AttackDefinition.Kind.PROJECTILE, 10,
            0.16, 0.06, 0.66, 0, 0, 0, 420, 1.8, 22, new Color(0x9C7B55));
    private static final AttackDefinition LICK = new AttackDefinition(
            "LICK", 65, AttackDefinition.Kind.HITBOX, 9,
            0.08, 0.10, 0.48, 48, 50, 0, 0, 0, 0, new Color(0xB57BE0));
    private static final AttackDefinition SHADOW_BALL = new AttackDefinition(
            "SHADOW_BALL", 59, AttackDefinition.Kind.PROJECTILE, 11,
            0.16, 0.06, 0.64, 0, 0, 0, 430, 1.8, 24, new Color(0x7B4FC4));
    private static final AttackDefinition CLOSE_COMBAT = new AttackDefinition(
            "CLOSE_COMBAT", 105, AttackDefinition.Kind.HITBOX, 13,
            0.12, 0.12, 0.70, 52, 58, 0, 0, 0, 0, new Color(0xE8664A));
    private static final AttackDefinition BODY_SLAM = new AttackDefinition(
            "BODY_SLAM", 69, AttackDefinition.Kind.DASH, 11,
            0.10, 0.20, 0.62, 58, 74, 360, 0, 0, 0, new Color(0xF2C28B));
    private static final AttackDefinition ZEN_HEADBUTT = new AttackDefinition(
            "ZEN_HEADBUTT", 63, AttackDefinition.Kind.HITBOX, 10,
            0.10, 0.10, 0.54, 50, 56, 0, 0, 0, 0, new Color(0xF27FC2));
    private static final AttackDefinition PSYCHIC = new AttackDefinition(
            "PSYCHIC", 60, AttackDefinition.Kind.PROJECTILE, 11,
            0.16, 0.06, 0.62, 0, 0, 0, 470, 1.7, 24, new Color(0xF27FC2));
    private static final AttackDefinition TELEPORT = new AttackDefinition(
            "TELEPORT", 109, AttackDefinition.Kind.DASH, 8,
            0.06, 0.18, 0.60, 56, 70, 520, 0, 0, 0, new Color(0xE6B8FF));

    // Jefe secreto Mewtwo: mismas animaciones de los ataques psiquicos, pero mas fuertes
    private static final AttackDefinition PSYCHO_CUT = new AttackDefinition(
            "PSYCHO_CUT", 63, AttackDefinition.Kind.HITBOX, 13,
            0.09, 0.12, 0.46, 64, 70, 0, 0, 0, 0, new Color(0xB78CFF));
    private static final AttackDefinition BOSS_PSYCHIC = new AttackDefinition(
            "PSYCHIC", 60, AttackDefinition.Kind.PROJECTILE, 12,
            0.18, 0.06, 0.55, 0, 0, 0, 540, 1.8, 28, new Color(0xB78CFF));
    private static final AttackDefinition BOSS_SHADOW_BALL = new AttackDefinition(
            "SHADOW_BALL", 59, AttackDefinition.Kind.PROJECTILE, 14,
            0.24, 0.06, 0.70, 0, 0, 0, 420, 2.0, 34, new Color(0x7B4FC4));
    private static final AttackDefinition BOSS_TELEPORT = new AttackDefinition(
            "TELEPORT", 109, AttackDefinition.Kind.DASH, 11,
            0.06, 0.18, 0.55, 64, 80, 620, 0, 0, 0, new Color(0xE6B8FF));

    // Onda mental de Mega Mewtwo: recorre el suelo, hay que saltarla
    private static final AttackDefinition MIND_WAVE = new AttackDefinition(
            "MIND_WAVE", 60, AttackDefinition.Kind.HITBOX, 14,
            0, 0.1, 0, 60, 60, 0, 0, 0, 0, new Color(0xFF5FB8));

    // Premonicion: el golpe que cae donde marco Mewtwo
    private static final AttackDefinition FUTURE_SIGHT = new AttackDefinition(
            "FUTURE_SIGHT", 60, AttackDefinition.Kind.HITBOX, 15,
            0, 0.1, 0, 60, 300, 0, 0, 0, 0, new Color(0xFF6FD0));

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
        private boolean hitConfirmed;
        private double cancelWindowRemaining;
        private double flyTargetY;
        private double flyLandingX;
        private double flyDirection;
        private double flyVelocityX;
        private boolean flyLaunched;
        private boolean flyRetreatStarted;
        private double flyRetreatRemaining;
        private double dashDirection;
        private double dashDistanceTravelled;
    }

    public record ComboInfo(int hits, double remainingSeconds) { }
    public record AttackVisual(AttackDefinition definition, String phase,
                               double progress, double activeProgress,
                               boolean hitConfirmed, double direction) { }

    private static final class ComboState {
        private int hits;
        private double remainingSeconds;
        private Fighter target;
    }

    public static final class ImpactEffect {
        private final double x;
        private final double y;
        private final double durationSeconds;
        private final String moveId;
        private final Color color;
        private final double size;
        private final boolean blocked;
        private final Fighter target;
        private double remainingSeconds;

        private ImpactEffect(double x, double y, double duration) {
            this(x, y, duration, "PROJECTILE", new Color(0xD39FF5), 20, false, null);
        }

        private ImpactEffect(double x, double y, double duration, String moveId,
                             Color color, double size, boolean blocked, Fighter target) {
            this.x = x;
            this.y = y;
            this.durationSeconds = duration;
            this.remainingSeconds = duration;
            this.moveId = moveId;
            this.color = color;
            this.size = size;
            this.blocked = blocked;
            this.target = target;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getRemainingSeconds() { return remainingSeconds; }
        public double getProgress() { return Math.max(0, Math.min(1, 1 - remainingSeconds / durationSeconds)); }
        public String getMoveId() { return moveId; }
        public Color getColor() { return color; }
        public double getSize() { return size; }
        public boolean isBlocked() { return blocked; }
        private Fighter getTarget() { return target; }

        private void update(double deltaSeconds) { remainingSeconds -= deltaSeconds; }
    }

    private final AttackState[] states = {new AttackState(), new AttackState()};
    private final int[] bufferedAttack = {-1, -1};
    private final double[] bufferedAttackRemaining = new double[2];
    private final List<Map<String, Double>> moveCooldowns = List.of(new HashMap<>(), new HashMap<>());
    private final ComboState[] combos = {new ComboState(), new ComboState()};
    private final UltimateController ultimateController = new UltimateController();
    private final List<FightingProjectile> projectiles = new ArrayList<>();
    private final List<FightingProjectile> projectileView = Collections.unmodifiableList(projectiles);
    private final List<ImpactEffect> impactEffects = new ArrayList<>();
    private final List<ImpactEffect> impactEffectView = Collections.unmodifiableList(impactEffects);
    // Lo que paso en este paso de la simulacion; FightingFx lo usa para efectos y sonidos
    private final List<CombatEvent> events = new ArrayList<>();

    public boolean hasAttackProfile(String pokemonId) {
        return switch (pokemonId) {
            case "PIKACHU", "CHARIZARD", "BULBASAUR", "CHARMANDER", "SQUIRTLE", "EEVEE",
                 "VULPIX", "SANDSHREW", "GASTLY", "MACHOP", "ABRA", "MEWTWO" -> true;
            default -> false;
        };
    }

    public void clearPlayerState(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= states.length) return;
        resetAttackState(states[playerIndex]);
        for (int i = 0; i < 2; i++) {
            bufferedAttack[i] = -1;
            bufferedAttackRemaining[i] = 0;
            combos[i].hits = 0;
            combos[i].remainingSeconds = 0;
            combos[i].target = null;
        }
        moveCooldowns.get(playerIndex).clear();
        AttackState other = states[1 - playerIndex];
        if (other.definition != null && other.definition.kind() == AttackDefinition.Kind.GRUNT) {
            resetAttackState(other);
        }
        projectiles.clear();
        impactEffects.clear();
        ultimateController.clearPlayerState(playerIndex);
    }

    public void clearAllState() {
        resetAttackState(states[0]);
        resetAttackState(states[1]);
        projectiles.clear();
        impactEffects.clear();
        ultimateController.clearAll();
        for (int i = 0; i < 2; i++) {
            bufferedAttack[i] = -1;
            bufferedAttackRemaining[i] = 0;
            moveCooldowns.get(i).clear();
            combos[i].hits = 0;
            combos[i].remainingSeconds = 0;
            combos[i].target = null;
        }
    }

    /** Quita todos los proyectiles (antes de la escena de Mega Mewtwo). */
    void clearProjectiles() {
        projectiles.clear();
    }

    public void clearPendingInputs() {
        for (int i = 0; i < 2; i++) {
            bufferedAttack[i] = -1;
            bufferedAttackRemaining[i] = 0;
        }
    }

    public void beginUltimateRequests(List<Fighter> fighters,
                                     boolean playerOneRequested, boolean playerTwoRequested) {
        boolean p1Ready = states[0].phase == Phase.READY && !isPlayerInputLocked(0, fighters);
        boolean p2Ready = states[1].phase == Phase.READY && !isPlayerInputLocked(1, fighters);
        ultimateController.begin(0, fighters, playerOneRequested, p1Ready);
        ultimateController.begin(1, fighters, playerTwoRequested, p2Ready);
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
        update(fighters, arenaWidth, platforms, playerOneRequest, playerTwoRequest, deltaSeconds);
    }

    public void update(List<Fighter> fighters, int arenaWidth, List<Platform> platforms,
                       int playerOneRequest, int playerTwoRequest, double deltaSeconds) {
        updateCooldownsAndCombos(deltaSeconds);
        captureInput(0, playerOneRequest, fighters, deltaSeconds);
        captureInput(1, playerTwoRequest, fighters, deltaSeconds);

        for (int i = 0; i < states.length; i++) {
            Fighter attacker = fighters.get(i);
            if (attacker.isDefeated() || attacker.isAsleep() || attacker.isBlocking()
                    || attacker.isHitStunned() || controlledByOtherGrowl(i)
                    || ultimateController.isPlayerInputLocked(i, fighters)) {
                resetAttackState(states[i]);
                continue;
            }
            tryStartBufferedAttack(i, fighters, arenaWidth);
            var firstBefore = PhysicsSystem.positionOf(fighters.get(0));
            var secondBefore = PhysicsSystem.positionOf(fighters.get(1));
            updateAttackState(i, fighters, arenaWidth, deltaSeconds);
            PhysicsSystem.resolveBodies(fighters.get(0), fighters.get(1), firstBefore, secondBefore, arenaWidth);
        }
        updateProjectiles(fighters, arenaWidth, platforms, deltaSeconds);
        var firstBeforeUltimate = PhysicsSystem.positionOf(fighters.get(0));
        var secondBeforeUltimate = PhysicsSystem.positionOf(fighters.get(1));
        ultimateController.update(fighters, arenaWidth, 720, deltaSeconds);
        PhysicsSystem.resolveBodies(fighters.get(0), fighters.get(1), firstBeforeUltimate,
                secondBeforeUltimate, arenaWidth);
        updateImpactEffects(deltaSeconds);
    }

    private void updateCooldownsAndCombos(double dt) {
        for (Map<String, Double> cooldowns : moveCooldowns) {
            cooldowns.replaceAll((move, seconds) -> Math.max(0, seconds - dt));
        }
        for (ComboState combo : combos) {
            combo.remainingSeconds = Math.max(0, combo.remainingSeconds - dt);
            if (combo.remainingSeconds == 0) { combo.hits = 0; combo.target = null; }
        }
    }

    private void captureInput(int player, int request, List<Fighter> fighters, double dt) {
        Fighter fighter = fighters.get(player);
        if (fighter.isDefeated() || fighter.isAsleep() || fighter.isBlocking()
                || fighter.isHitStunned() || ultimateController.isPlayerInputLocked(player, fighters)
                || controlledByOtherGrowl(player)) {
            bufferedAttack[player] = -1;
            bufferedAttackRemaining[player] = 0;
            return;
        }
        if (request >= 0 && request < 4
                && attackFor(fighter.getBasePokemonId(), request) != null) {
            bufferedAttack[player] = request;
            bufferedAttackRemaining[player] = INPUT_BUFFER_SECONDS;
        } else if (bufferedAttack[player] >= 0) {
            bufferedAttackRemaining[player] -= dt;
            if (bufferedAttackRemaining[player] <= 0) {
                bufferedAttack[player] = -1;
                bufferedAttackRemaining[player] = 0;
            }
        }
    }

    private void tryStartBufferedAttack(int player, List<Fighter> fighters, int arenaWidth) {
        int slot = bufferedAttack[player];
        if (slot < 0) return;
        Fighter attacker = fighters.get(player);
        AttackState state = states[player];
        AttackDefinition requested = attackFor(attacker.getBasePokemonId(), slot);
        if (requested == null) { bufferedAttack[player] = -1; return; }
        double cooldown = moveCooldowns.get(player).getOrDefault(requested.moveId(), 0.0);
        boolean ready = state.phase == Phase.READY;
        boolean cancel = state.definition != null && state.hitConfirmed
                && state.cancelWindowRemaining > 0 && state.definition != requested
                && (state.phase == Phase.ACTIVE || state.phase == Phase.RECOVERY);
        if (cooldown > 0 || (!ready && !cancel)) return;
        if (cancel) resetAttackState(state);
        if (startRequestedAttack(state, requested, attacker, fighters.get(1 - player))) {
            emit(CombatEvent.Type.ATTACK_START, requested.moveId(), player,
                    attacker.getX(), attacker.getY(), state.dashDirection, 0);
            moveCooldowns.get(player).put(requested.moveId(), requested.cooldownSeconds());
            bufferedAttack[player] = -1;
            bufferedAttackRemaining[player] = 0;
        }
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
        AttackDefinition[] newMoves = switch (pokemonId) {
            case "VULPIX" -> new AttackDefinition[]{QUICK_ATTACK, EMBER, GROWL, FIRE_FANG};
            case "SANDSHREW" -> new AttackDefinition[]{TACKLE, DIG, TAIL_WHIP, ROCK_SLIDE};
            case "GASTLY" -> new AttackDefinition[]{LICK, SHADOW_BALL, SLEEP_POWDER, GROWL};
            case "MACHOP" -> new AttackDefinition[]{TACKLE, CLOSE_COMBAT, GROWL, BODY_SLAM};
            case "ABRA" -> new AttackDefinition[]{ZEN_HEADBUTT, PSYCHIC, GROWTH, TELEPORT};
            case "MEWTWO" -> new AttackDefinition[]{PSYCHO_CUT, BOSS_PSYCHIC, BOSS_SHADOW_BALL, BOSS_TELEPORT};
            default -> null;
        };
        if (newMoves != null) return requestedAttack < newMoves.length ? newMoves[requestedAttack] : null;
        if ("PIKACHU".equals(pokemonId)) {
            return switch (requestedAttack) {
                case 0 -> QUICK_ATTACK;
                case 1 -> THUNDER_SHOCK;
                case 2 -> TAIL_WHIP;
                case 3 -> THUNDER_WAVE;
                default -> null;
            };
        }
        if (!isSelectableFreeFightPokemon(pokemonId)) {
            if ("PIKACHU_PROTOTYPE".equals(pokemonId)) {
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
                case "SQUIRTLE" -> BITE;
                case "EEVEE" -> TAIL_WHIP;
                default -> null;
            };
            default -> null;
        };
    }

    /** Ataque Rapido: siempre avanza su distancia fija (Eevee, Pikachu y Vulpix). */
    private static boolean usesQuickAttackDash(String basePokemonId) {
        return "EEVEE".equals(basePokemonId) || "PIKACHU".equals(basePokemonId)
                || "VULPIX".equals(basePokemonId);
    }

    private boolean isSelectableFreeFightPokemon(String pokemonId) {
        return "BULBASAUR".equals(pokemonId) || "CHARMANDER".equals(pokemonId)
                || "SQUIRTLE".equals(pokemonId) || "EEVEE".equals(pokemonId)
                || "PIKACHU".equals(pokemonId) || "VULPIX".equals(pokemonId)
                || "SANDSHREW".equals(pokemonId) || "GASTLY".equals(pokemonId)
                || "MACHOP".equals(pokemonId) || "ABRA".equals(pokemonId);
    }

    private boolean startRequestedAttack(AttackState state, AttackDefinition requested,
                                      Fighter attacker, Fighter target) {
        if (requested == null || state.phase != Phase.READY
                || attacker.isDefeated() || attacker.isAsleep() || attacker.isBlocking()
                || attacker.isHitStunned() || ultimateController.isFighterInputLocked(attacker)) return false;
        if (requested.kind() == AttackDefinition.Kind.FLY
                && !prepareFlight(state, requested, attacker, target)) return false;
        state.definition = requested;
        state.phase = Phase.STARTUP;
        state.remaining = requested.startupSeconds();
        state.activeElapsed = 0;
        state.hasHit = false;
        state.activeEffectStarted = false;
        state.hitConfirmed = false;
        state.cancelWindowRemaining = 0;
        state.dashDirection = attacker.isFacingLeft() ? -1 : 1;
        state.dashDistanceTravelled = 0;
        return true;
    }

    private boolean prepareFlight(AttackState state, AttackDefinition definition,
                                  Fighter attacker, Fighter target) {
        double deltaX = target.getX() - attacker.getX();
        double distance = Math.abs(deltaX);
        double direction = deltaX < 0 ? -1 : 1;
        if (distance < FLY_MIN_RANGE || distance > FLY_MAX_RANGE) return false;
        attacker.setFacingLeft(direction < 0);
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
        state.cancelWindowRemaining = Math.max(0, state.cancelWindowRemaining - deltaSeconds);
        state.remaining -= deltaSeconds;

        if (state.phase == Phase.STARTUP && state.remaining <= 0) {
            state.phase = Phase.ACTIVE;
            state.remaining += state.definition.activeSeconds();
        } else if (state.phase == Phase.ACTIVE) {
            Fighter attacker = fighters.get(attackerIndex);
            Fighter target = fighters.get(1 - attackerIndex);
            var attackerBefore = PhysicsSystem.positionOf(attacker);
            var targetBefore = PhysicsSystem.positionOf(target);
            if (attacker.isDefeated()) {
                resetAttackState(state);
                return;
            }
            if (!state.activeEffectStarted) {
                state.activeEffectStarted = true;
                emit(CombatEvent.Type.ATTACK_ACTIVE, state.definition.moveId(), attackerIndex,
                        attacker.getX(), attacker.getY(), attacker.isFacingLeft() ? -1 : 1, 0);
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
                if (usesQuickAttackDash(attacker.getBasePokemonId())
                        && "QUICK_ATTACK".equals(state.definition.moveId())) {
                    moveQuickAttack(attacker, state, arenaWidth, deltaSeconds);
                } else {
                    moveTowardTarget(attacker, target, state.definition, arenaWidth, deltaSeconds);
                }
            }

            // Resolve dash/lunge movement before checking its independent offensive box.
            PhysicsSystem.resolveBodies(attacker, target, attackerBefore, targetBefore, arenaWidth);
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
                        resolveContact(attackerIndex, attacker, target, state.definition, arenaWidth);
                        state.hasHit = true;
                    }
                }
            }

            if (state.remaining <= 0) {
                state.phase = Phase.RECOVERY;
                state.remaining += state.definition.actionRecoverySeconds()
                        * (state.hitConfirmed ? HIT_RECOVERY_MULTIPLIER : 1.0);
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
        if (ultimateController.isPlayerInputLocked(playerIndex, fighters)
                || fighter.isAsleep() || fighter.isBlocking() || fighter.isHitStunned()) return true;
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

    public boolean canBlock(int playerIndex, List<Fighter> fighters) {
        if (playerIndex < 0 || playerIndex >= fighters.size()) return false;
        Fighter fighter = fighters.get(playerIndex);
        AttackState own = states[playerIndex];
        return fighter.isOnGround() && !fighter.isDefeated() && !fighter.isAsleep()
                && !fighter.isHitStunned() && !fighter.isUltimateStunned()
                && own.phase == Phase.READY
                && !ultimateController.isPlayerInputLocked(playerIndex, fighters)
                && !controlledByOtherGrowl(playerIndex);
    }

    public ComboInfo getComboInfo(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= combos.length) return new ComboInfo(0, 0);
        ComboState combo = combos[playerIndex];
        return new ComboInfo(combo.hits, combo.remainingSeconds);
    }

    /** Evolution is only allowed after all of that fighter's actions have resolved. */
    public boolean canEvolve(int playerIndex, List<Fighter> fighters) {
        if (playerIndex < 0 || playerIndex >= states.length || fighters.size() < 2) return false;
        Fighter fighter = fighters.get(playerIndex);
        return states[playerIndex].phase == Phase.READY
                && !controlledByOtherGrowl(playerIndex)
                && !isPlayerInputLocked(playerIndex, fighters)
                && !fighter.isDefeated() && !fighter.isAsleep();
    }

    public List<UltimateController.Visual> getUltimateVisuals() {
        return ultimateController.getVisuals();
    }

    public String getUltimatePhaseName(int playerIndex) {
        return ultimateController.getPhaseName(playerIndex);
    }

    public int getUltimatePulsesApplied(int playerIndex) {
        return ultimateController.getPulsesApplied(playerIndex);
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

    private void moveQuickAttack(Fighter attacker, AttackState state, int arenaWidth,
                                 double deltaSeconds) {
        double remaining = EEVEE_QUICK_ATTACK_MAX_DISTANCE - state.dashDistanceTravelled;
        if (remaining <= 0) return;
        attacker.setFacingLeft(state.dashDirection < 0);
        double halfWidth = PhysicsSystem.halfWidthFor(attacker);
        double startX = attacker.getX();
        double distance = Math.min(remaining, state.definition.dashSpeed() * deltaSeconds);
        double nextX = Math.max(halfWidth, Math.min(arenaWidth - halfWidth,
                startX + state.dashDirection * distance));
        attacker.setX(nextX);
        state.dashDistanceTravelled += Math.abs(nextX - startX);
    }

    private Hitbox attackHitbox(Fighter attacker, AttackDefinition definition) {
        double direction = attacker.isFacingLeft() ? -1 : 1;
        double halfWidth = PhysicsSystem.halfWidthFor(attacker);
        double hitboxWidth = definition.hitboxWidth();
        double hitboxHeight = definition.hitboxHeight();
        double hitboxTop = hitboxHeight;
        FightingSpriteProfile profile = FightingSpriteProfile.forFighter(attacker);
        FightingSpriteProfile.Contact contact = profile == null ? null : profile.contact(definition.moveId());
        if (contact != null) {
            hitboxWidth = contact.reach();
            hitboxHeight = contact.height();
            hitboxTop = contact.top();
        }
        if ("SQUIRTLE".equals(attacker.getBasePokemonId()) && !attacker.isEvolved()) {
            switch (definition.moveId()) {
                case "TACKLE" -> { hitboxWidth = 44; hitboxHeight = 54; }
                case "BITE" -> { hitboxWidth = 30; hitboxHeight = 42; }
                case "TAIL_WHIP" -> { hitboxWidth = 76; hitboxHeight = 60; }
                default -> { }
            }
        }
        double left = direction > 0
                ? attacker.getX() + halfWidth
                : attacker.getX() - halfWidth - hitboxWidth;
        if (contact == null) hitboxTop = hitboxHeight;
        return new Hitbox(left, attacker.getY() - hitboxTop,
                hitboxWidth, hitboxHeight);
    }

    /** Cada bola de la triple Bola Sombra pega menos que una normal (son tres). */
    static final int VOLLEY_BALL_DAMAGE = 10;

    /** Una de las tres Bolas Sombra de Mewtwo, a la altura que se pida (sobre el suelo). */
    void spawnBossShadowBall(int ownerIndex, Fighter boss, double heightAboveGround) {
        double direction = boss.isFacingLeft() ? -1 : 1;
        double originX = boss.getX() + direction * (PhysicsSystem.halfWidthFor(boss) + 18);
        double originY = boss.getY() - heightAboveGround;
        emit(CombatEvent.Type.PROJECTILE_FIRED, BOSS_SHADOW_BALL.moveId(), ownerIndex,
                originX, originY, direction, 0);
        projectiles.add(new FightingProjectile(BOSS_SHADOW_BALL.moveId(), originX, originY,
                direction * BOSS_SHADOW_BALL.projectileSpeed(), BOSS_SHADOW_BALL.projectileSize(),
                VOLLEY_BALL_DAMAGE, BOSS_SHADOW_BALL.projectileLifetime(),
                BOSS_SHADOW_BALL.effectColor(), ownerIndex));
    }

    /** Cae la Premonicion: golpe normal (respeta escudo y parry no; solo esquivandolo). */
    void bossFutureSightStrike(int attackerIndex, Fighter boss, Fighter target) {
        applyHit(attackerIndex, boss, target, FUTURE_SIGHT);
    }

    /** La Onda mental alcanza al rival en el suelo. */
    void bossMindWaveHit(int attackerIndex, Fighter boss, Fighter target) {
        applyHit(attackerIndex, boss, target, MIND_WAVE);
    }

    private void spawnProjectile(Fighter attacker, int ownerIndex, AttackDefinition definition) {
        double direction = attacker.isFacingLeft() ? -1 : 1;
        double originX;
        double originY;
        FightingSpriteProfile profile = FightingSpriteProfile.forFighter(attacker);
        if (profile != null) {
            FightingSpriteProfile.Origin origin = profile.projectileOrigin(attacker, definition.moveId());
            originX = origin.x();
            originY = origin.y();
        } else {
            originX = attacker.getX() + direction * (PhysicsSystem.halfWidthFor(attacker) + 12);
            originY = attacker.getY() - 82;
        }
        // Mewtwo es muy alto: sus disparos salen a la altura de un Pokemon pequeno (se pueden saltar)
        if (attacker.isBoss()) originY = Math.max(originY, attacker.getY() - 62);
        double aim = attacker.isBoss() ? attacker.getAimUpAngle() : 0;
        if (aim > 0) {
            // Rival en una plataforma: Mewtwo se inclina hacia arriba y el disparo sube hacia el
            originY = attacker.getY() - attacker.getRenderHeight() * 0.62;
            originX = attacker.getX() + direction * (PhysicsSystem.halfWidthFor(attacker) * 0.7);
        }
        double speed = definition.projectileSpeed();
        emit(CombatEvent.Type.PROJECTILE_FIRED, definition.moveId(), ownerIndex,
                originX, originY, direction, 0);
        projectiles.add(new FightingProjectile(definition.moveId(), originX, originY,
                direction * speed * Math.cos(aim), -speed * Math.sin(aim), definition.projectileSize(),
                definition.damage(), definition.projectileLifetime(), definition.effectColor(), ownerIndex));
    }

    private void applyStatusMove(String moveId, Fighter target) {
        if ("TAIL_WHIP".equals(moveId)) target.applyDefenseReduction(DEFENSE_REDUCTION_DURATION);
    }

    private void applyHit(int attackerIndex, Fighter attacker, Fighter target,
                          AttackDefinition definition) {
        if (attacker.isDefeated() || target.isDefeated()) return;
        int damage = Math.max(1, (int) Math.round(definition.damage()
                * attacker.getOutgoingDamageMultiplier()
                * target.getIncomingDamageMultiplier()
                * comboDamageMultiplier(nextComboHits(attackerIndex, target))));
        target.receiveDamage(damage);
        target.applyKnockback(target.getX() >= attacker.getX() ? 1 : -1, knockbackFor(definition.kind(), damage));
        emit(CombatEvent.Type.HIT, definition.moveId(), attackerIndex,
                (attacker.getX() + target.getX()) / 2.0,
                target.getY() - target.getRenderHeight() * 0.55,
                target.getX() >= attacker.getX() ? 1 : -1, damage);
        ultimateController.onDamagingHit(target);
        attacker.addEvolutionEnergy(EVOLUTION_ENERGY_PER_HIT);
        int hitCount = target.recordNormalComboHit(COMBO_STUN_RESET_SECONDS);
        if (hitCount <= COMBO_HIT_STUN_SECONDS.length) {
            target.applyNormalHitStun(COMBO_HIT_STUN_SECONDS[hitCount - 1]);
        }
        AttackState state = states[attackerIndex];
        if (state.definition == definition && state.phase != Phase.READY) {
            state.hitConfirmed = true;
            state.cancelWindowRemaining = COMBO_CANCEL_WINDOW_SECONDS;
        }
        registerCombo(attackerIndex, target);
        addPhysicalImpact((attacker.getX() + target.getX()) / 2.0,
                target.getY() - target.getRenderHeight() * 0.55,
                definition, false, target);
        if ("TAIL_WHIP".equals(definition.moveId())) {
            target.applyDefenseReduction(DEFENSE_REDUCTION_DURATION);
        }
    }

    private void resolveContact(int attackerIndex, Fighter attacker, Fighter target,
                                AttackDefinition definition, int arenaWidth) {
        if (definition.kind() != AttackDefinition.Kind.HITBOX
                && definition.kind() != AttackDefinition.Kind.DASH
                && definition.kind() != AttackDefinition.Kind.LUNGE
                && definition.kind() != AttackDefinition.Kind.FLY) {
            applyHit(attackerIndex, attacker, target, definition);
            return;
        }
        if (target.isInParryWindow() && isAttackerInFront(target, attacker)) {
            parry(attackerIndex, attacker, target, definition, arenaWidth);
            return;
        }
        if (target.isBlocking() && isAttackerInFront(target, attacker)) {
            // El escudo para casi todo el golpe, pero algo pasa (ver ShieldRules)
            int chip = blockedDamage(attacker, target, definition.damage(), definition.moveId());
            if (chip > 0) target.receiveDamage(chip);
            target.applyKnockback(target.getX() >= attacker.getX() ? 1 : -1, BLOCK_KNOCKBACK);
            emit(CombatEvent.Type.BLOCK, definition.moveId(), attackerIndex,
                    (attacker.getX() + target.getX()) / 2.0,
                    target.getY() - target.getRenderHeight() * 0.55,
                    target.getX() >= attacker.getX() ? 1 : -1, chip);
            AttackState state = states[attackerIndex];
            addPhysicalImpact((attacker.getX() + target.getX()) / 2.0,
                    target.getY() - target.getRenderHeight() * 0.55,
                    definition, true, null);
            state.hasHit = true;
            state.phase = Phase.RECOVERY;
            state.remaining = definition.recoverySeconds();
            double direction = target.getX() >= attacker.getX() ? -1 : 1;
            double safeGap = PhysicsSystem.halfWidthFor(attacker)
                    + PhysicsSystem.halfWidthFor(target) + 1;
            double x = target.getX() + direction * safeGap;
            double half = PhysicsSystem.halfWidthFor(attacker);
            attacker.setX(Math.max(half, Math.min(arenaWidth - half, x)));
            attacker.applyBlockStun(BLOCK_STUN_SECONDS);
            state.phase = Phase.RECOVERY;
            state.remaining = Math.max(state.remaining, definition.recoverySeconds());
            return;
        }
        applyHit(attackerIndex, attacker, target, definition);
    }

    // ---- Parry: cubrirse justo a tiempo
    static final double PARRY_STUN_SECONDS = 0.65;
    static final double PARRY_PUSH = 420;

    private void parry(int attackerIndex, Fighter attacker, Fighter target,
                       AttackDefinition definition, int arenaWidth) {
        double dir = attacker.getX() >= target.getX() ? 1 : -1;
        emit(CombatEvent.Type.PARRY, definition.moveId(), attackerIndex,
                (attacker.getX() + target.getX()) / 2.0,
                target.getY() - target.getRenderHeight() * 0.55, dir, 0);
        AttackState state = states[attackerIndex];
        state.hasHit = true;
        state.phase = Phase.RECOVERY;
        state.remaining = Math.max(definition.recoverySeconds(), PARRY_STUN_SECONDS);
        attacker.applyBlockStun(PARRY_STUN_SECONDS);
        attacker.applyKnockback(dir, PARRY_PUSH);
        double safeGap = PhysicsSystem.halfWidthFor(attacker) + PhysicsSystem.halfWidthFor(target) + 1;
        double half = PhysicsSystem.halfWidthFor(attacker);
        attacker.setX(Math.max(half, Math.min(arenaWidth - half, target.getX() + dir * safeGap)));
        target.addEvolutionEnergy(EVOLUTION_ENERGY_PER_HIT);
    }

    // ---- Combos: del tercer golpe seguido en adelante pegan mas
    static double comboDamageMultiplier(int comboHits) {
        return comboHits >= 4 ? 1.25 : comboHits == 3 ? 1.15 : 1.0;
    }

    private int nextComboHits(int owner, Fighter target) {
        ComboState combo = combos[owner];
        return combo.target == target && combo.remainingSeconds > 0 ? combo.hits + 1 : 1;
    }

    // Cuanto retrocede el que recibe el golpe (pixeles por segundo al inicio)
    static final double BLOCK_KNOCKBACK = 110;

    /** Physical hits push more than projectiles; strong hits push more. */
    static double knockbackFor(AttackDefinition.Kind kind, int damage) {
        double base = switch (kind) {
            case HITBOX -> 300;
            case LUNGE -> 380;
            case DASH -> 430;
            case FLY -> 470;
            case PROJECTILE -> 230;
            default -> 0;
        };
        return damage >= 12 ? base * 1.3 : base;
    }

    /** Damage that gets through the shield, with the same multipliers as a normal hit. */
    private int blockedDamage(Fighter attacker, Fighter target, int baseDamage, String moveId) {
        if (attacker.isDefeated() || target.isDefeated() || baseDamage <= 0) return 0;
        int full = Math.max(1, (int) Math.round(baseDamage
                * attacker.getOutgoingDamageMultiplier()
                * target.getIncomingDamageMultiplier()));
        return ShieldRules.chipDamage(full,
                ShieldRules.blockedFraction(moveId, target.getPokemon(), false));
    }

    private boolean isAttackerInFront(Fighter defender, Fighter attacker) {
        return defender.isFacingLeft() ? attacker.getX() < defender.getX()
                : attacker.getX() > defender.getX();
    }

    private void registerCombo(int owner, Fighter target) {
        ComboState combo = combos[owner];
        combo.hits = combo.target == target && combo.remainingSeconds > 0 ? combo.hits + 1 : 1;
        combo.target = target;
        combo.remainingSeconds = COMBO_STUN_RESET_SECONDS;
    }

    private void updateProjectiles(List<Fighter> fighters, int arenaWidth,
                                   List<Platform> platforms, double deltaSeconds) {
        Iterator<FightingProjectile> iterator = projectiles.iterator();
        List<FightingProjectile> reflected = new ArrayList<>();
        while (iterator.hasNext()) {
            FightingProjectile projectile = iterator.next();
            projectile.update(deltaSeconds);
            Fighter target = fighters.get(1 - projectile.getOwnerIndex());
            if (target.isDefeated()) {
                iterator.remove();
            } else if (target.hasBarrier() && projectile.getDamage() > 0
                    && projectile.hitbox().intersects(target)) {
                // Barrera de Mewtwo: el proyectil vuelve hacia el que lo lanzo
                int newOwner = 1 - projectile.getOwnerIndex();
                double back = -Math.signum(projectile.getVelocityX());
                emit(CombatEvent.Type.REFLECT, projectile.getMoveId(), newOwner,
                        projectile.getX(), projectile.getY(), back, 0);
                reflected.add(new FightingProjectile(projectile.getMoveId(),
                        projectile.getX() + back * (projectile.getSize() + 6), projectile.getY(),
                        -projectile.getVelocityX() * 1.15, -projectile.getVelocityY(), projectile.getSize(),
                        projectile.getDamage(), 2.0, projectile.getColor(), newOwner));
                iterator.remove();
            } else if ("SLEEP_POWDER".equals(projectile.getMoveId())
                    && isBlockedByPlatform(projectile.hitbox(), platforms)) {
                emit(CombatEvent.Type.FIZZLE, projectile.getMoveId(), projectile.getOwnerIndex(),
                        projectile.getX(), projectile.getY(), Math.signum(projectile.getVelocityX()), 0);
                impactEffects.add(new ImpactEffect(projectile.getX(), projectile.getY(), 0.22));
                iterator.remove();
            } else if (projectile.hitbox().intersects(target)) {
                Fighter attacker = fighters.get(projectile.getOwnerIndex());
                if ("SLEEP_POWDER".equals(projectile.getMoveId())) {
                    boolean wasAsleep = target.isAsleep();
                    if (!attacker.isDefeated() && projectile.getDamage() > 0) {
                        int damage = Math.max(1, (int) Math.round(projectile.getDamage()
                                * attacker.getOutgoingDamageMultiplier()
                                * target.getIncomingDamageMultiplier()));
                        target.receiveDamage(damage);
                        emitProjectileHit(projectile, damage);
                        ultimateController.onDamagingHit(target);
                        attacker.addEvolutionEnergy(EVOLUTION_ENERGY_PER_HIT);
                        registerDamagingProjectileHit(projectile.getOwnerIndex(), target,
                                projectile.getMoveId());
                    }
                    if (!wasAsleep && !target.isDefeated()) target.applySleep(SLEEP_DURATION);
                    impactEffects.add(new ImpactEffect(projectile.getX(), projectile.getY(), 0.22));
                } else if (!attacker.isDefeated() && projectile.getDamage() > 0
                        && target.isInParryWindow() && ShieldRules.blocks(target, attacker)) {
                    // Parry contra un proyectil: lo deshace sin dano
                    emit(CombatEvent.Type.PARRY, projectile.getMoveId(), projectile.getOwnerIndex(),
                            projectile.getX(), projectile.getY(), Math.signum(projectile.getVelocityX()), 0);
                    target.addEvolutionEnergy(EVOLUTION_ENERGY_PER_HIT);
                } else if (!attacker.isDefeated() && projectile.getDamage() > 0
                        && ShieldRules.blocks(target, attacker)) {
                    // Proyectil contra el escudo: solo pasa una parte del dano
                    int chip = blockedDamage(attacker, target, projectile.getDamage(), projectile.getMoveId());
                    if (chip > 0) target.receiveDamage(chip);
                    target.applyKnockback(Math.signum(projectile.getVelocityX()), BLOCK_KNOCKBACK);
                    emit(CombatEvent.Type.BLOCK, projectile.getMoveId(), projectile.getOwnerIndex(),
                            projectile.getX(), projectile.getY(), Math.signum(projectile.getVelocityX()), chip);
                    impactEffects.add(new ImpactEffect(projectile.getX(), projectile.getY(), 0.18));
                } else if (!attacker.isDefeated() && projectile.getDamage() > 0) {
                    int damage = Math.max(1, (int) Math.round(projectile.getDamage()
                            * attacker.getOutgoingDamageMultiplier()
                            * target.getIncomingDamageMultiplier()
                            * comboDamageMultiplier(nextComboHits(projectile.getOwnerIndex(), target))));
                    target.receiveDamage(damage);
                    target.applyKnockback(Math.signum(projectile.getVelocityX()),
                            knockbackFor(AttackDefinition.Kind.PROJECTILE, damage));
                    emitProjectileHit(projectile, damage);
                    ultimateController.onDamagingHit(target);
                    attacker.addEvolutionEnergy(EVOLUTION_ENERGY_PER_HIT);
                    registerDamagingProjectileHit(projectile.getOwnerIndex(), target,
                            projectile.getMoveId());
                }
                iterator.remove();
            } else if (projectile.isExpired(arenaWidth)) {
                iterator.remove();
            }
        }
        projectiles.addAll(reflected);
    }

    private void registerDamagingProjectileHit(int ownerIndex, Fighter target, String moveId) {
        if (target.isDefeated()) return;
        int hitCount = target.recordNormalComboHit(COMBO_STUN_RESET_SECONDS);
        if (hitCount <= COMBO_HIT_STUN_SECONDS.length) {
            target.applyNormalHitStun(COMBO_HIT_STUN_SECONDS[hitCount - 1]);
        }
        registerCombo(ownerIndex, target);
        AttackState ownerState = states[ownerIndex];
        if (ownerState.definition != null && ownerState.definition.moveId().equals(moveId)) {
            ownerState.hitConfirmed = true;
            ownerState.cancelWindowRemaining = COMBO_CANCEL_WINDOW_SECONDS;
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

    private void addPhysicalImpact(double x, double y, AttackDefinition definition,
                                   boolean blocked, Fighter target) {
        double size = switch (definition.moveId()) {
            case "FLY" -> 48;
            case "TAIL_WHIP", "VINE_WHIP" -> 34;
            case "BITE" -> 29;
            default -> 27;
        };
        impactEffects.add(new ImpactEffect(x, y, blocked ? 0.18 : 0.22,
                definition.moveId(), blocked ? new Color(0xA7E8FF) : definition.effectColor(),
                size, blocked, target));
    }

    private void resetAttackState(AttackState state) {
        state.phase = Phase.READY;
        state.definition = null;
        state.remaining = 0;
        state.activeElapsed = 0;
        state.hasHit = false;
        state.activeEffectStarted = false;
        state.hitConfirmed = false;
        state.cancelWindowRemaining = 0;
        state.flyTargetY = 0;
        state.flyLandingX = 0;
        state.flyDirection = 0;
        state.flyVelocityX = 0;
        state.flyLaunched = false;
        state.flyRetreatStarted = false;
        state.flyRetreatRemaining = 0;
    }

    private void emit(CombatEvent.Type type, String moveId, int attackerIndex,
                      double x, double y, double direction, int damage) {
        events.add(new CombatEvent(type, moveId, attackerIndex, x, y, direction, damage));
    }

    private void emitProjectileHit(FightingProjectile projectile, int damage) {
        emit(CombatEvent.Type.HIT, projectile.getMoveId(), projectile.getOwnerIndex(),
                projectile.getX(), projectile.getY(), Math.signum(projectile.getVelocityX()), damage);
    }

    /** Returns what happened since the last call and forgets it. */
    public List<CombatEvent> drainEvents() {
        if (events.isEmpty()) return List.of();
        List<CombatEvent> copy = List.copyOf(events);
        events.clear();
        return copy;
    }

    public List<FightingProjectile> getProjectiles() { return projectileView; }
    public List<ImpactEffect> getImpactEffects() { return impactEffectView; }

    public AttackVisual getAttackVisual(Fighter fighter, List<Fighter> fighters) {
        int index = fighters.indexOf(fighter);
        if (index < 0) return null;
        AttackState state = states[index];
        if (state.phase == Phase.READY || state.definition == null) return null;
        double progress;
        if (state.phase == Phase.STARTUP) {
            progress = state.definition.startupSeconds() <= 0 ? 1
                    : 1 - state.remaining / state.definition.startupSeconds();
        } else if (state.phase == Phase.ACTIVE) {
            progress = state.activeElapsed / state.definition.activeSeconds();
        } else {
            double recovery = state.definition.actionRecoverySeconds()
                    * (state.hitConfirmed ? HIT_RECOVERY_MULTIPLIER : 1.0);
            progress = recovery <= 0 ? 1 : 1 - state.remaining / recovery;
        }
        double activeProgress = state.activeElapsed / state.definition.activeSeconds();
        return new AttackVisual(state.definition, state.phase.name(),
                Math.max(0, Math.min(1, progress)),
                Math.max(0, Math.min(1, activeProgress)), state.hitConfirmed,
                fighter.isFacingLeft() ? -1 : 1);
    }

    /** Render-only shake; it never changes Fighter coordinates or physics. */
    public double getVisualShakeX(Fighter fighter) {
        double strongest = 0;
        for (ImpactEffect effect : impactEffects) {
            if (effect.getTarget() != fighter || effect.isBlocked()) continue;
            double fade = 1 - effect.getProgress();
            double shake = Math.sin(effect.getProgress() * Math.PI * 9)
                    * 6 * fade * Math.min(1.5, effect.getSize() / 30.0);
            if (Math.abs(shake) > Math.abs(strongest)) strongest = shake;
        }
        return strongest;
    }

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

package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Pokemon;

import java.util.ArrayList;
import java.util.List;

/** Holds the participants and fixed arena data for one free-fight session. */
public class FightingGameSession {

    public enum State { RUNNING, PAUSED, FINISHED }
    public enum Outcome { PLAYER_ONE_WINS, PLAYER_TWO_WINS, DRAW }

    private static final double PLAYER_SPEED = 320.0;
    private final int width;
    private final int height;
    private final int groundY;
    private final List<Fighter> fighters;
    private final FightingTeam playerOneTeam;
    private final FightingTeam playerTwoTeam;
    private final List<Platform> platforms;
    private final FightingStage stage;
    private final PhysicsSystem physicsSystem = new PhysicsSystem();
    private final FightingCombatSystem combatSystem = new FightingCombatSystem();
    private final boolean[] defeatCleanupDone = new boolean[2];
    private State state = State.RUNNING;
    private Outcome outcome;

    public FightingGameSession() {
        this(FightingStage.BOSQUE);
    }

    /** Combate de prueba en el mapa indicado. */
    FightingGameSession(FightingStage stage) {
        this("JUGADOR 1", List.of(), "JUGADOR 2", List.of(), true, stage);
    }

    public FightingGameSession(FreeFightSetup setup) {
        this(setup.getPlayerOne().getName(), setup.getTeamOne(),
                setup.getPlayerTwo().getName(), setup.getTeamTwo(), false, setup.getStage());
        if (!setup.isComplete()) {
            throw new IllegalArgumentException("La selección de Combate Libre está incompleta");
        }
    }

    private FightingGameSession(String playerOneName, List<Pokemon> teamOne,
                                String playerTwoName, List<Pokemon> teamTwo,
                                boolean usePrototypeTeams) {
        this(playerOneName, teamOne, playerTwoName, teamTwo, usePrototypeTeams, FightingStage.BOSQUE);
    }

    private FightingGameSession(String playerOneName, List<Pokemon> teamOne,
                                String playerTwoName, List<Pokemon> teamTwo,
                                boolean usePrototypeTeams, FightingStage stage) {
        this.width = 1280;
        this.height = 720;
        this.groundY = 677;
        this.stage = stage == null ? FightingStage.BOSQUE : stage;
        // El bosque tiene las plataformas de Angel; el templo y el volcan solo el piso
        this.platforms = this.stage == FightingStage.BOSQUE
                ? List.of(
                        new Platform(100, 570, 180, 24),
                        new Platform(320, 475, 160, 24),
                        new Platform(510, 375, 155, 24),
                        new Platform(700, 425, 145, 24),
                        new Platform(870, 475, 160, 24),
                        new Platform(1060, 570, 170, 24),
                        new Platform(0, groundY, width, 24))
                : List.of(new Platform(0, groundY, width, 24));
        List<Pokemon> firstTeam = usePrototypeTeams ? List.of() : teamOne;
        List<Pokemon> secondTeam = usePrototypeTeams ? List.of() : teamTwo;
        this.playerOneTeam = usePrototypeTeams
                ? FightingTeam.prototype(playerOneName,
                    new String[]{"PIKACHU", "BULBASAUR", "EEVEE"}, 230, groundY, false)
                : new FightingTeam(playerOneName, firstTeam, 230, groundY, false);
        this.playerTwoTeam = usePrototypeTeams
                ? FightingTeam.prototype(playerTwoName,
                    new String[]{"CHARIZARD", "SQUIRTLE", "CHARMANDER"}, 1050, groundY, true)
                : new FightingTeam(playerTwoName, secondTeam, 1050, groundY, true);
        this.fighters = new ArrayList<>(List.of(playerOneTeam.getActive(), playerTwoTeam.getActive()));
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getGroundY() { return groundY; }
    public FightingStage getStage() { return stage; }
    public List<Fighter> getFighters() { return List.copyOf(fighters); }
    public FightingTeam getPlayerOneTeam() { return playerOneTeam; }
    public FightingTeam getPlayerTwoTeam() { return playerTwoTeam; }
    public List<Platform> getPlatforms() { return platforms; }
    public List<FightingProjectile> getProjectiles() { return combatSystem.getProjectiles(); }
    public FightingCombatSystem getCombatSystem() { return combatSystem; }
    public State getState() { return state; }
    public Outcome getOutcome() { return outcome; }
    public String getTrainerName(int playerIndex) {
        return playerIndex == 0 ? playerOneTeam.getTrainerName() : playerTwoTeam.getTrainerName();
    }

    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            combatSystem.clearPendingInputs();
        }
    }

    public void resume() {
        if (state == State.PAUSED) state = State.RUNNING;
    }

    public void update(FightingInput input, double deltaSeconds) {
        if (state != State.RUNNING) return;
        for (Fighter fighter : playerOneTeam.getMembers()) {
            fighter.advanceDefeatedAnimation(deltaSeconds);
        }
        for (Fighter fighter : playerTwoTeam.getMembers()) {
            fighter.advanceDefeatedAnimation(deltaSeconds);
        }
        replaceDefeatedFighters();
        checkForWinner();
        if (state != State.RUNNING) return;
        if (hasPendingDefeatAnimation()) {
            discardTransientInputs(input);
            return;
        }
        Fighter playerOne = fighters.get(0);
        Fighter playerTwo = fighters.get(1);
        playerOne.updateStatusEffects(deltaSeconds);
        playerTwo.updateStatusEffects(deltaSeconds);
        playerOne.setBlocking(input.isPlayerOneBlockingRequested()
                && combatSystem.canBlock(0, fighters));
        playerTwo.setBlocking(input.isPlayerTwoBlockingRequested()
                && combatSystem.canBlock(1, fighters));
        boolean playerOneJump = input.consumePlayerOneJump();
        boolean playerTwoJump = input.consumePlayerTwoJump();
        boolean playerOneEvolution = input.consumePlayerOneEvolution();
        boolean playerTwoEvolution = input.consumePlayerTwoEvolution();
        boolean playerOneUltimate = input.consumePlayerOneUltimate();
        boolean playerTwoUltimate = input.consumePlayerTwoUltimate();
        combatSystem.beginUltimateRequests(fighters, playerOneUltimate, playerTwoUltimate);
        var beforeOne = PhysicsSystem.positionOf(playerOne);
        var beforeTwo = PhysicsSystem.positionOf(playerTwo);
        boolean walkingOne = updateFighter(playerOne, 0, input.getPlayerOneDirection(), playerOneJump,
                input.isPlayerOneJumpHeld(), deltaSeconds);
        boolean walkingTwo = updateFighter(playerTwo, 1, input.getPlayerTwoDirection(), playerTwoJump,
                input.isPlayerTwoJumpHeld(), deltaSeconds);
        PhysicsSystem.resolveBodies(playerOne, playerTwo, beforeOne, beforeTwo, width);
        updateMotion(playerOne, walkingOne && Math.abs(playerOne.getX() - beforeOne.x()) > 0.01, deltaSeconds);
        updateMotion(playerTwo, walkingTwo && Math.abs(playerTwo.getX() - beforeTwo.x()) > 0.01, deltaSeconds);
        combatSystem.update(fighters, width, platforms,
                input.consumePlayerOneAttackRequest(), input.consumePlayerTwoAttackRequest(),
                deltaSeconds);
        for (Fighter fighter : fighters) {
            fighter.advanceKnockback(deltaSeconds, width, PhysicsSystem.halfWidthFor(fighter));
        }
        replaceDefeatedFighters();
        boolean playerOneSwitch = input.consumePlayerOneSwitch();
        boolean playerTwoSwitch = input.consumePlayerTwoSwitch();
        if (playerOneSwitch) trySwitchActiveFighter(0);
        if (playerTwoSwitch) trySwitchActiveFighter(1);
        if (switchCooldown[0] > 0) switchCooldown[0] -= deltaSeconds;
        if (switchCooldown[1] > 0) switchCooldown[1] -= deltaSeconds;
        if (playerOneEvolution) tryEvolveActiveFighter(0);
        if (playerTwoEvolution) tryEvolveActiveFighter(1);
        Fighter first = fighters.get(0);
        Fighter second = fighters.get(1);
        PhysicsSystem.resolveBodies(first, second, PhysicsSystem.positionOf(first),
                PhysicsSystem.positionOf(second), width);
        for (Fighter fighter : fighters) {
            fighter.updateShieldVisual(combatSystem.getAttackVisual(fighter, fighters) != null, deltaSeconds);
        }
        checkForWinner();
    }

    private boolean tryEvolveActiveFighter(int playerIndex) {
        Fighter fighter = fighters.get(playerIndex);
        if (!fighter.canEvolve() || !combatSystem.canEvolve(playerIndex, fighters)) return false;
        return fighter.evolve();
    }

    public boolean debugDefeatActiveFighter(int playerIndex) {
        if (state != State.RUNNING || (playerIndex != 0 && playerIndex != 1)) return false;
        Fighter active = fighters.get(playerIndex);
        if (active.isDefeated()) return false;
        active.markDefeated();
        replaceDefeatedFighters();
        checkForWinner();
        return true;
    }

    public boolean debugFillActiveEnergy(int playerIndex) {
        if (state != State.RUNNING || (playerIndex != 0 && playerIndex != 1)) return false;
        Fighter active = fighters.get(playerIndex);
        if (active.isDefeated()) return false;
        active.addEvolutionEnergy(100);
        return true;
    }

    // Cambio de Pokemon tocando la pared (Q jugador 1, U jugador 2)
    static final double SWITCH_COOLDOWN_SECONDS = 4.0;
    static final double WALL_TOUCH_MARGIN = 8;
    private final double[] switchCooldown = new double[2];

    /** True when the fighter is touching the left or right wall of the arena. */
    boolean isTouchingWall(Fighter fighter) {
        double halfWidth = PhysicsSystem.halfWidthFor(fighter);
        return fighter.getX() <= halfWidth + WALL_TOUCH_MARGIN
                || fighter.getX() >= width - halfWidth - WALL_TOUCH_MARGIN;
    }

    /**
     * Switches the active Pokemon for the other one of the team. Only against the wall,
     * on the ground, when the player is free (not attacking, stunned, asleep or in an
     * ultimate) and not more than once every few seconds. Each Pokemon keeps its health.
     */
    boolean trySwitchActiveFighter(int playerIndex) {
        Fighter current = fighters.get(playerIndex);
        FightingTeam team = playerIndex == 0 ? playerOneTeam : playerTwoTeam;
        if (switchCooldown[playerIndex] > 0 || !current.isOnGround() || current.isHitStunned()
                || current.isEvolutionAnimating() || !isTouchingWall(current)
                || !combatSystem.canEvolve(playerIndex, fighters)
                || java.util.Set.of("CHARGING", "RELEASING", "FINISHING", "RECOVERY")
                        .contains(combatSystem.getUltimatePhaseName(playerIndex))) {
            return false;
        }
        if (!team.switchActive()) return false;
        Fighter incoming = team.getActive();
        current.setBlocking(false);
        combatSystem.clearPlayerState(playerIndex);
        incoming.setX(current.getX());
        incoming.setY(groundY);
        incoming.setVelocityY(0);
        incoming.setOnGround(true);
        incoming.setFacingLeft(current.isFacingLeft());
        fighters.set(playerIndex, incoming);
        switchCooldown[playerIndex] = SWITCH_COOLDOWN_SECONDS;
        return true;
    }

    private void replaceDefeatedFighters() {
        for (int playerIndex = 0; playerIndex < fighters.size(); playerIndex++) {
            Fighter defeated = fighters.get(playerIndex);
            if (!defeated.isDefeated()) {
                defeatCleanupDone[playerIndex] = false;
                continue;
            }
            if (!defeatCleanupDone[playerIndex]) {
                combatSystem.clearPlayerState(playerIndex);
                defeatCleanupDone[playerIndex] = true;
            }
            if (shouldPlayFaint(defeated)) continue;
            FightingTeam team = playerIndex == 0 ? playerOneTeam : playerTwoTeam;
            if (team.advanceAfterDefeat()) {
                Fighter replacement = team.getActive();
                Fighter opponent = fighters.get(1 - playerIndex);
                positionReplacement(replacement, opponent, playerIndex);
                fighters.set(playerIndex, replacement);
                defeatCleanupDone[playerIndex] = false;
            }
        }
    }

    private boolean hasPendingDefeatAnimation() {
        for (Fighter fighter : fighters) {
            if (shouldPlayFaint(fighter)) return true;
        }
        return false;
    }

    private boolean shouldPlayFaint(Fighter fighter) {
        FightingSpriteProfile profile = FightingSpriteProfile.forFighter(fighter);
        return profile != null && fighter.isDefeated()
                && fighter.getDefeatedAnimationSeconds() < profile.faintDuration();
    }

    private void discardTransientInputs(FightingInput input) {
        input.consumePlayerOneJump();
        input.consumePlayerTwoJump();
        input.consumePlayerOneAttackRequest();
        input.consumePlayerTwoAttackRequest();
        input.consumePlayerOneEvolution();
        input.consumePlayerTwoEvolution();
        input.consumePlayerOneUltimate();
        input.consumePlayerTwoUltimate();
    }

    private void positionReplacement(Fighter replacement, Fighter opponent, int playerIndex) {
        double halfWidth = PhysicsSystem.halfWidthFor(replacement);
        double preferred = playerIndex == 0 ? 230 : width - 230;
        double[] candidates = playerIndex == 0
                ? new double[]{preferred, 350, 180, 450, 100}
                : new double[]{preferred, width - 350, width - 180, width - 450, width - 100};
        double opponentHalfWidth = PhysicsSystem.halfWidthFor(opponent);
        for (double candidate : candidates) {
            double x = Math.max(halfWidth, Math.min(width - halfWidth, candidate));
            if (Math.abs(x - opponent.getX()) >= halfWidth + opponentHalfWidth + 20) {
                replacement.setX(x);
                replacement.setY(groundY);
                replacement.setVelocityY(0);
                replacement.setOnGround(true);
                return;
            }
        }
        replacement.setX(playerIndex == 0 ? halfWidth : width - halfWidth);
        replacement.setY(groundY);
        replacement.setVelocityY(0);
        replacement.setOnGround(true);
    }

    private void checkForWinner() {
        boolean playerOneDefeated = !playerOneTeam.hasAvailableFighter();
        boolean playerTwoDefeated = !playerTwoTeam.hasAvailableFighter();
        if (!playerOneDefeated && !playerTwoDefeated) return;
        if (hasPendingDefeatAnimation()) return;

        if (playerOneDefeated && playerTwoDefeated) outcome = Outcome.DRAW;
        else if (playerTwoDefeated) outcome = Outcome.PLAYER_ONE_WINS;
        else outcome = Outcome.PLAYER_TWO_WINS;
        state = State.FINISHED;
        combatSystem.clearAllState();
    }

    private boolean updateFighter(Fighter fighter, int playerIndex, double direction,
                               boolean jumpRequested, boolean jumpHeld,
                               double deltaSeconds) {
        double halfWidth = PhysicsSystem.halfWidthFor(fighter);
        double forcedDelta = combatSystem.getForcedHorizontalDelta(
                fighter, fighters, deltaSeconds, width);
        boolean inputLocked = combatSystem.isPlayerInputLocked(playerIndex, fighters);
        double beforeInputX = fighter.getX();
        boolean walkingFromInput = false;
        if (!fighter.isUltimateStunned() && !fighter.isHitStunned()
                && !fighter.isBlocking() && forcedDelta != 0) {
            fighter.setX(fighter.getX() + forcedDelta);
        } else if (!inputLocked && !fighter.isUltimateStunned()) {
            fighter.moveHorizontally(direction,
                    PLAYER_SPEED * fighter.getMovementSpeedMultiplier() * deltaSeconds);
            walkingFromInput = direction != 0;
        }
        double minX = halfWidth;
        double maxX = width - halfWidth;
        fighter.setX(Math.max(minX, Math.min(maxX, fighter.getX())));
        walkingFromInput &= Math.abs(fighter.getX() - beforeInputX) > 0.01;
        physicsSystem.update(fighter, platforms, halfWidth,
                (jumpRequested || jumpHeld) && !inputLocked, jumpHeld, deltaSeconds);
        return walkingFromInput;
    }

    private void updateMotion(Fighter fighter, boolean walkingFromInput, double deltaSeconds) {
        Fighter.MotionAnimation motion = fighter.isBlocking()
                ? Fighter.MotionAnimation.BLOCKING
                : !fighter.isOnGround() ? Fighter.MotionAnimation.JUMPING
                : walkingFromInput ? Fighter.MotionAnimation.WALKING
                : Fighter.MotionAnimation.IDLE;
        fighter.updateMotionAnimation(motion, deltaSeconds);
    }
}

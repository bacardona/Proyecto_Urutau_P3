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
    private final PhysicsSystem physicsSystem = new PhysicsSystem();
    private final FightingCombatSystem combatSystem = new FightingCombatSystem();
    private State state = State.RUNNING;
    private Outcome outcome;

    public FightingGameSession() {
        this("JUGADOR 1", List.of(), "JUGADOR 2", List.of(), true);
    }

    public FightingGameSession(FreeFightSetup setup) {
        this(setup.getPlayerOne().getName(), setup.getTeamOne(),
                setup.getPlayerTwo().getName(), setup.getTeamTwo(), false);
        if (!setup.isComplete()) {
            throw new IllegalArgumentException("La selección de Combate Libre está incompleta");
        }
    }

    private FightingGameSession(String playerOneName, List<Pokemon> teamOne,
                                String playerTwoName, List<Pokemon> teamTwo,
                                boolean usePrototypeTeams) {
        this.width = 1280;
        this.height = 720;
        this.groundY = 677;
        this.platforms = List.of(
                new Platform(100, 570, 180, 24),
                new Platform(320, 475, 160, 24),
                new Platform(510, 375, 155, 24),
                new Platform(700, 425, 145, 24),
                new Platform(870, 475, 160, 24),
                new Platform(1060, 570, 170, 24),
                new Platform(0, groundY, width, 24)
        );
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
        if (state == State.RUNNING) state = State.PAUSED;
    }

    public void resume() {
        if (state == State.PAUSED) state = State.RUNNING;
    }

    public void update(FightingInput input, double deltaSeconds) {
        if (state != State.RUNNING) return;
        updateFighter(fighters.get(0), input.getPlayerOneDirection(),
                input.consumePlayerOneJump(), deltaSeconds);
        updateFighter(fighters.get(1), input.getPlayerTwoDirection(),
                input.consumePlayerTwoJump(), deltaSeconds);
        combatSystem.update(fighters, width, input.consumePlayerOneAttackOne(),
                input.consumePlayerOneAttackTwo(), input.consumePlayerTwoAttackOne(),
                input.consumePlayerTwoAttackTwo(), deltaSeconds);
        replaceDefeatedFighters();
        checkForWinner();
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

    private void replaceDefeatedFighters() {
        for (int playerIndex = 0; playerIndex < fighters.size(); playerIndex++) {
            Fighter defeated = fighters.get(playerIndex);
            if (!defeated.isDefeated()) continue;
            combatSystem.clearPlayerState(playerIndex);
            FightingTeam team = playerIndex == 0 ? playerOneTeam : playerTwoTeam;
            if (team.advanceAfterDefeat()) {
                Fighter replacement = team.getActive();
                Fighter opponent = fighters.get(1 - playerIndex);
                positionReplacement(replacement, opponent, playerIndex);
                fighters.set(playerIndex, replacement);
            }
        }
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

        if (playerOneDefeated && playerTwoDefeated) outcome = Outcome.DRAW;
        else if (playerTwoDefeated) outcome = Outcome.PLAYER_ONE_WINS;
        else outcome = Outcome.PLAYER_TWO_WINS;
        state = State.FINISHED;
    }

    private void updateFighter(Fighter fighter, double direction,
                               boolean jumpRequested,
                               double deltaSeconds) {
        double halfWidth = PhysicsSystem.halfWidthFor(fighter);
        fighter.moveHorizontally(direction, PLAYER_SPEED * deltaSeconds);
        double minX = halfWidth;
        double maxX = width - halfWidth;
        fighter.setX(Math.max(minX, Math.min(maxX, fighter.getX())));
        physicsSystem.update(fighter, platforms, halfWidth, jumpRequested, deltaSeconds);
    }
}

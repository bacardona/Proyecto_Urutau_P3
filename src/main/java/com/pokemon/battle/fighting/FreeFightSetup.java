package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;

import java.util.List;
import java.util.Objects;

/** Confirmed trainer and ordered team selections for a free-fight match. */
public final class FreeFightSetup {

    private Player playerOne;
    private List<Pokemon> teamOne = List.of();
    private Player playerTwo;
    private List<Pokemon> teamTwo = List.of();

    public void clear() {
        playerOne = null;
        teamOne = List.of();
        playerTwo = null;
        teamTwo = List.of();
    }

    public void setPlayerOne(Player player, List<Pokemon> team) {
        playerOne = Objects.requireNonNull(player);
        teamOne = List.copyOf(team);
    }

    public void setPlayerTwo(Player player, List<Pokemon> team) {
        playerTwo = Objects.requireNonNull(player);
        teamTwo = List.copyOf(team);
    }

    public Player getPlayerOne() { return playerOne; }
    public List<Pokemon> getTeamOne() { return teamOne; }
    public Player getPlayerTwo() { return playerTwo; }
    public List<Pokemon> getTeamTwo() { return teamTwo; }

    public boolean isComplete() {
        return playerOne != null && playerTwo != null
                && teamOne.size() == 3 && teamTwo.size() == 3;
    }
}

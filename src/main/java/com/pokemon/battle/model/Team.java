package com.pokemon.battle.model;

import com.pokemon.battle.exceptions.BusinessRuleException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Pokemon a trainer takes to a battle. Only one of them (the active one)
 * fights at a time; the others wait and can come in by switching.
 */
public class Team {

    /** Number of Pokemon every team must have. */
    public static final int SIZE = 3;

    private final List<Combatant> members = new ArrayList<>();
    private int activeIndex = 0;

    /**
     * Builds a team where the first Pokemon starts fighting. The list must be
     * validated before (see TeamValidator).
     *
     * @param pokemon the Pokemon of the team, in order
     */
    public Team(List<Pokemon> pokemon) {
        for (Pokemon p : pokemon) {
            members.add(new Combatant(p));
        }
    }

    /**
     * @return the Pokemon that is fighting now
     */
    public Combatant getActive() {
        return members.get(activeIndex);
    }

    /**
     * @return the position (starting at 0) of the active Pokemon
     */
    public int getActiveIndex() {
        return activeIndex;
    }

    /**
     * @return every Pokemon of the team, in order (read only)
     */
    public List<Combatant> getMembers() {
        return Collections.unmodifiableList(members);
    }

    /**
     * @return true while at least one Pokemon of the team can still fight
     */
    public boolean hasAvailable() {
        for (Combatant member : members) {
            if (!member.isFainted()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param index position (starting at 0) of the Pokemon
     * @return true when that Pokemon is not the active one and has not fainted
     */
    public boolean canSwitchTo(int index) {
        return index >= 0 && index < members.size()
                && index != activeIndex
                && !members.get(index).isFainted();
    }

    /**
     * @return true when there is some other Pokemon that could come in
     */
    public boolean hasBench() {
        return !getBenchIndexes().isEmpty();
    }

    /**
     * @return the positions (starting at 0) of the Pokemon that could come in
     */
    public List<Integer> getBenchIndexes() {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            if (canSwitchTo(i)) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    /**
     * Changes the active Pokemon. The one that leaves stops charging a move and
     * loses its flinch.
     *
     * @param index position (starting at 0) of the Pokemon that comes in
     * @throws BusinessRuleException when that Pokemon cannot come in
     */
    public void switchTo(int index) {
        if (!canSwitchTo(index)) {
            throw new BusinessRuleException("No se puede cambiar a ese Pokemon");
        }
        Combatant leaving = getActive();
        leaving.cancelCharge();
        leaving.consumeFlinch();
        activeIndex = index;
    }
}
package com.pokemon.battle.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable description of a Pokemon species: base stats, types and moves.
 * Battle state (current HP, status, stages) lives in {@link Combatant}.
 */
public class Pokemon {

    // Nivel que usan todos los Pokemon de este juego
    private static final int DEFAULT_LEVEL = 5;

    private final int id;
    private final String name;
    private final int hp;
    private final int attack;
    private final int defense;
    private final int specialAttack;
    private final int specialDefense;
    private final int speed;
    private final Type primaryType;
    private final Type secondaryType;
    private final List<Move> moves;
    private final int level = DEFAULT_LEVEL;

    /**
     * Creates a Pokemon.
     *
     * @param id             Pokedex number
     * @param name           display name
     * @param hp             base HP stat
     * @param attack         base attack stat
     * @param defense        base defense stat
     * @param specialAttack  base special attack stat
     * @param specialDefense base special defense stat
     * @param speed          base speed stat
     * @param primaryType    first type
     * @param secondaryType  second type ({@link Type#NONE} when it has only one)
     * @param moves          known moves (up to four)
     */
    public Pokemon(int id, String name, int hp, int attack, int defense, int specialAttack,
                   int specialDefense, int speed, Type primaryType, Type secondaryType,
                   List<Move> moves) {
        this.id = id;
        this.name = name;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.specialAttack = specialAttack;
        this.specialDefense = specialDefense;
        this.speed = speed;
        this.primaryType = primaryType;
        this.secondaryType = secondaryType;
        this.moves = Collections.unmodifiableList(new ArrayList<>(moves));
    }

    /**
     * @return the Pokedex number
     */
    public int getId() {
        return id;
    }

    /**
     * @return the display name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the base HP stat
     */
    public int getHp() {
        return hp;
    }

    /**
     * @return the base attack stat
     */
    public int getAttack() {
        return attack;
    }

    /**
     * @return the base defense stat
     */
    public int getDefense() {
        return defense;
    }

    /**
     * @return the base special attack stat
     */
    public int getSpecialAttack() {
        return specialAttack;
    }

    /**
     * @return the base special defense stat
     */
    public int getSpecialDefense() {
        return specialDefense;
    }

    /**
     * @return the base speed stat
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * @return the first type
     */
    public Type getPrimaryType() {
        return primaryType;
    }

    /**
     * @return the second type, or {@link Type#NONE}
     */
    public Type getSecondaryType() {
        return secondaryType;
    }

    /**
     * @return the level of the Pokemon
     */
    public int getLevel() {
        return level;
    }

    /**
     * @return an unmodifiable list with the known moves
     */
    public List<Move> getMoves() {
        return moves;
    }

    /**
     * @param index zero-based position of the move
     * @return the move at that position
     */
    public Move getMove(int index) {
        return moves.get(index);
    }

    /**
     * @param type type to check
     * @return true when the Pokemon has that type as primary or secondary
     */
    public boolean hasType(Type type) {
        return primaryType == type || secondaryType == type;
    }

    /**
     * Calculates the maximum HP with the level formula.
     *
     * @return the maximum hit points of this Pokemon
     */
    public int getMaxHitPoints() {
        return (int) (((2.0 * hp) * level) / 100.0) + level + 10;
    }
}

package com.pokemon.battle.persistence.codec;

import com.pokemon.battle.model.Move;

/**
 * Knows how to turn one kind of {@link Move} into CSV columns and back.
 * The repository only talks to this abstraction, so a new kind of move needs a
 * new codec and no change in the repository.
 */
public interface MoveCsvCodec {

    /**
     * @return the discriminator written in the first CSV column
     */
    String kind();

    /**
     * @param move move to check
     * @return true when this codec handles the exact class of the move
     */
    boolean supports(Move move);

    /**
     * Builds a move from a CSV row.
     *
     * @param common shared columns
     * @param params the three parameter columns (param1 to param3)
     * @return the move
     */
    Move parse(MoveCommonFields common, String[] params);

    /**
     * @param move move to write
     * @return the specific parameters of the move (at most three values)
     */
    String[] params(Move move);
}

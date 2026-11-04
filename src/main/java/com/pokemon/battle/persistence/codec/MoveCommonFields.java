package com.pokemon.battle.persistence.codec;

import com.pokemon.battle.model.MoveCategory;
import com.pokemon.battle.model.Type;

/**
 * Columns shared by every move row of the CSV file.
 *
 * @param id       unique key of the move
 * @param name     display name
 * @param type     elemental type
 * @param category physical, special or status
 * @param power    base power
 * @param accuracy base accuracy
 * @param pp       power points
 * @param priority move priority
 */
public record MoveCommonFields(String id, String name, Type type, MoveCategory category, int power,
                               int accuracy, int pp, int priority) {
}

package com.pokemon.battle.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects the messages produced during a round so the UI can show them.
 * This keeps the model and the services free of console output.
 */
public class BattleLog {

    private final List<String> messages = new ArrayList<>();

    /**
     * @param message line to add to the log
     */
    public void log(String message) {
        messages.add(message);
    }

    /**
     * @return an unmodifiable copy of the collected messages
     */
    public List<String> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }
}

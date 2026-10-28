package com.pokemon.battle.persistence;

import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores Pokemon in a CSV file. Moves are referenced by id and resolved
 * through the injected {@link MoveRepository}.
 */
public class CsvPokemonRepository implements PokemonRepository {

    private static final String SEPARATOR = ";";
    private static final String MOVES_SEPARATOR = "|";
    private static final int COLUMNS = 11;
    private static final int MOVES_COLUMN = 10;

    private final Path file;
    private final MoveReader moveReader;

    /**
     * @param file           path of the CSV file
     * @param moveReader source used to resolve the move ids
     */
    public CsvPokemonRepository(Path file, MoveReader moveReader) {
        this.file = file;
        this.moveReader = moveReader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pokemon> loadAll() {
        List<Pokemon> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer el archivo '" + file + "' (lectura)", e);
        }
        Map<String, Move> movesById = new HashMap<>();
        for (Move move : moveReader.loadAll()) {
            movesById.put(move.getId(), move);
        }
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                result.add(parse(line.split(SEPARATOR, -1), movesById));
            } catch (RuntimeException e) {
                throw new PersistenceException("Linea invalida en '" + file + "' (lectura): " + line, e);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAll(List<Pokemon> pokemon) {
        List<String> lines = new ArrayList<>();
        for (Pokemon p : pokemon) {
            lines.add(format(p));
        }
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo escribir el archivo '" + file + "' (escritura)", e);
        }
    }

    private Pokemon parse(String[] f, Map<String, Move> movesById) {
        if (f.length < COLUMNS) {
            throw new IllegalArgumentException("Se esperaban " + COLUMNS + " columnas");
        }
        List<Move> moves = new ArrayList<>();
        for (String moveId : f[MOVES_COLUMN].split("\\|")) {
            if (moveId.isBlank()) {
                continue;
            }
            Move move = movesById.get(moveId.trim());
            if (move == null) {
                throw new IllegalArgumentException("Ataque desconocido: " + moveId);
            }
            moves.add(move);
        }
        return new Pokemon(Integer.parseInt(f[0]), f[1], Integer.parseInt(f[2]), Integer.parseInt(f[3]),
                Integer.parseInt(f[4]), Integer.parseInt(f[5]), Integer.parseInt(f[6]),
                Integer.parseInt(f[7]), Type.valueOf(f[8]), Type.valueOf(f[9]), moves);
    }

    private String format(Pokemon p) {
        List<String> columns = new ArrayList<>();
        columns.add(String.valueOf(p.getId()));
        columns.add(p.getName());
        columns.add(String.valueOf(p.getHp()));
        columns.add(String.valueOf(p.getAttack()));
        columns.add(String.valueOf(p.getDefense()));
        columns.add(String.valueOf(p.getSpecialAttack()));
        columns.add(String.valueOf(p.getSpecialDefense()));
        columns.add(String.valueOf(p.getSpeed()));
        columns.add(p.getPrimaryType().name());
        columns.add(p.getSecondaryType().name());
        List<String> moveIds = new ArrayList<>();
        for (Move move : p.getMoves()) {
            moveIds.add(move.getId());
        }
        columns.add(String.join(MOVES_SEPARATOR, moveIds));
        return String.join(SEPARATOR, columns);
    }
}

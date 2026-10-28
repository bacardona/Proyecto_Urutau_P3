package com.pokemon.battle.persistence;

import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.MoveCategory;
import com.pokemon.battle.model.Type;
import com.pokemon.battle.persistence.codec.MoveCommonFields;
import com.pokemon.battle.persistence.codec.MoveCsvCodec;
import com.pokemon.battle.persistence.codec.MoveCsvCodecs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores moves in a text file (data/moves.csv), one move per line, with columns
 * separated by semicolons and no header line. The first column (kind) is the
 * discriminator that selects the {@link MoveCsvCodec} in charge of each row, so
 * this class does not know any concrete kind of move.
 */
public class CsvMoveRepository implements MoveRepository {

    private static final String SEPARATOR = ";";
    private static final int COMMON_COLUMNS = 9;
    private static final int PARAM_COLUMNS = 3;

    private final Path file;
    private final List<MoveCsvCodec> codecs;
    private final Map<String, MoveCsvCodec> codecsByKind = new HashMap<>();

    /**
     * Creates a repository that understands every standard kind of move.
     *
     * @param file path of the CSV file
     */
    public CsvMoveRepository(Path file) {
        this(file, MoveCsvCodecs.standard());
    }

    /**
     * @param file   path of the CSV file
     * @param codecs codecs of the kinds of move this repository can read and write
     */
    public CsvMoveRepository(Path file, List<MoveCsvCodec> codecs) {
        this.file = file;
        this.codecs = codecs;
        for (MoveCsvCodec codec : codecs) {
            codecsByKind.put(codec.kind(), codec);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Move> loadAll() {
        List<Move> moves = new ArrayList<>();
        if (!Files.exists(file)) {
            return moves;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer el archivo '" + file + "' (lectura)", e);
        }
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                moves.add(parse(line.split(SEPARATOR, -1)));
            } catch (RuntimeException e) {
                throw new PersistenceException("Linea invalida en '" + file + "' (lectura): " + line, e);
            }
        }
        return moves;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAll(List<Move> moves) {
        List<String> lines = new ArrayList<>();
        for (Move move : moves) {
            lines.add(format(move));
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

    private Move parse(String[] f) {
        if (f.length < COMMON_COLUMNS + PARAM_COLUMNS) {
            throw new IllegalArgumentException("Se esperaban " + (COMMON_COLUMNS + PARAM_COLUMNS) + " columnas");
        }
        MoveCsvCodec codec = codecsByKind.get(f[0]);
        if (codec == null) {
            throw new IllegalArgumentException("Tipo de ataque desconocido: " + f[0]);
        }
        MoveCommonFields common = new MoveCommonFields(f[1], f[2], Type.valueOf(f[3]),
                MoveCategory.valueOf(f[4]), Integer.parseInt(f[5]), Integer.parseInt(f[6]),
                Integer.parseInt(f[7]), Integer.parseInt(f[8]));
        String[] params = {f[9], f[10], f[11]};
        return codec.parse(common, params);
    }

    private String format(Move move) {
        MoveCsvCodec codec = codecs.stream()
                .filter(c -> c.supports(move))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay codec para el ataque: " + move.getClass().getSimpleName()));
        String[] params = new String[PARAM_COLUMNS];
        String[] specific = codec.params(move);
        for (int i = 0; i < PARAM_COLUMNS; i++) {
            params[i] = i < specific.length ? specific[i] : "";
        }
        return String.join(SEPARATOR, codec.kind(), move.getId(), move.getName(), move.getType().name(),
                move.getCategory().name(), String.valueOf(move.getPower()),
                String.valueOf(move.getAccuracy()), String.valueOf(move.getPp()),
                String.valueOf(move.getPriority()), params[0], params[1], params[2]);
    }
}

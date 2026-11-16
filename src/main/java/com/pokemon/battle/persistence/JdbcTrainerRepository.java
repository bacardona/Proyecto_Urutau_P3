package com.pokemon.battle.persistence;

import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the trainers from Oracle. JUGADOR and MAQUINA share their identifier
 * with ENTRENADOR, so each one is joined with it to get the name.
 */
public class JdbcTrainerRepository implements TrainerRepository {

    private static final String PLAYERS_SQL =
            "SELECT e.id_entrenador, e.nombre, j.batallas_ganadas, j.batallas_perdidas " +
                    "FROM entrenador e " +
                    "JOIN jugador j ON j.id_entrenador = e.id_entrenador " +
                    "ORDER BY e.nombre";

    private static final String MACHINES_SQL =
            "SELECT e.id_entrenador, e.nombre, m.rango " +
                    "FROM entrenador e " +
                    "JOIN maquina m ON m.id_entrenador = e.id_entrenador " +
                    "ORDER BY e.nombre";

    private static final String NEXT_ID_SQL =
            "SELECT NVL(MAX(id_entrenador), 0) + 1 FROM entrenador";

    private static final String INSERT_TRAINER_SQL =
            "INSERT INTO entrenador (id_entrenador, nombre) VALUES (?, ?)";

    private static final String INSERT_PLAYER_SQL =
            "INSERT INTO jugador (id_entrenador, batallas_ganadas, batallas_perdidas) VALUES (?, 0, 0)";

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Player> loadPlayers() {
        List<Player> players = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(PLAYERS_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                players.add(new Player(
                        rs.getInt("id_entrenador"),
                        rs.getString("nombre"),
                        rs.getInt("batallas_ganadas"),
                        rs.getInt("batallas_perdidas")));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error al consultar jugadores en Oracle BD", e);
        }
        loadPlayerStats(players);
        return players;
    }

    private static final String GAMES_PLAYED_SQL =
            "SELECT id_entrenador, func_partidas_jugadas_jugador(id_entrenador) AS valor FROM jugador";

    private static final String WIN_RATE_SQL =
            "SELECT id_entrenador, func_winrate_entrendaor(id_entrenador) AS valor FROM jugador";

    /**
     * Fills the games played and the win rate of each player with the stored functions
     * func_partidas_jugadas_jugador and func_winrate_entrendaor. If a function fails the
     * players still load and the screen shows "--" for that value.
     */
    private void loadPlayerStats(List<Player> players) {
        java.util.Map<Integer, Double> games = readFunction(GAMES_PLAYED_SQL, "func_partidas_jugadas_jugador");
        java.util.Map<Integer, Double> rates = readFunction(WIN_RATE_SQL, "func_winrate_entrendaor");
        for (Player p : players) {
            Double played = games.get(p.getId());
            if (played != null) {
                p.setGamesPlayed((int) Math.round(played));
            }
            Double rate = rates.get(p.getId());
            if (rate != null) {
                p.setWinRate(rate);
            }
        }
    }

    // id_entrenador -> valor que devuelve la funcion
    private java.util.Map<Integer, Double> readFunction(String sql, String functionName) {
        java.util.Map<Integer, Double> values = new java.util.HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                values.put(rs.getInt("id_entrenador"), rs.getDouble("valor"));
            }
        } catch (SQLException e) {
            System.err.println("No se pudo usar " + functionName + ": " + e.getMessage());
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Machine> loadMachines() {
        List<Machine> machines = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(MACHINES_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                machines.add(new Machine(
                        rs.getInt("id_entrenador"),
                        rs.getString("nombre"),
                        rs.getString("rango")));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error al consultar maquinas en Oracle BD", e);
        }
        return machines;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Player createPlayer(String name) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Sin autocommit, ENTRENADOR y JUGADOR se confirman juntos con commit() o se deshacen con rollback()
            conn.setAutoCommit(false);
            try {
                int id = nextTrainerId(conn);
                insertTrainer(conn, id, name);
                insertPlayer(conn, id);
                conn.commit();
                return new Player(id, name, 0, 0);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error al crear el jugador en Oracle BD", e);
        }
    }

    private int nextTrainerId(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(NEXT_ID_SQL);
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void insertTrainer(Connection conn, int id, String name) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_TRAINER_SQL)) {
            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.executeUpdate();
        }
    }

    private void insertPlayer(Connection conn, int id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_PLAYER_SQL)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
}
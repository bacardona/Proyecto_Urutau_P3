package com.pokemon.battle.persistence;

import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.model.CombatHistoryEntry;
import com.pokemon.battle.model.CombatResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes combats in Oracle (COMBATE and HISTORIAL_COMBATES) and keeps
 * the win/loss counters of JUGADOR up to date.
 */
public class JdbcCombatRepository implements CombatRepository {

    private static final String NEXT_ID_SQL =
            "SELECT NVL(MAX(id_combate), 0) + 1 FROM combate";

    private static final String INSERT_COMBAT_SQL =
            "INSERT INTO combate (id_combate, fecha_combate) VALUES (?, SYSDATE)";

    private static final String INSERT_HISTORY_SQL =
            "INSERT INTO historial_combates (id_entrenador, id_combate, resultado) VALUES (?, ?, ?)";

    // Si el entrenador es una MAQUINA no hay fila en JUGADOR y el UPDATE no modifica nada
    private static final String ADD_WIN_SQL =
            "UPDATE jugador SET batallas_ganadas = batallas_ganadas + 1 WHERE id_entrenador = ?";

    private static final String ADD_LOSS_SQL =
            "UPDATE jugador SET batallas_perdidas = batallas_perdidas + 1 WHERE id_entrenador = ?";

    // Une el historial del entrenador con el de su rival (mismo combate, otro entrenador)
    private static final String HISTORY_SQL =
            "SELECT c.id_combate, c.fecha_combate, h.resultado, e.nombre AS rival " +
                    "FROM historial_combates h " +
                    "JOIN combate c ON c.id_combate = h.id_combate " +
                    "LEFT JOIN historial_combates h2 " +
                    "       ON h2.id_combate = h.id_combate AND h2.id_entrenador <> h.id_entrenador " +
                    "LEFT JOIN entrenador e ON e.id_entrenador = h2.id_entrenador " +
                    "WHERE h.id_entrenador = ? " +
                    "ORDER BY c.fecha_combate DESC, c.id_combate DESC";

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CombatHistoryEntry> loadHistory(int trainerId) {
        List<CombatHistoryEntry> history = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(HISTORY_SQL)) {

            stmt.setInt(1, trainerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp date = rs.getTimestamp("fecha_combate");
                    history.add(new CombatHistoryEntry(
                            rs.getInt("id_combate"),
                            date == null ? null : date.toLocalDateTime(),
                            rs.getString("rival"),
                            rs.getString("resultado")));
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error al consultar el historial de combates en Oracle BD", e);
        }
        return history;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int registerCombat(int winnerId, int loserId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Sin autocommit, todo se confirma junto con commit() o se deshace con rollback()
            conn.setAutoCommit(false);
            try {
                int combatId = nextCombatId(conn);
                insertCombat(conn, combatId);
                insertHistory(conn, winnerId, combatId, CombatResult.WIN);
                insertHistory(conn, loserId, combatId, CombatResult.LOSS);
                updateCounter(conn, ADD_WIN_SQL, winnerId);
                updateCounter(conn, ADD_LOSS_SQL, loserId);
                conn.commit();
                return combatId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error al registrar el combate en Oracle BD", e);
        }
    }

    private int nextCombatId(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(NEXT_ID_SQL);
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void insertCombat(Connection conn, int combatId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_COMBAT_SQL)) {
            stmt.setInt(1, combatId);
            stmt.executeUpdate();
        }
    }

    private void insertHistory(Connection conn, int trainerId, int combatId, CombatResult result)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_HISTORY_SQL)) {
            stmt.setInt(1, trainerId);
            stmt.setInt(2, combatId);
            stmt.setString(3, result.getDbValue());
            stmt.executeUpdate();
        }
    }

    private void updateCounter(Connection conn, String sql, int trainerId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, trainerId);
            stmt.executeUpdate();
        }
    }
}

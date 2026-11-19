package com.pokemon.battle.persistence;

import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.model.CombatHistoryEntry;
import com.pokemon.battle.model.CombatResult;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes combats in Oracle via the PKG_COMBATE PL/SQL package.
 * The win/loss counters in JUGADOR are kept up to date automatically by
 * trig_actualizar_registros_jugador upon inserting into HISTORIAL_COMBATES.
 */
public class JdbcCombatRepository implements CombatRepository {

    private static final String CALL_REGISTER_COMBAT =
            "{ call registrar_combate(?, ?, ?, ?, ?) }";

    private static final String CALL_LOAD_HISTORY =
            "{ call listar_historial(?, ?) }";

    @Override
    public List<CombatHistoryEntry> loadHistory(int trainerId) {
        List<CombatHistoryEntry> history = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(CALL_LOAD_HISTORY)) {

            stmt.setInt(1, trainerId);
            stmt.registerOutParameter(2, Types.REF_CURSOR);
            stmt.execute();

            try (ResultSet rs = (ResultSet) stmt.getObject(2)) {
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
            throw new PersistenceException("Error al consultar el historial de combates en Oracle BD");
        }
        return history;
    }

    @Override
    public int registerCombat(int winnerId, int loserId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (CallableStatement stmt = conn.prepareCall(CALL_REGISTER_COMBAT)) {
                stmt.setInt(1, winnerId);
                stmt.setInt(2, loserId);
                stmt.setString(3, CombatResult.WIN.getDbValue());
                stmt.setString(4, CombatResult.LOSS.getDbValue());
                stmt.registerOutParameter(5, Types.NUMERIC);

                stmt.execute();
                int combatId = stmt.getInt(5);

                conn.commit();
                return combatId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error al registrar el combate en Oracle BD");
        }
    }
}
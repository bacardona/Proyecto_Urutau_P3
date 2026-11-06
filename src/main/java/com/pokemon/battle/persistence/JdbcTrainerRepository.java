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
public class JdbcTrainerRepository implements TrainerReader {

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
        return players;
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
}

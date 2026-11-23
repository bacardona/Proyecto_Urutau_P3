package com.pokemon.battle.persistence;

import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.model.Machine;
import com.pokemon.battle.model.Player;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes trainers in Oracle via stored procedures (ENTRENADOR, JUGADOR, MAQUINA).
 */
public class JdbcTrainerRepository implements TrainerRepository {

    private static final String CALL_LOAD_PLAYERS =
            "{ call entrenador__listar_jugadores(?) }";

    private static final String CALL_LOAD_MACHINES =
            "{ call entrenador__listar_maquinas(?) }";

    private static final String CALL_CREATE_PLAYER =
            "{ call entrenador__crear_jugador(?, ?) }";

    @Override
    public List<Player> loadPlayers() {
        List<Player> players = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(CALL_LOAD_PLAYERS)) {

            stmt.registerOutParameter(1, Types.REF_CURSOR);
            stmt.execute();

            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    Player player = new Player(
                            rs.getInt("id_entrenador"),
                            rs.getString("nombre"),
                            rs.getInt("batallas_ganadas"),
                            rs.getInt("batallas_perdidas"));

                    double played = rs.getDouble("partidas_jugadas");
                    if (!rs.wasNull()) {
                        player.setGamesPlayed((int) Math.round(played));
                    }

                    double rate = rs.getDouble("winrate");
                    if (!rs.wasNull()) {
                        player.setWinRate(rate);
                    }

                    players.add(player);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error al consultar jugadores en Oracle BD");
        }
        return players;
    }

    @Override
    public List<Machine> loadMachines() {
        List<Machine> machines = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(CALL_LOAD_MACHINES)) {

            stmt.registerOutParameter(1, Types.REF_CURSOR);
            stmt.execute();

            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    machines.add(new Machine(
                            rs.getInt("id_entrenador"),
                            rs.getString("nombre"),
                            rs.getString("rango")));
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error al consultar maquinas en Oracle BD");
        }
        return machines;
    }

    @Override
    public Player createPlayer(String name) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (CallableStatement stmt = conn.prepareCall(CALL_CREATE_PLAYER)) {
                stmt.setString(1, name);
                stmt.registerOutParameter(2, Types.NUMERIC);

                stmt.execute();
                int id = stmt.getInt(2);

                conn.commit();
                return new Player(id, name, 0, 0);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error al crear el jugador en Oracle BD");
        }
    }
}
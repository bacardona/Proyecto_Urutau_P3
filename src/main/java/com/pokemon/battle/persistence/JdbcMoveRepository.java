package com.pokemon.battle.persistence;

import com.pokemon.battle.exceptions.PersistenceException;
import com.pokemon.battle.model.*;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class JdbcMoveRepository implements MoveRepository {

    private static final String CALL_LOAD_ALL_MOVES =
            "{ call ataque__listar_todos(?) }";

    @Override
    public List<Move> loadAll() {
        List<Move> moves = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(CALL_LOAD_ALL_MOVES)) {

            stmt.registerOutParameter(1, Types.REF_CURSOR);
            stmt.execute();

            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    String clas = rs.getString("clase");
                    String id = rs.getString("id_ataque");
                    String name = rs.getString("nombre");
                    Type type = Type.valueOf(rs.getString("tipo").toUpperCase());
                    MoveCategory category = MoveCategory.valueOf(rs.getString("categoria").toUpperCase());
                    int power = rs.getInt("dano");
                    int accuracy = rs.getInt("precision");
                    int pp = rs.getInt("pp");
                    int priority = rs.getInt("prioridad");

                    Move move;
                    if (clas == null) {
                        throw new PersistenceException("El discriminador no puede ser nulo para el ataque ID: " + id);
                    }

                    switch (clas.toUpperCase()) {
                        case "DAMAGE_STATUS":
                            String estadoStr = rs.getString("estado_alterado");
                            StatusCondition statusCondition = estadoStr != null ? StatusCondition.valueOf(estadoStr.toUpperCase()) : null;
                            int probEstado = rs.getInt("probabilidad");
                            move = new DamageWithStatusMove(id, name, type, category, power, accuracy, pp, priority, statusCondition, probEstado);
                            break;

                        case "DAMAGE_FLINCH":
                            int flinchProb = rs.getInt("p");
                            move = new DamageWithFlinchMove(id, name, type, category, power, accuracy, pp, priority, flinchProb);
                            break;

                        case "INFLICT_STATUS":
                            String tipoEstado = rs.getString("tipo_de_estado");
                            StatusCondition statusToInflict = tipoEstado != null ? StatusCondition.valueOf(tipoEstado.toUpperCase()) : null;
                            move = new InflictStatusMove(id, name, type, accuracy, pp, priority, statusToInflict);
                            break;

                        case "STAT_CHANGE":
                            StatType statType = StatType.valueOf(rs.getString("tipo_de_estadistica"));
                            int stage = rs.getInt("etapa");
                            boolean target = "SELF".equalsIgnoreCase(rs.getString("objetivo"));

                            move = new StatChangeMove(id, name, type, accuracy, pp, priority, statType, stage, target);
                            break;

                        case "TWO_TURN":
                            String message = rs.getString("mensaje");
                            boolean invulnerable = "true".equalsIgnoreCase(rs.getString("invulnerable"));

                            move = new TwoTurnMove(id, name, type, category, power, accuracy, pp, priority, message, invulnerable);
                            break;

                        case "DAMAGE":
                        default:
                            move = new DamageMove(id, name, type, category, power, accuracy, pp, priority);
                            break;
                    }

                    moves.add(move);
                }
            }

        } catch (SQLException e) {
            throw new PersistenceException("Error al consultar movimientos en Oracle BD");
        }

        return moves;
    }

    public void saveAll(List<Move> moveList) {
        // Si tu juego requiere guardar cambios en la BD, aquí implementas un INSERT / UPDATE en lote (batch)
    }
}
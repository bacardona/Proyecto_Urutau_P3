package com.pokemon.battle.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Apunta directamente a la carpeta config/wallet en la raíz del proyecto
    private static final String TNS_ADMIN_DIR = System.getProperty("user.dir") + "/config/wallet";

    // Alias de tu tnsnames.ora
    private static final String DB_URL = "jdbc:oracle:thin:@pokkenbd_low?TNS_ADMIN=" + TNS_ADMIN_DIR;

    private static final String USER = "POKKEN_ADMIN";
    private static final String PASSWORD = "@Chiripa100%";

    public static Connection getConnection() throws SQLException {
        // Indicar a Oracle la ruta del Wallet fuera de src
        System.setProperty("oracle.net.tns_admin", TNS_ADMIN_DIR);

        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el Driver JDBC de Oracle", e);
        }

        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }
}

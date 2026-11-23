package com.pokemon.battle.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    // Apunta directamente a la carpeta config/wallet en la raíz del proyecto
    private static final String TNS_ADMIN_DIR =
            (System.getProperty("user.dir") + "/config/wallet").replace("\\", "/");

    private static final String DB_URL = "jdbc:oracle:thin:@pokkenbd_low";

    private static final String USER = "POKKEN_ADMIN";
    private static final String PASSWORD = "@Chiripa100%";

    private static final HikariDataSource dataSource;

    static {
        // Indicar a Oracle la ruta del Wallet antes de inicializar el pool
        System.setProperty("oracle.net.tns_admin", TNS_ADMIN_DIR);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setDriverClassName("oracle.jdbc.OracleDriver");

        // Tamaño del pool ideal para aplicación de escritorio conectada a OCI Free/Low
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);

        // Tiempos pensados para tolerar redes móviles o lentas sin caerse rápido
        config.setConnectionTimeout(20000); // 20s máximo esperando conexión
        config.setIdleTimeout(300000);      // 5 min antes de cerrar conexiones extra inactivas
        config.setMaxLifetime(1500000);     // 25 min de vida máxima (evita cortes del firewall de OCI)
        config.setKeepaliveTime(60000);     // Ping cada 60s para mantener viva la sesión en redes inestables

        config.setPoolName("PokkenOraclePool");

        dataSource = new HikariDataSource(config);
    }

    private DatabaseConnection() {
        // Evita instanciación innecesaria
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Cierra limpiamente el pool al salir del juego (opcional, llamar al cerrar la ventana principal).
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
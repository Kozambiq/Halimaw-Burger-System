package com.myapp.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    private static HikariDataSource dataSource;

    private DatabaseConnection() {
    }

    public static void initialize() {
        if (dataSource != null) {
            return;
        }

        Properties props = new Properties();
        try (InputStream is = DatabaseConnection.class.getResourceAsStream("/com/myapp/halimawburgersystem/db.properties")) {
            if (is == null) {
                return;
            }
            props.load(is);
        } catch (IOException e) {
            logger.severe("Failed to load db.properties: " + e.getMessage());
            return;
        }

        String host = props.getProperty("db.host");
        String port = props.getProperty("db.port");
        String name = props.getProperty("db.name");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");
        String ssl = props.getProperty("db.ssl", "false");

        if (host == null || host.startsWith("YOUR_")) {
            return;
        }

        String jdbcUrl = String.format("jdbc:mariadb://%s:%s/%s?ssl=%s",
                host, port, name, ssl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);
        logger.info("Database connection pool initialized");
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initialize();
        }
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized");
        }
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}
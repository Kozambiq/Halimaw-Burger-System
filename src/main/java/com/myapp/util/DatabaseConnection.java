package com.myapp.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

public class DatabaseConnection {

    private static HikariDataSource dataSource;

    private DatabaseConnection() {
    }

    public static void initialize() {
        Properties props = new Properties();
        try (InputStream is = DatabaseConnection.class.getResourceAsStream("/com/myapp/halimawburgersystem/db.properties")) {
            if (is == null) {
                System.out.println("db.properties not found - running without database");
                return;
            }
            props.load(is);
        } catch (IOException e) {
            System.out.println("Could not load db.properties: " + e.getMessage());
            return;
        }

        String host = props.getProperty("db.host");
        String port = props.getProperty("db.port");
        String name = props.getProperty("db.name");
        String dbUser = EnvLoader.get("db_user");
        String dbPassword = EnvLoader.get("db_password");

        if (host == null || host.startsWith("YOUR_")) {
            System.out.println("Database not configured - running without database");
            return;
        }

        String sslMode = props.getProperty("db.sslMode", "disable");
        String timezone = props.getProperty("db.timezone", "UTC");
        String dbUrl = String.format(
                "jdbc:mariadb://%s:%s/%s?sslMode=%s&serverTimezone=%s&useLegacyDatetimeCode=false",
                host, port, name, sslMode, timezone
        );

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        
        // Optimizations for MariaDB/MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        // Pool settings
        config.setMaximumPoolSize(15);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000); // 5 minutes
        config.setConnectionTimeout(20000); // 20 seconds
        config.setLeakDetectionThreshold(2000); // 2 seconds

        try {
            dataSource = new HikariDataSource(config);
            System.out.println("Database connection pool initialized: " + dbUrl);
        } catch (Exception e) {
            System.out.println("Database connection pool failed: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            // Attempt to initialize if not already done
            initialize();
            if (dataSource == null) {
                throw new SQLException("Database not configured or pool not initialized");
            }
        }
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            System.out.println("Database connection pool closed");
        }
    }
}
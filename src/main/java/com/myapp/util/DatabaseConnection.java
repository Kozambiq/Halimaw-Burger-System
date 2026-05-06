package com.myapp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import com.myapp.util.EnvLoader;

public class DatabaseConnection {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

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
        dbUser = EnvLoader.get("db_user");
        dbPassword = EnvLoader.get("db_password");

        if (host == null || host.startsWith("YOUR_")) {
            System.out.println("Database not configured - running without database");
            return;
        }

        String sslMode = props.getProperty("db.sslMode", "disable");
        dbUrl = String.format("jdbc:mariadb://%s:%s/%s?sslMode=%s", host, port, name, sslMode);

        try {
            DriverManager.getConnection(dbUrl, dbUser, dbPassword).close();
            System.out.println("Database connected: " + dbUrl);
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dbUrl == null) {
            throw new SQLException("Database not configured");
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public static void close() {
        System.out.println("Database connection closed");
    }
}
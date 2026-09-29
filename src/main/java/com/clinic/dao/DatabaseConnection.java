package com.clinic.dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConnection {
    // Configurable database URL supporting CLI system properties (-Dclinic.db.url) and environment variables
    public static final String DEFAULT_DB_URL = System.getProperty(
            "clinic.db.url",
            System.getenv().getOrDefault("CLINIC_DB_URL", "jdbc:sqlite:clinic.db")
    );
    private static String currentDbUrl = DEFAULT_DB_URL;

    // Instantiated Database Connection with a private constructor to prevent direct instantiation of its utility
    private DatabaseConnection() {
    }

    // Returns an active SQLite database connection with foreign key enforcement enabled using PRAGMA.
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(currentDbUrl);
        // Enforce SQLite Foreign Key constraints on every new connection
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    // Sets a custom database URL (e.g., 'jdbc:sqlite::memory:' for fast, isolated unit tests)
    public static synchronized void setDatabaseUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Database URL cannot be empty.");
        }
        currentDbUrl = url;
    }

    // Resets the connection manager to the default disk-backed database
    public static synchronized void resetToDefaultDatabaseUrl() {
        currentDbUrl = DEFAULT_DB_URL;
    }

    public static synchronized String getCurrentDbUrl() {
        return currentDbUrl;
    }

    // Diagnostic helper to verify that SQLite foreign key enforcement is active (returns true if PRAGMA foreign_keys = 1).
    public static boolean isForeignKeyEnforcementActive(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("PRAGMA foreign_keys;")) {
            if (rs.next()) {
                return rs.getInt(1) == 1;
            }
        }
        return false;
    }

    // Bootstraps and migrates the database schema using PRAGMA user_version.
    // Idempotent: safe to run on every application startup.
    public static synchronized void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            int currentVersion = 0;
            try (var rs = stmt.executeQuery("PRAGMA user_version;")) {
                if (rs.next()) {
                    currentVersion = rs.getInt(1);
                }
            }

            // Version 0 -> Initial Schema Creation
            if (currentVersion < 1) {
                executeSqlScript(conn, "/schema.sql");
                stmt.execute("PRAGMA user_version = 1;");
            }

            // Future automated DDL migrations can be added here:
            // if (currentVersion < 2) { ... stmt.execute("PRAGMA user_version = 2;"); }
        }
    }

    // Helper method to parse and execute SQL script statements from classpath
    private static void executeSqlScript(Connection conn, String resourcePath) throws SQLException {
        try (InputStream is = DatabaseConnection.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Critical Error: " + resourcePath + " resource not found on classpath!");
            }

            StringBuilder sqlBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    // Skip full-line SQL comments
                    if (trimmed.startsWith("--")) {
                        continue;
                    }
                    // Strip trailing comments if present
                    int commentIdx = line.indexOf("--");
                    if (commentIdx != -1) {
                        line = line.substring(0, commentIdx);
                    }
                    sqlBuilder.append(line).append("\n");
                }
            }

            String[] statements = sqlBuilder.toString().split(";");
            try (Statement stmt = conn.createStatement()) {
                for (String rawSql : statements) {
                    String sql = rawSql.trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }
            }
        } catch (java.io.IOException e) {
            throw new SQLException("Failed to read " + resourcePath + " from classpath: " + e.getMessage(), e);
        }
    }
}
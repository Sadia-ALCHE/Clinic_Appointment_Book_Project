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
    public static final String DEFAULT_DB_URL = "jdbc:sqlite:clinic.db";
    private static String currentDbUrl = DEFAULT_DB_URL;

    // Instantiated Database Connection with a private constructor to prevent direct instantiation of it's utility
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


    // Resets the connection manager to the default disk-backed database ('jdbc:sqlite:clinic.db').
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


     // Bootstraps the database schema by executing schema.sql from the application classpath.
     // Idempotent: safe to run on every application startup.
    public static synchronized void initializeDatabase() throws SQLException {
        try (InputStream is = DatabaseConnection.class.getResourceAsStream("/schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("Critical Error: schema.sql resource not found on classpath!");
            }

            StringBuilder sqlBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
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

            // Split statements by semicolon
            String[] statements = sqlBuilder.toString().split(";");

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                for (String rawSql : statements) {
                    String sql = rawSql.trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }
            }
        } catch (java.io.IOException e) {
            throw new SQLException("Failed to read schema.sql from classpath: " + e.getMessage(), e);
        }
    }
}
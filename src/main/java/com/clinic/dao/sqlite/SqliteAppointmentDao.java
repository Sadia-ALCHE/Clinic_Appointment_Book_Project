package com.clinic.dao;

import com.clinic.model.Appointment;

import java.time.format.DateTimeFormatter;

public class SqliteAppointmentDao implements AppointmentDao {

    private final DatabaseConnection dbConnection;

    // Keeps appointment date/time values in a consistent format
    // when storing and reading them from SQLite.
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Receives the database connection that this DAO will use.
    public SqliteAppointmentDao(DatabaseConnection dbConnection) {
        // The DAO cannot work without a database connection.
        if (dbConnection == null) {
            throw new IllegalArgumentException(
                    "DatabaseConnection cannot be null."
            );
        }

        this.dbConnection = dbConnection;
    }
}

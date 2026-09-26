package com.clinic.dao.sqlite;

import com.clinic.dao.DatabaseConnection;
import com.clinic.dao.InvoiceDao;
import com.clinic.model.Invoice;
import com.clinic.model.InvoiceItem;
import com.clinic.model.PaymentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class SqliteInvoiceDao implements InvoiceDao {

    private final DatabaseConnection dbConnection;
    // Creates the DAO using the provided database connection.

    public SqliteInvoiceDao(DatabaseConnection dbConnection) {
        if (dbConnection == null) {throw new IllegalArgumentException("DatabaseConnection cannot be null.");}
        this.dbConnection = dbConnection;
    }

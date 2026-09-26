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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteInvoiceDao implements InvoiceDao {

    private final DatabaseConnection dbConnection;
    // Creates the DAO using the provided database connection.

    public SqliteInvoiceDao(DatabaseConnection dbConnection) {
        if (dbConnection == null) {
            throw new IllegalArgumentException("DatabaseConnection cannot be null.");
        }
        this.dbConnection = dbConnection;
    }

    @Override
    public Invoice save(Invoice invoice) {
        if (invoice == null) throw new IllegalArgumentException("Invoice cannot be null.");

        // Insert the main invoice record.
        String insertInvoiceSql = """
                INSERT INTO invoices (appointment_id, invoice_number, issue_date, status)
                VALUES (?, ?, ?, ?);
                """;
        // Insert each item belonging to the invoice.
        String insertItemSql = """
                INSERT INTO invoice_items (invoice_id, description, amount_mur)
                VALUES (?, ?, ?);
                """;

        Connection conn = null;
        boolean originalAutoCommit = true;

        try {
            conn = dbConnection.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            // Start transaction so invoice and its items are saved together.
            conn.setAutoCommit(false);

            long generatedInvoiceId;
            try (PreparedStatement pstmt = conn.prepareStatement(insertInvoiceSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setLong(1, invoice.getAppointmentId());
                pstmt.setString(2, invoice.getInvoiceNumber());
                pstmt.setString(3, invoice.getIssueDate().toString());
                pstmt.setString(4, invoice.getStatus().name());
                pstmt.executeUpdate();

                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedInvoiceId = keys.getLong(1);
                    } else {
                        throw new SQLException("Failed to retrieve generated invoice ID.");
                    }
                }
            }

            // Insert each itemized charge in Mauritian Rupees (MUR)
            try (PreparedStatement itemStmt = conn.prepareStatement(insertItemSql)) {
                for (InvoiceItem item : invoice.getItems()) {
                    itemStmt.setLong(1, generatedInvoiceId);
                    itemStmt.setString(2, item.getDescription());
                    itemStmt.setDouble(3, item.getAmountMur());
                    itemStmt.executeUpdate();
                }
            }

            conn.commit(); // Transaction success: commit all atomically!

            // Return reconstructed persistent entity with items
            Invoice persisted = new Invoice(
                    generatedInvoiceId,
                    invoice.getAppointmentId(),
                    invoice.getInvoiceNumber(),
                    invoice.getIssueDate(),
                    invoice.getStatus()
            );
            for (InvoiceItem item : invoice.getItems()) {
                persisted.addItem(new InvoiceItem(item.getDescription(), item.getAmountMur()));
            }
            return persisted;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    // Keep the original database error.
                }
            }
            throw new RuntimeException("Transaction rolled back while saving invoice: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                    conn.close();
                } catch (SQLException e) {
                    // Connection cleanup failure.
                }
            }
        }
    }

    @Override
    public Optional<Invoice> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, appointment_id, invoice_number, issue_date, status FROM invoices WHERE id = ?; ";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(hydrateInvoice(conn, rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding invoice by ID: " + id, e);
        }
        return Optional.empty();
    }

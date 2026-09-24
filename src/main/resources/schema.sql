PRAGMA foreign_keys = ON;

-- 1. Patients Directory
CREATE TABLE IF NOT EXISTS patients (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        first_name TEXT NOT NULL,
                                        last_name TEXT NOT NULL,
                                        email TEXT UNIQUE NOT NULL,
                                        phone TEXT NOT NULL,
                                        date_of_birth TEXT NOT NULL,
                                        blood_group TEXT NOT NULL,
                                        created_at TEXT NOT NULL DEFAULT (datetime('now'))
    );

-- 2. Medical Doctors / Specialists
-- Constraint: Consultation rates must be between MUR 500.00 and MUR 10,000.00
CREATE TABLE IF NOT EXISTS doctors (
                                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                                       first_name TEXT NOT NULL,
                                       last_name TEXT NOT NULL,
                                       specialty TEXT NOT NULL,
                                       hourly_rate REAL NOT NULL CHECK(hourly_rate >= 500.0 AND hourly_rate <= 10000.0),
    email TEXT UNIQUE NOT NULL,
    phone TEXT NOT NULL
    );

-- 3. Appointments & Follow-up Care Chains
-- Self-referential parent_appointment_id establishes recursive follow-up trees
CREATE TABLE IF NOT EXISTS appointments (
                                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                                            patient_id INTEGER NOT NULL,
                                            doctor_id INTEGER NOT NULL,
                                            appointment_datetime TEXT NOT NULL,
                                            reason TEXT NOT NULL,
                                            status TEXT NOT NULL CHECK(status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    parent_appointment_id INTEGER,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE RESTRICT,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE RESTRICT,
    FOREIGN KEY (parent_appointment_id) REFERENCES appointments(id) ON DELETE RESTRICT
    );

-- Fiscal invoices to create an immutable financial audit record
CREATE TABLE IF NOT EXISTS invoices (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        appointment_id INTEGER UNIQUE NOT NULL,
                                        invoice_number TEXT UNIQUE NOT NULL,
                                        issue_date TEXT NOT NULL,
                                        status TEXT NOT NULL CHECK(status IN ('PENDING', 'PARTIALLY_PAID', 'PAID', 'REFUNDED')),
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE RESTRICT
    );

-- Invoice charges all itemized
CREATE TABLE IF NOT EXISTS invoice_items (
                                             id INTEGER PRIMARY KEY AUTOINCREMENT,
                                             invoice_id INTEGER NOT NULL,
                                             description TEXT NOT NULL,
                                             amount_mur REAL NOT NULL CHECK(amount_mur >= 0.0),
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE RESTRICT
    );

-- Performance Indices for Rapid Lookups
CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor ON appointments(doctor_id);
CREATE INDEX IF NOT EXISTS idx_appointments_parent ON appointments(parent_appointment_id);
CREATE INDEX IF NOT EXISTS idx_invoices_appointment ON invoices(appointment_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items(invoice_id);

-- Seed Baseline Doctors (Mauritian Rupees)
INSERT OR IGNORE INTO doctors (id, first_name, last_name, specialty, hourly_rate, email, phone) VALUES
    (1, 'Sarah', 'Mensah', 'General Practice', 1500.0, 's.mensah@mediche.mu', '+230 5842 1099'),
    (2, 'Jean-Luc', 'Poisson', 'Cardiology', 2200.0, 'jl.poisson@mediche.mu', '+230 5712 3456'),
    (3, 'Aisha', 'Patel', 'Pediatrics', 1800.0, 'a.patel@mediche.mu', '+230 5923 8811');
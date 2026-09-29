package com.clinic;

import com.clinic.dao.sqlite.SqliteAppointmentDao;
import com.clinic.dao.sqlite.SqliteDoctorDao;
import com.clinic.dao.sqlite.SqliteInvoiceDao;
import com.clinic.dao.sqlite.SqlitePatientDao;
import com.clinic.model.*;
import com.clinic.service.AppointmentService;
import com.clinic.service.BillingService;
import com.clinic.service.ScheduleValidator;
import com.clinic.ui.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// JavaFX Desktop Application Composition Root with Real SQLite Persistence
public class App extends Application {

    public static final String APP_TITLE = "MediCare Clinic Appointment Book · ALCHE Mauritius";
    public static final int DEFAULT_WIDTH = 1150;
    public static final int DEFAULT_HEIGHT = 720;
    public static final int MIN_WIDTH = 950;
    public static final int MIN_HEIGHT = 600;

    @Override
    public void start(Stage primaryStage) {
        // 1. Initialize SQLite DAOs and Business Services
        SqlitePatientDao patientDao = new SqlitePatientDao();
        SqliteDoctorDao doctorDao = new SqliteDoctorDao();
        SqliteAppointmentDao appointmentDao = new SqliteAppointmentDao();
        SqliteInvoiceDao invoiceDao = new SqliteInvoiceDao();

        ScheduleValidator validator = new ScheduleValidator(appointmentDao);
        AppointmentService appointmentService = new AppointmentService(appointmentDao, doctorDao);
        BillingService billingService = new BillingService(invoiceDao, appointmentDao, doctorDao, patientDao, appointmentService);

        // 2. Ensure Initial Mauritian Physician Test Records & Invoices Exist
        seedInitialDoctorsIfEmpty(doctorDao);
        seedInitialInvoicesIfEmpty(billingService, appointmentDao, patientDao, doctorDao, invoiceDao);

        // 3. Root Layout Shell & Navigation Coordinator
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");
        NavigationManager navManager = new NavigationManager(root);

        // 4. Construct Concrete Views with Injected Dependencies
        PatientView patientView = new PatientView(patientDao);
        // Line 53 in App.java:
        ScheduleView scheduleView = new ScheduleView(appointmentDao, doctorDao, patientDao, validator);
        BillingView billingView = new BillingView(billingService, patientDao, appointmentDao, doctorDao, appointmentService);

        // 5. Construct Sidebar
        SidebarView sidebar = new SidebarView();
        root.setLeft(sidebar.getRoot());

        // 6. Register Views with Navigation Manager
        navManager.registerView(NavigationManager.VIEW_PATIENTS, patientView, sidebar.getBtnPatients());
        navManager.registerView(NavigationManager.VIEW_SCHEDULE, scheduleView, sidebar.getBtnSchedule());
        navManager.registerView(NavigationManager.VIEW_BILLING, billingView, sidebar.getBtnBilling());

        // 7. Activate Default View
        navManager.showPatients();

        // 8. Construct Primary Scene with CSS Stylesheet
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        String cssPath = getClass().getResource("/style.css") != null
                ? getClass().getResource("/style.css").toExternalForm()
                : null;
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }

        // 9. Configure Stage Window Bounds & Show
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void seedInitialDoctorsIfEmpty(SqliteDoctorDao doctorDao) {
        List<Doctor> existingDoctors = doctorDao.findAll();
        if (existingDoctors.isEmpty()) {
            doctorDao.save(new Doctor(null, "Sarah", "Mensah", "General Practice", 1500.0, "s.mensah@medicare.mu", "+230 5842 1001"));
            doctorDao.save(new Doctor(null, "Jean-Luc", "Pierre", "Cardiology", 2500.0, "jl.pierre@medicare.mu", "+230 5842 1002"));
            doctorDao.save(new Doctor(null, "Amina", "Patel", "Dermatology", 1800.0, "a.patel@medicare.mu", "+230 5842 1003"));
        }
    }

    private void seedInitialInvoicesIfEmpty(BillingService billingService, SqliteAppointmentDao appointmentDao,
                                            SqlitePatientDao patientDao, SqliteDoctorDao doctorDao,
                                            SqliteInvoiceDao invoiceDao) {
        if (!invoiceDao.findAll().isEmpty()) {
            return;
        }

        // Ensure at least one test patient exists
        List<Patient> patients = patientDao.findAll();
        Patient testPatient;
        if (patients.isEmpty()) {
            testPatient = patientDao.save(new Patient(null, "Adebayo", "Ogunlesi", "adebayo@alche.edu.mu", "+230 5842 1099", LocalDate.of(2003, 5, 14), "O+"));
        } else {
            testPatient = patients.get(0);
        }

        List<Doctor> doctors = doctorDao.findAll();
        Doctor doc = doctors.isEmpty()
                ? doctorDao.save(new Doctor(null, "Sarah", "Mensah", "General Practice", 1500.0, "s.mensah@medicare.mu", "+230 5842 1001"))
                : doctors.get(0);

        // Seed Root Consultation Appointment
        Appointment anchorAppt = appointmentDao.save(new Appointment(
                null, testPatient.getId(), doc.getId(),
                LocalDateTime.of(2026, 9, 17, 9, 0),
                "Initial Clinical Triage",
                AppointmentStatus.COMPLETED,
                null,
                AppointmentType.STANDARD_CONSULTATION
        ));
        // Generate Invoice with consultation fee + diagnostic item
        Invoice inv1 = billingService.generateInvoiceForAppointment(anchorAppt.getId());
        inv1.addItem(new InvoiceItem(null, inv1.getId(), "Blood Glucose Panel (Campus Lab)", 1200.0));
        inv1.addItem(new InvoiceItem(null, inv1.getId(), "Resting ECG Diagnostic", 1150.0));
        inv1.addItem(new InvoiceItem(null, inv1.getId(), "Sterile Wound Dressing", 850.0));
        // Seed Follow-up Appointment in Care Chain
        Appointment followUpAppt = appointmentDao.save(new Appointment(
                null, testPatient.getId(), doc.getId(),
                LocalDateTime.of(2026, 9, 17, 14, 0),
                "Follow-Up Suture Review",
                AppointmentStatus.CONFIRMED,
                anchorAppt.getId(),
                AppointmentType.FOLLOW_UP
        ));

        Invoice inv2 = billingService.generateInvoiceForAppointment(followUpAppt.getId());

        // Settle first invoice to demonstrate PAID status
        billingService.settlePayment(inv1.getId(), PaymentMethod.MCB_JUICE, "JUICE-58421099-01");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
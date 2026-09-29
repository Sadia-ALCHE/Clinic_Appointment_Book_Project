package com.clinic;

import com.clinic.dao.DatabaseConnection;
import com.clinic.dao.sqlite.SqliteAppointmentDao;
import com.clinic.dao.sqlite.SqliteDoctorDao;
import com.clinic.dao.sqlite.SqliteInvoiceDao;
import com.clinic.dao.sqlite.SqlitePatientDao;
import com.clinic.factory.AppointmentFactory;
import com.clinic.model.*;
import com.clinic.service.AppointmentService;
import com.clinic.service.BillingService;
import com.clinic.service.ScheduleValidator;
import com.clinic.ui.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
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
        // Preload Poppins custom typography
        loadCustomFonts();

        // Bootstrap database schema if not already present
        try {
            DatabaseConnection.initializeDatabase();
        } catch (Exception e) {
            System.err.println("Database initialization note: " + e.getMessage());
        }

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
        ScheduleView scheduleView = new ScheduleView(appointmentDao, doctorDao, patientDao, validator, billingService);
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

        // 1. Seed diverse Mauritian patients
        Patient p1 = patientDao.save(new Patient(null, "Adebayo", "Ogunlesi", "adebayo@alche.edu.mu", "+230 5842 1099", LocalDate.of(2003, 5, 14), "O+"));
        Patient p2 = patientDao.save(new Patient(null, "Marie Claire", "Dupont", "m.dupont@campus.mu", "+230 5711 2233", LocalDate.of(1998, 11, 20), "A+"));
        Patient p3 = patientDao.save(new Patient(null, "Jean-Paul", "Ah-Kee", "jp.ahkee@umail.mu", "+230 5822 4455", LocalDate.of(2001, 8, 3), "B+"));
        Patient p4 = patientDao.save(new Patient(null, "Priya", "Ramgoolam", "p.ramgoolam@alche.edu.mu", "+230 5933 6677", LocalDate.of(2004, 2, 17), "AB+"));
        Patient p5 = patientDao.save(new Patient(null, "Kevin", "Tremblay", "k.tremblay@intnet.mu", "+230 5744 8899", LocalDate.of(1995, 7, 29), "O-"));

        List<Doctor> doctors = doctorDao.findAll();
        Doctor docGp = doctors.get(0);
        Doctor docCardio = doctors.get(1);
        Doctor docDerm = doctors.get(2);

        LocalDate today = LocalDate.now();

        // 2. Multi-tier recursive care chain for Adebayo Ogunlesi
        // Root visit: Initial clinical triage 5 days ago
        Appointment apptRoot = appointmentDao.save(AppointmentFactory.createStandardConsultation(
                p1.getId(), docGp.getId(), LocalDateTime.of(today.minusDays(5), java.time.LocalTime.of(9, 0)), "Acute Chest Discomfort Triage"));
        apptRoot.setStatus(AppointmentStatus.COMPLETED);
        appointmentDao.update(apptRoot);

        Invoice inv1 = billingService.generateInvoiceForAppointment(apptRoot.getId());
        inv1.addItem(new InvoiceItem(null, inv1.getId(), "Blood Glucose Panel (Campus Lab)", 1200.0));
        inv1.addItem(new InvoiceItem(null, inv1.getId(), "Sterile Wound Dressing", 450.0));
        billingService.settlePayment(inv1.getId(), PaymentMethod.MCB_JUICE, "JUICE-58421099-01");

        // Follow-up 1: Specialist cardiology evaluation 2 days ago
        Appointment apptFollow1 = appointmentDao.save(AppointmentFactory.createFollowUpVisit(
                p1.getId(), docCardio.getId(), LocalDateTime.of(today.minusDays(2), java.time.LocalTime.of(14, 0)), apptRoot.getId(), "Cardiology Referral & Resting ECG"));
        apptFollow1.setStatus(AppointmentStatus.COMPLETED);
        appointmentDao.update(apptFollow1);

        Invoice inv2 = billingService.generateInvoiceForAppointment(apptFollow1.getId());
        inv2.addItem(new InvoiceItem(null, inv2.getId(), "12-Lead Diagnostic ECG", 1800.0));

        // Follow-up 2: Connected visit on today's schedule at 10:00
        Appointment apptFollow2 = appointmentDao.save(AppointmentFactory.createFollowUpVisit(
                p1.getId(), docCardio.getId(), LocalDateTime.of(today, java.time.LocalTime.of(10, 0)), apptFollow1.getId(), "Post-ECG Holter Review"));
        apptFollow2.setStatus(AppointmentStatus.CONFIRMED);
        appointmentDao.update(apptFollow2);

        // 3. Populate today's timetable across other physicians
        // General practice appointment today at 09:00 (Completed)
        Appointment apptTodayGp1 = appointmentDao.save(AppointmentFactory.createStandardConsultation(
                p2.getId(), docGp.getId(), LocalDateTime.of(today, java.time.LocalTime.of(9, 0)), "Campus Pre-Sports Physical"));
        apptTodayGp1.setStatus(AppointmentStatus.COMPLETED);
        appointmentDao.update(apptTodayGp1);
        Invoice inv3 = billingService.generateInvoiceForAppointment(apptTodayGp1.getId());
        billingService.settlePayment(inv3.getId(), PaymentMethod.DEBIT_CARD, "AUTH-982144");

        // General practice appointment today at 11:00 (Confirmed)
        Appointment apptTodayGp2 = appointmentDao.save(AppointmentFactory.createStandardConsultation(
                p5.getId(), docGp.getId(), LocalDateTime.of(today, java.time.LocalTime.of(11, 0)), "Seasonal Allergy Assessment"));
        apptTodayGp2.setStatus(AppointmentStatus.CONFIRMED);
        appointmentDao.update(apptTodayGp2);

        // Dermatology appointment today at 14:00 (Confirmed)
        Appointment apptTodayDerm = appointmentDao.save(AppointmentFactory.createStandardConsultation(
                p4.getId(), docDerm.getId(), LocalDateTime.of(today, java.time.LocalTime.of(14, 0)), "Cryotherapy Lesion Follow-up"));
        apptTodayDerm.setStatus(AppointmentStatus.CONFIRMED);
        appointmentDao.update(apptTodayDerm);
    }

    private void loadCustomFonts() {
        String[] fontFiles = {
                "/fonts/Poppins-Regular.ttf",
                "/fonts/Poppins-Medium.ttf",
                "/fonts/Poppins-SemiBold.ttf",
                "/fonts/Poppins-Bold.ttf"
        };
        for (String file : fontFiles) {
            try (var is = getClass().getResourceAsStream(file)) {
                if (is != null) {
                    Font.loadFont(is, 13);
                }
            } catch (Exception e) {
                System.err.println("Note: Could not preload font " + file + ": " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
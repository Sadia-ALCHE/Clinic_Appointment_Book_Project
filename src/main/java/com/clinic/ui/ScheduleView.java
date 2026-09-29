package com.clinic.ui;

import com.clinic.dao.AppointmentDao;
import com.clinic.dao.DoctorDao;
import com.clinic.dao.PatientDao;
import com.clinic.factory.AppointmentFactory;
import com.clinic.model.*;
import com.clinic.service.BillingService;
import com.clinic.service.ScheduleValidator;
import com.clinic.service.ValidationResult;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Doctor Timetable and Appointment Booking Screen with Real-Time Conflict Alerts
public class ScheduleView implements View {

    private final AppointmentDao appointmentDao;
    private final DoctorDao doctorDao;
    private final PatientDao patientDao;
    private final ScheduleValidator validator;
    private final BillingService billingService;

    private final BorderPane root;
    private final ComboBox<Doctor> cbDoctor = new ComboBox<>();
    private final DatePicker dpDate = new DatePicker(LocalDate.now());
    private final VBox slotContainer = new VBox(10);
    private final VBox bookingDrawer = new VBox(12);

    // Booking Drawer Form Controls
    private final ComboBox<Patient> cbPatient = new ComboBox<>();
    private final ComboBox<AppointmentType> cbType = new ComboBox<>();
    private final ComboBox<Appointment> cbParentAppointment = new ComboBox<>();
    private final VBox followUpContainer = new VBox(4);
    private final Spinner<Integer> spinStartHour = new Spinner<>(8, 16, 9);
    private final Spinner<Integer> spinStartMinute = new Spinner<>(0, 45, 0, 15);
    private final Spinner<Integer> spinDuration = new Spinner<>(15, 120, 30, 15);
    private final TextField txtReason = new TextField();
    private final VBox conflictAlertBanner = new VBox(4);
    private final Label successBanner = new Label();
    private final PauseTransition successTimer = new PauseTransition(Duration.seconds(4));

    // Default constructor providing backward compatibility with composition root
    public ScheduleView() {
        this(new com.clinic.dao.sqlite.SqliteAppointmentDao(),
                new com.clinic.dao.sqlite.SqliteDoctorDao(),
                new com.clinic.dao.sqlite.SqlitePatientDao(),
                new com.clinic.service.ScheduleValidator(new com.clinic.dao.sqlite.SqliteAppointmentDao()),
                null);
    }

    public ScheduleView(AppointmentDao appointmentDao, DoctorDao doctorDao, PatientDao patientDao, ScheduleValidator validator) {
        this(appointmentDao, doctorDao, patientDao, validator, null);
    }

    public ScheduleView(AppointmentDao appointmentDao, DoctorDao doctorDao, PatientDao patientDao, ScheduleValidator validator, BillingService billingService) {
        this.appointmentDao = appointmentDao;
        this.doctorDao = doctorDao;
        this.patientDao = patientDao;
        this.validator = validator;
        this.billingService = billingService;
        this.root = new BorderPane();
        this.root.getStyleClass().add("view-container");

        buildUI();
        loadDoctors();
        loadPatients();
    }

    private void buildUI() {
        // 1. Header with Title and Filter Bar
        VBox topBox = new VBox(12);
        topBox.getStyleClass().add("view-header");

        BorderPane titleRow = new BorderPane();
        VBox titleBox = new VBox(2);
        Label title = new Label("Doctor Timetable & Appointments");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("View physician timetables, book appointments, and prevent double-booking.");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        titleRow.setLeft(titleBox);

        Button btnNewBooking = new Button("+ New Appointment");
        btnNewBooking.getStyleClass().add("btn-primary");
        btnNewBooking.setOnAction(e -> openBookingDrawer(null));
        titleRow.setRight(btnNewBooking);
        BorderPane.setAlignment(btnNewBooking, Pos.CENTER_RIGHT);

        // Doctor and Date Selection Bar
        HBox filterBar = new HBox(14);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12px 16px; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

        Label lblDoctor = new Label("Physician:");
        lblDoctor.setStyle("-fx-font-weight: bold;");
        cbDoctor.setPrefWidth(220);
        cbDoctor.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Doctor doc, boolean empty) {
                super.updateItem(doc, empty);
                setText(empty || doc == null ? "" : doc.getFullName() + " (" + doc.getSpecialty() + ")");
            }
        });
        cbDoctor.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Doctor doc, boolean empty) {
                super.updateItem(doc, empty);
                setText(empty || doc == null ? "Select Doctor" : doc.getFullName() + " (" + doc.getSpecialty() + ")");
            }
        });
        cbDoctor.setOnAction(e -> refreshTimetable());

        Label lblDate = new Label("Date:");
        lblDate.setStyle("-fx-font-weight: bold;");
        dpDate.setOnAction(e -> refreshTimetable());

        filterBar.getChildren().addAll(lblDoctor, cbDoctor, lblDate, dpDate);
        topBox.getChildren().addAll(titleRow, filterBar);
        root.setTop(topBox);

        // 2. Central Scrollable Timetable Matrix
        VBox centerCard = new VBox(12);
        centerCard.getStyleClass().add("view-card");

        successBanner.setStyle("-fx-background-color: transparent; -fx-text-fill: #059669; -fx-padding: 4px 0; -fx-font-size: 13px; -fx-font-weight: bold;");
        successBanner.setVisible(false);
        successBanner.setManaged(false);

        ScrollPane scrollPane = new ScrollPane(slotContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        centerCard.getChildren().addAll(successBanner, scrollPane);
        root.setCenter(centerCard);

        // 3. Slide-Over Appointment Booking Drawer
        buildBookingDrawer();
    }

    private void buildBookingDrawer() {
        bookingDrawer.getStyleClass().add("view-card");
        bookingDrawer.setStyle("-fx-background-color: #ffffff; -fx-pref-width: 340px; -fx-border-color: #cbd5e1; -fx-border-width: 0 0 0 1;");
        bookingDrawer.setPadding(new Insets(20));
        bookingDrawer.setVisible(false);
        bookingDrawer.setManaged(false);

        Label drawerTitle = new Label("Book Appointment");
        drawerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #0f172a;");

        // Real-Time Inline Conflict Alert Banner
        conflictAlertBanner.setStyle("-fx-background-color: transparent; -fx-padding: 4px 0;");
        conflictAlertBanner.setVisible(false);
        conflictAlertBanner.setManaged(false);

        // Patient ComboBox
        cbPatient.setMaxWidth(Double.MAX_VALUE);
        cbPatient.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getFullName() + " (" + p.getPhoneNumber() + ")");
            }
        });
        cbPatient.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "Select Patient" : p.getFullName());
            }
        });

        // Appointment Type ComboBox
        cbType.setItems(FXCollections.observableArrayList(AppointmentType.values()));
        cbType.setValue(AppointmentType.STANDARD_CONSULTATION);
        cbType.setMaxWidth(Double.MAX_VALUE);

        // Follow-Up Linking ComboBox
        cbParentAppointment.setMaxWidth(Double.MAX_VALUE);
        cbParentAppointment.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Appointment a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) {
                    setText("");
                } else {
                    setText(String.format("Visit #%d · %s (%s)", a.getId(), a.getAppointmentDateTime().toLocalDate(), a.getReason()));
                }
            }
        });
        cbParentAppointment.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Appointment a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) {
                    setText("Select Prior Visit to Follow Up");
                } else {
                    setText(String.format("Visit #%d · %s (%s)", a.getId(), a.getAppointmentDateTime().toLocalDate(), a.getReason()));
                }
            }
        });

        Label lblFollowUp = new Label("Prior Visit to Follow Up *");
        lblFollowUp.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        followUpContainer.getChildren().addAll(lblFollowUp, cbParentAppointment);
        followUpContainer.setVisible(false);
        followUpContainer.setManaged(false);

        cbType.valueProperty().addListener((obs, oldVal, newVal) -> updateFollowUpOptions());
        cbPatient.valueProperty().addListener((obs, oldVal, newVal) -> updateFollowUpOptions());

        // Time Pickers
        HBox timeBox = new HBox(8, new Label("Hour:"), spinStartHour, new Label("Min:"), spinStartMinute);
        timeBox.setAlignment(Pos.CENTER_LEFT);

        txtReason.setPromptText("e.g. Routine general consultation");

        Button btnConfirm = new Button("Confirm Booking");
        btnConfirm.getStyleClass().add("btn-primary");
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setOnAction(e -> handleConfirmBooking());

        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setOnAction(e -> toggleBookingDrawer(false));

        bookingDrawer.getChildren().addAll(
                drawerTitle, conflictAlertBanner,
                new Label("Patient *"), cbPatient,
                new Label("Consultation Type *"), cbType,
                followUpContainer,
                new Label("Start Time *"), timeBox,
                new Label("Duration (minutes) *"), spinDuration,
                new Label("Clinical Reason"), txtReason,
                btnConfirm, btnCancel
        );
        root.setRight(bookingDrawer);
    }

    public void refreshTimetable() {
        hideSuccessBanner();
        slotContainer.getChildren().clear();
        Doctor selectedDoctor = cbDoctor.getValue();
        LocalDate selectedDate = dpDate.getValue();

        if (selectedDoctor == null || selectedDate == null) {
            Label prompt = new Label("Please select a physician and date above to display timetable slots.");
            prompt.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 20px;");
            slotContainer.getChildren().add(prompt);
            return;
        }

        List<Appointment> existingAppointments = new ArrayList<>();
        try {
            existingAppointments = appointmentDao.findByDate(selectedDate).stream()
                    .filter(a -> Objects.equals(a.getDoctorId(), selectedDoctor.getId()))
                    .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                    .toList();
        } catch (Exception ex) {
            System.err.println("Error fetching doctor appointments: " + ex.getMessage());
        }

        // Generate hourly slots from 08:00 to 17:00
        for (int hour = 8; hour < 17; hour++) {
            LocalTime slotStart = LocalTime.of(hour, 0);
            LocalTime slotEnd = LocalTime.of(hour + 1, 0);
            Appointment coveringAppt = findCoveringAppointment(existingAppointments, slotStart, slotEnd);

            HBox slotRow = new HBox(16);
            slotRow.setAlignment(Pos.CENTER_LEFT);
            slotRow.setStyle("-fx-padding: 10px 14px; -fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

            Label timeLabel = new Label(String.format("%02d:00 - %02d:00", hour, hour + 1));
            timeLabel.setStyle("-fx-font-weight: bold; -fx-pref-width: 110px; -fx-text-fill: #334155;");

            if (coveringAppt != null) {
                final Appointment appt = coveringAppt;
                boolean isCompleted = appt.getStatus() == AppointmentStatus.COMPLETED;

                slotRow.setStyle("-fx-padding: 10px 14px; -fx-background-color: #ffffff; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

                Label badge = new Label(isCompleted ? "COMPLETED" : "OCCUPIED");
                badge.getStyleClass().addAll("status-badge", isCompleted ? "badge-confirmed" : "badge-cancelled");

                String patientName = resolvePatientName(appt.getPatientId());
                Label desc = new Label(patientName + " · " + appt.getType() + " (" + appt.getReason() + ")");
                desc.setStyle(isCompleted ? "-fx-text-fill: #166534; -fx-font-weight: 500;" : "-fx-text-fill: #334155; -fx-font-weight: 500;");
                HBox.setHgrow(desc, Priority.ALWAYS);

                HBox actions = new HBox(8);
                actions.setAlignment(Pos.CENTER_RIGHT);

                if (!isCompleted) {
                    Button btnComplete = new Button("Complete Visit");
                    btnComplete.setStyle("-fx-background-color: #059669; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 4; -fx-cursor: hand;");
                    btnComplete.setOnAction(e -> handleCompleteAppointment(appt));

                    Button btnCancelAppt = new Button("Cancel Slot");
                    btnCancelAppt.setStyle("-fx-background-color: transparent; -fx-text-fill: #dc2626; -fx-border-color: #fca5a5; -fx-border-radius: 4; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 9; -fx-cursor: hand;");
                    btnCancelAppt.setOnAction(e -> handleCancelAppointment(appt));

                    actions.getChildren().addAll(btnComplete, btnCancelAppt);
                } else {
                    Label billedBadge = new Label("Billed ✓");
                    billedBadge.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-font-size: 11px;");
                    actions.getChildren().add(billedBadge);
                }

                slotRow.getChildren().addAll(timeLabel, badge, desc, actions);
            } else {
                // Available Slot
                Label badge = new Label("AVAILABLE");
                badge.getStyleClass().addAll("status-badge", "badge-confirmed");

                Label desc = new Label("No appointment scheduled");
                desc.setStyle("-fx-text-fill: #64748b;");
                HBox.setHgrow(desc, Priority.ALWAYS);

                final int h = hour;
                Button btnBookSlot = new Button("Book Slot");
                btnBookSlot.getStyleClass().add("btn-secondary");
                btnBookSlot.setOnAction(e -> openBookingDrawer(LocalTime.of(h, 0)));

                slotRow.getChildren().addAll(timeLabel, badge, desc, btnBookSlot);
            }
            slotContainer.getChildren().add(slotRow);
        }
    }

    private Appointment findCoveringAppointment(List<Appointment> appts, LocalTime start, LocalTime end) {
        for (Appointment a : appts) {
            LocalTime aStart = a.getAppointmentDateTime().toLocalTime();
            LocalTime aEnd = aStart.plusMinutes(ScheduleValidator.DEFAULT_DURATION_MINUTES);
            if (aStart.isBefore(end) && start.isBefore(aEnd)) {
                return a;
            }
        }
        return null;
    }

    private String resolvePatientName(Long patientId) {
        if (patientId == null) return "Patient #?";
        try {
            return patientDao.findById(patientId)
                    .map(Patient::getFullName)
                    .orElse("Patient #" + patientId);
        } catch (Exception e) {
            return "Patient #" + patientId;
        }
    }

    private void openBookingDrawer(LocalTime initialTime) {
        hideSuccessBanner();
        if (initialTime != null) {
            spinStartHour.getValueFactory().setValue(initialTime.getHour());
            spinStartMinute.getValueFactory().setValue(initialTime.getMinute());
        }
        updateFollowUpOptions();
        conflictAlertBanner.getChildren().clear();
        conflictAlertBanner.setVisible(false);
        conflictAlertBanner.setManaged(false);
        toggleBookingDrawer(true);
    }

    private void updateFollowUpOptions() {
        AppointmentType type = cbType.getValue();
        Patient patient = cbPatient.getValue();
        if (type == AppointmentType.FOLLOW_UP && patient != null && patient.getId() != null) {
            try {
                List<Appointment> patientVisits = appointmentDao.findByPatientId(patient.getId()).stream()
                        .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                        .toList();
                cbParentAppointment.setItems(FXCollections.observableArrayList(patientVisits));
                if (!patientVisits.isEmpty()) {
                    cbParentAppointment.setValue(patientVisits.get(0));
                } else {
                    cbParentAppointment.setValue(null);
                }
                followUpContainer.setVisible(true);
                followUpContainer.setManaged(true);
            } catch (Exception e) {
                System.err.println("Error fetching patient appointments for follow-up: " + e.getMessage());
            }
        } else {
            followUpContainer.setVisible(false);
            followUpContainer.setManaged(false);
            cbParentAppointment.setValue(null);
        }
    }

    private void toggleBookingDrawer(boolean show) {
        bookingDrawer.setVisible(show);
        bookingDrawer.setManaged(show);
    }

    private void handleConfirmBooking() {
        conflictAlertBanner.getChildren().clear();
        conflictAlertBanner.setVisible(false);
        conflictAlertBanner.setManaged(false);

        Doctor doctor = cbDoctor.getValue();
        Patient patient = cbPatient.getValue();
        LocalDate date = dpDate.getValue();
        AppointmentType type = cbType.getValue();

        if (doctor == null) {
            showConflictError("Please select a physician from the top filter bar");
            return;
        }
        if (patient == null) {
            showConflictError("Please select a patient for this appointment");
            return;
        }
        if (date == null) {
            showConflictError("Please select an appointment date");
            return;
        }

        LocalTime startTime = LocalTime.of(spinStartHour.getValue(), spinStartMinute.getValue());
        int durationMins = spinDuration.getValue();
        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        String reason = txtReason.getText() != null && !txtReason.getText().trim().isEmpty()
                ? txtReason.getText().trim()
                : "Consultation";

        Long parentId = null;
        if (type == AppointmentType.FOLLOW_UP) {
            if (cbParentAppointment.getValue() != null) {
                parentId = cbParentAppointment.getValue().getId();
            } else {
                try {
                    List<Appointment> patientVisits = appointmentDao.findByPatientId(patient.getId()).stream()
                            .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                            .toList();
                    if (!patientVisits.isEmpty()) {
                        parentId = patientVisits.get(0).getId();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // Construct Candidate Appointment via AppointmentFactory to enforce domain invariants
        Appointment candidate;
        if (type == AppointmentType.FOLLOW_UP) {
            if (parentId == null) {
                showConflictError("Please select a prior consultation to link this follow-up appointment");
                return;
            }
            candidate = AppointmentFactory.createFollowUpVisit(
                    patient.getId(), doctor.getId(), startDateTime, parentId, reason);
            candidate.setStatus(AppointmentStatus.CONFIRMED);
        } else if (type == AppointmentType.EMERGENCY) {
            candidate = AppointmentFactory.createEmergencyCheckup(
                    patient.getId(), doctor.getId(), startDateTime, reason);
        } else {
            candidate = AppointmentFactory.createStandardConsultation(
                    patient.getId(), doctor.getId(), startDateTime, reason);
            candidate.setStatus(AppointmentStatus.CONFIRMED);
        }

        // Execute ScheduleValidator conflict engine
        ValidationResult result = validator.validateBooking(candidate, durationMins);
        if (!result.isValid()) {
            showConflictError(result.getErrorMessage());
            return;
        }

        // Persist confirmed appointment to SQLite
        try {
            Appointment saved = appointmentDao.save(candidate);
            toggleBookingDrawer(false);
            refreshTimetable();
            showSuccess("Appointment booked for " + patient.getFullName() + " at " + startTime + ".");
        } catch (Exception ex) {
            showConflictError("Could not save appointment: " + ex.getMessage());
        }
    }

    private void showConflictError(String msg) {
        Label err = new Label(msg);
        err.setStyle("-fx-text-fill: #991b1b; -fx-font-weight: bold; -fx-font-size: 11px;");
        conflictAlertBanner.getChildren().add(err);
        conflictAlertBanner.setVisible(true);
        conflictAlertBanner.setManaged(true);
    }

    private void showSuccess(String msg) {
        successBanner.setText(msg);
        successBanner.setVisible(true);
        successBanner.setManaged(true);
        successTimer.setOnFinished(e -> hideSuccessBanner());
        successTimer.playFromStart();
    }

    private void hideSuccessBanner() {
        successBanner.setVisible(false);
        successBanner.setManaged(false);
    }

    private void handleCompleteAppointment(Appointment appt) {
        if (appt == null || appt.getId() == null) return;
        try {
            appt.setStatus(AppointmentStatus.COMPLETED);
            appointmentDao.update(appt);
            if (billingService != null) {
                billingService.generateInvoiceForAppointment(appt.getId());
            }
            refreshTimetable();
            showSuccess("Consultation completed. Bill generated and ready in Invoices & Billing.");
        } catch (Exception ex) {
            showConflictError("Error completing appointment: " + ex.getMessage());
        }
    }

    private void handleCancelAppointment(Appointment appt) {
        if (appt == null || appt.getId() == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Cancellation");
        confirm.setHeaderText("Cancel appointment for " + resolvePatientName(appt.getPatientId()) + "?");
        confirm.setContentText("This will cancel the booking and reopen the time slot on the doctor's schedule.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                appt.setStatus(AppointmentStatus.CANCELLED);
                appointmentDao.update(appt);
                refreshTimetable();
                showSuccess("Appointment cancelled. Timetable slot is now open.");
            } catch (Exception ex) {
                showConflictError("Error cancelling appointment: " + ex.getMessage());
            }
        }
    }

    private void loadDoctors() {
        try {
            List<Doctor> docs = doctorDao.findAll();
            cbDoctor.setItems(FXCollections.observableArrayList(docs));
            if (!docs.isEmpty()) {
                cbDoctor.setValue(docs.get(0));
                refreshTimetable();
            }
        } catch (Exception ex) {
            System.err.println("Error loading doctors: " + ex.getMessage());
        }
    }

    private void loadPatients() {
        try {
            List<Patient> patients = patientDao.findAll();
            cbPatient.setItems(FXCollections.observableArrayList(patients));
        } catch (Exception ex) {
            System.err.println("Error loading patients into schedule: " + ex.getMessage());
        }
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public void onShow() {
        loadDoctors();
        loadPatients();
        refreshTimetable();
    }
}
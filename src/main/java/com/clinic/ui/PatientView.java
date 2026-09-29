package com.clinic.ui;

import com.clinic.dao.PatientDao;
import com.clinic.model.Patient;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Patient Directory and Medical Records Screen with Live Search
public class PatientView implements View {

    private final PatientDao patientDao;
    private final BorderPane root;
    private final ObservableList<Patient> patientList = FXCollections.observableArrayList();
    private final FilteredList<Patient> filteredPatients;
    private final TableView<Patient> table = new TableView<>();
    private final TextField searchField = new TextField();
    private final VBox formDrawer = new VBox(12);

    // Form input controls
    private final TextField txtName = new TextField();
    private final TextField txtPhone = new TextField();
    private final TextField txtEmail = new TextField();
    private final DatePicker dpDob = new DatePicker(LocalDate.of(1990, 1, 1));
    private final ComboBox<String> cbBloodGroup = new ComboBox<>();
    private final TextArea txtNotes = new TextArea();
    private final VBox alertBanner = new VBox(4);

    public PatientView() {
        this(new com.clinic.dao.sqlite.SqlitePatientDao());
    }
    public PatientView(PatientDao patientDao) {
        this.patientDao = patientDao;
        this.root = new BorderPane();
        this.root.getStyleClass().add("view-container");

        // Wrap master list in reactive FilteredList
        this.filteredPatients = new FilteredList<>(patientList, p -> true);

        buildUI();
        loadPatients();
    }

    private void buildUI() {
        // 1. Header with Title and 'Register Patient' Button
        BorderPane headerBar = new BorderPane();
        headerBar.getStyleClass().add("view-header");

        VBox titleBox = new VBox(2);
        Label title = new Label("Patient Directory");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Manage patient registrations, contact details, and records");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        headerBar.setLeft(titleBox);

        Button btnAddPatient = new Button("+ Register Patient");
        btnAddPatient.getStyleClass().add("btn-primary");
        btnAddPatient.setOnAction(e -> toggleFormDrawer(true));
        headerBar.setRight(btnAddPatient);
        BorderPane.setAlignment(btnAddPatient, Pos.CENTER_RIGHT);
        root.setTop(headerBar);

        // 2. Main Content Card with Search Bar and TableView
        VBox card = new VBox(14);
        card.getStyleClass().add("view-card");

        // Search Box
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        Label searchLabel = new Label("Search Patients:");
        searchLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        searchField.setPromptText("Type patient full name to filter in real time...");
        searchField.setPrefWidth(350);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredPatients.setPredicate(patient -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                return patient.getFullName().toLowerCase().contains(newVal.trim().toLowerCase());
            });
        });
        searchBox.getChildren().addAll(searchLabel, searchField);

        // Configure TableView Columns
        TableColumn<Patient, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colId.setPrefWidth(50);

        TableColumn<Patient, String> colName = new TableColumn<>("FULL NAME");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colName.setPrefWidth(180);

        TableColumn<Patient, String> colPhone = new TableColumn<>("PHONE");
        colPhone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhoneNumber()));
        colPhone.setPrefWidth(130);

        TableColumn<Patient, String> colEmail = new TableColumn<>("EMAIL");
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail() != null ? d.getValue().getEmail() : "-"));
        colEmail.setPrefWidth(170);

        TableColumn<Patient, String> colDob = new TableColumn<>("DATE OF BIRTH");
        colDob.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDateOfBirth() != null ? d.getValue().getDateOfBirth().toString() : "-"));
        colDob.setPrefWidth(110);

        TableColumn<Patient, String> colBlood = new TableColumn<>("BLOOD");
        colBlood.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBloodGroup() != null ? d.getValue().getBloodGroup() : "-"));
        colBlood.setPrefWidth(70);

        TableColumn<Patient, Void> colActions = new TableColumn<>("ACTION");
        colActions.setPrefWidth(85);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");
            {
                btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #dc2626; -fx-border-color: #fca5a5; -fx-border-radius: 4; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 2 8; -fx-cursor: hand;");
                btnDelete.setOnAction(e -> {
                    Patient p = getTableView().getItems().get(getIndex());
                    handleDeletePatient(p);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });

        table.getColumns().addAll(colId, colName, colPhone, colEmail, colDob, colBlood, colActions);
        table.setItems(filteredPatients);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        card.getChildren().addAll(searchBox, table);
        root.setCenter(card);

        // 3. Build Slide-over Registration Drawer
        buildFormDrawer();
    }

    private void buildFormDrawer() {
        formDrawer.getStyleClass().add("view-card");
        formDrawer.setStyle("-fx-background-color: #ffffff; -fx-pref-width: 320px; -fx-border-color: #cbd5e1; -fx-border-width: 0 0 0 1;");
        formDrawer.setPadding(new Insets(20));
        formDrawer.setVisible(false);
        formDrawer.setManaged(false);

        Label drawerTitle = new Label("Register New Patient");
        drawerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #0f172a;");

        // Inline Alert Banner
        alertBanner.setStyle("-fx-background-color: transparent; -fx-padding: 4px 0;");
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);

        cbBloodGroup.setItems(FXCollections.observableArrayList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        cbBloodGroup.setValue("O+");
        cbBloodGroup.setMaxWidth(Double.MAX_VALUE);

        txtName.setPromptText("e.g. Marie Claire Dupont");
        txtPhone.setPromptText("e.g. +230 5123 4567");
        txtEmail.setPromptText("e.g. marie@dupont.mu");
        txtNotes.setPromptText("Medical background, allergies...");
        txtNotes.setPrefRowCount(3);

        Button btnSave = new Button("Save Patient");
        btnSave.getStyleClass().add("btn-primary");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setOnAction(e -> handleSavePatient());

        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.setOnAction(e -> toggleFormDrawer(false));

        formDrawer.getChildren().addAll(
                drawerTitle, alertBanner,
                new Label("Full Name *"), txtName,
                new Label("Phone Number *"), txtPhone,
                new Label("Email Address"), txtEmail,
                new Label("Date of Birth"), dpDob,
                new Label("Blood Group *"), cbBloodGroup,
                new Label("Clinical Notes"), txtNotes,
                btnSave, btnCancel
        );
        root.setRight(formDrawer);
    }

    private void toggleFormDrawer(boolean show) {
        formDrawer.setVisible(show);
        formDrawer.setManaged(show);
        if (show) {
            txtName.requestFocus();
        } else {
            clearForm();
        }
    }

    private void handleSavePatient() {
        alertBanner.getChildren().clear();
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);

        String name = txtName.getText() != null ? txtName.getText().trim() : "";
        String phone = txtPhone.getText() != null ? txtPhone.getText().trim() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        LocalDate dob = dpDob.getValue();
        String blood = cbBloodGroup.getValue() != null ? cbBloodGroup.getValue() : "O+";
        String notes = txtNotes.getText() != null ? txtNotes.getText().trim() : "";

        // Validation guards using Patient static rules
        try {
            Patient.validatePhone(phone);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        // Validate custom email if provided
        if (!email.isEmpty()) {
            try {
                Patient.validateEmail(email);
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
                return;
            }
        }

        try {
            Patient newPatient = new Patient((Long) null, name, email, phone, dob != null ? dob : LocalDate.of(1990, 1, 1), blood);
            Patient created = patientDao.save(newPatient);
            patientList.add(created);
            toggleFormDrawer(false);
        } catch (Exception ex) {
            showError("Could not save patient: " + ex.getMessage());
        }
    }

    private void handleDeletePatient(Patient patient) {
        if (patient == null || patient.getId() == null) return;
        alertBanner.getChildren().clear();
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Patient Deletion");
        confirm.setHeaderText("Delete patient: " + patient.getFullName() + "?");
        confirm.setContentText("This will permanently remove the patient record from the database.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = patientDao.deleteById(patient.getId());
                if (deleted) {
                    patientList.remove(patient);
                } else {
                    showDeleteBlockedModal(patient, "Patient record #" + patient.getId() + " could not be removed from the database.");
                }
            } catch (Exception ex) {
                showDeleteBlockedModal(patient, "Patient " + patient.getFullName() + " has active clinical appointments or billing records on file.\n\nTo preserve medical history and financial audit compliance, patients with existing appointments or invoices cannot be deleted.");
            }
        }
    }

    // Modal dialog informing user why parent patient record deletion was prevented
    private void showDeleteBlockedModal(Patient patient, String reason) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Cannot Delete Patient");
        alert.setHeaderText("Deletion Blocked: " + patient.getFullName());
        alert.setContentText(reason);
        if (root.getScene() != null && !root.getScene().getStylesheets().isEmpty()) {
            alert.getDialogPane().getStylesheets().addAll(root.getScene().getStylesheets());
        }
        alert.showAndWait();
    }

    private void showError(String msg) {
        Label err = new Label(msg);
        err.setStyle("-fx-text-fill: #991b1b; -fx-font-weight: bold; -fx-font-size: 11px;");
        alertBanner.getChildren().add(err);
        alertBanner.setVisible(true);
        alertBanner.setManaged(true);
    }

    private void clearForm() {
        txtName.clear();
        txtPhone.clear();
        txtEmail.clear();
        dpDob.setValue(LocalDate.of(1990, 1, 1));
        cbBloodGroup.setValue("O+");
        txtNotes.clear();
        alertBanner.getChildren().clear();
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);
    }

    private void loadPatients() {
        try {
            List<Patient> patients = patientDao.findAll();
            patientList.setAll(patients);
        } catch (Exception ex) {
            System.err.println("Error loading patients: " + ex.getMessage());
        }
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public void onShow() {
        loadPatients();
    }
}
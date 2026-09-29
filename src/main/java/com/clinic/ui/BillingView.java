package com.clinic.ui;

import com.clinic.dao.AppointmentDao;
import com.clinic.dao.DoctorDao;
import com.clinic.dao.PatientDao;
import com.clinic.model.*;
import com.clinic.service.AppointmentService;
import com.clinic.service.BillingService;
import com.clinic.service.CareChainSummary;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

// Complete Master-Detail Billing Ledger and Financial Rollup View
// Directly implements Paper Board 07 (GW-0) with live status filters and care chain metrics
public class BillingView implements View {

    private final BillingService billingService;
    private final PatientDao patientDao;
    private final AppointmentDao appointmentDao;
    private final DoctorDao doctorDao;
    private final AppointmentService appointmentService;

    private final BorderPane root;
    private final ObservableList<Invoice> invoiceObservableList;
    private TableView<Invoice> masterTable;

    // Metric card labels for dynamic recomputation
    private Label totalBilledValue;
    private Label collectedRevenueValue;
    private Label pendingReceivablesValue;
    private Label settledCountValue;

    // Detail Panel Controls
    private VBox detailCard;
    private Label detailTitle;
    private Label detailDate;
    private Label detailStatusBadge;
    private Label detailPatientName;
    private Label detailDoctorName;
    private HBox careChainBanner;
    private Label careChainText;
    private TableView<InvoiceItem> itemsTable;
    private Label subtotalLbl;
    private Label totalPayableLbl;
    private Button recordPaymentBtn;
    private Button printReceiptBtn;

    public BillingView(BillingService billingService, PatientDao patientDao,
                       AppointmentDao appointmentDao, DoctorDao doctorDao,
                       AppointmentService appointmentService) {
        this.billingService = billingService;
        this.patientDao = patientDao;
        this.appointmentDao = appointmentDao;
        this.doctorDao = doctorDao;
        this.appointmentService = appointmentService;

        this.root = new BorderPane();
        this.root.getStyleClass().add("view-container");
        this.invoiceObservableList = FXCollections.observableArrayList();

        buildUi();
        refreshLedger();
    }

    // Default constructor fallback for backwards compatibility
    public BillingView() {
        this(null, null, null, null, null);
    }

    private void buildUi() {
        if (billingService == null) {
            Label placeholder = new Label("Billing View · Dependencies Not Injected");
            root.setCenter(placeholder);
            return;
        }

        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(24));

        // 1. Header Section
        VBox headerBox = new VBox(4);
        Label title = new Label("Invoices & Billing Ledger");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label subtitle = new Label("Review patient consultation bills, record payments in MUR, and track connected treatment plans.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        headerBox.getChildren().addAll(title, subtitle);

        // 2. Metric Summary Cards Bar
        HBox metricBar = buildMetricCardsBar();

        // 3. Search and Filter Bar
        HBox filterBar = buildFilterBar();

        // 4. Master-Detail Split Container
        HBox masterDetailBox = new HBox(20);
        VBox.setVgrow(masterDetailBox, Priority.ALWAYS);

        // Left Master Table (Width: 55%)
        VBox masterContainer = buildMasterTableContainer();
        masterContainer.setPrefWidth(560);
        HBox.setHgrow(masterContainer, Priority.ALWAYS);

        // Right Detail Pane (Width: 45%)
        detailCard = buildDetailPaneContainer();
        detailCard.setPrefWidth(460);
        detailCard.setMinWidth(420);

        masterDetailBox.getChildren().addAll(masterContainer, detailCard);

        contentBox.getChildren().addAll(headerBox, metricBar, filterBar, masterDetailBox);
        root.setCenter(contentBox);

        // Wire Master Table Selection Listener
        masterTable.getSelectionModel().selectedItemProperty().addListener((obs, oldInv, newInv) -> displayInvoiceDetails(newInv));
    }

    private HBox buildMetricCardsBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);

        String moneyGreen = "#059669"; // Unified professional clinical green

        VBox card1 = createMetricCard("TOTAL INVOICED (MUR)", totalBilledValue = new Label("MUR 0.00"), moneyGreen);
        VBox card2 = createMetricCard("COLLECTED PAYMENTS", collectedRevenueValue = new Label("MUR 0.00"), moneyGreen);
        VBox card3 = createMetricCard("PENDING PAYMENTS", pendingReceivablesValue = new Label("MUR 0.00"), moneyGreen);
        VBox card4 = createMetricCard("PAID INVOICES", settledCountValue = new Label("0 / 0"), moneyGreen);

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        bar.getChildren().addAll(card1, card2, card3, card4);
        return bar;
    }

    private VBox createMetricCard(String title, Label valLabel, String accentColor) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10px; -fx-border-color: #e2e8f0; -fx-border-radius: 10px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        valLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");

        card.getChildren().addAll(titleLabel, valLabel);
        return card;
    }

    private HBox buildFilterBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search by invoice # or patient name...");
        searchField.setPrefWidth(280);
        searchField.setStyle("-fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #cbd5e1; -fx-border-radius: 6;");

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All Invoices", "Pending Payment", "Paid in Full");
        statusFilter.setValue("All Invoices");
        statusFilter.setStyle("-fx-padding: 6 10; -fx-background-radius: 6;");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(statusFilter.getValue(), newVal));
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal, searchField.getText()));

        bar.getChildren().addAll(searchField, statusFilter);
        return bar;
    }

    private void applyFilter(String statusOption, String searchText) {
        String filter = "ALL";
        if (statusOption != null && statusOption.toLowerCase().contains("pending")) filter = "PENDING";
        else if (statusOption != null && statusOption.toLowerCase().contains("paid")) filter = "PAID";

        List<Invoice> filtered = billingService.filterInvoices(filter, searchText);
        invoiceObservableList.setAll(filtered);
    }

    private VBox buildMasterTableContainer() {
        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10px; -fx-border-color: #e2e8f0; -fx-border-radius: 10px; -fx-padding: 16;");
        VBox.setVgrow(box, Priority.ALWAYS);

        masterTable = new TableView<>();
        masterTable.setItems(invoiceObservableList);
        masterTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(masterTable, Priority.ALWAYS);

        TableColumn<Invoice, String> colNum = new TableColumn<>("Invoice #");
        colNum.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInvoiceNumber()));
        colNum.setPrefWidth(110);

        TableColumn<Invoice, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getIssueDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        ));
        colDate.setPrefWidth(90);

        TableColumn<Invoice, String> colPatient = new TableColumn<>("Patient");
        colPatient.setCellValueFactory(data -> {
            Optional<Appointment> appt = appointmentDao.findById(data.getValue().getAppointmentId());
            if (appt.isPresent()) {
                Optional<Patient> p = patientDao.findById(appt.get().getPatientId());
                if (p.isPresent()) return new SimpleStringProperty(p.get().getFullName());
            }
            return new SimpleStringProperty("Unknown Patient");
        });
        colPatient.setPrefWidth(140);

        TableColumn<Invoice, String> colTotal = new TableColumn<>("Total (MUR)");
        colTotal.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("MUR %,.2f", data.getValue().calculateTotalMur())
        ));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold; -fx-text-fill: #059669;");
                }
            }
        });
        colTotal.setPrefWidth(110);

        TableColumn<Invoice, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: transparent;");
                    if (item.contains("Paid")) {
                        badge.setStyle(badge.getStyle() + " -fx-text-fill: #059669;");
                    } else {
                        badge.setStyle(badge.getStyle() + " -fx-text-fill: #d97706;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        colStatus.setPrefWidth(110);

        masterTable.getColumns().addAll(colNum, colDate, colPatient, colTotal, colStatus);
        box.getChildren().add(masterTable);
        return box;
    }

    private VBox buildDetailPaneContainer() {
        VBox box = new VBox(14);
        box.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10px; -fx-border-color: #e2e8f0; -fx-border-radius: 10px; -fx-padding: 20;");
        VBox.setVgrow(box, Priority.ALWAYS);

        // Header Line
        HBox headerLine = new HBox(12);
        headerLine.setAlignment(Pos.CENTER_LEFT);

        detailTitle = new Label("Select Invoice");
        detailTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        HBox.setHgrow(detailTitle, Priority.ALWAYS);

        detailStatusBadge = new Label();
        detailStatusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: transparent;");
        detailStatusBadge.setVisible(false);

        headerLine.getChildren().addAll(detailTitle, detailStatusBadge);

        detailDate = new Label();
        detailDate.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        // Patient & Doctor Box
        VBox participantBox = new VBox(4);
        participantBox.setPadding(new Insets(10));
        participantBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 6px;");

        detailPatientName = new Label("Patient: --");
        detailPatientName.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");

        detailDoctorName = new Label("Physician: --");
        detailDoctorName.setStyle("-fx-text-fill: #475569;");

        participantBox.getChildren().addAll(detailPatientName, detailDoctorName);

        // Care Chain Banner (shown if part of recursive follow-up treatment plan)
        careChainBanner = new HBox(8);
        careChainBanner.setAlignment(Pos.CENTER_LEFT);
        careChainBanner.setPadding(new Insets(4, 0, 4, 0));
        careChainBanner.setStyle("-fx-background-color: transparent;");
        careChainBanner.setVisible(false);
        careChainBanner.setManaged(false);

        careChainText = new Label();
        careChainText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #059669;");
        careChainBanner.getChildren().add(careChainText);

        // Items Table
        Label itemsHeader = new Label("Itemized Charges");
        itemsHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");

        itemsTable = new TableView<>();
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        itemsTable.setPrefHeight(160);

        TableColumn<InvoiceItem, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));

        TableColumn<InvoiceItem, String> colAmt = new TableColumn<>("Amount (MUR)");
        colAmt.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("MUR %,.2f", data.getValue().getAmountMur())
        ));
        colAmt.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: 600; -fx-text-fill: #059669;");
                }
            }
        });
        colAmt.setPrefWidth(120);

        itemsTable.getColumns().addAll(colDesc, colAmt);

        // Financial Summary Card
        VBox summaryCard = new VBox(6);
        summaryCard.setPadding(new Insets(12));
        summaryCard.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 6px;");

        subtotalLbl = new Label("Subtotal: MUR 0.00");
        subtotalLbl.setStyle("-fx-text-fill: #475569;");

        Label subsidyLbl = new Label("Student Health Subsidy (0% VAT): MUR 0.00");
        subsidyLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        totalPayableLbl = new Label("Total Payable: MUR 0.00");
        totalPayableLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #059669;");

        summaryCard.getChildren().addAll(subtotalLbl, subsidyLbl, totalPayableLbl);

        // Actions HBox
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        recordPaymentBtn = new Button("Record Payment");
        recordPaymentBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6;");
        recordPaymentBtn.setDisable(true);
        recordPaymentBtn.setOnAction(e -> handleRecordPayment());

        printReceiptBtn = new Button("Print Receipt (PDF)");
        printReceiptBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6;");
        printReceiptBtn.setDisable(true);
        printReceiptBtn.setOnAction(e -> handlePrintReceipt());

        actionsBox.getChildren().addAll(printReceiptBtn, recordPaymentBtn);

        box.getChildren().addAll(headerLine, detailDate, participantBox, careChainBanner, itemsHeader, itemsTable, summaryCard, actionsBox);
        return box;
    }

    private void displayInvoiceDetails(Invoice invoice) {
        if (invoice == null) {
            detailTitle.setText("Select Invoice");
            detailDate.setText("");
            detailStatusBadge.setVisible(false);
            detailPatientName.setText("Patient: --");
            detailDoctorName.setText("Physician: --");
            careChainBanner.setVisible(false);
            careChainBanner.setManaged(false);
            itemsTable.getItems().clear();
            subtotalLbl.setText("Subtotal: MUR 0.00");
            totalPayableLbl.setText("Total Payable: MUR 0.00");
            recordPaymentBtn.setDisable(true);
            printReceiptBtn.setDisable(true);
            return;
        }

        detailTitle.setText("Invoice " + invoice.getInvoiceNumber());
        detailDate.setText("Issue Date: " + invoice.getIssueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));

        detailStatusBadge.setText(invoice.getStatus().getDisplayName());
        detailStatusBadge.setVisible(true);
        if (invoice.getStatus() == PaymentStatus.PAID) {
            detailStatusBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: #059669;");
        } else {
            detailStatusBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-color: transparent; -fx-text-fill: #d97706;");
        }

        // Retrieve Patient & Doctor
        String patName = "Unknown Patient";
        String docName = "Unknown Physician";
        Optional<Appointment> apptOpt = appointmentDao.findById(invoice.getAppointmentId());
        if (apptOpt.isPresent()) {
            Appointment appt = apptOpt.get();
            Optional<Patient> p = patientDao.findById(appt.getPatientId());
            if (p.isPresent()) patName = p.get().getFullName();

            Optional<Doctor> d = doctorDao.findById(appt.getDoctorId());
            if (d.isPresent()) docName = d.get().getFullName() + " (" + d.get().getSpecialty() + ")";

            // Check Care Chain Rollup
            if (appt.getParentAppointmentId() != null || !appointmentDao.findByParentAppointmentId(appt.getId()).isEmpty()) {
                CareChainSummary summary = billingService.getCareChainFinancialRollup(appt.getId());
                careChainText.setText(String.format("Treatment Plan: %d Connected Visits · Total Course Cost: MUR %,.2f",
                        summary.getTotalVisits(), summary.getTotalCostMur()));
                careChainBanner.setVisible(true);
                careChainBanner.setManaged(true);
            } else {
                careChainBanner.setVisible(false);
                careChainBanner.setManaged(false);
            }
        }

        detailPatientName.setText("Patient: " + patName);
        detailDoctorName.setText("Physician: " + docName);

        // Populate items
        itemsTable.getItems().setAll(invoice.getItems());

        double totalMur = invoice.calculateTotalMur();
        subtotalLbl.setText(String.format("Subtotal: MUR %,.2f", totalMur));
        totalPayableLbl.setText(String.format("Total Payable: MUR %,.2f", totalMur));

        // Enable buttons based on state
        recordPaymentBtn.setDisable(invoice.getStatus() != PaymentStatus.PENDING);
        printReceiptBtn.setDisable(invoice.getStatus() != PaymentStatus.PAID);
    }

    private void handleRecordPayment() {
        Invoice selected = masterTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Stage parentStage = (Stage) root.getScene().getWindow();
        String patientName = detailPatientName.getText().replace("Patient: ", "");

        PaymentDialog dialog = new PaymentDialog(parentStage, billingService, selected, patientName);
        boolean confirmed = dialog.showAndWait();

        if (confirmed) {
            refreshLedger();
        }
    }

    private void handlePrintReceipt() {
        Invoice selected = masterTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Stage parentStage = (Stage) root.getScene().getWindow();
        String patientName = detailPatientName.getText().replace("Patient: ", "");
        String doctorName = detailDoctorName.getText().replace("Physician: ", "");

        ReceiptModal modal = new ReceiptModal(parentStage, selected, patientName, doctorName);
        modal.show();
    }

    public void refreshLedger() {
        if (billingService == null) return;

        // Recompute Metric Cards
        BillingService.LedgerMetrics metrics = billingService.calculateLedgerMetrics();
        totalBilledValue.setText(String.format("MUR %,.2f", metrics.getTotalBilledMur()));
        collectedRevenueValue.setText(String.format("MUR %,.2f", metrics.getCollectedRevenueMur()));
        pendingReceivablesValue.setText(String.format("MUR %,.2f", metrics.getPendingRevenueMur()));
        settledCountValue.setText(String.format("%d / %d", metrics.getPaidCount(), metrics.getPaidCount() + metrics.getPendingCount()));

        // Reload Master Table
        int selectedIndex = masterTable.getSelectionModel().getSelectedIndex();
        List<Invoice> all = billingService.filterInvoices("ALL", "");
        invoiceObservableList.setAll(all);

        if (!all.isEmpty()) {
            if (selectedIndex >= 0 && selectedIndex < all.size()) {
                masterTable.getSelectionModel().select(selectedIndex);
            } else {
                masterTable.getSelectionModel().selectFirst();
            }
        } else {
            displayInvoiceDetails(null);
        }
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public void onShow() {
        refreshLedger();
    }
}
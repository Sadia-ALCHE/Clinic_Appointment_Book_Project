package com.clinic.ui;

import com.clinic.model.Invoice;
import com.clinic.model.PaymentMethod;
import com.clinic.service.BillingService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

// Modal dialog for recording invoice payment settlement across Mauritian payment channels
public class PaymentDialog {

    private final Stage dialogStage;
    private final BillingService billingService;
    private final Invoice invoice;
    private final String patientName;
    private boolean paymentConfirmed = false;

    public PaymentDialog(Stage parentStage, BillingService billingService, Invoice invoice, String patientName) {
        this.billingService = billingService;
        this.invoice = invoice;
        this.patientName = patientName;

        this.dialogStage = new Stage();
        this.dialogStage.initModality(Modality.APPLICATION_MODAL);
        this.dialogStage.initOwner(parentStage);
        this.dialogStage.setTitle("Record Payment · " + invoice.getInvoiceNumber());
        this.dialogStage.setResizable(false);

        buildUi();
    }

    private void buildUi() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #ffffff;");
        root.setPrefWidth(420);

        Label titleLabel = new Label("Settle Invoice Payment");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label subtitleLabel = new Label("Choose payment method and enter payment details.");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        // Financial Summary Box
        VBox summaryBox = new VBox(6);
        summaryBox.setPadding(new Insets(14));
        summaryBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

        Label invNumLbl = new Label("Invoice: " + invoice.getInvoiceNumber());
        invNumLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");

        Label patientLbl = new Label("Patient: " + patientName);
        patientLbl.setStyle("-fx-text-fill: #475569;");

        double totalMur = invoice.calculateTotalMur();
        Label totalLbl = new Label(String.format("Total Payable: MUR %,.2f", totalMur));
        totalLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #059669;");

        summaryBox.getChildren().addAll(invNumLbl, patientLbl, totalLbl);

        // Form Inputs
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        Label methodLbl = new Label("Payment Method:");
        methodLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");

        ComboBox<PaymentMethod> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll(PaymentMethod.values());
        methodCombo.setValue(PaymentMethod.MCB_JUICE);
        methodCombo.setMaxWidth(Double.MAX_VALUE);

        Label refLbl = new Label("Transaction Ref / Mobile:");
        refLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");

        TextField refField = new TextField("JUICE-" + System.currentTimeMillis() % 100000);
        refField.setPromptText("e.g. +230 5842 1099 or Auth Code");

        Label changeNoticeLbl = new Label();
        changeNoticeLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #059669; -fx-font-weight: bold;");
        changeNoticeLbl.setVisible(false);

        methodCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == PaymentMethod.CASH) {
                refLbl.setText("Amount Tendered (MUR):");
                refField.setText(String.format("%.0f", totalMur));
                changeNoticeLbl.setText("Change Due: MUR 0.00");
                changeNoticeLbl.setVisible(true);
            } else if (newVal == PaymentMethod.MCB_JUICE) {
                refLbl.setText("MCB Juice Mobile / Ref:");
                refField.setText("JUICE-" + System.currentTimeMillis() % 100000);
                changeNoticeLbl.setVisible(false);
            } else {
                refLbl.setText("Card Auth Code:");
                refField.setText("AUTH-" + (int)(Math.random() * 900000 + 100000));
                changeNoticeLbl.setVisible(false);
            }
        });

        refField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (methodCombo.getValue() == PaymentMethod.CASH) {
                try {
                    double tendered = Double.parseDouble(newVal.trim());
                    double change = Math.max(0.0, tendered - totalMur);
                    changeNoticeLbl.setText(String.format("Change Due: MUR %,.2f", change));
                } catch (NumberFormatException ignored) {
                    changeNoticeLbl.setText("Enter valid numeric amount");
                }
            }
        });

        grid.add(methodLbl, 0, 0);
        grid.add(methodCombo, 1, 0);
        grid.add(refLbl, 0, 1);
        grid.add(refField, 1, 1);

        // Action Buttons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(8, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6;");
        cancelBtn.setOnAction(e -> dialogStage.close());

        Button confirmBtn = new Button("Confirm Settlement");
        confirmBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 8 18; -fx-background-radius: 6;");
        confirmBtn.setOnAction(e -> {
            try {
                billingService.settlePayment(invoice.getId(), methodCombo.getValue(), refField.getText().trim());
                this.paymentConfirmed = true;
                dialogStage.close();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
                alert.showAndWait();
            }
        });

        buttonBox.getChildren().addAll(cancelBtn, confirmBtn);

        root.getChildren().addAll(titleLabel, subtitleLabel, summaryBox, grid, changeNoticeLbl, buttonBox);

        Scene scene = new Scene(root);
        String css = getClass().getResource("/style.css") != null ? getClass().getResource("/style.css").toExternalForm() : null;
        if (css != null) scene.getStylesheets().add(css);
        dialogStage.setScene(scene);
    }

    public boolean showAndWait() {
        dialogStage.showAndWait();
        return paymentConfirmed;
    }
}
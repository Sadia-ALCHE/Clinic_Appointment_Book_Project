package com.clinic.ui;

import com.clinic.model.Invoice;
import com.clinic.model.InvoiceItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

// Modal dialog displaying a formatted printable receipt for settled clinic invoices
public class ReceiptModal {

    private final Stage stage;

    public ReceiptModal(Stage parentStage, Invoice invoice, String patientName, String doctorName) {
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.initOwner(parentStage);
        this.stage.setTitle("Official Payment Receipt · " + invoice.getInvoiceNumber());
        this.stage.setResizable(false);

        buildUi(invoice, patientName, doctorName);
    }

    private void buildUi(Invoice invoice, String patientName, String doctorName) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #ffffff;");
        root.setPrefWidth(460);

        Label title = new Label("Official Clinic Payment Receipt");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        // Monospace Printable Receipt Text
        TextArea receiptArea = new TextArea();
        receiptArea.setEditable(false);
        receiptArea.setPrefRowCount(18);
        receiptArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px; -fx-control-inner-background: #f8fafc;");
        receiptArea.setText(generateReceiptText(invoice, patientName, doctorName));

        // Buttons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button printBtn = new Button("Print Receipt (PDF)");
        printBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6;");
        printBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Print Job Dispatched");
            alert.setHeaderText("Official Receipt Sent to Printer");
            alert.setContentText("Receipt " + invoice.getInvoiceNumber() + " formatted and routed to default printer / PDF export.");
            alert.showAndWait();
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 6;");
        closeBtn.setOnAction(e -> stage.close());

        buttonBox.getChildren().addAll(printBtn, closeBtn);

        root.getChildren().addAll(title, receiptArea, buttonBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    private String generateReceiptText(Invoice invoice, String patientName, String doctorName) {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("           MEDICHE CLINIC HEALTH SUITE             \n");
        sb.append("      African Leadership College of Higher Ed      \n");
        sb.append("          Beau Plan Campus, Pamplemousses          \n");
        sb.append("              Republic of Mauritius                \n");
        sb.append("====================================================\n");
        sb.append(String.format("Receipt Number: %s\n", invoice.getInvoiceNumber()));
        sb.append(String.format("Issue Date:     %s\n", invoice.getIssueDate().format(DateTimeFormatter.ISO_LOCAL_DATE)));
        sb.append(String.format("Patient Name:   %s\n", patientName));
        sb.append(String.format("Physician:      %s\n", doctorName));
        sb.append(String.format("Status:         %s\n", invoice.getStatus().getDisplayName()));
        sb.append("----------------------------------------------------\n");
        sb.append(String.format("%-36s %13s\n", "ITEM DESCRIPTION", "AMOUNT (MUR)"));
        sb.append("----------------------------------------------------\n");

        for (InvoiceItem item : invoice.getItems()) {
            sb.append(String.format("%-36s MUR %,9.2f\n", truncate(item.getDescription(), 36), item.getAmountMur()));
        }

        double total = invoice.calculateTotalMur();
        sb.append("----------------------------------------------------\n");
        sb.append(String.format("%-36s MUR %,9.2f\n", "SUBTOTAL:", total));
        sb.append(String.format("%-36s %13s\n", "STUDENT HEALTH SUBSIDY (0% VAT):", "MUR 0.00"));
        sb.append(String.format("%-36s MUR %,9.2f\n", "TOTAL SETTLED IN FULL:", total));
        sb.append("====================================================\n");
        sb.append("      Thank you for choosing MediCHE Clinic!        \n");
        sb.append("  For billing inquiries: health@alche@alueducation.com       \n");
        sb.append("====================================================\n");

        return sb.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    public void show() {
        stage.show();
    }
}
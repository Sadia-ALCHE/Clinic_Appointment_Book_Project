// BillingView.java
package com.clinic.ui;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

// Invoicing, Billing, and MUR Financial Rollup Screen Scaffold
public class BillingView implements View {

    private final BorderPane root;

    public BillingView() {
        this.root = new BorderPane();
        this.root.getStyleClass().add("view-container");
        buildUI();
    }

    private void buildUI() {
        VBox header = new VBox();
        header.getStyleClass().add("view-header");

        Label title = new Label("Billing & Invoicing (MUR)");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Official ALCHE Mauritian Rupee invoices, care chain cost rollups, and payment records");
        subtitle.getStyleClass().add("view-subtitle");

        header.getChildren().addAll(title, subtitle);
        root.setTop(header);

        VBox card = new VBox(12);
        card.getStyleClass().add("view-card");

        Label cardTitle = new Label("Master-Detail Invoice Directory & Receipt Generator");
        cardTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

        Label cardStatus = new Label("Scaffold Ready · Day 11 Implementation Target");
        cardStatus.getStyleClass().addAll("status-badge", "badge-wip");

        Label cardDesc = new Label("Atomic invoice generation, line item breakdowns, and CareChainSummary recursive cost totals in MUR.");
        cardDesc.setStyle("-fx-text-fill: #64748b;");

        card.getChildren().addAll(cardTitle, cardStatus, cardDesc);
        root.setCenter(card);
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}

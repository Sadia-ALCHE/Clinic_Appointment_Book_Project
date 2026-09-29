// PatientView.java
package com.clinic.ui;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

// Patient Directory and Medical Records Screen Scaffold
public class PatientView implements View {

    private final BorderPane root;

    public PatientView() {
        this.root = new BorderPane();
        this.root.getStyleClass().add("view-container");
        buildUI();
    }

    private void buildUI() {
        // Header
        VBox header = new VBox();
        header.getStyleClass().add("view-header");

        Label title = new Label("Patient Directory");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Manage patient registrations, contact information, and medical histories");
        subtitle.getStyleClass().add("view-subtitle");

        header.getChildren().addAll(title, subtitle);
        root.setTop(header);

        // Main Card Placeholder
        VBox card = new VBox(12);
        card.getStyleClass().add("view-card");

        Label cardTitle = new Label("Patient Records Table & Live Search");
        cardTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

        Label cardStatus = new Label("Scaffold Ready · Day 10 Implementation Target");
        cardStatus.getStyleClass().addAll("status-badge", "badge-wip");

        Label cardDesc = new Label("Full CRUD TableView, search filtering, and patient registration drawer will be mounted here.");
        cardDesc.setStyle("-fx-text-fill: #64748b;");

        card.getChildren().addAll(cardTitle, cardStatus, cardDesc);
        root.setCenter(card);
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public void onShow() {
        // Will trigger patientTable refresh on Day 10
    }
}
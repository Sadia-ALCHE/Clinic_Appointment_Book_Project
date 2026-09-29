// ScheduleView.java
package com.clinic.ui;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

// Doctor Timetable and Appointment Booking Screen Scaffold
public class ScheduleView implements View {

    private final BorderPane root;

    public ScheduleView() {
        this.root = new BorderPane();
        this.root.getStyleClass().add("view-container");
        buildUI();
    }

    private void buildUI() {
        VBox header = new VBox();
        header.getStyleClass().add("view-header");

        Label title = new Label("Doctor Timetable & Appointments");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Real-time schedule grid, appointment booking, and ScheduleValidator conflict alerts");
        subtitle.getStyleClass().add("view-subtitle");

        header.getChildren().addAll(title, subtitle);
        root.setTop(header);

        VBox card = new VBox(12);
        card.getStyleClass().add("view-card");

        Label cardTitle = new Label("Consultation Calendar & Booking Matrix");
        cardTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

        Label cardStatus = new Label("Scaffold Ready · Day 10 Implementation Target");
        cardStatus.getStyleClass().addAll("status-badge", "badge-wip");

        Label cardDesc = new Label("Interactive doctor schedule grid and booking modal with real-time double-booking warnings.");
        cardDesc.setStyle("-fx-text-fill: #64748b;");

        card.getChildren().addAll(cardTitle, cardStatus, cardDesc);
        root.setCenter(card);
    }

    @Override
    public Parent getRoot() {
        return root;
    }
}


package com.clinic.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

// Persistent left navigation sidebar component
public class SidebarView {

    private final VBox root;
    private final Button btnPatients;
    private final Button btnSchedule;
    private final Button btnBilling;

    public SidebarView() {
        this.root = new VBox();
        this.root.getStyleClass().add("sidebar");

        // 1. Clinic Branding Header
        VBox brandBox = new VBox(2);
        Label brandTitle = new Label("MediCare Clinic");
        brandTitle.getStyleClass().add("sidebar-brand-title");

        Label brandSubtitle = new Label("ALCHE Mauritius · EHR");
        brandSubtitle.getStyleClass().add("sidebar-brand-subtitle");
        brandBox.getChildren().addAll(brandTitle, brandSubtitle);

        // 2. Navigation Action Buttons
        this.btnPatients = createNavButton("Patient Directory");
        this.btnSchedule = createNavButton("Doctor Timetable");
        this.btnBilling  = createNavButton("Billing & Invoices");

        // Assemble Sidebar Hierarchy
        this.root.getChildren().addAll(brandBox, btnPatients, btnSchedule, btnBilling);
    }

    private Button createNavButton(String labelText) {
        Button btn = new Button(labelText);
        btn.getStyleClass().add("sidebar-nav-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        return btn;
    }

    public VBox getRoot() {
        return root;
    }

    public Button getBtnPatients() {
        return btnPatients;
    }

    public Button getBtnSchedule() {
        return btnSchedule;
    }

    public Button getBtnBilling() {
        return btnBilling;
    }
}
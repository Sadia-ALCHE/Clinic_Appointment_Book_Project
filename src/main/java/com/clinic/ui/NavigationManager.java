package com.clinic.ui;

import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import java.util.HashMap;
import java.util.Map;

// Central view coordinator managing screen transitions and active navigation state
public class NavigationManager {

    public static final String VIEW_PATIENTS = "PATIENTS";
    public static final String VIEW_SCHEDULE = "SCHEDULE";
    public static final String VIEW_BILLING = "BILLING";

    private static final String ACTIVE_NAV_CLASS = "sidebar-nav-btn-active";

    private final BorderPane mainContainer;
    private final Map<String, View> views = new HashMap<>();
    private final Map<String, Button> navButtons = new HashMap<>();
    private String currentViewKey = null;

    public NavigationManager(BorderPane mainContainer) {
        if (mainContainer == null) {
            throw new IllegalArgumentException("Main container BorderPane cannot be null");
        }
        this.mainContainer = mainContainer;
    }

    // Register a view and its corresponding sidebar navigation button
    public void registerView(String key, View view, Button navButton) {
        if (key == null || view == null) {
            throw new IllegalArgumentException("View key and View instance cannot be null");
        }
        views.put(key, view);
        if (navButton != null) {
            navButtons.put(key, navButton);
            navButton.setOnAction(e -> showView(key));
        }
    }

    // Switch the active view displayed in the center region
    public void showView(String key) {
        if (!views.containsKey(key)) {
            throw new IllegalArgumentException("No view registered for key: " + key);
        }

        // Update active sidebar button styles
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            Button btn = entry.getValue();
            if (entry.getKey().equals(key)) {
                if (!btn.getStyleClass().contains(ACTIVE_NAV_CLASS)) {
                    btn.getStyleClass().add(ACTIVE_NAV_CLASS);
                }
            } else {
                btn.getStyleClass().remove(ACTIVE_NAV_CLASS);
            }
        }

        // Swap center node in main container
        View targetView = views.get(key);
        mainContainer.setCenter(targetView.getRoot());
        currentViewKey = key;

        // Trigger lifecycle refresh hook
        targetView.onShow();
    }

    // Convenience navigation helper methods
    public void showPatients() {
        showView(VIEW_PATIENTS);
    }

    public void showSchedule() {
        showView(VIEW_SCHEDULE);
    }

    public void showBilling() {
        showView(VIEW_BILLING);
    }

    public String getCurrentViewKey() {
        return currentViewKey;
    }
}

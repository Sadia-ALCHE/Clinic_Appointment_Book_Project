package com.clinic.ui;

import javafx.scene.Parent;

// Common polymorphic contract for all desktop view screens
public interface View {

    // Return the root JavaFX node container for this view
    Parent getRoot();

    // Optional lifecycle hook executed whenever this view becomes active
    default void onShow() {
        // Default empty implementation; override to reload table data
    }
}

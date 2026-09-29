// App.java
package com.clinic;

import com.clinic.ui.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

// JavaFX Desktop Application Composition Root
public class App extends Application {

    public static final String APP_TITLE = "MediCare Clinic Appointment Book · ALCHE Mauritius";
    public static final int DEFAULT_WIDTH = 1150;
    public static final int DEFAULT_HEIGHT = 720;
    public static final int MIN_WIDTH = 950;
    public static final int MIN_HEIGHT = 600;

    @Override
    public void start(Stage primaryStage) {
        // 1. Root Layout Shell
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");

        // 2. Navigation Coordinator
        NavigationManager navManager = new NavigationManager(root);

        // 3. Construct Views
        PatientView patientView = new PatientView();
        ScheduleView scheduleView = new ScheduleView();
        BillingView billingView = new BillingView();

        // 4. Construct Sidebar
        SidebarView sidebar = new SidebarView();
        root.setLeft(sidebar.getRoot());

        // 5. Register Views with Navigation Manager
        navManager.registerView(NavigationManager.VIEW_PATIENTS, patientView, sidebar.getBtnPatients());
        navManager.registerView(NavigationManager.VIEW_SCHEDULE, scheduleView, sidebar.getBtnSchedule());
        navManager.registerView(NavigationManager.VIEW_BILLING,  billingView,  sidebar.getBtnBilling());

        // 6. Set Default View to Patients
        navManager.showPatients();

        // 7. Construct Primary Scene
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);

        // 8. Attach Clinical Stylesheet
        String cssPath = getClass().getResource("/style.css") != null
                ? getClass().getResource("/style.css").toExternalForm()
                : null;
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }

        // 9. Configure Stage Window Properties
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

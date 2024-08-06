module com.projects.pointtracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.projects.pointtracker to javafx.fxml;
    exports com.projects.pointtracker;
}
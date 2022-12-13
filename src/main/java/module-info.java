module com.kmeans.kmeans {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires java.desktop;

    opens com.kmeans.kmeans to javafx.fxml;
    exports com.kmeans.kmeans;
}
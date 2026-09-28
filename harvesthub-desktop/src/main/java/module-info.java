module com.harvesthub.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http; // Built-in Java 21 HttpClient
    requires com.fasterxml.jackson.databind; // For JSON mapping

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.fontawesome5;

    opens com.harvesthub.desktop.model to javafx.base, com.fasterxml.jackson.databind;
    opens com.harvesthub.desktop to javafx.fxml;
    exports com.harvesthub.desktop;
}
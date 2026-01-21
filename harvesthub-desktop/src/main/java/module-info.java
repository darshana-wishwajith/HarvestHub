module com.harvesthub.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.net.http; // Built-in Java 21 HttpClient
    requires com.fasterxml.jackson.databind; // For JSON mapping

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    opens com.harvesthub.desktop.model to javafx.base, com.fasterxml.jackson.databind;
//    opens com.harvesthub.desktop.model to com.fasterxml.jackson.databind; // Allow Jackson to read your Model
    opens com.harvesthub.desktop to javafx.fxml;
    exports com.harvesthub.desktop;
}
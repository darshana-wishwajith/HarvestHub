module com.harvesthub.desktop {
    requires javafx.controls;
    requires javafx.media;          // inline playback of the farmer's 10 s field video
    requires java.net.http;
    requires java.prefs;            // "Remember me" (officer email only, never the password)
    requires com.fasterxml.jackson.databind;

    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.fontawesome5;

    opens com.harvesthub.desktop.api to com.fasterxml.jackson.databind;

    exports com.harvesthub.desktop;
    exports com.harvesthub.desktop.api;
    exports com.harvesthub.desktop.ui;
    exports com.harvesthub.desktop.pages;
    exports com.harvesthub.desktop.util;
}

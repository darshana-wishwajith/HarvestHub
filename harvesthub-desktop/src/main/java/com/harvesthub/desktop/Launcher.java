package com.harvesthub.desktop;

import javafx.application.Application;

/** Plain main class for packaged launchers (jpackage / Launch4j) that cannot start an Application subclass directly. */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(PortalApp.class, args);
    }
}

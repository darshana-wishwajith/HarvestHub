package com.harvesthub.desktop.ui;

import javafx.scene.Node;

/** One screen inside the portal shell. */
public interface Page {

    String title();

    String subtitle();

    /** Built once when the page is shown. */
    Node view();

    /** Reload from the server (top-bar Refresh button). */
    default void refresh() {
    }
}

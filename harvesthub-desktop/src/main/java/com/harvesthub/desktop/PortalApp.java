package com.harvesthub.desktop;

import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.api.Session;
import com.harvesthub.desktop.pages.LoginView;
import com.harvesthub.desktop.ui.Shell;
import com.harvesthub.desktop.ui.Ui;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * HarvestHub Officer Portal – JavaFX desktop app for Agricultural Officers (proposal 3.3, 6.3.1).
 * Talks to the backend over HTTPS (default https://harvesthubapp.tech).
 */
public class PortalApp extends Application {

    public static final String APP_NAME = "HarvestHub Officer Portal";

    private static Stage stage;
    private static HostServices hostServices;

    public static Stage stage() {
        return stage;
    }

    public static String css() {
        return PortalApp.class.getResource("/com/harvesthub/desktop/portal.css").toExternalForm();
    }

    /** Opens a web link (map, video, photo) in the officer's default browser. */
    public static void openInBrowser(String url) {
        if (url != null && hostServices != null) hostServices.showDocument(url);
    }

    @Override
    public void start(Stage primary) {
        stage = primary;
        hostServices = getHostServices();
        List<Image> icons = new ArrayList<>();
        for (int s : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            Image i = Ui.resource("icon-" + s + ".png");
            if (i != null) icons.add(i);
        }
        primary.getIcons().setAll(icons);
        primary.setTitle(APP_NAME);
        loadFonts();
        ApiClient.setOnSessionExpired(() -> Platform.runLater(() -> {
            if (Session.isLoggedIn()) {
                Session.clear();
                showLogin("Your session ended (signed out or the account was suspended). Please sign in again.");
            }
        }));
        // Normal window at the standard 1280x720 (with title bar: minimise / maximise / close), shrunk to the
        // usable screen area when the screen is smaller (1366x768 laptops lose ~48 px to the taskbar).
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        primary.setMinWidth(Math.min(1024, screen.getWidth()));
        primary.setMinHeight(Math.min(600, screen.getHeight()));
        primary.setScene(new Scene(new LoginView(null).view()));
        primary.getScene().getStylesheets().add(css());
        primary.setWidth(Math.min(1280, screen.getWidth()));
        primary.setHeight(Math.min(720, screen.getHeight()));
        primary.setX(screen.getMinX() + (screen.getWidth() - primary.getWidth()) / 2);
        primary.setY(screen.getMinY() + Math.max(0, (screen.getHeight() - primary.getHeight()) / 2));
        primary.show();
    }

    /** Bundled Plus Jakarta Sans (OFL, github.com/tokotype/PlusJakartaSans) so the brand font works on every PC. */
    private static void loadFonts() {
        for (String w : new String[]{"Regular", "Medium", "SemiBold", "Bold", "ExtraBold"}) {
            var url = PortalApp.class.getResource("/com/harvesthub/desktop/fonts/PlusJakartaSans-" + w + ".ttf");
            if (url != null) Font.loadFont(url.toExternalForm(), 13);
        }
    }

    public static void showLogin(String message) {
        Shell.stopTimers();
        setRoot(new LoginView(message).view(), APP_NAME);
    }

    public static void showPortal() {
        setRoot(Shell.create().view(), APP_NAME);
    }

    public static void setRoot(Parent root, String title) {
        stage.getScene().setRoot(root);
        stage.setTitle(title);
    }

    /** Page titles look like "HarvestHub · Verification Queue". */
    public static void setPageTitle(String page) {
        if (stage != null) stage.setTitle(page == null ? APP_NAME : "HarvestHub · " + page);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

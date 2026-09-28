package com.harvesthub.desktop;

import com.harvesthub.desktop.model.Listing;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Shared helper for swapping Officer Portal scenes while preserving window size.
 */
public final class SceneNavigator {

    private SceneNavigator() {
    }

    public static void navigate(Node source, String fxmlClasspath, String title) {
        try {
            URL fxml = SceneNavigator.class.getResource(fxmlClasspath);
            if (fxml == null) {
                throw new IOException("FXML not found: " + fxmlClasspath);
            }
            Parent root = FXMLLoader.load(fxml);
            applyScene(source, root, title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void navigateToReview(Node source, Listing listing) {
        try {
            URL fxml = SceneNavigator.class.getResource("/com/harvesthub/desktop/ReviewView.fxml");
            if (fxml == null) {
                throw new IOException("FXML not found: ReviewView.fxml");
            }
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            ReviewController controller = loader.getController();
            controller.initListing(listing);
            String title = "HarvestHub - Reviewing: "
                    + (listing != null ? listing.getCropName() + " - " + listing.getFarmerName() : "Listing");
            applyScene(source, root, title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void applyScene(Node source, Parent root, String title) {
        Stage stage = (Stage) source.getScene().getWindow();
        double width = stage.getScene().getWidth();
        double height = stage.getScene().getHeight();
        stage.setScene(new Scene(root, width, height));
        if (title != null) {
            stage.setTitle(title);
        }
    }
}

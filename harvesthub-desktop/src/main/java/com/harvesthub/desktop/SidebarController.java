package com.harvesthub.desktop;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

/**
 * Shared officer-portal sidebar. Call {@link #activate(Nav)} after load to highlight
 * the current page. Navigation and logout are handled here so host screens stay lean.
 */
public class SidebarController {

    public enum Nav {
        DASHBOARD,
        VERIFICATION_QUEUE,
        REVIEW_HISTORY,
        PROFILE
    }

    @FXML private HBox navDashboard;
    @FXML private HBox navVerificationQueue;
    @FXML private HBox navReviewHistory;
    @FXML private HBox navProfile;

    private Nav active = Nav.DASHBOARD;

    public void activate(Nav nav) {
        this.active = nav;
        clearActive();
        if (nav == null) return;
        switch (nav) {
            case DASHBOARD -> navDashboard.getStyleClass().add("nav-item-active");
            case VERIFICATION_QUEUE -> navVerificationQueue.getStyleClass().add("nav-item-active");
            case REVIEW_HISTORY -> navReviewHistory.getStyleClass().add("nav-item-active");
            case PROFILE -> navProfile.getStyleClass().add("nav-item-active");
        }
    }

    private void clearActive() {
        navDashboard.getStyleClass().remove("nav-item-active");
        navVerificationQueue.getStyleClass().remove("nav-item-active");
        navReviewHistory.getStyleClass().remove("nav-item-active");
        navProfile.getStyleClass().remove("nav-item-active");
    }

    @FXML
    private void handleNavDashboard(MouseEvent event) {
        if (active == Nav.DASHBOARD) {
            return;
        }
        SceneNavigator.navigate((Node) event.getSource(),
                "/com/harvesthub/desktop/DashboardView.fxml",
                "HarvestHub â€” Officer Portal");
    }

    @FXML
    private void handleNavVerificationQueue(MouseEvent event) {
        if (active == Nav.VERIFICATION_QUEUE) {
            return;
        }
        SceneNavigator.navigate((Node) event.getSource(),
                "/com/harvesthub/desktop/VerificationQueueView.fxml",
                "HarvestHub â€” Verification Queue");
    }

    @FXML
    private void handleNavReviewHistory(MouseEvent event) {
        if (active == Nav.REVIEW_HISTORY) {
            return;
        }
        SceneNavigator.navigate((Node) event.getSource(),
                "/com/harvesthub/desktop/ReviewHistoryView.fxml",
                "HarvestHub â€” Review History");
    }

    @FXML
    private void handleNavProfile(MouseEvent event) {
        if (active == Nav.PROFILE) {
            return;
        }
        SceneNavigator.navigate((Node) event.getSource(),
                "/com/harvesthub/desktop/ProfileView.fxml",
                "HarvestHub â€” My Profile");
    }

    @FXML
    private void handleLogout() {
        SceneNavigator.navigate(navDashboard,
                "/com/harvesthub/desktop/LoginView.fxml",
                "HarvestHub â€” Agricultural Officer Portal");
    }
}

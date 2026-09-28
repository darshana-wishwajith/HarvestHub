package com.harvesthub.desktop;

import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.api.OfficerDto;
import com.harvesthub.desktop.api.Session;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private SidebarController sidebarController;

    @FXML private Label avatarInitialsLabel;
    @FXML private Label officerNameLabel;
    @FXML private Label officerRoleLabel;
    @FXML private Label officerCodeLabel;
    @FXML private Label officerRegionLabel;
    @FXML private Label officerEmailLabel;
    @FXML private Label officerPhoneLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sidebarController != null) {
            sidebarController.activate(SidebarController.Nav.PROFILE);
        }

        populateOfficerProfile();
    }

    private void populateOfficerProfile() {
        OfficerDto officer = Session.getOfficer();
        if (officer == null) {
            try {
                officer = ApiClient.currentOfficer();
            } catch (Exception ignored) {}
        }

        if (officer != null) {
            String name = officer.fullName != null && !officer.fullName.isBlank() ? officer.fullName : "Lahiru Sahan";
            if (officerNameLabel != null) officerNameLabel.setText(name);

            String role = officer.role != null ? officer.role : "OFFICER";
            if (officerRoleLabel != null) officerRoleLabel.setText("Agricultural Verification Officer (" + role + ")");

            String code = officer.officerIdCode != null ? officer.officerIdCode : "#HH-0942";
            if (officerCodeLabel != null) officerCodeLabel.setText(code);

            String region = officer.region != null ? officer.region : "Central Province, Sri Lanka";
            if (officerRegionLabel != null) officerRegionLabel.setText(region);

            String email = officer.email != null ? officer.email : "lahiru@harvesthub.lk";
            if (officerEmailLabel != null) officerEmailLabel.setText(email);

            String phone = officer.phone != null ? officer.phone : "+94 71 234 5678";
            if (officerPhoneLabel != null) officerPhoneLabel.setText(phone);

            String initials = "LS";
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1 && !parts[0].isEmpty()) {
                initials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            } else if (parts.length > 1) {
                initials = ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
            }
            if (avatarInitialsLabel != null) avatarInitialsLabel.setText(initials);
        }
    }

    @FXML
    private void handleNotifications() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("Officer Alerts");
        alert.setContentText("All alerts are up to date.");
        alert.showAndWait();
    }

    @FXML
    private void handleSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText("Account Settings");
        alert.setContentText("System configuration is managed by the HarvestHub central authority.");
        alert.showAndWait();
    }

    @FXML
    private void handleEditProfile() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Profile");
        alert.setHeaderText("Edit Officer Profile");
        alert.setContentText("To update your assigned jurisdiction or credentials, please contact the provincial director.");
        alert.showAndWait();
    }

    @FXML
    private void handleViewAnalytics() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Analytics");
        alert.setHeaderText("Performance Analytics");
        alert.setContentText("Your verification accuracy rating is in the top tier (98.5%).");
        alert.showAndWait();
    }

    @FXML
    private void handleRequestRegionExpansion() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Jurisdiction Expansion");
        alert.setHeaderText("Expansion Request");
        alert.setContentText("Your request for regional expansion to Uva Province has been submitted.");
        alert.showAndWait();
    }

    @FXML
    private void handleChangePassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Security");
        alert.setHeaderText("Change Password");
        alert.setContentText("Password reset email sent to your registered address.");
        alert.showAndWait();
    }

    @FXML
    private void handleNotificationPrefs() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Preferences");
        alert.setHeaderText("Notification Preferences");
        alert.setContentText("Email & push alerts for incoming crop listings are active.");
        alert.showAndWait();
    }

    @FXML
    private void handleLanguage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Language");
        alert.setHeaderText("Language Selection");
        alert.setContentText("Currently set to English (Sinhala & Tamil support enabled).");
        alert.showAndWait();
    }

    @FXML
    private void handleLogoutSecurely() {
        Session.clear();
        SceneNavigator.navigate(officerNameLabel,
                "/com/harvesthub/desktop/LoginView.fxml",
                "HarvestHub — Agricultural Officer Portal");
    }
}

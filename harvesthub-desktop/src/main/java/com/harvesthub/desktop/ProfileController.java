package com.harvesthub.desktop;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private SidebarController sidebarController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sidebarController != null) {
            sidebarController.activate(SidebarController.Nav.PROFILE);
        }
    }
    
    @FXML
    private void handleNotifications() {
        System.out.println("Notifications clicked");
    }
    
    @FXML
    private void handleSettings() {
        System.out.println("Settings clicked");
    }
    
    @FXML
    private void handleEditProfile() {
        System.out.println("Edit Profile clicked");
    }
    
    @FXML
    private void handleViewAnalytics() {
        System.out.println("View Analytics clicked");
    }
    
    @FXML
    private void handleRequestRegionExpansion() {
        System.out.println("Request Region Expansion clicked");
    }
    
    @FXML
    private void handleChangePassword() {
        System.out.println("Change Password clicked");
    }
    
    @FXML
    private void handleNotificationPrefs() {
        System.out.println("Notification Preferences clicked");
    }
    
    @FXML
    private void handleLanguage() {
        System.out.println("Language clicked");
    }
    
    @FXML
    private void handleLogoutSecurely() {
        System.out.println("Logout Securely clicked");
    }
}

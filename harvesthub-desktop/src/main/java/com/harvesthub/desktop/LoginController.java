package com.harvesthub.desktop;

import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.api.ApiException;
import com.harvesthub.desktop.api.LoginResponseDto;
import com.harvesthub.desktop.api.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private Button loginButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private FontIcon eyeIcon;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Label loginErrorLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Keep plain-text and masked password fields in sync
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());

        // Ensure password fields can receive click + Tab focus
        passwordField.setFocusTraversable(true);
        passwordField.setMouseTransparent(false);
        passwordField.setDisable(false);
        passwordTextField.setFocusTraversable(false);
        passwordTextField.setMouseTransparent(true);

        // Add focus styling to input containers
        setupFocusStyling(usernameField);
        setupFocusStyling(passwordField);
        setupFocusStyling(passwordTextField);

        // Tab order: username â†’ password â†’ remember me â†’ login
        usernameField.setFocusTraversable(true);

        if (loginErrorLabel != null) {
            loginErrorLabel.setVisible(false);
            loginErrorLabel.setManaged(false);
        }
    }

    /**
     * Walks up the scene graph from the focused field to find the .input-container
     * parent, then adds/removes a CSS class for the focused border effect.
     */
    private void setupFocusStyling(Node field) {
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            Node parent = field.getParent();
            while (parent != null && !parent.getStyleClass().contains("input-container")) {
                parent = parent.getParent();
            }
            if (parent != null) {
                if (isFocused) {
                    if (!parent.getStyleClass().contains("input-container-focused")) {
                        parent.getStyleClass().add("input-container-focused");
                    }
                } else {
                    parent.getStyleClass().remove("input-container-focused");
                }
            }
        });
    }

    @FXML
    private void togglePasswordVisibility() {
        if (passwordField.isVisible()) {
            // Show plain text
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordField.setMouseTransparent(true);
            passwordField.setFocusTraversable(false);

            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordTextField.setMouseTransparent(false);
            passwordTextField.setFocusTraversable(true);
            passwordTextField.requestFocus();
            passwordTextField.positionCaret(passwordTextField.getText() == null
                    ? 0 : passwordTextField.getText().length());
            eyeIcon.setIconLiteral("fas-eye-slash");
        } else {
            // Show masked password
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            passwordTextField.setMouseTransparent(true);
            passwordTextField.setFocusTraversable(false);

            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordField.setMouseTransparent(false);
            passwordField.setFocusTraversable(true);
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText() == null
                    ? 0 : passwordField.getText().length());
            eyeIcon.setIconLiteral("fas-eye");
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showLoginError("Please enter both Officer ID and password.");
            return;
        }

        hideLoginError();
        setBusy(true);

        // Hit the backend off the JavaFX Application Thread so the UI doesn't freeze while waiting.
        Thread apiThread = new Thread(() -> {
            try {
                LoginResponseDto response = ApiClient.login(username, password);
                Session.set(response.token, response.officer);
                Platform.runLater(this::openDashboard);
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    setBusy(false);
                    if (e.getStatusCode() == 401) {
                        showLoginError("Invalid credentials. Try: lahiru / officer123");
                    } else {
                        showLoginError(e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setBusy(false);
                    showLoginError("Unexpected error: " + e.getMessage());
                });
            }
        }, "harvesthub-login");
        apiThread.setDaemon(true);
        apiThread.start();
    }

    private void openDashboard() {
        setBusy(false);
        try {
            URL fxml = getClass().getResource("/com/harvesthub/desktop/DashboardView.fxml");
            if (fxml == null) {
                showAlert("Login Error", "Dashboard view was not found on the classpath.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
            stage.setTitle("HarvestHub â€” Officer Portal");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Login Error", "Could not open dashboard:\n" + e.getMessage());
        } catch (RuntimeException e) {
            e.printStackTrace();
            showAlert("Login Error", "Dashboard failed to start:\n" + e.getMessage());
        }
    }

    private void setBusy(boolean busy) {
        if (loginButton != null) {
            loginButton.setDisable(busy);
        }
        usernameField.setDisable(busy);
        passwordField.setDisable(busy);
        passwordTextField.setDisable(busy);
    }

    private void showLoginError(String message) {
        if (loginErrorLabel != null) {
            loginErrorLabel.setText(message);
            loginErrorLabel.setVisible(true);
            loginErrorLabel.setManaged(true);
        } else {
            showAlert("Login Failed", message);
        }
    }

    private void hideLoginError() {
        if (loginErrorLabel != null) {
            loginErrorLabel.setText("");
            loginErrorLabel.setVisible(false);
            loginErrorLabel.setManaged(false);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleForgotPassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Forgot Password");
        alert.setHeaderText("Demo credentials");
        alert.setContentText(
                "Username: lahiru  (or ITT-122)\n" +
                "Password: officer123"
        );
        alert.showAndWait();
    }
}

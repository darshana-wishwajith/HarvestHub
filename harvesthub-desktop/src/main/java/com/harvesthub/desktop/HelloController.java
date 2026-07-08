package com.harvesthub.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harvesthub.desktop.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    @FXML
    private TableView<User> userTable;

    // CRITICAL FIX: Changed from <User, Long> to <User, String>
    @FXML
    private TableColumn<User, String> idColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> passwordColumn;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        passwordColumn.setCellValueFactory(new PropertyValueFactory<>("password"));
    }

    @FXML
    private void handleLoadUsers() {
        System.out.println("Connecting to HarvestHub backend at https://harvesthubapp.tech/api/users...");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://harvesthubapp.tech/api/users"))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        System.err.println("Server returned error: " + response.statusCode());
                        return "";
                    }
                    return response.body();
                })
                .thenAccept(json -> {
                    if (json.isEmpty()) return;

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        List<User> users = mapper.readValue(json, new TypeReference<List<User>>(){});

                        // Safely update the JavaFX UI thread
                        Platform.runLater(() -> {
                            userTable.getItems().clear();
                            userTable.getItems().setAll(users);
                            System.out.println("Successfully loaded " + users.size() + " users from MongoDB Atlas!");
                        });
                    } catch (Exception e) {
                        System.err.println("Failed to parse JSON from backend:");
                        e.printStackTrace();
                    }
                });
    }
}
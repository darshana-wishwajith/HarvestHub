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

    @FXML
    private TableColumn<User, Long> idColumn; // Must match fx:id in Scene Builder exactly

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    public void initialize() {
        // This connects the 'id' and 'name' variables from your User.java to the UI columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
    }
    @FXML
    private void handleLoadUsers() { // Must match the name in the error message exactly
        System.out.println("Connecting to HarvestHub backend...");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/users"))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    // Check if the server actually sent data (Status 200)
                    if (response.statusCode() != 200) {
                        System.err.println("Server returned error: " + response.statusCode());
                        return ""; // Return empty string so Jackson doesn't crash
                    }
                    return response.body();
                })
                .thenAccept(json -> {
                    if (json.isEmpty()) return; // Stop if there is no content

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        List<User> users = mapper.readValue(json, new TypeReference<List<User>>(){});
                        Platform.runLater(() -> userTable.getItems().setAll(users));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }


}

package com.harvesthub.desktop;

import com.harvesthub.desktop.model.ActivityLog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private SidebarController sidebarController;
    @FXML private TextField searchField;
    @FXML private TableView<ActivityLog> activityTable;
    @FXML private TableColumn<ActivityLog, String> cropColumn;
    @FXML private TableColumn<ActivityLog, String> locationColumn;
    @FXML private TableColumn<ActivityLog, String> gradeColumn;
    @FXML private TableColumn<ActivityLog, String> statusColumn;
    @FXML private TableColumn<ActivityLog, String> timestampColumn;
    @FXML private TableColumn<ActivityLog, Void> actionsColumn;

    private FilteredList<ActivityLog> filteredLogs;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sidebarController != null) {
            sidebarController.activate(SidebarController.Nav.DASHBOARD);
        }

        ObservableList<ActivityLog> logs = FXCollections.observableArrayList(
                new ActivityLog("Paddy", "Bg 300", "Kalinga Perera",
                        "Anuradhapura North Sector", "A", "Pending", "10:30 AM"),
                new ActivityLog("Cinnamon", "Sri Lankan Alba", "Anura Silva",
                        "Galle Central Hub", "B", "Pending", "09:45 AM"),
                new ActivityLog("Tea", "Nuwara Eliya Pekoe", "Bandara Wijesinghe",
                        "Ella Highlands Plains", "C", "Pending", "08:15 AM"),
                new ActivityLog("Pepper", "Matale Black Spice", "Nimal Siriwardena",
                        "Matale East", "A", "Approved", "Yesterday"),
                new ActivityLog("Coconut", "King Coconut Premium", "Sunethra Ranasinghe",
                        "Kurunegala Highlands", "A", "Approved", "Yesterday")
        );

        filteredLogs = new FilteredList<>(logs, p -> true);
        activityTable.setItems(filteredLogs);
        activityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        activityTable.setFocusTraversable(false);
        activityTable.setPlaceholder(createPlaceholderLabel("No activity logs found"));

        setupCropColumn();
        setupLocationColumn();
        setupGradeColumn();
        setupStatusColumn();
        setupTimestampColumn();
        setupActionsColumn();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearchFilter(newVal));
    }

    private Label createPlaceholderLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("activity-placeholder");
        return label;
    }

    private void setupCropColumn() {
        cropColumn.setCellValueFactory(new PropertyValueFactory<>("cropName"));
        cropColumn.setCellFactory(col -> new TableCell<>() {
            private final VBox box = new VBox(3);
            private final HBox titleRow = new HBox(4);
            private final Label cropLabel = new Label();
            private final Label varietyLabel = new Label();
            private final Label farmerLabel = new Label();

            {
                cropLabel.getStyleClass().add("cell-crop-name");
                varietyLabel.getStyleClass().add("cell-variety");
                farmerLabel.getStyleClass().add("cell-farmer");
                titleRow.setAlignment(Pos.CENTER_LEFT);
                titleRow.getChildren().addAll(cropLabel, varietyLabel);
                box.getChildren().addAll(titleRow, farmerLabel);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    ActivityLog log = getTableRow().getItem();
                    cropLabel.setText(log.getCropName());
                    varietyLabel.setText("(" + log.getVariety() + ")");
                    farmerLabel.setText("Farmer: " + log.getFarmerName());
                    setGraphic(box);
                    setText(null);
                }
            }
        });
    }

    private void setupLocationColumn() {
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        locationColumn.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(6);
            private final FontIcon pin = new FontIcon("fas-map-marker-alt");
            private final Label locationLabel = new Label();

            {
                pin.setIconSize(12);
                pin.getStyleClass().add("location-pin-icon");
                locationLabel.getStyleClass().add("cell-location");
                locationLabel.setWrapText(true);
                box.setAlignment(Pos.CENTER_LEFT);
                box.getChildren().addAll(pin, locationLabel);
                HBox.setHgrow(locationLabel, Priority.ALWAYS);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    locationLabel.setText(item);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
    }

    private void setupGradeColumn() {
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));
        gradeColumn.setCellFactory(col -> new TableCell<>() {
            private final Label pill = new Label();
            private final StackPane wrapper = new StackPane(pill);

            {
                pill.getStyleClass().add("table-pill");
                wrapper.setAlignment(Pos.CENTER);
                wrapper.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    pill.setText("Grade " + item);
                    pill.getStyleClass().removeAll("pill-grade-a", "pill-grade-b", "pill-grade-c");
                    switch (item) {
                        case "A" -> pill.getStyleClass().add("pill-grade-a");
                        case "B" -> pill.getStyleClass().add("pill-grade-b");
                        default -> pill.getStyleClass().add("pill-grade-c");
                    }
                    setGraphic(wrapper);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void setupStatusColumn() {
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            private final Label pill = new Label();
            private final StackPane wrapper = new StackPane(pill);

            {
                pill.getStyleClass().add("table-pill");
                wrapper.setAlignment(Pos.CENTER);
                wrapper.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    pill.setText(item);
                    pill.getStyleClass().removeAll("pill-status-pending", "pill-status-approved");
                    if ("Pending".equalsIgnoreCase(item)) {
                        pill.getStyleClass().add("pill-status-pending");
                    } else {
                        pill.getStyleClass().add("pill-status-approved");
                    }
                    setGraphic(wrapper);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void setupTimestampColumn() {
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timestampColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("cell-timestamp");
                } else {
                    setText(item);
                    if (!getStyleClass().contains("cell-timestamp")) {
                        getStyleClass().add("cell-timestamp");
                    }
                }
            }
        });
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button detailsBtn = new Button("Details");
            private final HBox wrapper = new HBox(detailsBtn);

            {
                detailsBtn.getStyleClass().add("details-button");
                detailsBtn.setOnAction(e -> {
                    ActivityLog log = getTableView().getItems().get(getIndex());
                    System.out.println("Details clicked for: " + log.getCropName()
                            + " / " + log.getFarmerName());
                });
                wrapper.setAlignment(Pos.CENTER);
                wrapper.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(wrapper);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void applySearchFilter(String query) {
        if (query == null || query.isBlank()) {
            filteredLogs.setPredicate(p -> true);
            return;
        }
        String lower = query.toLowerCase().trim();
        filteredLogs.setPredicate(log ->
                log.getCropName().toLowerCase().contains(lower)
                        || log.getVariety().toLowerCase().contains(lower)
                        || log.getFarmerName().toLowerCase().contains(lower)
                        || log.getLocation().toLowerCase().contains(lower)
                        || log.getStatus().toLowerCase().contains(lower)
                        || log.getGrade().toLowerCase().contains(lower)
        );
    }

    @FXML
    private void handleLaunchQueue() {
        SceneNavigator.navigate(activityTable,
                "/com/harvesthub/desktop/VerificationQueueView.fxml",
                "HarvestHub â€” Verification Queue");
    }

    @FXML
    private void handleNotifications() {
        System.out.println("Notifications clicked");
    }

    @FXML
    private void handleFilter() {
        System.out.println("Filter clicked");
    }
}

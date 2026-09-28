package com.harvesthub.desktop;

import com.harvesthub.desktop.api.*;
import com.harvesthub.desktop.model.ActivityLog;
import com.harvesthub.desktop.model.Listing;
import com.harvesthub.desktop.model.QueueListing;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DashboardController implements Initializable {

    @FXML private SidebarController sidebarController;
    @FXML private Label greetingLabel;
    @FXML private Label chipInitialsLabel;
    @FXML private Label chipNameLabel;
    @FXML private Label statPendingCount;
    @FXML private Label statReviewedCount;
    @FXML private Label statApprovalRate;
    @FXML private Label queueBannerText;

    @FXML private TextField searchField;
    @FXML private TableView<ActivityLog> activityTable;
    @FXML private TableColumn<ActivityLog, String> cropColumn;
    @FXML private TableColumn<ActivityLog, String> locationColumn;
    @FXML private TableColumn<ActivityLog, String> gradeColumn;
    @FXML private TableColumn<ActivityLog, String> statusColumn;
    @FXML private TableColumn<ActivityLog, String> timestampColumn;
    @FXML private TableColumn<ActivityLog, Void> actionsColumn;

    private final ObservableList<ActivityLog> rawLogs = FXCollections.observableArrayList();
    private FilteredList<ActivityLog> filteredLogs;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sidebarController != null) {
            sidebarController.activate(SidebarController.Nav.DASHBOARD);
        }

        setupOfficerProfile();

        filteredLogs = new FilteredList<>(rawLogs, p -> true);
        activityTable.setItems(filteredLogs);
        activityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        activityTable.setFocusTraversable(false);
        activityTable.setPlaceholder(createPlaceholderLabel("Loading activity logs from database..."));

        setupCropColumn();
        setupLocationColumn();
        setupGradeColumn();
        setupStatusColumn();
        setupTimestampColumn();
        setupActionsColumn();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySearchFilter(newVal));

        loadDataFromBackend();
    }

    private void setupOfficerProfile() {
        OfficerDto officer = Session.getOfficer();
        if (officer != null) {
            String name = officer.fullName != null && !officer.fullName.isBlank() ? officer.fullName : "Officer";
            String first = name.contains(" ") ? name.substring(0, name.indexOf(' ')) : name;
            if (greetingLabel != null) greetingLabel.setText("Good day, " + first);
            if (chipNameLabel != null) chipNameLabel.setText(name);

            String initials = "OF";
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1 && !parts[0].isEmpty()) {
                initials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            } else if (parts.length > 1) {
                initials = ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
            }
            if (chipInitialsLabel != null) chipInitialsLabel.setText(initials);
        }
    }

    private void loadDataFromBackend() {
        Thread worker = new Thread(() -> {
            try {
                List<ActivityLogDto> dtos = ApiClient.getRecentActivity();
                OfficerStatsDto stats = ApiClient.getDashboardStats();

                Platform.runLater(() -> {
                    rawLogs.clear();
                    if (dtos != null && !dtos.isEmpty()) {
                        rawLogs.addAll(dtos.stream().map(ActivityLog::fromDto).collect(Collectors.toList()));
                    } else {
                        // Fallback sample if database has no activity yet
                        loadDefaultMockLogs();
                    }

                    if (stats != null) {
                        if (statPendingCount != null) statPendingCount.setText(String.valueOf(stats.pendingCount));
                        if (statReviewedCount != null) statReviewedCount.setText(String.valueOf(stats.reviewedTodayCount));
                        if (statApprovalRate != null) statApprovalRate.setText(stats.approvalRate + "%");
                        if (queueBannerText != null) {
                            queueBannerText.setText("QUEUE ACTION: Go to Verification Queue (" + stats.pendingCount + " items awaiting review)");
                        }
                    }
                    activityTable.setPlaceholder(createPlaceholderLabel("No activity logs found"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (rawLogs.isEmpty()) {
                        loadDefaultMockLogs();
                    }
                    activityTable.setPlaceholder(createPlaceholderLabel("Offline mode: Using cached activity logs"));
                });
            }
        }, "dashboard-data-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void loadDefaultMockLogs() {
        rawLogs.addAll(
                new ActivityLog("Paddy", "Bg 300", "Kalinga Perera",
                        "Anuradhapura North Sector", "A", "Pending", "10:30 AM"),
                new ActivityLog("Cinnamon", "Sri Lankan Alba", "Anura Silva",
                        "Galle Central Hub", "B", "Pending", "09:45 AM"),
                new ActivityLog("Tea", "Nuwara Eliya Pekoe", "Bandara Wijesinghe",
                        "Ella Highlands Plains", "C", "Pending", "08:15 AM"),
                new ActivityLog("Big Onions", "Dambulla Red", "Priyantha Kumara",
                        "Dambulla Town", "A", "Approved", "Yesterday")
        );
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
                    switch (item.toUpperCase()) {
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
                    if ("Pending".equalsIgnoreCase(item) || "Under Review".equalsIgnoreCase(item)) {
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
                    if (log != null) {
                        Thread loader = new Thread(() -> {
                            Listing l = null;
                            if (log.getId() != null) {
                                try {
                                    ListingDetailDto dto = ApiClient.getListing(log.getId());
                                    if (dto != null) l = Listing.fromDto(dto);
                                } catch (Exception ignored) {}
                            }
                            if (l == null) {
                                l = Listing.fromQueueListing(new QueueListing(log.getId(), log.getCropName(), log.getFarmerName(), log.getLocation(), log.getTimestamp(), "500 kg", "Rs. 250/kg", log.getStatus(), true, QueueListing.CropTone.AMBER, "fas-seedling"));
                            }
                            final Listing target = l;
                            Platform.runLater(() -> SceneNavigator.navigateToReview(detailsBtn, target));
                        });
                        loader.setDaemon(true);
                        loader.start();
                    }
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
                "HarvestHub — Verification Queue");
    }

    @FXML
    private void handleNotifications() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("Officer Notifications");
        alert.setContentText("You have no unread system notifications at this time.");
        alert.showAndWait();
    }

    @FXML
    private void handleFilter() {
        searchField.requestFocus();
    }
}

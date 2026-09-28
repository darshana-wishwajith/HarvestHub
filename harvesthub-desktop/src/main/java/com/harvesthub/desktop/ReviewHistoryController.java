package com.harvesthub.desktop;

import com.harvesthub.desktop.model.Listing;
import com.harvesthub.desktop.model.ReviewHistoryRecord;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import java.util.Locale;
import java.util.ResourceBundle;

public class ReviewHistoryController implements Initializable {

    @FXML private SidebarController sidebarController;
    @FXML private TextField globalSearchField;
    @FXML private TextField recordsSearchField;
    @FXML private ComboBox<String> decisionTypeCombo;
    @FXML private ComboBox<String> cropTypeCombo;
    @FXML private ComboBox<String> dateRangeCombo;
    @FXML private TableView<ReviewHistoryRecord> historyTable;
    @FXML private TableColumn<ReviewHistoryRecord, String> cropColumn;
    @FXML private TableColumn<ReviewHistoryRecord, String> locationColumn;
    @FXML private TableColumn<ReviewHistoryRecord, String> decisionColumn;
    @FXML private TableColumn<ReviewHistoryRecord, String> dateColumn;
    @FXML private TableColumn<ReviewHistoryRecord, Void> actionColumn;
    @FXML private Button page1Button;
    @FXML private Button page2Button;
    @FXML private Button page3Button;
    @FXML private Button page4Button;
    @FXML private Button page18Button;

    private ObservableList<ReviewHistoryRecord> rawRecords;
    private FilteredList<ReviewHistoryRecord> filteredRecords;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sidebarController != null) {
            sidebarController.activate(SidebarController.Nav.REVIEW_HISTORY);
        }

        decisionTypeCombo.setItems(FXCollections.observableArrayList(
                "All Decisions", "Approved", "Retake Requested", "Rejected"));
        cropTypeCombo.setItems(FXCollections.observableArrayList(
                "All Crops", "Paddy", "Tea", "Cinnamon", "Onion", "Tomato", "Potato"));
        dateRangeCombo.setItems(FXCollections.observableArrayList(
                "Last 30 Days", "Last 7 Days", "Last 90 Days", "All Time"));

        decisionTypeCombo.getSelectionModel().select("All Decisions");
        cropTypeCombo.getSelectionModel().select("All Crops");
        dateRangeCombo.getSelectionModel().select("Last 30 Days");

        dateRangeCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            {
                setGraphicTextGap(8);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    FontIcon icon = new FontIcon("fas-calendar-alt");
                    icon.setIconSize(13);
                    icon.getStyleClass().add("history-calendar-icon");
                    setGraphic(icon);
                }
            }
        });
        dateRangeCombo.setValue(dateRangeCombo.getValue());

        rawRecords = FXCollections.observableArrayList();
        filteredRecords = new FilteredList<>(rawRecords, r -> true);
        historyTable.setItems(filteredRecords);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.setFocusTraversable(false);
        historyTable.setPlaceholder(createPlaceholder("Loading review history..."));

        setupCropColumn();
        setupLocationColumn();
        setupDecisionColumn();
        setupDateColumn();
        setupActionColumn();

        globalSearchField.textProperty().addListener((obs, o, n) -> applyFilters());
        recordsSearchField.textProperty().addListener((obs, o, n) -> applyFilters());
        decisionTypeCombo.setOnAction(e -> applyFilters());
        cropTypeCombo.setOnAction(e -> applyFilters());
        dateRangeCombo.setOnAction(e -> applyFilters());

        loadHistoryFromBackend();
    }

    private Label createPlaceholder(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("activity-placeholder");
        return label;
    }

    private void setupCropColumn() {
        cropColumn.setCellValueFactory(new PropertyValueFactory<>("cropName"));
        cropColumn.setCellFactory(col -> new TableCell<>() {
            private final HBox row = new HBox(12);
            private final StackPane avatar = new StackPane();
            private final Label avatarLabel = new Label();
            private final VBox textBox = new VBox(3);
            private final HBox titleRow = new HBox(4);
            private final Label cropLabel = new Label();
            private final Label varietyLabel = new Label();
            private final Label farmerLabel = new Label();

            {
                avatar.getStyleClass().add("history-crop-avatar");
                avatar.setPrefSize(40, 40);
                avatar.setMinSize(40, 40);
                avatar.setMaxSize(40, 40);
                avatarLabel.getStyleClass().add("history-crop-avatar-text");
                avatar.getChildren().add(avatarLabel);

                cropLabel.getStyleClass().add("cell-crop-name");
                varietyLabel.getStyleClass().add("cell-variety");
                farmerLabel.getStyleClass().add("cell-farmer");
                titleRow.setAlignment(Pos.CENTER_LEFT);
                titleRow.getChildren().addAll(cropLabel, varietyLabel);
                textBox.getChildren().addAll(titleRow, farmerLabel);
                textBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                row.setAlignment(Pos.CENTER_LEFT);
                row.getChildren().addAll(avatar, textBox);
                row.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    ReviewHistoryRecord record = getTableRow() != null ? getTableRow().getItem() : null;
                    if (record != null) {
                        avatar.getChildren().clear();
                        Label initials = new Label(record.getAvatarCode());
                        initials.getStyleClass().add("avatar-initials");
                        avatar.getChildren().add(initials);

                        cropLabel.setText(record.getCropName());
                        varietyLabel.setText("(" + record.getVariety() + ")");
                        farmerLabel.setText("Farmer: " + record.getFarmerName());
                    }
                    setGraphic(row);
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

    private void setupDecisionColumn() {
        decisionColumn.setCellValueFactory(new PropertyValueFactory<>("decisionLabel"));
        decisionColumn.setCellFactory(col -> new TableCell<>() {
            private final Label pill = new Label();
            private final StackPane wrapper = new StackPane(pill);

            {
                pill.getStyleClass().add("table-pill");
                wrapper.setAlignment(Pos.CENTER_LEFT);
                wrapper.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    ReviewHistoryRecord record = getTableRow().getItem();
                    pill.setText(record.getDecisionLabel());
                    pill.getStyleClass().removeAll(
                            "history-pill-grade-ab", "history-pill-grade-c",
                            "history-pill-retake", "history-pill-rejected");
                    switch (record.getDecisionKind()) {
                        case GRADE_A, GRADE_B -> pill.getStyleClass().add("history-pill-grade-ab");
                        case GRADE_C -> pill.getStyleClass().add("history-pill-grade-c");
                        case RETAKE -> pill.getStyleClass().add("history-pill-retake");
                        case REJECTED -> pill.getStyleClass().add("history-pill-rejected");
                    }
                    setGraphic(wrapper);
                    setText(null);
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });
    }

    private void setupDateColumn() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateReviewed"));
        dateColumn.setCellFactory(col -> new TableCell<>() {
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

    private void setupActionColumn() {
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button link = new Button("View Details");
            private final StackPane wrapper = new StackPane(link);

            {
                link.getStyleClass().add("history-view-link");
                link.setMnemonicParsing(false);
                link.setOnAction(e -> {
                    ReviewHistoryRecord record = getTableView().getItems().get(getIndex());
                    if (record != null) {
                        Thread loader = new Thread(() -> {
                            Listing l = null;
                            if (record.getId() != null) {
                                try {
                                    com.harvesthub.desktop.api.ListingDetailDto dto =
                                            com.harvesthub.desktop.api.ApiClient.getListing(record.getId());
                                    if (dto != null) l = Listing.fromDto(dto);
                                } catch (Exception ignored) {}
                            }
                            if (l == null) {
                                l = Listing.fromReviewHistoryRecord(record);
                            }
                            final Listing target = l;
                            javafx.application.Platform.runLater(() -> SceneNavigator.navigateToReview(link, target));
                        });
                        loader.setDaemon(true);
                        loader.start();
                    }
                });
                wrapper.setAlignment(Pos.CENTER_RIGHT);
                wrapper.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(wrapper);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });
    }

    private void loadHistoryFromBackend() {
        Thread worker = new Thread(() -> {
            try {
                String search = recordsSearchField != null && recordsSearchField.getText() != null
                        ? recordsSearchField.getText().trim() : "";
                List<com.harvesthub.desktop.api.ReviewHistoryRecordDto> dtos =
                        com.harvesthub.desktop.api.ApiClient.getReviewHistory(search);
                javafx.application.Platform.runLater(() -> {
                    rawRecords.clear();
                    if (dtos != null && !dtos.isEmpty()) {
                        rawRecords.addAll(dtos.stream().map(ReviewHistoryRecord::fromDto).collect(java.util.stream.Collectors.toList()));
                    } else {
                        loadMockHistory();
                    }
                    historyTable.setPlaceholder(createPlaceholder("No review history found"));
                    applyFilters();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    if (rawRecords.isEmpty()) {
                        loadMockHistory();
                    }
                    historyTable.setPlaceholder(createPlaceholder("Offline: Cached review history"));
                    applyFilters();
                });
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void loadMockHistory() {
        rawRecords.addAll(List.of(
                new ReviewHistoryRecord("listing_mock_1", "PA", "Paddy", "Bg 300", "Kalinga Perera",
                        "Anuradhapura North", "Grade A",
                        ReviewHistoryRecord.DecisionKind.GRADE_A, "Oct 24, 2023"),
                new ReviewHistoryRecord("listing_mock_2", "TE", "Tea", "Nuwara Eliya Pekoe", "Anura Silva",
                        "Nuwara Eliya East", "Grade B",
                        ReviewHistoryRecord.DecisionKind.GRADE_B, "Oct 24, 2023"),
                new ReviewHistoryRecord("listing_mock_3", "CI", "Cinnamon", "Sri Lankan Alba", "Bandara Wijesinghe",
                        "Galle Coast", "Retake Requested",
                        ReviewHistoryRecord.DecisionKind.RETAKE, "Oct 23, 2023"),
                new ReviewHistoryRecord("listing_mock_4", "ON", "Onion", "Dambulla Red", "Priyantha Kumara",
                        "Dambulla Town", "Grade C",
                        ReviewHistoryRecord.DecisionKind.GRADE_C, "Oct 23, 2023")
        ));
    }

    private void applyFilters() {
        String global = normalize(globalSearchField.getText());
        String records = normalize(recordsSearchField.getText());
        String decision = decisionTypeCombo.getValue();
        String crop = cropTypeCombo.getValue();

        filteredRecords.setPredicate(record ->
                matchesSearch(record, global)
                        && matchesSearch(record, records)
                        && matchesDecision(record, decision)
                        && matchesCrop(record, crop));
    }

    private boolean matchesSearch(ReviewHistoryRecord record, String query) {
        if (query.isBlank()) {
            return true;
        }
        return record.getCropName().toLowerCase(Locale.ROOT).contains(query)
                || record.getVariety().toLowerCase(Locale.ROOT).contains(query)
                || record.getFarmerName().toLowerCase(Locale.ROOT).contains(query)
                || record.getLocation().toLowerCase(Locale.ROOT).contains(query)
                || record.getDecisionLabel().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesDecision(ReviewHistoryRecord record, String decision) {
        if (decision == null || "All Decisions".equals(decision)) {
            return true;
        }
        return switch (decision) {
            case "Approved" -> record.getDecisionKind() == ReviewHistoryRecord.DecisionKind.GRADE_A
                    || record.getDecisionKind() == ReviewHistoryRecord.DecisionKind.GRADE_B
                    || record.getDecisionKind() == ReviewHistoryRecord.DecisionKind.GRADE_C;
            case "Retake Requested" ->
                    record.getDecisionKind() == ReviewHistoryRecord.DecisionKind.RETAKE;
            case "Rejected" ->
                    record.getDecisionKind() == ReviewHistoryRecord.DecisionKind.REJECTED;
            default -> true;
        };
    }

    private boolean matchesCrop(ReviewHistoryRecord record, String crop) {
        if (crop == null || "All Crops".equals(crop)) {
            return true;
        }
        return record.getCropName().equalsIgnoreCase(crop);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void setActivePage(Button active) {
        for (Button button : new Button[]{page1Button, page2Button, page3Button, page4Button, page18Button}) {
            button.getStyleClass().remove("page-number-active");
        }
        if (!active.getStyleClass().contains("page-number-active")) {
            active.getStyleClass().add("page-number-active");
        }
    }

    @FXML
    private void handleApplyFilters() {
        applyFilters();
    }

    @FXML
    private void handlePagePrev() {
        System.out.println("Pagination: previous");
    }

    @FXML
    private void handlePageNext() {
        System.out.println("Pagination: next");
    }

    @FXML
    private void handlePage1() {
        setActivePage(page1Button);
    }

    @FXML
    private void handlePage2() {
        setActivePage(page2Button);
    }

    @FXML
    private void handlePage3() {
        setActivePage(page3Button);
    }

    @FXML
    private void handlePage4() {
        setActivePage(page4Button);
    }

    @FXML
    private void handlePage18() {
        setActivePage(page18Button);
    }
}

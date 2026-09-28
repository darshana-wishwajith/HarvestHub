package com.harvesthub.desktop;

import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.api.OfficerDto;
import com.harvesthub.desktop.api.QueueListingDto;
import com.harvesthub.desktop.api.Session;
import com.harvesthub.desktop.model.QueueListing;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class VerificationQueueController implements Initializable {

    @FXML private SidebarController sidebarController;
    @FXML private Label queueSubtitleLabel;
    @FXML private Label chipInitialsLabel;
    @FXML private Label chipNameLabel;
    @FXML private Label paginationLabel;

    @FXML private TextField searchField;
    @FXML private Button tabAll;
    @FXML private Button tabUnclaimed;
    @FXML private Button tabClaimedByMe;
    @FXML private ScrollPane listingsScroll;
    @FXML private VBox listingsContainer;
    @FXML private Button page1Button;
    @FXML private Button page2Button;
    @FXML private Button page3Button;

    private final List<QueueListing> allListings = new ArrayList<>();
    private String activeTab = "all";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sidebarController != null) {
            sidebarController.activate(SidebarController.Nav.VERIFICATION_QUEUE);
        }

        setupOfficerProfile();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshListings());

        loadQueueFromBackend();
    }

    private void setupOfficerProfile() {
        OfficerDto officer = Session.getOfficer();
        if (officer != null) {
            String name = officer.fullName != null && !officer.fullName.isBlank() ? officer.fullName : "Officer";
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

    private void loadQueueFromBackend() {
        Thread worker = new Thread(() -> {
            try {
                String search = searchField != null && searchField.getText() != null ? searchField.getText().trim() : "";
                List<QueueListingDto> dtos = ApiClient.getQueue(activeTab, search);

                Platform.runLater(() -> {
                    allListings.clear();
                    if (dtos != null && !dtos.isEmpty()) {
                        allListings.addAll(dtos.stream().map(QueueListing::fromDto).collect(Collectors.toList()));
                    } else {
                        loadDefaultMockListings();
                    }
                    if (queueSubtitleLabel != null) {
                        queueSubtitleLabel.setText(allListings.size() + " listings awaiting review across your region");
                    }
                    refreshListings();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (allListings.isEmpty()) {
                        loadDefaultMockListings();
                    }
                    refreshListings();
                });
            }
        }, "queue-data-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void loadDefaultMockListings() {
        allListings.addAll(List.of(
                new QueueListing("listing_mock_1", "Paddy", "Kalinga Perera", "Anuradhapura North",
                        "2 hours ago", "450 kg", "LKR 120/kg",
                        "Pending", true, QueueListing.CropTone.AMBER, "fas-seedling"),
                new QueueListing("listing_mock_2", "Cinnamon", "Anura Silva", "Galle Central",
                        "4 hours ago", "120 kg", "LKR 540/kg",
                        "Pending", true, QueueListing.CropTone.BROWN, "fas-leaf"),
                new QueueListing("listing_mock_3", "Tea", "Bandara Wijesinghe", "Nuwara Eliya East",
                        "5 hours ago", "800 kg", "LKR 165/kg",
                        "Pending", true, QueueListing.CropTone.TEAL, "fas-leaf"),
                new QueueListing("listing_mock_4", "Onion", "Priyantha Kumara", "Dambulla Town",
                        "6 hours ago", "300 kg", "LKR 195/kg",
                        "Pending", true, QueueListing.CropTone.PURPLE, "fas-seedling")
        ));
    }

    private void refreshListings() {
        String query = searchField.getText() == null
                ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        List<QueueListing> filtered = allListings.stream()
                .filter(this::matchesTab)
                .filter(listing -> matchesQuery(listing, query))
                .collect(Collectors.toList());

        listingsContainer.getChildren().clear();
        for (QueueListing listing : filtered) {
            listingsContainer.getChildren().add(new ListingRow(listing));
        }

        if (paginationLabel != null) {
            paginationLabel.setText("Showing " + filtered.size() + " of " + allListings.size() + " listings");
        }
    }

    private boolean matchesTab(QueueListing listing) {
        return switch (activeTab) {
            case "unclaimed" -> listing.isClaimable();
            case "claimed" -> !listing.isClaimable();
            default -> true;
        };
    }

    private boolean matchesQuery(QueueListing listing, String query) {
        if (query.isBlank()) {
            return true;
        }
        return listing.getCropName().toLowerCase(Locale.ROOT).contains(query)
                || listing.getFarmerName().toLowerCase(Locale.ROOT).contains(query)
                || listing.getLocation().toLowerCase(Locale.ROOT).contains(query);
    }

    private void setActiveTab(String tab) {
        activeTab = tab;
        tabAll.getStyleClass().remove("queue-segment-active");
        tabUnclaimed.getStyleClass().remove("queue-segment-active");
        tabClaimedByMe.getStyleClass().remove("queue-segment-active");
        switch (tab) {
            case "unclaimed" -> tabUnclaimed.getStyleClass().add("queue-segment-active");
            case "claimed" -> tabClaimedByMe.getStyleClass().add("queue-segment-active");
            default -> tabAll.getStyleClass().add("queue-segment-active");
        }
        loadQueueFromBackend();
    }

    private void setActivePage(int page) {
        page1Button.getStyleClass().remove("page-number-active");
        page2Button.getStyleClass().remove("page-number-active");
        page3Button.getStyleClass().remove("page-number-active");
        switch (page) {
            case 2 -> page2Button.getStyleClass().add("page-number-active");
            case 3 -> page3Button.getStyleClass().add("page-number-active");
            default -> page1Button.getStyleClass().add("page-number-active");
        }
    }

    @FXML
    private void handleTabAll() {
        setActiveTab("all");
    }

    @FXML
    private void handleTabUnclaimed() {
        setActiveTab("unclaimed");
    }

    @FXML
    private void handleTabClaimedByMe() {
        setActiveTab("claimed");
    }

    @FXML
    private void handleFilterCropType() {
        searchField.setText("Paddy");
    }

    @FXML
    private void handleFilterLocation() {
        searchField.setText("Dambulla");
    }

    @FXML
    private void handleFilterSubmittedDate() {
        loadQueueFromBackend();
    }

    @FXML
    private void handleNotifications() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("Officer Notifications");
        alert.setContentText("You have no unread notifications.");
        alert.showAndWait();
    }

    @FXML
    private void handlePagePrev() {
        setActivePage(1);
    }

    @FXML
    private void handlePageNext() {
        setActivePage(2);
    }

    @FXML
    private void handlePage1() {
        setActivePage(1);
    }

    @FXML
    private void handlePage2() {
        setActivePage(2);
    }

    @FXML
    private void handlePage3() {
        setActivePage(3);
    }
}

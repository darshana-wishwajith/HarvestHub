package com.harvesthub.desktop;

import com.harvesthub.desktop.model.QueueListing;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
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

        allListings.addAll(List.of(
                new QueueListing("Paddy", "Kalinga Perera", "Anuradhapura North",
                        "2 hours ago", "450 kg", "LKR 120/kg",
                        "Pending", true, QueueListing.CropTone.AMBER, "fas-seedling"),
                new QueueListing("Cinnamon", "Anura Silva", "Galle Central",
                        "4 hours ago", "120 kg", "LKR 540/kg",
                        "Pending", true, QueueListing.CropTone.BROWN, "fas-leaf"),
                new QueueListing("Tea", "Bandara Wijesinghe", "Nuwara Eliya East",
                        "5 hours ago", "800 kg", "LKR 165/kg",
                        "Pending", true, QueueListing.CropTone.TEAL, "fas-leaf"),
                new QueueListing("Onion", "Priyantha Kumara", "Dambulla Town",
                        "6 hours ago", "300 kg", "LKR 195/kg",
                        "Pending", true, QueueListing.CropTone.PURPLE, "fas-seedling")
        ));

        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshListings());
        refreshListings();
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
        refreshListings();
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
        System.out.println("Filter: Crop Type");
    }

    @FXML
    private void handleFilterLocation() {
        System.out.println("Filter: Location");
    }

    @FXML
    private void handleFilterSubmittedDate() {
        System.out.println("Filter: Submitted Date");
    }

    @FXML
    private void handleNotifications() {
        System.out.println("Notifications clicked");
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

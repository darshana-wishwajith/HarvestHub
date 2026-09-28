package com.harvesthub.desktop;

import com.harvesthub.desktop.model.Listing;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class ReviewController implements Initializable {

    private enum DecisionSection {
        APPROVE, RETAKE, REJECT
    }

    @FXML private SidebarController sidebarController;

    @FXML private Button backButton;
    @FXML private Label headerTitleLabel;
    @FXML private Label headerSubLabel;
    @FXML private Label mediaBadgeLabel;
    @FXML private Label mediaCaption1Label;
    @FXML private Label mediaCaption2Label;
    @FXML private Label videoLabelText;
    @FXML private Label gpsLabel;
    @FXML private Label timestampLabel;
    @FXML private Label deviceLabel;
    @FXML private Label mapParcelLabel;
    @FXML private Label refCodeLabel;
    @FXML private Label statusPillLabel;
    @FXML private Label farmerAvatarLabel;
    @FXML private Label farmerNameLabel;
    @FXML private Label farmerMetaLabel;
    @FXML private Label cropValueLabel;
    @FXML private Label yieldValueLabel;
    @FXML private Label yieldSubLabel;
    @FXML private Label priceValueLabel;
    @FXML private Label totalValueLabel;

    @FXML private HBox accordionHeader1;
    @FXML private HBox accordionHeader2;
    @FXML private HBox accordionHeader3;
    @FXML private VBox accordionBody1;
    @FXML private VBox accordionBody2;
    @FXML private VBox accordionBody3;
    @FXML private FontIcon chevron1;
    @FXML private FontIcon chevron2;
    @FXML private FontIcon chevron3;

    @FXML private Button gradeAButton;
    @FXML private Button gradeBButton;
    @FXML private Button gradeCButton;
    @FXML private TextArea approveNotesArea;
    @FXML private TextArea retakeNotesArea;
    @FXML private TextArea rejectNotesArea;

    private Listing listing;
    private String selectedGrade = "A";
    private DecisionSection activeSection = DecisionSection.APPROVE;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (sidebarController != null) {
            sidebarController.activate(SidebarController.Nav.VERIFICATION_QUEUE);
        }
    }

    public void initListing(Listing listing) {
        this.listing = listing != null ? listing : Listing.getDefaultSample();
        populateListing();
        selectGrade("A");
        expandSection(DecisionSection.APPROVE);
    }

    private void populateListing() {
        headerTitleLabel.setText("Reviewing: " + listing.getCropName() + " â€“ " + listing.getFarmerName());
        headerSubLabel.setText(listing.getLocation() + " â€¢ Submitted " + listing.getSubmittedTimeAgo());

        mediaBadgeLabel.setText(listing.getMediaFileCount() + " Files Total");
        mediaCaption1Label.setText(listing.getMediaCaption1());
        mediaCaption2Label.setText(listing.getMediaCaption2());
        videoLabelText.setText(listing.getVideoLabel());

        gpsLabel.setText("GPS: " + listing.getGpsCoordinates());
        timestampLabel.setText("Timestamp: " + listing.getSubmittedTimestamp());
        deviceLabel.setText("Device: " + listing.getDeviceModel());

        mapParcelLabel.setText(buildParcelLabel());

        refCodeLabel.setText(listing.getReferenceCode());
        statusPillLabel.setText(listing.getStatus());
        farmerAvatarLabel.setText(listing.getFarmerAvatar());
        farmerNameLabel.setText(listing.getFarmerName());
        farmerMetaLabel.setText(listing.getFarmerIdNumber() + "  |  " + listing.getCooperativeName());

        cropValueLabel.setText(listing.getCropName());
        yieldValueLabel.setText(listing.getQuantity());
        yieldSubLabel.setText(listing.getVariancePercent());
        priceValueLabel.setText(listing.getPricePerUnit());
        totalValueLabel.setText(listing.getTotalValuation());
    }

    private String buildParcelLabel() {
        String farmer = listing.getFarmerName();
        String firstName = farmer.contains(" ")
                ? farmer.substring(0, farmer.indexOf(' '))
                : farmer;
        String location = listing.getLocation().toUpperCase(Locale.ROOT);
        if (location.contains(" ")) {
            location = location.substring(0, location.indexOf(' '));
        }
        return firstName.toUpperCase(Locale.ROOT) + "'S PARCEL (" + location + ")";
    }

    private void expandSection(DecisionSection section) {
        activeSection = section;

        setSectionExpanded(DecisionSection.APPROVE, section == DecisionSection.APPROVE);
        setSectionExpanded(DecisionSection.RETAKE, section == DecisionSection.RETAKE);
        setSectionExpanded(DecisionSection.REJECT, section == DecisionSection.REJECT);
    }

    private void setSectionExpanded(DecisionSection section, boolean expanded) {
        switch (section) {
            case APPROVE -> {
                accordionBody1.setVisible(expanded);
                accordionBody1.setManaged(expanded);
                updateHeaderStyle(accordionHeader1, expanded,
                        "accordion-header-1-expanded", null);
                chevron1.setIconLiteral(expanded ? "fas-chevron-up" : "fas-chevron-down");
            }
            case RETAKE -> {
                accordionBody2.setVisible(expanded);
                accordionBody2.setManaged(expanded);
                updateHeaderStyle(accordionHeader2, expanded,
                        "accordion-header-2-expanded", "accordion-header-2-collapsed");
                chevron2.setIconLiteral(expanded ? "fas-chevron-up" : "fas-chevron-down");
            }
            case REJECT -> {
                accordionBody3.setVisible(expanded);
                accordionBody3.setManaged(expanded);
                updateHeaderStyle(accordionHeader3, expanded,
                        "accordion-header-3-expanded", "accordion-header-3-collapsed");
                chevron3.setIconLiteral(expanded ? "fas-chevron-up" : "fas-chevron-down");
            }
        }
    }

    private void updateHeaderStyle(HBox header, boolean expanded,
                                   String expandedClass, String collapsedClass) {
        header.getStyleClass().removeAll(
                "accordion-header-1-expanded",
                "accordion-header-2-expanded", "accordion-header-2-collapsed",
                "accordion-header-3-expanded", "accordion-header-3-collapsed"
        );
        if (expanded) {
            header.getStyleClass().add(expandedClass);
        } else if (collapsedClass != null) {
            header.getStyleClass().add(collapsedClass);
        }
    }

    private void selectGrade(String grade) {
        selectedGrade = grade;
        gradeAButton.getStyleClass().remove("grade-toggle-btn-selected");
        gradeBButton.getStyleClass().remove("grade-toggle-btn-selected");
        gradeCButton.getStyleClass().remove("grade-toggle-btn-selected");

        switch (grade) {
            case "A" -> gradeAButton.getStyleClass().add("grade-toggle-btn-selected");
            case "B" -> gradeBButton.getStyleClass().add("grade-toggle-btn-selected");
            case "C" -> gradeCButton.getStyleClass().add("grade-toggle-btn-selected");
            default -> gradeAButton.getStyleClass().add("grade-toggle-btn-selected");
        }
    }

    @FXML
    private void handleBackToQueue() {
        SceneNavigator.navigate(backButton,
                "/com/harvesthub/desktop/VerificationQueueView.fxml",
                "HarvestHub â€” Verification Queue");
    }

    @FXML
    private void handleNotifications() {
        System.out.println("Notifications clicked");
    }

    @FXML
    private void handleAccordionApprove() {
        expandSection(DecisionSection.APPROVE);
    }

    @FXML
    private void handleAccordionRetake() {
        expandSection(DecisionSection.RETAKE);
    }

    @FXML
    private void handleAccordionReject() {
        expandSection(DecisionSection.REJECT);
    }

    @FXML
    private void handleGradeA() {
        selectGrade("A");
    }

    @FXML
    private void handleGradeB() {
        selectGrade("B");
    }

    @FXML
    private void handleGradeC() {
        selectGrade("C");
    }

    @FXML
    private void handleConfirmApproval() {
        showDecisionAlert("Approval Confirmed",
                "Listing approved with Grade " + selectedGrade + " for "
                        + listing.getFarmerName() + "'s " + listing.getCropName() + ".");
        handleBackToQueue();
    }

    @FXML
    private void handleSubmitRetake() {
        if (isBlank(retakeNotesArea.getText())) {
            showWarning("Retake instructions are required before submitting.");
            expandSection(DecisionSection.RETAKE);
            return;
        }
        showDecisionAlert("Retake Requested",
                "Media retake instructions sent to " + listing.getFarmerName() + ".");
        handleBackToQueue();
    }

    @FXML
    private void handleSubmitRejection() {
        if (isBlank(rejectNotesArea.getText())) {
            showWarning("A rejection reason is required before submitting.");
            expandSection(DecisionSection.REJECT);
            return;
        }
        showDecisionAlert("Listing Rejected",
                listing.getCropName() + " listing from " + listing.getFarmerName() + " has been rejected.");
        handleBackToQueue();
    }

    private void showDecisionAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Missing Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

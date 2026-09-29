package com.harvesthub.desktop;

import com.harvesthub.desktop.model.Listing;
import com.harvesthub.desktop.model.QueueListing;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Reusable Verification Queue listing row. Build from a {@link QueueListing} data model.
 * Column widths are fixed so every row aligns on the same vertical lines.
 */
public class ListingRow extends HBox {

    private static final double COL_CROP = 210;
    private static final double COL_LOCATION = 200;
    private static final double COL_QUANTITY = 140;
    private static final double COL_STATUS = 100;

    private final QueueListing listing;

    public ListingRow(QueueListing listing) {
        this.listing = listing;
        getStyleClass().add("listing-row");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        setMaxWidth(Double.MAX_VALUE);
        setPadding(new Insets(18, 20, 18, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setMinWidth(8);

        getChildren().addAll(
                buildCropIcon(listing),
                buildColumn("CROP & FARMER", listing.getCropName(),
                        "Farmer: " + listing.getFarmerName(), COL_CROP),
                buildColumn("LOCATION & TIME", listing.getLocation(),
                        listing.getSubmittedAgo(), COL_LOCATION),
                buildColumn("QUANTITY & PRICE", listing.getQuantity(),
                        listing.getPricePerKg(), COL_QUANTITY),
                spacer,
                buildStatusPill(listing),
                buildClaimButton(listing)
        );
    }

    private StackPane buildCropIcon(QueueListing listing) {
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().addAll("listing-crop-icon", toneClass(listing.getTone()));
        iconBox.setPrefSize(50, 50);
        iconBox.setMinSize(50, 50);
        iconBox.setMaxSize(50, 50);

        FontIcon icon = new FontIcon(listing.getIconLiteral());
        icon.setIconSize(20);
        icon.getStyleClass().add("listing-crop-icon-glyph");
        iconBox.getChildren().add(icon);
        return iconBox;
    }

    private VBox buildColumn(String header, String primary, String secondary, double width) {
        Label headerLabel = new Label(header);
        headerLabel.getStyleClass().add("listing-col-header");
        headerLabel.setMaxWidth(width);

        Label primaryLabel = new Label(primary);
        primaryLabel.getStyleClass().add("listing-col-primary");
        primaryLabel.setMaxWidth(width);
        primaryLabel.setWrapText(false);
        primaryLabel.setEllipsisString("â€¦");

        Label secondaryLabel = new Label(secondary);
        secondaryLabel.getStyleClass().add("listing-col-secondary");
        secondaryLabel.setMaxWidth(width);
        secondaryLabel.setWrapText(false);
        secondaryLabel.setEllipsisString("â€¦");

        VBox column = new VBox(3, headerLabel, primaryLabel, secondaryLabel);
        column.setAlignment(Pos.CENTER_LEFT);
        column.setMinWidth(width);
        column.setPrefWidth(width);
        column.setMaxWidth(width);

        return column;
    }

    private StackPane buildStatusPill(QueueListing listing) {
        String label = listing.getStatusLabel() != null ? listing.getStatusLabel() : "Pending";
        Label pill = new Label(label);
        pill.getStyleClass().add("listing-status-pill");
        if (label.startsWith("Grade") || "Approved".equalsIgnoreCase(label) || "Active".equalsIgnoreCase(label) || "Listed".equalsIgnoreCase(label)) {
            pill.getStyleClass().add("listing-status-approved");
        } else if ("Rejected".equalsIgnoreCase(label)) {
            pill.getStyleClass().add("listing-status-rejected");
        } else if ("In Review".equalsIgnoreCase(label) || "Under Review".equalsIgnoreCase(label)) {
            pill.getStyleClass().add("listing-status-review");
        } else {
            pill.getStyleClass().add("listing-status-pending");
        }

        StackPane wrapper = new StackPane(pill);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMinWidth(COL_STATUS);
        wrapper.setPrefWidth(COL_STATUS);
        wrapper.setMaxWidth(COL_STATUS);
        return wrapper;
    }

    private Button buildClaimButton(QueueListing listing) {
        String label = listing.getStatusLabel();
        boolean isReviewed = label != null && (label.startsWith("Grade")
                || "Approved".equalsIgnoreCase(label)
                || "Active".equalsIgnoreCase(label)
                || "Listed".equalsIgnoreCase(label)
                || "Rejected".equalsIgnoreCase(label));
        boolean isInReview = label != null && ("In Review".equalsIgnoreCase(label) || "Under Review".equalsIgnoreCase(label));

        String buttonText;
        if (isReviewed) {
            buttonText = "Details";
        } else if (isInReview) {
            buttonText = "Review";
        } else {
            buttonText = "Claim & Review";
        }

        Button button = new Button(buttonText);
        button.getStyleClass().add("claim-review-button");
        button.setMnemonicParsing(false);
        button.setMinWidth(140);
        button.setPrefWidth(140);
        button.setMaxWidth(140);

        if (!listing.isClaimable() && !isReviewed && !isInReview) {
            button.setDisable(true);
            button.getStyleClass().add("claim-review-button-disabled");
        } else {
            button.setOnAction(e -> {
                button.setDisable(true);
                button.setText("Loading...");
                Thread worker = new Thread(() -> {
                    Listing target = null;
                    if (listing.getId() != null) {
                        if (!isReviewed && !isInReview) {
                            try {
                                com.harvesthub.desktop.api.ListingDetailDto dto = com.harvesthub.desktop.api.ApiClient.claimListing(listing.getId());
                                if (dto != null) target = Listing.fromDto(dto);
                            } catch (Exception ignored) {}
                        }
                        if (target == null) {
                            try {
                                com.harvesthub.desktop.api.ListingDetailDto dto = com.harvesthub.desktop.api.ApiClient.getListing(listing.getId());
                                if (dto != null) target = Listing.fromDto(dto);
                            } catch (Exception ignored) {}
                        }
                    }
                    if (target == null) {
                        target = Listing.fromQueueListing(listing);
                    }
                    final Listing finalTarget = target;
                    javafx.application.Platform.runLater(() -> SceneNavigator.navigateToReview(button, finalTarget));
                });
                worker.setDaemon(true);
                worker.start();
            });
        }
        return button;
    }

    private static String toneClass(QueueListing.CropTone tone) {
        return switch (tone) {
            case AMBER -> "listing-icon-amber";
            case BROWN -> "listing-icon-brown";
            case TEAL -> "listing-icon-teal";
            case PURPLE -> "listing-icon-purple";
        };
    }
}

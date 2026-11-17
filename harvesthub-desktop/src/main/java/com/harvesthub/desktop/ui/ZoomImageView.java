package com.harvesthub.desktop.ui;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Officer photo inspection: the photo fits the frame first; zoom with the mouse wheel or the +/- buttons
 * (up to 6x the original pixels), drag to move around, double-click to go back to "fit".
 */
public final class ZoomImageView extends VBox {

    private static final double MAX_SCALE = 6.0;

    private final Image image;
    private final ImageView view = new ImageView();
    private final ScrollPane scroll = new ScrollPane();
    private final Label zoomLabel = Ui.label("Fit", "small");
    /** Displayed width / original width; <= 0 means "fit to the frame". */
    private double scale = -1;

    public ZoomImageView(Image image) {
        this.image = image;
        view.setImage(image);
        view.setPreserveRatio(true);
        view.setSmooth(true);

        StackPane centre = new StackPane(new Group(view));
        centre.setAlignment(Pos.CENTER);
        centre.minWidthProperty().bind(scroll.widthProperty().subtract(4));
        centre.minHeightProperty().bind(scroll.heightProperty().subtract(4));
        scroll.setContent(centre);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: #0B1F17; -fx-background-color: #0B1F17;");
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);

        // wheel = zoom around the photo, drag = pan, double-click = fit
        scroll.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() == 0) return;
            zoomBy(e.getDeltaY() > 0 ? 1.25 : 0.8);
            e.consume();
        });
        centre.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) fit();
        });
        scroll.widthProperty().addListener((o, a, b) -> apply());
        scroll.heightProperty().addListener((o, a, b) -> apply());

        Button minus = Ui.button("", "fas-search-minus", "btn-small");
        Button plus = Ui.button("", "fas-search-plus", "btn-small");
        Button fit = Ui.button("Fit", "fas-compress", "btn-small");
        Button actual = Ui.button("100%", null, "btn-small");
        minus.setOnAction(e -> zoomBy(0.8));
        plus.setOnAction(e -> zoomBy(1.25));
        fit.setOnAction(e -> fit());
        actual.setOnAction(e -> setScale(1.0));
        zoomLabel.setStyle("-fx-text-fill: #D8F3DC;");
        zoomLabel.setMinWidth(44);
        javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip("Scroll to zoom · drag to move · double-click to fit");
        javafx.scene.control.Tooltip.install(scroll, tip);
        minus.setTooltip(new javafx.scene.control.Tooltip("Zoom out"));
        plus.setTooltip(new javafx.scene.control.Tooltip("Zoom in (or scroll)"));
        HBox bar = new HBox(8, minus, plus, fit, actual, zoomLabel, Ui.grow());
        bar.setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        getChildren().addAll(scroll, bar);
        apply();
    }

    private double fitScale() {
        double w = Math.max(50, scroll.getWidth() - 20), h = Math.max(50, scroll.getHeight() - 20);
        if (image.getWidth() <= 0 || image.getHeight() <= 0) return 1;
        return Math.min(w / image.getWidth(), h / image.getHeight());
    }

    private void zoomBy(double factor) {
        double current = scale > 0 ? scale : fitScale();
        setScale(current * factor);
    }

    private void fit() {
        scale = -1;
        apply();
    }

    private void setScale(double s) {
        double min = Math.min(fitScale(), 1.0);
        scale = Math.max(min, Math.min(MAX_SCALE, s));
        if (Math.abs(scale - fitScale()) < 0.01) scale = -1;
        apply();
    }

    private void apply() {
        double s = scale > 0 ? scale : fitScale();
        view.setFitWidth(image.getWidth() * s);
        zoomLabel.setText(scale > 0 ? Math.round(s * 100) + "%" : "Fit");
    }
}

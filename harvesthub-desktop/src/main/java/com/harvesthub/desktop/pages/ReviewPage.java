package com.harvesthub.desktop.pages;

import com.fasterxml.jackson.databind.JsonNode;
import com.harvesthub.desktop.PortalApp;
import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.ui.Page;
import com.harvesthub.desktop.ui.Shell;
import com.harvesthub.desktop.ui.Ui;
import com.harvesthub.desktop.ui.ZoomImageView;
import com.harvesthub.desktop.util.Fmt;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Listing review: media authenticity check (GPS + time), visual inspection of the two photos and the
 * 10 s video, then Approve with Grade A/B/C, Request retake or Reject. Only the claiming officer can decide.
 */
public class ReviewPage implements Page {

    /**grade definitions, shown next to the grade buttons. */
    static final Map<String, String> GRADES = Map.of(
            "A", "Export / supermarket quality – uniform size and colour, no visible damage or decay, optimal freshness.",
            "B", "Standard retail quality – slight size variation or minor skin blemishes; no rot or structural damage.",
            "C", "Processing / bulk quality – safe to eat and process, but not uniform or has visible cosmetic defects.");

    private final String listingId;
    private final Shell.Nav returnTo;
    private final BorderPane root = new BorderPane();
    private final VBox feedback = new VBox();
    private MediaPlayer player;
    private JsonNode listing;

    public ReviewPage(String listingId, Shell.Nav returnTo) {
        this.listingId = listingId;
        this.returnTo = returnTo == null ? Shell.Nav.QUEUE : returnTo;
    }

    @Override
    public String title() {
        return "Review listing";
    }

    @Override
    public String subtitle() {
        return "Check the evidence, then grade, ask for a retake or reject";
    }

    @Override
    public Node view() {
        root.sceneProperty().addListener((o, a, b) -> {
            if (b == null) stopVideo(); // left the page through the sidebar
        });
        refresh();
        return root;
    }

    @Override
    public void refresh() {
        stopVideo();
        root.setCenter(Ui.loading("Loading listing…"));
        Ui.async(() -> ApiClient.get("/api/listings/" + ApiClient.enc(listingId)), this::render,
                err -> root.setCenter(new VBox(12, backButton(), Ui.banner(err, "error"))));
    }

    private void render(JsonNode l) {
        listing = l;
        String crop = Fmt.textOr(l, "cropName", "Crop");
        Shell.current().setSubtitle(crop + " from " + Fmt.textOr(l.get("farmer"), "fullName", "an unknown farmer")
                + (Fmt.text(l, "referenceCode") != null ? " · Ref " + Fmt.text(l, "referenceCode") : ""));

        HBox header = Ui.row(12, backButton(), Ui.grow(), Ui.pill(Fmt.textOr(l, "statusLabel", "Unknown"), Ui.toneForListing(Fmt.text(l, "status"))));
        if (Fmt.text(l, "grade") != null) header.getChildren().add(Ui.gradePill(Fmt.text(l, "grade")));

        VBox left = new VBox(16, mediaCard(l), checksCard(l), captureCard(l));
        VBox right = new VBox(16, decisionCard(l), listingCard(l), farmerCard(l));
        JsonNode reviews = l.get("reviews");
        if (reviews != null && reviews.size() > 0) right.getChildren().add(historyCard(reviews));

        GridPane grid = new GridPane();
        grid.setHgap(18);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(57);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(43);
        grid.getColumnConstraints().addAll(c1, c2);
        grid.add(left, 0, 0);
        grid.add(right, 1, 0);
        GridPane.setValignment(left, javafx.geometry.VPos.TOP);
        GridPane.setValignment(right, javafx.geometry.VPos.TOP);

        VBox page = new VBox(14, header, feedback, grid);
        root.setCenter(Ui.scroll(page));
    }

    private Button backButton() {
        String where = switch (returnTo) {
            case HISTORY -> "Back to review history";
            case DASHBOARD -> "Back to dashboard";
            case USERS -> "Back to users";
            default -> "Back to queue";
        };
        Button b = Ui.button(where, "fas-arrow-left", "btn-small");
        b.setOnAction(e -> {
            stopVideo();
            Shell.current().go(returnTo);
        });
        return b;
    }

    // ---- media (visual inspection) ----------------------------------------------------------------------

    private Node mediaCard(JsonNode l) {
        List<String> media = new ArrayList<>();
        JsonNode arr = l.get("mediaUrls");
        if (arr != null) arr.forEach(n -> media.add(n.asText()));
        VBox card = Ui.card(Ui.row(8, Ui.label("Photos & video", "h2"), Ui.grow(),
                Ui.label(media.size() + " file" + (media.size() == 1 ? "" : "s"), "muted")));
        if (media.isEmpty()) {
            card.getChildren().add(Ui.banner("The farmer did not upload any photos or video with this listing. "
                    + "Ask for a retake – the app captures them at the farm.", "warn"));
            return card;
        }
        StackPane viewer = new StackPane();
        viewer.getStyleClass().add("media-frame");
        viewer.setMinHeight(380);
        viewer.setPrefHeight(380);
        Hyperlink openExternal = new Hyperlink("Open in browser");
        HBox thumbs = new HBox(10);
        String[] captions = {"Wide angle – cultivated area", "Close-up – harvested sample", "10 s field + 360° sample video"};
        Label caption = Ui.label("", "strong");
        List<StackPane> boxes = new ArrayList<>();
        for (int i = 0; i < media.size(); i++) {
            String url = media.get(i);
            boolean video = url.toLowerCase(Locale.ROOT).endsWith(".mp4");
            StackPane thumb = video ? videoThumb() : Ui.remoteImage(url, 120, 84);
            if (!video) thumb.setMinSize(120, 84);
            boxes.add(thumb);
            final int index = i;
            String cap = i < captions.length && (video == (i == 2)) ? captions[i] : (video ? "Video" : "Photo " + (i + 1));
            thumb.setOnMouseClicked(e -> {
                boxes.forEach(b -> b.getStyleClass().remove("media-thumb-active"));
                thumb.getStyleClass().add("media-thumb-active");
                caption.setText(cap);
                openExternal.setOnAction(ev -> PortalApp.openInBrowser(ApiClient.absolute(url)));
                showMedia(viewer, url, video);
            });
            thumbs.getChildren().add(new VBox(4, thumb, Ui.label(video ? "Video" : (index == 0 ? "Wide angle" : index == 1 ? "Close-up" : "Photo " + (index + 1)), "muted", "small")));
        }
        card.getChildren().addAll(viewer, Ui.row(10, caption, Ui.grow(), openExternal), thumbs);
        // show the first item
        javafx.application.Platform.runLater(() -> boxes.get(0).getOnMouseClicked().handle(null));
        return card;
    }

    private StackPane videoThumb() {
        StackPane s = new StackPane(new VBox(4, Ui.icon("fas-play-circle", 26, "#1B4332"), Ui.label("10 s video", "small")));
        ((VBox) s.getChildren().get(0)).setAlignment(Pos.CENTER);
        s.getStyleClass().add("media-thumb");
        s.setMinSize(120, 84);
        s.setPrefSize(120, 84);
        return s;
    }

    /** Videos already downloaded in this session (url -> local file), so replays start at once. */
    private static final Map<String, java.io.File> VIDEO_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private void showMedia(StackPane viewer, String url, boolean video) {
        stopVideo();
        String abs = ApiClient.absolute(url);
        if (!video) {
            Label wait = Ui.label("Loading photo…", "muted");
            wait.setStyle("-fx-text-fill: #D8F3DC;");
            viewer.getChildren().setAll(wait);
            // full resolution (capped) so officers can zoom into the crop
            Image img = new Image(abs, 3000, 3000, true, true, true);
            Runnable ready = () -> {
                ZoomImageView zoom = new ZoomImageView(img);
                Button enlarge = Ui.button("Enlarge", "fas-expand", "btn-small", "btn-accent");
                enlarge.setOnAction(e -> openLarge(img));
                ((HBox) zoom.getChildren().get(1)).getChildren().add(enlarge);
                zoom.setPadding(new Insets(6));
                viewer.getChildren().setAll(zoom);
            };
            if (img.getProgress() >= 1 && !img.isError()) ready.run();
            img.progressProperty().addListener((o, x, y) -> {
                if (y.doubleValue() >= 1 && !img.isError()) ready.run();
            });
            img.errorProperty().addListener((o, x, y) -> {
                if (y) viewer.getChildren().setAll(Ui.label("The photo could not be loaded from the server.", "error-text"));
            });
            return;
        }
        // Video: download once, then play the local file (streaming over HTTPS stuttered in JavaFX).
        java.io.File cached = VIDEO_CACHE.get(abs);
        if (cached != null && cached.length() > 0) {
            playVideo(viewer, cached, abs);
            return;
        }
        ProgressBar bar = new ProgressBar(-1);
        bar.setPrefWidth(220);
        Label msg = Ui.label("Loading video…", "small");
        msg.setStyle("-fx-text-fill: #D8F3DC;");
        VBox loading = new VBox(10, Ui.icon("fas-film", 28, "#95D5B2"), msg, bar);
        loading.setAlignment(Pos.CENTER);
        viewer.getChildren().setAll(loading);
        Thread t = new Thread(() -> {
            try {
                java.io.File f = java.io.File.createTempFile("harvesthub-video-", ".mp4");
                f.deleteOnExit();
                java.net.URLConnection c = java.net.URI.create(abs).toURL().openConnection();
                c.setConnectTimeout(15_000);
                c.setReadTimeout(30_000);
                long total = c.getContentLengthLong();
                try (java.io.InputStream in = c.getInputStream(); java.io.OutputStream out = new java.io.FileOutputStream(f)) {
                    byte[] buf = new byte[64 * 1024];
                    long done = 0;
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        done += n;
                        if (total > 0) {
                            double p = (double) done / total;
                            long kb = done / 1024;
                            javafx.application.Platform.runLater(() -> {
                                bar.setProgress(p);
                                msg.setText(String.format("Loading video… %d KB", kb));
                            });
                        }
                    }
                }
                VIDEO_CACHE.put(abs, f);
                javafx.application.Platform.runLater(() -> {
                    if (viewer.getChildren().contains(loading)) playVideo(viewer, f, abs);
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> viewer.getChildren().setAll(videoFallback(abs)));
            }
        }, "video-download");
        t.setDaemon(true);
        t.start();
    }

    private void playVideo(StackPane viewer, java.io.File file, String abs) {
        stopVideo();
        try {
            player = new MediaPlayer(new Media(file.toURI().toString()));
            MediaView mv = new MediaView(player);
            mv.setPreserveRatio(true);
            mv.setSmooth(true);
            mv.fitWidthProperty().bind(viewer.widthProperty().subtract(16));
            mv.setFitHeight(320);
            Button play = Ui.button("Pause", "fas-pause", "btn-small", "btn-accent");
            play.setOnAction(e -> {
                if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                    player.pause();
                } else {
                    if (player.getTotalDuration() != null && player.getCurrentTime().greaterThanOrEqualTo(player.getTotalDuration())) {
                        player.seek(javafx.util.Duration.ZERO);
                    }
                    player.play();
                }
            });
            Slider seek = new Slider(0, 1, 0);
            seek.setPrefWidth(260);
            HBox.setHgrow(seek, Priority.ALWAYS);
            boolean[] dragging = {false};
            seek.setOnMousePressed(e -> dragging[0] = true);
            seek.setOnMouseReleased(e -> {
                dragging[0] = false;
                if (player.getTotalDuration() != null) player.seek(player.getTotalDuration().multiply(seek.getValue()));
            });
            Label time = Ui.label("0 s", "small");
            time.setStyle("-fx-text-fill: #D8F3DC;");
            player.statusProperty().addListener((o, x, y) -> {
                boolean playing = y == MediaPlayer.Status.PLAYING;
                play.setText(playing ? "Pause" : "Play");
                ((org.kordamp.ikonli.javafx.FontIcon) play.getGraphic()).setIconLiteral(playing ? "fas-pause" : "fas-play");
            });
            player.currentTimeProperty().addListener((o, x, y) -> {
                javafx.util.Duration total = player.getTotalDuration();
                double secs = total == null ? 0 : total.toSeconds();
                if (!dragging[0] && secs > 0) seek.setValue(y.toSeconds() / secs);
                time.setText(String.format("%.0f s / %.0f s", y.toSeconds(), secs));
            });
            player.setOnEndOfMedia(() -> {
                player.pause();
                play.setText("Replay");
                ((org.kordamp.ikonli.javafx.FontIcon) play.getGraphic()).setIconLiteral("fas-redo");
            });
            player.setOnError(() -> viewer.getChildren().setAll(videoFallback(abs)));
            player.setAutoPlay(true);
            HBox controls = Ui.row(10, play, seek, time);
            controls.setPadding(new Insets(0, 12, 8, 12));
            controls.setAlignment(Pos.CENTER);
            VBox box = new VBox(6, mv, controls);
            box.setAlignment(Pos.CENTER);
            viewer.getChildren().setAll(box);
        } catch (Exception ex) {
            viewer.getChildren().setAll(videoFallback(abs));
        }
    }

    /** Big window for close inspection of a photo (same zoom controls). */
    private void openLarge(Image img) {
        javafx.stage.Stage st = new javafx.stage.Stage();
        st.initOwner(PortalApp.stage());
        st.setTitle("HarvestHub · Photo inspection");
        ZoomImageView zoom = new ZoomImageView(img);
        zoom.setPadding(new Insets(8));
        zoom.setStyle("-fx-background-color: #0B1F17;");
        javafx.scene.Scene scene = new javafx.scene.Scene(zoom, 1100, 760);
        scene.getStylesheets().addAll(PortalApp.stage().getScene().getStylesheets());
        st.setScene(scene);
        st.setMaximized(true);
        st.show();
    }

    private Node videoFallback(String abs) {
        Label l = Ui.wrap("This video format cannot be played inside the portal.", "small");
        l.setStyle("-fx-text-fill: #D8F3DC;");
        Button open = Ui.button("Open video in browser", "fas-external-link-alt", "btn-small", "btn-accent");
        open.setOnAction(e -> PortalApp.openInBrowser(abs));
        VBox v = new VBox(10, Ui.icon("fas-film", 30, "#95D5B2"), l, open);
        v.setAlignment(Pos.CENTER);
        return v;
    }

    private void stopVideo() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
    }

    // ---- authenticity + capture --------------------------------------------------------------------------

    private Node checksCard(JsonNode l) {
        VBox card = Ui.card(Ui.label("Media authenticity check", "h2"),
                Ui.wrap("Checked automatically from the GPS position and capture time that the app recorded.", "muted", "small"));
        JsonNode checks = l.get("checks");
        if (checks != null) {
            for (JsonNode c : checks) {
                boolean ok = Fmt.bool(c, "ok");
                HBox r = Ui.row(10, Ui.icon(ok ? "fas-check-circle" : "fas-exclamation-triangle", 16, ok ? "#1E7B3A" : "#B54708"),
                        new VBox(1, Ui.label(Fmt.text(c, "label"), "strong"), Ui.label(Fmt.text(c, "detail"), "muted", "small")));
                r.getStyleClass().add("check-row");
                card.getChildren().add(r);
            }
        }
        return card;
    }

    private Node captureCard(JsonNode l) {
        Double lat = Fmt.num(l, "gpsLatitude");
        Double lng = Fmt.num(l, "gpsLongitude");
        String gps = lat == null || lng == null ? null : String.format(Locale.US, "%.6f, %.6f", lat, lng);
        Double flat = Fmt.num(l, "farmLatitude");
        Double flng = Fmt.num(l, "farmLongitude");
        String farm = flat == null || flng == null ? null : String.format(Locale.US, "%.6f, %.6f", flat, flng);
        VBox card = Ui.card(Ui.label("Where and when it was captured", "h2"), Ui.details(
                "Capture GPS", gps,
                "Registered farm", farm == null ? Fmt.text(l, "farmAddress") : Fmt.textOr(l, "farmAddress", "") + "  (" + farm + ")",
                "Distance", Fmt.metres(Fmt.num(l, "distanceFromFarmMeters")),
                "Captured", Fmt.dateTime(Fmt.text(l, "capturedAt")),
                "Submitted", Fmt.dateTime(Fmt.text(l, "submittedAt")),
                "Device", Fmt.text(l, "deviceModel")));
        if (gps != null) {
            card.getChildren().add(Ui.row(14,
                    Ui.link("Open capture point in Google Maps", () -> PortalApp.openInBrowser(
                            "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng)),
                    farm == null ? new Region() : Ui.link("Open registered farm", () -> PortalApp.openInBrowser(
                            "https://www.google.com/maps/search/?api=1&query=" + flat + "," + flng))));
        }
        return card;
    }

    // ---- right column -------------------------------------------------------------------------------------

    private Node listingCard(JsonNode l) {
        VBox card = Ui.card(Ui.label("Listing", "h2"), Ui.details(
                "Crop", Fmt.textOr(l, "cropName", null) + (Fmt.text(l, "variety") != null ? " (" + Fmt.text(l, "variety") + ")" : ""),
                "Estimated yield", Fmt.kg(Fmt.num(l, "quantityKg")),
                "Asking price", Fmt.perKg(Fmt.num(l, "pricePerKg")),
                "Estimated value", Fmt.money(Fmt.num(l, "totalValue")),
                "Reference", Fmt.text(l, "referenceCode"),
                "Submitted", Fmt.ago(Fmt.text(l, "submittedAt"))));
        if (Fmt.bool(l, "cropPending")) {
            card.getChildren().add(Ui.banner("The farmer added this crop with “Add New”. Approving the listing also adds "
                    + Fmt.textOr(l, "cropName", "the crop") + " to the verified crop list.", "info"));
        }
        return card;
    }

    private Node farmerCard(JsonNode l) {
        JsonNode f = l.get("farmer");
        VBox card = Ui.card(Ui.label("Farmer", "h2"));
        if (f == null || f.isNull()) {
            card.getChildren().add(Ui.wrap("The farmer account for this listing was not found.", "muted"));
            return card;
        }
        Double rating = Fmt.num(f, "ratingAverage");
        card.getChildren().addAll(
                Ui.row(12, Ui.avatar(Fmt.text(f, "fullName"), 42, true),
                        Ui.twoLine(Fmt.textOr(f, "fullName", "Farmer"), Fmt.textOr(f, "address", "Address not recorded")),
                        Ui.grow(), Fmt.bool(f, "accountSuspended") ? Ui.pill("Suspended", "red") : new Region()),
                Ui.details("Mobile", Fmt.text(f, "mobile"), "Email", Fmt.text(f, "email"),
                        "Trust score", Fmt.num(f, "trustScore") == null ? null : Math.round(Fmt.num(f, "trustScore")) + " %",
                        "Rating", rating == null ? "Not rated yet" : String.format(Locale.US, "%.1f ★ (%d)", rating, Fmt.count(f, "ratingCount")),
                        "Completed orders", String.valueOf(Fmt.count(f, "completedOrders"))),
                Ui.link("Open farmer account", () -> Shell.current().open(new UserDetailPage(Fmt.text(f, "id"), null))));
        return card;
    }

    private Node historyCard(JsonNode reviews) {
        VBox card = Ui.card(Ui.label("Decision log", "h2"));
        for (int i = reviews.size() - 1; i >= 0; i--) {
            JsonNode r = reviews.get(i);
            String d = Fmt.text(r, "decision");
            VBox text = new VBox(2,
                    Ui.row(8, Ui.pill(Fmt.decision(d) + (Fmt.text(r, "grade") != null ? " · Grade " + Fmt.text(r, "grade") : ""), Ui.toneForDecision(d)),
                            Ui.label(Fmt.textOr(r, "officerName", "Officer") + " · " + Fmt.dateTime(Fmt.text(r, "at")), "muted", "small")));
            if (Fmt.text(r, "notes") != null) text.getChildren().add(Ui.wrap("“" + Fmt.text(r, "notes") + "”"));
            card.getChildren().add(text);
        }
        return card;
    }

    // ---- decision -------------------------------------------------------------------------------------------

    private Node decisionCard(JsonNode l) {
        String status = Fmt.text(l, "status");
        VBox card = Ui.card(Ui.label("Officer decision", "h2"));
        boolean orphan = "UNDER_REVIEW".equals(status) && Fmt.text(l, "claimedByOfficerId") == null;
        if ("UNDER_REVIEW".equals(status) && !Fmt.bool(l, "claimedByMe") && !orphan) {
            card.getChildren().add(Ui.banner(Fmt.textOr(l, "claimedByOfficerName", "Another officer")
                    + " is reviewing this listing. It is locked so two officers never grade the same crop.", "info"));
            return card;
        }
        if (orphan || List.of("PENDING", "PENDING_VERIFICATION", "SUBMITTED").contains(status)) {
            Button claim = Ui.button("Claim this listing", "fas-hand-paper", "btn-accent", "btn-wide");
            claim.setMaxWidth(Double.MAX_VALUE);
            claim.setOnAction(e -> {
                claim.setDisable(true);
                Ui.async(() -> ApiClient.post("/api/queue/" + ApiClient.enc(listingId) + "/claim", null), this::render, err -> {
                    claim.setDisable(false);
                    feedback.getChildren().setAll(Ui.banner(err, "warn"));
                });
            });
            card.getChildren().addAll(Ui.wrap("Claim the listing to lock it to you before grading.", "muted"), claim);
            return card;
        }
        if (!"UNDER_REVIEW".equals(status)) {
            card.getChildren().add(Ui.wrap("This listing is not waiting for verification (" + Fmt.textOr(l, "statusLabel", status)
                    + "). Decisions are shown in the decision log.", "muted"));
            return card;
        }

        // claimed by me: grade and decide
        ToggleGroup gradeGroup = new ToggleGroup();
        VBox grades = new VBox(8);
        for (String g : List.of("A", "B", "C")) {
            ToggleButton t = new ToggleButton("Grade " + g);
            t.getStyleClass().add("grade-button");
            t.setToggleGroup(gradeGroup);
            t.setUserData(g);
            t.setMinWidth(110);
            Label desc = Ui.wrap(GRADES.get(g), "muted", "small");
            HBox.setHgrow(desc, Priority.ALWAYS);
            grades.getChildren().add(Ui.row(12, t, desc));
        }
        TextArea notes = new TextArea();
        notes.setPromptText("Note for the farmer (optional when approving)");
        notes.setPrefRowCount(2);
        notes.setWrapText(true);
        Button approve = Ui.button("Approve listing", "fas-check", "btn-primary", "btn-wide");
        approve.setMaxWidth(Double.MAX_VALUE);
        approve.disableProperty().bind(gradeGroup.selectedToggleProperty().isNull());
        approve.setOnAction(e -> {
            String grade = (String) gradeGroup.getSelectedToggle().getUserData();
            if (Ui.confirm("Approve as Grade " + grade + "?", "The listing goes live on the marketplace as Grade " + grade
                    + " and the farmer is notified.", "Approve")) {
                decide(Map.of("decision", "APPROVE", "grade", grade, "notes", notes.getText() == null ? "" : notes.getText().trim()),
                        "Approved as Grade " + grade + ". The farmer has been notified.");
            }
        });
        Button retake = Ui.button("Request retake", "fas-camera", "btn-warn");
        retake.setOnAction(e -> Ui.askText("Ask the farmer for new photos/video",
                        "Say exactly what to capture again. The farmer must return to the farm to capture it.",
                        "e.g. The close-up photo is blurred – capture a sharp close-up of the harvested sample.", 5, "Send request")
                .ifPresent(reason -> decide(Map.of("decision", "RETAKE", "notes", reason), "Retake requested. The farmer has been notified.")));
        Button reject = Ui.button("Reject", "fas-times", "btn-danger");
        reject.setOnAction(e -> Ui.askText("Reject this listing",
                        "Rejecting ends this listing. Give the reason the farmer will see.",
                        "e.g. Leaves show fungal damage; not safe for sale.", 5, "Reject listing")
                .ifPresent(reason -> decide(Map.of("decision", "REJECT", "notes", reason), "Listing rejected. The farmer has been notified.")));
        Button release = Ui.button("Release claim", "fas-unlock", "btn-ghost", "btn-small");
        release.setOnAction(e -> {
            if (Ui.confirm("Release this listing?", "It goes back to the shared queue so another officer can review it.", "Release")) {
                Ui.async(() -> ApiClient.post("/api/queue/" + ApiClient.enc(listingId) + "/release", null),
                        ok -> Shell.current().go(returnTo), err -> feedback.getChildren().setAll(Ui.banner(err, "error")));
            }
        });
        HBox other = Ui.row(10, retake, reject, Ui.grow(), release);
        card.getChildren().addAll(Ui.pill("Claimed by you", "blue"), Ui.label("1. Choose a grade", "h3"), grades,
                Ui.label("2. Note to the farmer", "h3"), notes, approve, new Separator(), other);
        return card;
    }

    private void decide(Map<String, String> body, String done) {
        feedback.getChildren().setAll(Ui.loading("Saving decision…"));
        Ui.async(() -> ApiClient.post("/api/listings/" + ApiClient.enc(listingId) + "/review", body), updated -> {
            render(updated);
            feedback.getChildren().setAll(Ui.banner(done, "ok"));
            Shell.current().refreshCounts();
        }, err -> feedback.getChildren().setAll(Ui.banner(err, "error")));
    }
}

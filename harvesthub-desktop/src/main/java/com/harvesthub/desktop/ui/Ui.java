package com.harvesthub.desktop.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.harvesthub.desktop.PortalApp;
import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.api.ApiException;
import com.harvesthub.desktop.util.Fmt;
import com.harvesthub.desktop.util.Pager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Small widget kit so every page looks the same (brand colours in portal.css). */
public final class Ui {

    private Ui() {
    }

    // ---- threading --------------------------------------------------------------------------

    /** Runs a server call off the UI thread; delivers the result or a readable error on the UI thread. */
    public static <T> void async(Supplier<T> call, Consumer<T> onOk, Consumer<String> onError) {
        Thread t = new Thread(() -> {
            try {
                T result = call.get();
                Platform.runLater(() -> onOk.accept(result));
            } catch (ApiException e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            } catch (RuntimeException e) {
                Platform.runLater(() -> onError.accept("Something went wrong: " + e.getMessage()));
            }
        }, "portal-call");
        t.setDaemon(true);
        t.start();
    }

    // ---- text ---------------------------------------------------------------------------------

    public static Label label(String text, String... styles) {
        Label l = new Label(text == null ? Fmt.NONE : text);
        l.getStyleClass().addAll(styles);
        return l;
    }

    public static Label wrap(String text, String... styles) {
        Label l = label(text, styles);
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    public static FontIcon icon(String literal, int size) {
        FontIcon i = new FontIcon(literal);
        i.setIconSize(size);
        return i;
    }

    public static FontIcon icon(String literal, int size, String color) {
        FontIcon i = icon(literal, size);
        i.setIconColor(javafx.scene.paint.Color.web(color));
        return i;
    }

    public static Label iconText(String literal, String text, String... styles) {
        Label l = label(text, styles);
        l.setGraphic(icon(literal, 13, "#6B7A72"));
        l.setGraphicTextGap(7);
        return l;
    }

    // ---- layout -------------------------------------------------------------------------------

    public static VBox card(Node... children) {
        VBox v = new VBox(12, children);
        v.getStyleClass().add("card");
        return v;
    }

    public static HBox row(double gap, Node... children) {
        HBox h = new HBox(gap, children);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    public static VBox col(double gap, Node... children) {
        return new VBox(gap, children);
    }

    public static Region grow() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static ScrollPane scroll(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    public static VBox section(String title, Node... body) {
        VBox v = card();
        v.getChildren().add(label(title, "h3"));
        v.getChildren().addAll(body);
        return v;
    }

    /** Two-column label/value grid. Pairs: label, value, label, value… */
    public static GridPane details(String... pairs) {
        GridPane g = new GridPane();
        g.setHgap(18);
        g.setVgap(8);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(130);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c1, c2);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            g.add(label(pairs[i], "muted"), 0, i / 2);
            Label v = wrap(Fmt.orDash(pairs[i + 1]), "strong");
            g.add(v, 1, i / 2);
        }
        return g;
    }

    // ---- buttons ------------------------------------------------------------------------------

    public static Button button(String text, String iconLiteral, String... styles) {
        Button b = new Button(text);
        if (iconLiteral != null) {
            b.setGraphic(icon(iconLiteral, 13));
            b.setGraphicTextGap(8);
        }
        b.getStyleClass().addAll(styles);
        b.setMnemonicParsing(false);
        b.setMinWidth(Region.USE_PREF_SIZE);   // never truncate button text
        return b;
    }

    public static Hyperlink link(String text, Runnable action) {
        Hyperlink h = new Hyperlink(text);
        h.setOnAction(e -> action.run());
        return h;
    }

    /** Groups chip toggles into one segmented control (selected = white raised segment). */
    public static HBox segmented(Node... chips) {
        HBox h = new HBox(2, chips);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("segmented");
        h.setMaxWidth(Region.USE_PREF_SIZE);
        return h;
    }

    /** Dark call-to-action banner (dashboard "Go to Verification Queue"). */
    public static HBox hero(String eyebrow, String title, String note, Runnable action) {
        StackPane arrow = new StackPane(icon("fas-arrow-right", 18));
        arrow.getStyleClass().add("hero-arrow");
        HBox h = new HBox(16, new VBox(6, label(eyebrow, "hero-eyebrow"), label(title, "hero-title")), grow(),
                label(note, "hero-note"), arrow);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("hero");
        h.setOnMouseClicked(e -> action.run());
        return h;
    }

    public static ToggleButton chip(String text, ToggleGroup group) {
        ToggleButton t = new ToggleButton(text);
        t.getStyleClass().add("chip-toggle");
        t.setMinWidth(Region.USE_PREF_SIZE);
        t.setToggleGroup(group);
        t.setMnemonicParsing(false);
        return t;
    }

    // ---- status pills -------------------------------------------------------------------------

    public static Label pill(String text, String tone) {
        Label l = label(text, "pill", "pill-" + tone);
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    public static String toneForListing(String status) {
        if (status == null) return "grey";
        return switch (status) {
            case "PENDING", "PENDING_VERIFICATION", "SUBMITTED", "RETAKE_REQUESTED" -> "amber";
            case "UNDER_REVIEW" -> "blue";
            case "ACTIVE", "APPROVED", "ONGOING" -> "green";
            case "REJECTED" -> "red";
            default -> "grey";
        };
    }

    public static String toneForDecision(String d) {
        if (d == null) return "grey";
        return switch (d) {
            case "APPROVED" -> "green";
            case "RETAKE" -> "amber";
            case "REJECTED" -> "red";
            default -> "grey";
        };
    }

    public static String toneForStage(String stage) {
        if (stage == null) return "grey";
        return switch (stage) {
            case "ACTIVE" -> "blue";
            case "COMPLETED" -> "green";
            case "PROBLEM" -> "red";
            default -> "grey";
        };
    }

    public static String toneForDispute(String status) {
        if (status == null) return "grey";
        return switch (status) {
            case "OPEN" -> "red";
            case "RESOLVED" -> "green";
            default -> "grey";
        };
    }

    public static Label gradePill(String grade) {
        return grade == null ? pill("No grade", "grey") : pill("Grade " + grade, "dark");
    }

    // ---- feedback -----------------------------------------------------------------------------

    public static HBox banner(String text, String tone) {
        String icon = switch (tone) {
            case "error" -> "fas-exclamation-circle";
            case "ok" -> "fas-check-circle";
            case "warn" -> "fas-exclamation-triangle";
            default -> "fas-info-circle";
        };
        String color = switch (tone) {
            case "error" -> "#B42318";
            case "ok" -> "#1E7B3A";
            case "warn" -> "#B54708";
            default -> "#1D5FA8";
        };
        Label l = wrap(text);
        HBox.setHgrow(l, Priority.ALWAYS);
        l.setMaxWidth(Double.MAX_VALUE);
        HBox h = new HBox(10, icon(icon, 15, color), l);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().addAll("banner", "banner-" + tone);
        return h;
    }

    public static VBox empty(String iconLiteral, String title, String text) {
        VBox v = new VBox(8, icon(iconLiteral, 34, "#A8B8AE"), label(title, "h3"), wrap(text, "muted"));
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(40));
        v.setMaxWidth(Double.MAX_VALUE);
        ((Label) v.getChildren().get(2)).setAlignment(Pos.CENTER);
        return v;
    }

    public static VBox loading(String text) {
        ProgressIndicator p = new ProgressIndicator();
        p.setPrefSize(34, 34);
        VBox v = new VBox(10, p, label(text, "muted"));
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(40));
        return v;
    }

    public static StackPane avatar(String name, double size, boolean light) {
        Label l = label(Fmt.initials(name));
        l.setStyle("-fx-font-size: " + Math.round(size * 0.38) + "px;");
        StackPane s = new StackPane(l);
        s.getStyleClass().add("avatar");
        if (light) s.getStyleClass().add("avatar-light");
        s.setMinSize(size, size);
        s.setMaxSize(size, size);
        return s;
    }

    public static VBox statCard(String iconLiteral, String tone, String value, String title, String sub) {
        return statCard(iconLiteral, tone, value, title, sub, null);
    }

    /** Dashboard number card: icon tile (+ small tag on the right), big value, caps title, optional note. */
    public static VBox statCard(String iconLiteral, String tone, String value, String title, String sub, String tag) {
        StackPane ic = new StackPane(icon(iconLiteral, 18));
        ic.getStyleClass().addAll("stat-icon", "tone-" + tone);
        HBox head = row(12, ic, grow());
        if (tag != null) head.getChildren().add(label(tag, "stat-tag"));
        VBox v = card(head, label(value, "value-lg"), label(title.toUpperCase(Locale.ROOT), "stat-title"));
        if (sub != null) v.getChildren().add(wrap(sub, "muted", "small"));
        v.setSpacing(4);
        ((Region) v.getChildren().get(1)).setPadding(new Insets(10, 0, 0, 0));
        v.getStyleClass().add("stat-card");
        HBox.setHgrow(v, Priority.ALWAYS);
        v.setMaxWidth(Double.MAX_VALUE);
        v.setPrefWidth(120);   // cards in a row share the width equally
        return v;
    }
    // ---- images -------------------------------------------------------------------------------

    public static Image resource(String name) {
        var url = PortalApp.class.getResource("/com/harvesthub/desktop/images/" + name);
        return url == null ? null : new Image(url.toExternalForm());
    }

    /**
     * Image decoded at the size it is shown (x screen scale) with smooth resampling. Shrinking a large PNG at draw
     * time made the logo edges jagged.
     */
    public static ImageView crispImage(String name, double width) {
        var url = PortalApp.class.getResource("/com/harvesthub/desktop/images/" + name);
        double scale = javafx.stage.Screen.getPrimary().getOutputScaleX();
        Image img = url == null ? null : new Image(url.toExternalForm(), Math.ceil(width * scale), 0, true, true);
        ImageView v = new ImageView(img);
        v.setFitWidth(width);
        v.setPreserveRatio(true);
        v.setSmooth(true);
        return v;
    }

    private static final java.util.Map<String, java.util.concurrent.CompletableFuture<Image>> THUMBS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ExecutorService THUMB_POOL = java.util.concurrent.Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "thumb-loader");
        t.setDaemon(true);
        return t;
    });

    /** Downloads and decodes a list thumbnail (server thumbnails are a few KB); completes with null on failure. */
    private static java.util.concurrent.CompletableFuture<Image> fetchThumb(String url) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                java.net.URLConnection c = java.net.URI.create(url).toURL().openConnection();
                c.setConnectTimeout(15_000);
                c.setReadTimeout(30_000);
                try (java.io.InputStream in = c.getInputStream()) {
                    double scale = javafx.stage.Screen.getPrimary().getOutputScaleX();
                    Image img = new Image(in, 42 * scale * 2, 42 * scale * 2, true, true);
                    return img.isError() || img.getWidth() <= 0 ? null : img;
                }
            } catch (Exception e) {
                return null;
            }
        }, THUMB_POOL);
    }

    /** List-row tile: the listing photo (cover) when there is one, otherwise a coloured tile with the crop initial. */
    public static StackPane cropTile(String cropName, String coverUrl) {
        StackPane tile = new StackPane();
        tile.getStyleClass().add("crop-tile");
        String[][] tones = {{"#DCF5E1", "#1E7B3A"}, {"#FDEBD3", "#C2410C"}, {"#E1EDFB", "#1D5FA8"}, {"#F3E8FF", "#7E22CE"},
                {"#FEF9C3", "#A16207"}, {"#E0F2F1", "#0F766E"}};
        String[] tone = tones[Math.floorMod(cropName == null ? 0 : cropName.toLowerCase(Locale.ROOT).hashCode(), tones.length)];
        tile.setStyle("-fx-background-color: " + tone[0] + ";");
        Label initial = label(cropName == null || cropName.isBlank() ? "?" : cropName.trim().substring(0, 1).toUpperCase(Locale.ROOT));
        initial.setStyle("-fx-text-fill: " + tone[1] + ";");
        tile.getChildren().add(initial);
        String url = ApiClient.absolute(coverUrl);
        if (url != null) {
            // one download per photo, done on a worker thread; the decoded image is placed on the UI thread
            THUMBS.computeIfAbsent(url, Ui::fetchThumb).thenAccept(img -> {
                if (img == null) return;
                Platform.runLater(() -> {
                    ImageView iv = new ImageView(img);
                    double w = img.getWidth(), h = img.getHeight(), side = Math.min(w, h);
                    iv.setViewport(new javafx.geometry.Rectangle2D((w - side) / 2, (h - side) / 2, side, side));   // centre square
                    iv.setFitWidth(42);
                    iv.setFitHeight(42);
                    iv.setSmooth(true);
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(42, 42);
                    clip.setArcWidth(20);
                    clip.setArcHeight(20);
                    iv.setClip(clip);
                    tile.getChildren().setAll(iv);
                });
            });
        }
        return tile;
    }

    /** Crop tile + crop name + second line (farmer), as in the queue / history rows. */
    public static HBox cropCell(String main, String sub, String coverUrl, String cropName) {
        HBox h = row(12, cropTile(cropName, coverUrl), twoLine(main, sub));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** Server image (e.g. /uploads/x.jpg) loaded in the background, with a placeholder until it arrives. */
    public static StackPane remoteImage(String serverPath, double w, double h) {
        StackPane box = new StackPane();
        box.setMinSize(w, h);
        box.setPrefSize(w, h);
        box.setMaxSize(w, h);
        box.getStyleClass().add("media-thumb");
        String url = ApiClient.absolute(serverPath);
        if (url == null) {
            box.getChildren().add(label("No image", "muted"));
            return box;
        }
        Label wait = label("Loading…", "muted", "small");
        box.getChildren().add(wait);
        Image img = new Image(url, w, h, true, true, true);
        ImageView view = new ImageView(img);
        img.progressProperty().addListener((o, a, b) -> {
            if (b.doubleValue() >= 1 && !img.isError()) box.getChildren().setAll(view);
        });
        img.errorProperty().addListener((o, a, b) -> {
            if (b) box.getChildren().setAll(label("Could not load image", "error-text", "small"));
        });
        return box;
    }

    // ---- tables -------------------------------------------------------------------------------

    public static TableView<JsonNode> table(String placeholder) {
        TableView<JsonNode> t = new TableView<>();
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setPlaceholder(label(placeholder, "muted"));
        t.setFixedCellSize(54);
        VBox.setVgrow(t, Priority.ALWAYS);
        return t;
    }

    public static TableColumn<JsonNode, String> textCol(String title, double width, Function<JsonNode, String> value) {
        TableColumn<JsonNode, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(Fmt.orDash(value.apply(cd.getValue()))));
        c.setPrefWidth(width);
        c.setSortable(false);
        return c;
    }

    public static TableColumn<JsonNode, JsonNode> nodeCol(String title, double width, Function<JsonNode, Node> render) {
        TableColumn<JsonNode, JsonNode> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue()));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(JsonNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : render.apply(item));
            }
        });
        c.setPrefWidth(width);
        c.setSortable(false);
        return c;
    }

    /** Name + small second line, e.g. crop and farmer. */
    public static VBox twoLine(String main, String sub) {
        VBox v = new VBox(2, label(main, "strong"), label(sub, "muted", "small"));
        v.setAlignment(Pos.CENTER_LEFT);
        return v;
    }

    public static void onOpen(TableView<JsonNode> t, Consumer<JsonNode> open) {
        t.setRowFactory(tv -> {
            TableRow<JsonNode> r = new TableRow<>();
            r.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !r.isEmpty()) open.accept(r.getItem());
            });
            return r;
        });
    }

    public static <T> HBox pagerBar(Pager<T> pager, Runnable redraw) {
        Label summary = label(pager.summary(), "muted");
        Button prev = button(null, "fas-chevron-left", "btn-small");
        Button next = button(null, "fas-chevron-right", "btn-small");
        Label page = label("Page " + pager.page() + " of " + pager.pages(), "muted");
        prev.setDisable(!pager.hasPrev());
        next.setDisable(!pager.hasNext());
        prev.setOnAction(e -> {
            pager.go(pager.page() - 1);
            redraw.run();
        });
        next.setOnAction(e -> {
            pager.go(pager.page() + 1);
            redraw.run();
        });
        return row(10, summary, grow(), page, prev, next);
    }

    public static void fill(TableView<JsonNode> t, List<JsonNode> rows) {
        t.setItems(FXCollections.observableArrayList(rows));
    }

    // ---- dialogs ------------------------------------------------------------------------------

    private static Window owner() {
        return PortalApp.stage();
    }

    private static void style(Dialog<?> d) {
        d.initOwner(owner());
        d.getDialogPane().getStylesheets().add(PortalApp.css());
        if (PortalApp.stage() != null && !PortalApp.stage().getIcons().isEmpty()) {
            Window w = d.getDialogPane().getScene().getWindow();
            if (w instanceof javafx.stage.Stage s) s.getIcons().setAll(PortalApp.stage().getIcons());
        }
    }

    public static void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setTitle("HarvestHub Officer Portal");
        a.setHeaderText(title);
        style(a);
        a.showAndWait();
    }

    public static void error(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setTitle("HarvestHub Officer Portal");
        a.setHeaderText(title);
        style(a);
        a.showAndWait();
    }

    public static boolean confirm(String title, String message, String okText) {
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ok, ButtonType.CANCEL);
        a.setTitle("HarvestHub Officer Portal");
        a.setHeaderText(title);
        style(a);
        return a.showAndWait().filter(b -> b == ok).isPresent();
    }

    /** Asks for a written reason; the OK button stays disabled until minChars are typed. */
    public static Optional<String> askText(String title, String message, String prompt, int minChars, String okText) {
        Dialog<String> d = new Dialog<>();
        d.setTitle("HarvestHub Officer Portal");
        d.setHeaderText(title);
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setWrapText(true);
        area.setPrefRowCount(4);
        area.setPrefWidth(460);
        Label counter = label("At least " + minChars + " characters", "muted", "small");
        VBox box = new VBox(10, wrap(message, "muted"), area, counter);
        d.getDialogPane().setContent(box);
        Node okButton = d.getDialogPane().lookupButton(ok);
        okButton.setDisable(minChars > 0);
        area.textProperty().addListener((o, a, b) -> {
            int n = b == null ? 0 : b.trim().length();
            okButton.setDisable(n < minChars);
            counter.setText(n + " characters" + (n < minChars ? " (at least " + minChars + ")" : ""));
        });
        d.setResultConverter(bt -> bt == ok ? area.getText().trim() : null);
        style(d);
        Platform.runLater(area::requestFocus);
        return d.showAndWait();
    }
}

package com.harvesthub.desktop.pages;

import com.fasterxml.jackson.databind.JsonNode;
import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.ui.Page;
import com.harvesthub.desktop.ui.Shell;
import com.harvesthub.desktop.ui.Ui;
import com.harvesthub.desktop.util.Fmt;
import com.harvesthub.desktop.util.Pager;
import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.*;

/** Every grading decision (approve / retake / reject) with filters; mine by default, or all officers. */
public class HistoryPage implements Page {

    private final VBox root = new VBox(14);
    private final ToggleGroup scope = new ToggleGroup();
    private final ComboBox<String> decision = new ComboBox<>();
    private final ComboBox<String> crop = new ComboBox<>();
    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();
    private final TextField search = new TextField();
    private final VBox feedback = new VBox();
    private final HBox summary = Ui.row(10);
    private final TableView<JsonNode> table = Ui.table("No decisions yet.");
    private final Pager<JsonNode> pager = new Pager<>(12);
    private final HBox pagerHost = new HBox();
    private boolean cropsLoaded;

    @Override
    public String title() {
        return "Review History";
    }

    @Override
    public String subtitle() {
        return "Grading decisions made in the portal";
    }

    @Override
    public Node view() {
        ToggleButton mine = Ui.chip("My decisions", scope);
        ToggleButton all = Ui.chip("All officers", scope);
        mine.setUserData("mine");
        all.setUserData("all");
        mine.setSelected(true);
        scope.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) a.setSelected(true);
            else refresh();
        });
        decision.getItems().setAll("All decisions", "Approved", "Retake requested", "Rejected");
        decision.setValue("All decisions");
        decision.setOnAction(e -> refresh());
        crop.getItems().setAll("All crops");
        crop.setValue("All crops");
        crop.setOnAction(e -> refresh());
        from.setPromptText("From");
        to.setPromptText("To");
        from.setPrefWidth(130);
        to.setPrefWidth(130);
        from.valueProperty().addListener((o, a, b) -> refresh());
        to.valueProperty().addListener((o, a, b) -> refresh());
        Button clear = Ui.button("Clear", "fas-times", "btn-small", "btn-ghost");
        clear.setOnAction(e -> {
            from.setValue(null);
            to.setValue(null);
            decision.setValue("All decisions");
            crop.setValue("All crops");
            search.clear();
        });
        search.setPromptText("Search crop, farmer, location, officer");
        search.getStyleClass().add("search-field");
        PauseTransition debounce = new PauseTransition(Duration.millis(350));
        debounce.setOnFinished(e -> refresh());
        search.textProperty().addListener((o, a, b) -> debounce.playFromStart());

        table.getColumns().setAll(List.of(
                Ui.nodeCol("CROP & FARMER", 240, n -> Ui.cropCell(Fmt.textOr(n, "cropName", "Crop")
                        + (Fmt.text(n, "variety") != null ? " (" + Fmt.text(n, "variety") + ")" : ""), Fmt.textOr(n, "farmerName", "Farmer not recorded"),
                        Fmt.text(n, "coverUrl"), Fmt.text(n, "cropName"))),
                Ui.textCol("LOCATION", 135, n -> Fmt.text(n, "location")),
                Ui.nodeCol("DECISION", 170, n -> {
                    String d = Fmt.text(n, "decision");
                    return Ui.pill(Fmt.decision(d) + (Fmt.text(n, "grade") != null ? " · Grade " + Fmt.text(n, "grade") : ""), Ui.toneForDecision(d));
                }),
                Ui.nodeCol("NOTE TO FARMER", 210, n -> {
                    Label l = Ui.label(Fmt.orDash(Fmt.text(n, "notes")), "small");
                    l.setTooltip(Fmt.text(n, "notes") == null ? null : new Tooltip(Fmt.text(n, "notes")));
                    return l;
                }),
                Ui.textCol("OFFICER", 140, n -> Fmt.bool(n, "byMe") ? "You" : Fmt.text(n, "officerName")),
                Ui.textCol("DATE", 160, n -> Fmt.dateTime(Fmt.text(n, "at"))),
                Ui.nodeCol("", 100, n -> {
                    Button b = Ui.button("Open", "fas-eye", "btn-small");
                    b.setOnAction(e -> Shell.current().open(new ReviewPage(Fmt.text(n, "listingId"), Shell.Nav.HISTORY)));
                    return b;
                })));
        Ui.onOpen(table, n -> Shell.current().open(new ReviewPage(Fmt.text(n, "listingId"), Shell.Nav.HISTORY)));

        HBox.setHgrow(search, Priority.ALWAYS);
        search.setMaxWidth(420);
        HBox filters = Ui.row(10, Ui.segmented(mine, all), Ui.grow(), search);
        HBox filters2 = Ui.row(10, decision, crop, from, to, clear, Ui.grow(), summary);
        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().setAll(filters, filters2, feedback, table, pagerHost);
        refresh();
        return root;
    }

    private String decisionParam() {
        return switch (decision.getValue() == null ? "" : decision.getValue()) {
            case "Approved" -> "APPROVED";
            case "Retake requested" -> "RETAKE";
            case "Rejected" -> "REJECTED";
            default -> null;
        };
    }

    @Override
    public void refresh() {
        String sc = scope.getSelectedToggle() == null ? "mine" : (String) scope.getSelectedToggle().getUserData();
        String c = crop.getValue() == null || "All crops".equals(crop.getValue()) ? null : crop.getValue();
        LocalDate f = from.getValue();
        LocalDate t = to.getValue();
        if (f != null && t != null && f.isAfter(t)) {
            feedback.getChildren().setAll(Ui.banner("The From date is after the To date.", "warn"));
            return;
        }
        feedback.getChildren().clear();
        table.setPlaceholder(Ui.loading("Loading decisions…"));
        String path = ApiClient.query("/api/review-history", "scope", sc, "decision", decisionParam(), "crop", c,
                "from", f == null ? null : f.toString(), "to", t == null ? null : t.toString(), "search", search.getText());
        Ui.async(() -> {
            JsonNode rows = ApiClient.get(path);
            JsonNode allRows = cropsLoaded ? null : ApiClient.get("/api/review-history?scope=all");
            return new JsonNode[]{rows, allRows};
        }, r -> {
            if (r[1] != null) {
                Set<String> crops = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                r[1].forEach(n -> {
                    if (Fmt.text(n, "cropName") != null) crops.add(Fmt.text(n, "cropName"));
                });
                List<String> items = new ArrayList<>(List.of("All crops"));
                items.addAll(crops);
                crop.getItems().setAll(items);
                crop.setValue(c == null ? "All crops" : c);
                cropsLoaded = true;
            }
            List<JsonNode> list = new ArrayList<>();
            r[0].forEach(list::add);
            long approved = list.stream().filter(n -> "APPROVED".equals(Fmt.text(n, "decision"))).count();
            long retake = list.stream().filter(n -> "RETAKE".equals(Fmt.text(n, "decision"))).count();
            long rejected = list.stream().filter(n -> "REJECTED".equals(Fmt.text(n, "decision"))).count();
            summary.getChildren().setAll(Ui.pill("● " + approved + " Approved", "green"), Ui.pill("● " + retake + " Retake requested", "amber"),
                    Ui.pill("● " + rejected + " Rejected", "red"));
            table.setPlaceholder(Ui.label("No decisions match these filters.", "muted"));
            pager.setItems(list);
            pager.go(1);
            draw();
        }, err -> {
            table.setPlaceholder(Ui.label("Could not load the history.", "error-text"));
            feedback.getChildren().setAll(Ui.banner(err, "error"));
        });
    }

    private void draw() {
        Ui.fill(table, pager.current());
        HBox bar = Ui.pagerBar(pager, this::draw);
        HBox.setHgrow(bar, Priority.ALWAYS);
        pagerHost.getChildren().setAll(bar);
    }
}

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

import java.util.*;

/**
 * Shared "Pending Verification Queue" (workflow 1.5): every officer sees it, oldest first. Claiming locks the listing
 * to one officer (server-side atomic claim) and opens the review screen.
 */
public class QueuePage implements Page {

    private final VBox root = new VBox(14);
    private final ToggleGroup tabs = new ToggleGroup();
    private final TextField search = new TextField();
    private final ComboBox<String> crop = new ComboBox<>();
    private final ComboBox<String> sort = new ComboBox<>();
    private final VBox feedback = new VBox();
    private final TableView<JsonNode> table = Ui.table("No listings are waiting for verification.");
    private final Pager<JsonNode> pager = new Pager<>(10);
    private final HBox pagerHost = new HBox();
    private List<JsonNode> all = List.of();
    private String tab = "all";

    @Override
    public String title() {
        return "Verification Queue";
    }

    @Override
    public String subtitle() {
        return "Listings submitted by farmers, waiting for an officer – first come, first served";
    }

    @Override
    public Node view() {
        ToggleButton tAll = Ui.chip("All waiting", tabs);
        ToggleButton tFree = Ui.chip("Not claimed", tabs);
        ToggleButton tMine = Ui.chip("Claimed by me", tabs);
        tAll.setUserData("all");
        tFree.setUserData("unclaimed");
        tMine.setUserData("claimed");
        tAll.setSelected(true);
        tabs.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                a.setSelected(true);
                return;
            }
            tab = (String) b.getUserData();
            refresh();
        });

        search.setPromptText("Search crop, farmer, location or reference");
        search.getStyleClass().add("search-field");
        PauseTransition debounce = new PauseTransition(Duration.millis(350));
        debounce.setOnFinished(e -> refresh());
        search.textProperty().addListener((o, a, b) -> debounce.playFromStart());

        crop.setPromptText("All crops");
        crop.setOnAction(e -> redraw());
        sort.getItems().setAll("Oldest first", "Newest first");
        sort.setValue("Oldest first");
        sort.setOnAction(e -> redraw());

        buildTable();
        HBox filters = Ui.row(10, tAll, tFree, tMine, Ui.grow(), search, crop, sort);
        VBox tableCard = new VBox(10, table, pagerHost);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        root.getChildren().setAll(filters, feedback, tableCard);
        refresh();
        return root;
    }

    private void buildTable() {
        table.getColumns().setAll(List.of(
                Ui.nodeCol("CROP & FARMER", 230, n -> {
                    VBox v = Ui.twoLine(Fmt.textOr(n, "cropName", "Crop") + (Fmt.text(n, "variety") != null ? " (" + Fmt.text(n, "variety") + ")" : ""),
                            Fmt.textOr(n, "farmerName", "Farmer not recorded"));
                    if (Fmt.bool(n, "cropPending")) v.getChildren().add(Ui.pill("New crop – not in catalogue", "blue"));
                    return v;
                }),
                Ui.textCol("LOCATION", 170, n -> Fmt.text(n, "location")),
                Ui.textCol("QUANTITY", 100, n -> Fmt.kg(Fmt.num(n, "quantityKg"))),
                Ui.textCol("ASKING PRICE", 120, n -> Fmt.perKg(Fmt.num(n, "pricePerKg"))),
                Ui.nodeCol("EVIDENCE", 150, n -> {
                    int media = (int) Fmt.count(n, "mediaCount");
                    Double d = Fmt.num(n, "distanceFromFarmMeters");
                    String gps = d == null ? "no GPS" : Fmt.metres(d) + " from farm";
                    boolean ok = media >= 3 && d != null && d <= 500;
                    return Ui.row(6, Ui.icon(ok ? "fas-check-circle" : "fas-exclamation-triangle", 13, ok ? "#1E7B3A" : "#B54708"),
                            Ui.label(media + " files · " + gps, "small"));
                }),
                Ui.textCol("SUBMITTED", 120, n -> Fmt.ago(Fmt.text(n, "submittedAt"))),
                Ui.nodeCol("STATUS", 150, this::statusPill),
                Ui.nodeCol("", 170, this::actionButton)));
        Ui.onOpen(table, n -> Shell.current().open(new ReviewPage(Fmt.text(n, "id"), Shell.Nav.QUEUE)));
    }

    private Node statusPill(JsonNode n) {
        if (Fmt.bool(n, "claimedByMe")) return Ui.pill("Claimed by you", "blue");
        if ("UNDER_REVIEW".equals(Fmt.text(n, "status")) && Fmt.text(n, "claimedByOfficerId") != null) return Ui.pill("With " + Fmt.textOr(n, "claimedByOfficerName", "another officer"), "grey");
        if (Fmt.bool(n, "resubmitted")) return Ui.pill("New photos sent", "amber");
        return Ui.pill("Pending", "amber");
    }

    private Node actionButton(JsonNode n) {
        String id = Fmt.text(n, "id");
        if (Fmt.bool(n, "claimedByMe")) {
            Button b = Ui.button("Continue review", "fas-arrow-right", "btn-small", "btn-primary");
            b.setOnAction(e -> Shell.current().open(new ReviewPage(id, Shell.Nav.QUEUE)));
            return b;
        }
        if ("UNDER_REVIEW".equals(Fmt.text(n, "status")) && Fmt.text(n, "claimedByOfficerId") != null) {
            Button b = Ui.button("View", "fas-eye", "btn-small");
            b.setOnAction(e -> Shell.current().open(new ReviewPage(id, Shell.Nav.QUEUE)));
            return b;
        }
        Button b = Ui.button("Claim & review", "fas-hand-paper", "btn-small", "btn-accent");
        b.setOnAction(e -> {
            b.setDisable(true);
            Ui.async(() -> ApiClient.post("/api/queue/" + ApiClient.enc(id) + "/claim", null),
                    ok -> Shell.current().open(new ReviewPage(id, Shell.Nav.QUEUE)),
                    err -> {
                        feedback.getChildren().setAll(Ui.banner(err, "warn"));
                        refresh();
                    });
        });
        return b;
    }

    @Override
    public void refresh() {
        table.setPlaceholder(Ui.loading("Loading queue…"));
        Ui.async(() -> ApiClient.get(ApiClient.query("/api/queue", "tab", tab, "search", search.getText())), arr -> {
            List<JsonNode> list = new ArrayList<>();
            arr.forEach(list::add);
            all = list;
            Set<String> crops = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            list.forEach(n -> {
                if (Fmt.text(n, "cropName") != null) crops.add(Fmt.text(n, "cropName"));
            });
            String keep = crop.getValue();
            List<String> options = new ArrayList<>();
            options.add("All crops");
            options.addAll(crops);
            crop.getItems().setAll(options);
            crop.setValue(keep != null && options.contains(keep) ? keep : "All crops");
            table.setPlaceholder(Ui.label("all".equals(tab) && (search.getText() == null || search.getText().isBlank())
                    ? "No listings are waiting for verification." : "Nothing matches these filters.", "muted"));
            if (root.getScene() != null) Shell.current().setSubtitle(list.size() + " listing" + (list.size() == 1 ? "" : "s")
                    + ("claimed".equals(tab) ? " claimed by you" : " waiting for verification") + " – first come, first served");
            redraw();
        }, err -> {
            table.setPlaceholder(Ui.label("Could not load the queue.", "error-text"));
            feedback.getChildren().setAll(Ui.banner(err, "error"));
        });
    }

    private void redraw() {
        List<JsonNode> list = new ArrayList<>(all);
        String c = crop.getValue();
        if (c != null && !"All crops".equals(c)) list.removeIf(n -> !c.equalsIgnoreCase(Fmt.text(n, "cropName")));
        if ("Newest first".equals(sort.getValue())) Collections.reverse(list);
        pager.setItems(list);
        Ui.fill(table, pager.current());
        pagerHost.getChildren().setAll(Ui.pagerBar(pager, this::redrawPage));
        HBox.setHgrow(pagerHost.getChildren().get(0), Priority.ALWAYS);
    }

    private void redrawPage() {
        Ui.fill(table, pager.current());
        pagerHost.getChildren().setAll(Ui.pagerBar(pager, this::redrawPage));
        HBox.setHgrow(pagerHost.getChildren().get(0), Priority.ALWAYS);
    }
}

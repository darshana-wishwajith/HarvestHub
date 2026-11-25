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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/** Read-only monitoring of every order: in progress, completed, or with a problem. */
public class OrdersPage implements Page {

    private final VBox root = new VBox(14);
    private final ToggleGroup stages = new ToggleGroup();
    private final TextField search = new TextField();
    private final VBox feedback = new VBox();
    private final Label summary = Ui.label("", "muted");
    private final TableView<JsonNode> table = Ui.table("No orders.");
    private final Pager<JsonNode> pager = new Pager<>(12);
    private final HBox pagerHost = new HBox();

    @Override
    public String title() {
        return "Orders";
    }

    @Override
    public String subtitle() {
        return "Every order between farmers, buyers and transporters";
    }

    @Override
    public Node view() {
        HBox chips = Ui.segmented();
        String[][] options = {{"All", "all"}, {"In progress", "active"}, {"Completed", "completed"}, {"Problems", "problem"}};
        for (String[] o : options) {
            ToggleButton t = Ui.chip(o[0], stages);
            t.setUserData(o[1]);
            chips.getChildren().add(t);
        }
        ((ToggleButton) chips.getChildren().get(0)).setSelected(true);
        stages.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) a.setSelected(true);
            else refresh();
        });
        search.setPromptText("Search order id, crop, farmer, buyer, driver");
        search.getStyleClass().add("search-field");
        PauseTransition debounce = new PauseTransition(Duration.millis(350));
        debounce.setOnFinished(e -> refresh());
        search.textProperty().addListener((o, a, b) -> debounce.playFromStart());

        table.getColumns().setAll(List.of(
                Ui.nodeCol("ORDER", 210, n -> Ui.twoLine(Fmt.textOr(n, "cropName", "Crop") + " · " + Fmt.kg(Fmt.num(n, "quantityKg")),
                        Fmt.textOr(n, "orderId", ""))),
                Ui.textCol("FARMER", 140, n -> Fmt.text(n, "farmerName")),
                Ui.textCol("BUYER", 140, n -> Fmt.text(n, "buyerName")),
                Ui.textCol("DRIVER", 140, n -> Fmt.text(n, "transporterName") == null ? "Not assigned" : Fmt.text(n, "transporterName")),
                Ui.nodeCol("STATUS", 230, n -> Ui.row(6, Ui.pill(Fmt.orderStatus(Fmt.text(n, "status")), Ui.toneForStage(Fmt.text(n, "stage"))),
                        Fmt.bool(n, "disputeOpen") ? Ui.pill("Dispute open", "red") : new Region())),
                Ui.textCol("CROP VALUE", 110, n -> Fmt.money(Fmt.num(n, "cropValue"))),
                Ui.textCol("CREATED", 110, n -> Fmt.date(Fmt.text(n, "createdDate"))),
                Ui.nodeCol("", 90, n -> {
                    Button b = Ui.button("Open", "fas-eye", "btn-small");
                    b.setOnAction(e -> Shell.current().open(new OrderDetailPage(Fmt.text(n, "orderId"), Shell.Nav.ORDERS)));
                    return b;
                })));
        Ui.onOpen(table, n -> Shell.current().open(new OrderDetailPage(Fmt.text(n, "orderId"), Shell.Nav.ORDERS)));
        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().setAll(Ui.row(10, chips, Ui.grow(), search), feedback, summary, table, pagerHost);
        refresh();
        return root;
    }

    @Override
    public void refresh() {
        String stage = stages.getSelectedToggle() == null ? "all" : (String) stages.getSelectedToggle().getUserData();
        table.setPlaceholder(Ui.loading("Loading orders…"));
        Ui.async(() -> ApiClient.get(ApiClient.query("/api/officer/orders", "stage", stage, "search", search.getText())), arr -> {
            List<JsonNode> list = new ArrayList<>();
            arr.forEach(list::add);
            double value = list.stream().mapToDouble(n -> Fmt.num(n, "cropValue") == null ? 0 : Fmt.num(n, "cropValue")).sum();
            summary.setText(list.size() + " order" + (list.size() == 1 ? "" : "s") + " · crop value " + Fmt.money(value));
            table.setPlaceholder(Ui.label("No orders match these filters.", "muted"));
            feedback.getChildren().clear();
            pager.setItems(list);
            pager.go(1);
            draw();
        }, err -> {
            table.setPlaceholder(Ui.label("Could not load orders.", "error-text"));
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

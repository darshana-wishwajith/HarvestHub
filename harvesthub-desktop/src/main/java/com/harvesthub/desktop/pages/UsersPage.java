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

import java.util.ArrayList;
import java.util.List;

/** All registered users (Farmer, Buyer, Transporter, Agricultural Officer) with search and filters. */
public class UsersPage implements Page {

    private final VBox root = new VBox(14);
    private final ToggleGroup roles = new ToggleGroup();
    private final ComboBox<String> status = new ComboBox<>();
    private final TextField search = new TextField();
    private final VBox feedback = new VBox();
    private final Label summary = Ui.label("", "muted");
    private final TableView<JsonNode> table = Ui.table("No users found.");
    private final Pager<JsonNode> pager = new Pager<>(12);
    private final HBox pagerHost = new HBox();

    @Override
    public String title() {
        return "Users";
    }

    @Override
    public String subtitle() {
        return "Farmers, buyers, transporters and officers registered on HarvestHub";
    }

    @Override
    public Node view() {
        HBox chips = Ui.segmented();
        String[][] options = {{"All", "all"}, {"Farmers", "FARMER"}, {"Buyers", "BUYER"}, {"Transporters", "TRANSPORTER"}, {"Officers", "OFFICER"}};
        for (String[] o : options) {
            ToggleButton t = Ui.chip(o[0], roles);
            t.setUserData(o[1]);
            chips.getChildren().add(t);
        }
        ((ToggleButton) chips.getChildren().get(0)).setSelected(true);
        roles.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) a.setSelected(true);
            else refresh();
        });
        status.getItems().setAll("Any status", "Active", "Suspended");
        status.setValue("Any status");
        status.setOnAction(e -> refresh());
        search.setPromptText("Search name, email, mobile, NIC, plate");
        search.getStyleClass().add("search-field");
        PauseTransition debounce = new PauseTransition(Duration.millis(350));
        debounce.setOnFinished(e -> refresh());
        search.textProperty().addListener((o, a, b) -> debounce.playFromStart());

        table.getColumns().setAll(List.of(
                Ui.nodeCol("NAME", 240, n -> Ui.row(10, Ui.avatar(Fmt.text(n, "fullName"), 34, true),
                        Ui.twoLine(Fmt.textOr(n, "fullName", "No name"), Fmt.textOr(n, "email", "No email")))),
                Ui.nodeCol("ROLE", 150, n -> Ui.pill(Fmt.role(Fmt.text(n, "role")), "grey")),
                Ui.textCol("MOBILE", 120, n -> Fmt.text(n, "mobile")),
                Ui.textCol("LOCATION / BASE", 200, n -> Fmt.text(n, "address")),
                Ui.textCol("TRUST", 70, n -> Fmt.num(n, "trustScore") == null ? null : Math.round(Fmt.num(n, "trustScore")) + " %"),
                Ui.nodeCol("WALLET", 130, n -> {
                    Double b = Fmt.num(n, "walletBalance");
                    Label l = Ui.label(b == null ? "No wallet" : Fmt.money(b), b != null && b < 0 ? "error-text" : "muted");
                    return Fmt.bool(n, "walletSuspended") ? Ui.row(6, l, Ui.pill("Owes", "red")) : l;
                }),
                Ui.nodeCol("ACCOUNT", 110, n -> Fmt.bool(n, "accountSuspended") ? Ui.pill("Suspended", "red") : Ui.pill("Active", "green")),
                Ui.nodeCol("", 90, n -> {
                    Button b = Ui.button("Open", "fas-eye", "btn-small");
                    b.setOnAction(e -> Shell.current().open(new UserDetailPage(Fmt.text(n, "id"), Shell.Nav.USERS)));
                    return b;
                })));
        Ui.onOpen(table, n -> Shell.current().open(new UserDetailPage(Fmt.text(n, "id"), Shell.Nav.USERS)));

        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().setAll(Ui.row(10, chips, Ui.grow(), search, status), feedback, summary, table, pagerHost);
        refresh();
        return root;
    }

    @Override
    public void refresh() {
        String role = roles.getSelectedToggle() == null ? "all" : (String) roles.getSelectedToggle().getUserData();
        String st = switch (status.getValue()) {
            case "Active" -> "active";
            case "Suspended" -> "suspended";
            default -> "all";
        };
        table.setPlaceholder(Ui.loading("Loading users…"));
        Ui.async(() -> ApiClient.get(ApiClient.query("/api/officer/users", "role", role, "status", st, "search", search.getText())), arr -> {
            List<JsonNode> list = new ArrayList<>();
            arr.forEach(list::add);
            long suspended = list.stream().filter(n -> Fmt.bool(n, "accountSuspended")).count();
            summary.setText(list.size() + " user" + (list.size() == 1 ? "" : "s") + (suspended > 0 ? " · " + suspended + " suspended" : ""));
            table.setPlaceholder(Ui.label("No users match these filters.", "muted"));
            feedback.getChildren().clear();
            pager.setItems(list);
            pager.go(1);
            draw();
        }, err -> {
            table.setPlaceholder(Ui.label("Could not load users.", "error-text"));
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

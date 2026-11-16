package com.harvesthub.desktop.pages;

import com.fasterxml.jackson.databind.JsonNode;
import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.ui.Page;
import com.harvesthub.desktop.ui.Shell;
import com.harvesthub.desktop.ui.Ui;
import com.harvesthub.desktop.util.Fmt;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Buyer / farmer issues reported after handover and drivers' appeals after a delivery rejection (workflow 5.3).
 * Officers resolve or dismiss each case with a written decision; money is not changed (decision 15 Nov).
 */
public class DisputesPage implements Page {

    private final VBox root = new VBox(14);
    private final ToggleGroup status = new ToggleGroup();
    private final ComboBox<String> type = new ComboBox<>();
    private final VBox list = new VBox(10);
    private final Label summary = Ui.label("", "muted");

    @Override
    public String title() {
        return "Disputes & Appeals";
    }

    @Override
    public String subtitle() {
        return "Issues reported on orders and drivers' appeals – decide with a written note";
    }

    @Override
    public Node view() {
        ToggleButton open = Ui.chip("Open", status);
        ToggleButton closed = Ui.chip("Closed", status);
        ToggleButton all = Ui.chip("All", status);
        open.setUserData("open");
        closed.setUserData("closed");
        all.setUserData("all");
        open.setSelected(true);
        status.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) a.setSelected(true);
            else refresh();
        });
        type.getItems().setAll("Issues and appeals", "Issues only", "Driver appeals only");
        type.setValue("Issues and appeals");
        type.setOnAction(e -> refresh());
        root.getChildren().setAll(Ui.row(10, open, closed, all, Ui.grow(), type), summary, list);
        refresh();
        return Ui.scroll(root);
    }

    @Override
    public void refresh() {
        String st = status.getSelectedToggle() == null ? "open" : (String) status.getSelectedToggle().getUserData();
        String ty = switch (type.getValue()) {
            case "Issues only" -> "ISSUE";
            case "Driver appeals only" -> "APPEAL";
            default -> "all";
        };
        list.getChildren().setAll(Ui.loading("Loading cases…"));
        Ui.async(() -> ApiClient.get(ApiClient.query("/api/officer/disputes", "status", st, "type", ty)), arr -> {
            list.getChildren().clear();
            summary.setText(arr.size() + " case" + (arr.size() == 1 ? "" : "s"));
            if (arr.isEmpty()) {
                list.getChildren().add(Ui.card(Ui.empty("fas-balance-scale", "open".equals(st) ? "No open cases" : "No cases",
                        "Buyers and farmers report issues after handover; drivers appeal after a delivery rejection. They appear here.")));
                return;
            }
            for (JsonNode d : arr) {
                Node row = disputeRow(d);
                list.getChildren().add(row);
            }
        }, err -> list.getChildren().setAll(Ui.banner(err, "error")));
    }

    /** Card used here, on the user page and on the order page. Clicking opens the case. */
    public static Node disputeRow(JsonNode d) {
        boolean appeal = "APPEAL".equals(Fmt.text(d, "type"));
        String st = Fmt.textOr(d, "status", "OPEN");
        VBox text = new VBox(4,
                Ui.row(8, Ui.pill(appeal ? "Driver appeal" : "Issue", appeal ? "amber" : "blue"),
                        Ui.pill(Fmt.title(st), Ui.toneForDispute(st)), Ui.label(Fmt.textOr(d, "reason", "No reason given"), "h3")),
                Ui.label("Raised by " + Fmt.textOr(d, "raisedByName", "a user") + " (" + Fmt.role(Fmt.text(d, "raisedByRole")) + ") · "
                        + Fmt.textOr(d, "cropName", "Order") + " · " + Fmt.textOr(d, "orderId", ""), "muted", "small"));
        if (Fmt.text(d, "details") != null) text.getChildren().add(Ui.wrap(Fmt.text(d, "details")));
        if (!"OPEN".equals(st) && Fmt.text(d, "resolutionNote") != null) {
            text.getChildren().add(Ui.wrap("Decision by " + Fmt.textOr(d, "resolvedByName", "an officer") + ": " + Fmt.text(d, "resolutionNote"), "ok-text"));
        }
        HBox.setHgrow(text, Priority.ALWAYS);
        int photos = d.has("photoUrls") ? d.get("photoUrls").size() : 0;
        VBox right = new VBox(6, Ui.label(Fmt.ago(Fmt.text(d, "createdAt")), "muted", "small"));
        if (photos > 0) right.getChildren().add(Ui.iconText("fas-image", photos + " photo" + (photos == 1 ? "" : "s"), "small"));
        right.setAlignment(Pos.TOP_RIGHT);
        Button open = Ui.button("OPEN".equals(st) ? "Review case" : "View", "fas-arrow-right", "btn-small", "OPEN".equals(st) ? "btn-primary" : "btn-ghost");
        open.setContentDisplay(ContentDisplay.RIGHT);
        open.setOnAction(e -> Shell.current().open(new DisputeDetailPage(Fmt.text(d, "id"), Shell.current().active())));
        right.getChildren().add(open);
        HBox row = new HBox(14, text, right);
        row.getStyleClass().add("card");
        return row;
    }
}

package com.harvesthub.desktop.pages;

import com.fasterxml.jackson.databind.JsonNode;
import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.api.Session;
import com.harvesthub.desktop.ui.Page;
import com.harvesthub.desktop.ui.Shell;
import com.harvesthub.desktop.ui.Ui;
import com.harvesthub.desktop.util.Fmt;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Officer home: queue and review numbers, monitoring numbers, oldest waiting listings, recent activity. */
public class DashboardPage implements Page {

    private final VBox root = new VBox(18);

    @Override
    public String title() {
        String name = Session.getOfficer() == null || Session.getOfficer().fullName == null ? "Officer"
                : Session.getOfficer().fullName.split("\\s+")[0];
        int h = LocalTime.now().getHour();
        return (h < 12 ? "Good morning, " : h < 17 ? "Good afternoon, " : "Good evening, ") + name;
    }

    @Override
    public String subtitle() {
        return "Here is what needs your attention today";
    }

    @Override
    public Node view() {
        refresh();
        return Ui.scroll(root);
    }

    @Override
    public void refresh() {
        root.getChildren().setAll(Ui.loading("Loading dashboard…"));
        Ui.async(() -> List.of(ApiClient.get("/api/dashboard/stats"), ApiClient.get("/api/queue?tab=all"),
                ApiClient.get("/api/dashboard/activity?limit=12"), ApiClient.get("/api/officer/disputes?status=open")),
                r -> render(r.get(0), r.get(1), r.get(2), r.get(3)),
                err -> root.getChildren().setAll(Ui.banner(err, "error")));
    }

    private void render(JsonNode s, JsonNode queue, JsonNode activity, JsonNode disputes) {
        HBox review = new HBox(16,
                Ui.statCard("fas-hourglass-half", "amber", String.valueOf(Fmt.count(s, "pendingCount")), "Waiting for verification",
                        Fmt.count(s, "unclaimedCount") + " not yet claimed by any officer"),
                Ui.statCard("fas-user-check", "blue", String.valueOf(Fmt.count(s, "inReviewByMe")), "Claimed by you",
                        "Finish or release them so others can help"),
                Ui.statCard("fas-clipboard-check", "green", String.valueOf(Fmt.count(s, "myReviewedToday")), "Your decisions today",
                        Fmt.count(s, "reviewedTodayCount") + " by all officers today"),
                Ui.statCard("fas-percentage", "grey", Fmt.count(s, "myReviewedTotal") == 0 ? Fmt.NONE : Fmt.count(s, "approvalRate") + "%",
                        "Your approval rate", Fmt.count(s, "myReviewedTotal") + " decisions in total"));
        HBox monitor = new HBox(16,
                Ui.statCard("fas-balance-scale", "red", String.valueOf(Fmt.count(s, "openDisputes")), "Open disputes & appeals", "Need an officer decision"),
                Ui.statCard("fas-truck", "blue", String.valueOf(Fmt.count(s, "activeOrders")), "Orders in progress",
                        Fmt.count(s, "problemOrders") + " rejected, returning or disputed"),
                Ui.statCard("fas-users", "green", String.valueOf(Fmt.count(s, "totalUsers")), "Registered users",
                        Fmt.count(s, "suspendedUsers") + " suspended"));

        // oldest waiting listings first (first come, first served)
        VBox waiting = Ui.card();
        Button openQueue = Ui.button("Open queue", "fas-arrow-right", "btn-small", "btn-primary");
        openQueue.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        openQueue.setOnAction(e -> Shell.current().go(Shell.Nav.QUEUE));
        waiting.getChildren().add(Ui.row(10, Ui.label("Waiting longest", "h2"), Ui.grow(), openQueue));
        List<JsonNode> q = new ArrayList<>();
        queue.forEach(q::add);
        if (q.isEmpty()) {
            waiting.getChildren().add(Ui.empty("fas-check-double", "Queue is empty", "Every submitted listing has been reviewed."));
        } else {
            for (JsonNode item : q.subList(0, Math.min(5, q.size()))) waiting.getChildren().add(queueRow(item));
        }

        VBox open = Ui.card(Ui.row(10, Ui.label("Open disputes", "h2"), Ui.grow(),
                Ui.link("View all", () -> Shell.current().go(Shell.Nav.DISPUTES))));
        if (disputes.isEmpty()) {
            open.getChildren().add(Ui.wrap("No open disputes or appeals.", "muted"));
        } else {
            int n = 0;
            for (JsonNode d : disputes) {
                if (n++ == 4) break;
                Label who = Ui.label(Fmt.textOr(d, "raisedByName", "User") + " · " + Fmt.role(Fmt.text(d, "raisedByRole")), "muted", "small");
                HBox r = Ui.row(10, Ui.pill("APPEAL".equals(Fmt.text(d, "type")) ? "Appeal" : "Issue", "APPEAL".equals(Fmt.text(d, "type")) ? "amber" : "red"),
                        new VBox(2, Ui.label(Fmt.textOr(d, "reason", "Issue"), "strong"), who), Ui.grow(),
                        Ui.label(Fmt.ago(Fmt.text(d, "createdAt")), "muted", "small"));
                open.getChildren().add(r);
            }
        }

        VBox recent = Ui.card(Ui.label("Recent activity", "h2"));
        if (activity.isEmpty()) recent.getChildren().add(Ui.wrap("No activity yet.", "muted"));
        for (JsonNode a : activity) recent.getChildren().add(activityRow(a));

        GridPane lower = new GridPane();
        lower.setHgap(16);
        lower.setVgap(16);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(58);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(42);
        lower.getColumnConstraints().addAll(c1, c2);
        lower.add(waiting, 0, 0);
        lower.add(new VBox(16, open, recent), 1, 0);
        GridPane.setValignment(waiting, javafx.geometry.VPos.TOP);

        root.getChildren().setAll(Ui.label("LISTING VERIFICATION", "caps"), review, Ui.label("MONITORING", "caps"), monitor, lower);
    }

    private Node queueRow(JsonNode item) {
        String status = Fmt.text(item, "status");
        Label pill = Fmt.bool(item, "claimedByMe") ? Ui.pill("Claimed by you", "blue")
                : "UNDER_REVIEW".equals(status) && Fmt.text(item, "claimedByOfficerId") != null ? Ui.pill("With " + Fmt.textOr(item, "claimedByOfficerName", "an officer"), "grey")
                : Fmt.bool(item, "resubmitted") ? Ui.pill("New photos", "amber") : Ui.pill("Pending", "amber");
        HBox r = Ui.row(12, Ui.avatar(Fmt.text(item, "cropName"), 38, true),
                Ui.twoLine(Fmt.textOr(item, "cropName", "Crop") + "  ·  " + Fmt.kg(Fmt.num(item, "quantityKg")),
                        Fmt.textOr(item, "farmerName", "Farmer") + " · " + Fmt.textOr(item, "location", "Location not recorded")),
                Ui.grow(), pill, Ui.label(Fmt.ago(Fmt.text(item, "submittedAt")), "muted", "small"));
        r.getStyleClass().add("card-flat");
        r.setOnMouseClicked(e -> Shell.current().open(new ReviewPage(Fmt.text(item, "id"), Shell.current().active())));
        r.setStyle("-fx-cursor: hand;");
        return r;
    }

    private Node activityRow(JsonNode a) {
        String kind = Fmt.textOr(a, "kind", "");
        String icon;
        String color;
        String text;
        String crop = Fmt.textOr(a, "cropName", "Crop");
        switch (kind) {
            case "APPROVED" -> {
                icon = "fas-check-circle";
                color = "#1E7B3A";
                text = Fmt.textOr(a, "by", "An officer") + " approved " + crop + " as Grade " + Fmt.textOr(a, "grade", "?");
            }
            case "RETAKE" -> {
                icon = "fas-camera";
                color = "#B54708";
                text = Fmt.textOr(a, "by", "An officer") + " asked for new photos of " + crop;
            }
            case "REJECTED" -> {
                icon = "fas-times-circle";
                color = "#B42318";
                text = Fmt.textOr(a, "by", "An officer") + " rejected " + crop;
            }
            case "CLAIMED" -> {
                icon = "fas-user-lock";
                color = "#1D5FA8";
                text = Fmt.textOr(a, "by", "An officer") + " is reviewing " + crop;
            }
            default -> {
                icon = "fas-upload";
                color = "#5B6A62";
                text = Fmt.textOr(a, "farmerName", "A farmer") + " submitted " + crop;
            }
        }
        HBox r = Ui.row(10, Ui.icon(icon, 15, color), Ui.wrap(text), Ui.grow(), Ui.label(Fmt.ago(Fmt.text(a, "at")), "muted", "small"));
        r.setAlignment(Pos.CENTER_LEFT);
        return r;
    }
}

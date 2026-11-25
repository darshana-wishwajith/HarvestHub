package com.harvesthub.desktop.pages;

import com.fasterxml.jackson.databind.JsonNode;
import com.harvesthub.desktop.PortalApp;
import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.ui.Page;
import com.harvesthub.desktop.ui.Shell;
import com.harvesthub.desktop.ui.Ui;
import com.harvesthub.desktop.util.Fmt;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;

import java.util.Map;

/** One dispute or appeal: what was reported, the order and money as the workflow left them, and the officer decision. */
public class DisputeDetailPage implements Page {

    private final String disputeId;
    private final Shell.Nav returnTo;
    private final VBox root = new VBox(16);
    private final VBox feedback = new VBox();

    public DisputeDetailPage(String disputeId, Shell.Nav returnTo) {
        this.disputeId = disputeId;
        this.returnTo = returnTo == null ? Shell.Nav.DISPUTES : returnTo;
    }

    @Override
    public String title() {
        return "Dispute case";
    }

    @Override
    public String subtitle() {
        return "Read both sides, then resolve or dismiss with a written decision";
    }

    @Override
    public Node view() {
        refresh();
        return Ui.scroll(root);
    }

    @Override
    public void refresh() {
        root.getChildren().setAll(Ui.loading("Loading case…"));
        Ui.async(() -> ApiClient.get("/api/officer/disputes/" + ApiClient.enc(disputeId)), this::render,
                err -> root.getChildren().setAll(back(), Ui.banner(err, "error")));
    }

    private Node back() {
        Button b = Ui.button("Back", "fas-arrow-left", "btn-small");
        b.setOnAction(e -> Shell.current().go(returnTo));
        return b;
    }

    private void render(JsonNode detail) {
        JsonNode d = detail.get("dispute");
        boolean appeal = "APPEAL".equals(Fmt.text(d, "type"));
        String st = Fmt.textOr(d, "status", "OPEN");
        Shell.current().setSubtitle((appeal ? "Driver appeal" : "Issue") + " · " + Fmt.textOr(d, "cropName", "order") + " · " + Fmt.text(d, "orderId"));

        VBox report = Ui.card(
                Ui.row(8, Ui.pill(appeal ? "Driver appeal" : "Issue after handover", appeal ? "amber" : "blue"), Ui.pill(Fmt.title(st), Ui.toneForDispute(st))),
                Ui.label(Fmt.textOr(d, "reason", "No reason given"), "h2"),
                Ui.wrap(Fmt.textOr(d, "details", "No details were written."), Fmt.text(d, "details") == null ? "muted" : "label"),
                Ui.details("Raised by", Fmt.textOr(d, "raisedByName", "User") + " (" + Fmt.role(Fmt.text(d, "raisedByRole")) + ")",
                        "Reported", Fmt.dateTime(Fmt.text(d, "createdAt")),
                        "Order status then", Fmt.orderStatus(Fmt.text(d, "orderStatusAtOpen")),
                        "Order status now", Fmt.orderStatus(Fmt.text(d, "orderStatus"))));
        if (appeal) {
            report.getChildren().add(Ui.banner("The buyer rejected this delivery, so the workflow refunded the buyer and charged the "
                    + "driver for the crop. The driver says this was not their fault.", "info"));
        }
        JsonNode photos = d.get("photoUrls");
        if (photos != null && photos.size() > 0) {
            HBox row = new HBox(10);
            for (JsonNode p : photos) {
                StackPane img = Ui.remoteImage(p.asText(), 170, 125);
                img.setOnMouseClicked(e -> PortalApp.openInBrowser(ApiClient.absolute(p.asText())));
                row.getChildren().add(img);
            }
            report.getChildren().addAll(Ui.label("Photos attached (click to open)", "caps"), row);
        }

        VBox order = Ui.card(Ui.label("Order", "h2"));
        JsonNode o = detail.get("order");
        if (o == null || o.isNull()) {
            order.getChildren().add(Ui.wrap("The order was not found.", "muted"));
        } else {
            JsonNode pay = detail.get("payment");
            order.getChildren().add(Ui.details(
                    "Crop", Fmt.textOr(o, "cropName", null) + " · " + Fmt.kg(Fmt.num(o, "quantityKg")),
                    "Crop value", Fmt.money(Fmt.num(o, "cropValue")),
                    "Transport fee", pay == null || pay.isNull() ? null : Fmt.money(Fmt.num(pay, "transportFee")),
                    "Settlement", pay == null || pay.isNull() ? null : Fmt.text(pay, "settlementNote")));
            Button open = Ui.button("Open order", "fas-external-link-alt", "btn-small");
            open.setOnAction(e -> Shell.current().open(new OrderDetailPage(Fmt.text(o, "orderId"), Shell.Nav.DISPUTES)));
            order.getChildren().add(open);
        }
        order.getChildren().addAll(new javafx.scene.control.Separator(), Ui.label("People", "h3"),
                person("Farmer", detail.get("farmer")), person("Buyer", detail.get("buyer")), person("Transporter", detail.get("transporter")));

        GridPane grid = new GridPane();
        grid.setHgap(16);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(58);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(42);
        grid.getColumnConstraints().addAll(c1, c2);
        VBox left = new VBox(16, report, decisionCard(d));
        grid.add(left, 0, 0);
        grid.add(order, 1, 0);
        GridPane.setValignment(left, VPos.TOP);
        GridPane.setValignment(order, VPos.TOP);

        root.getChildren().setAll(back(), feedback, grid);
        JsonNode others = detail.get("otherDisputes");
        if (others != null && others.size() > 0) {
            VBox card = Ui.card(Ui.label("Other cases on this order", "h2"));
            for (JsonNode x : others) card.getChildren().add(DisputesPage.disputeRow(x));
            root.getChildren().add(card);
        }
    }

    private Node person(String label, JsonNode p) {
        if (p == null || p.isNull()) return Ui.row(10, Ui.label(label, "muted"), Ui.grow(), Ui.label("—", "muted"));
        HBox row = Ui.row(10, Ui.avatar(Fmt.text(p, "fullName"), 30, true),
                Ui.twoLine(Fmt.textOr(p, "fullName", label), label + " · " + Fmt.textOr(p, "mobile", "no mobile")
                        + (Fmt.num(p, "trustScore") == null ? "" : " · trust " + Math.round(Fmt.num(p, "trustScore")) + " %")));
        row.setStyle("-fx-cursor: hand;");
        row.setOnMouseClicked(e -> Shell.current().open(new UserDetailPage(Fmt.text(p, "id"), Shell.Nav.DISPUTES)));
        return row;
    }

    private Node decisionCard(JsonNode d) {
        VBox card = Ui.card(Ui.label("Officer decision", "h2"));
        if (!"OPEN".equals(Fmt.text(d, "status"))) {
            card.getChildren().addAll(
                    Ui.banner(Fmt.title(Fmt.text(d, "status")) + " by " + Fmt.textOr(d, "resolvedByName", "an officer")
                            + " on " + Fmt.dateTime(Fmt.text(d, "resolvedAt")), "RESOLVED".equals(Fmt.text(d, "status")) ? "ok" : "info"),
                    Ui.wrap(Fmt.text(d, "resolutionNote")));
            return card;
        }
        TextArea note = new TextArea();
        note.setPromptText("Write the decision everyone on the order will read (at least 10 characters).");
        note.setWrapText(true);
        note.setPrefRowCount(4);
        Button resolve = Ui.button("Resolve", "fas-check", "btn-primary");
        Button dismiss = Ui.button("Dismiss", "fas-ban", "btn-ghost");
        resolve.setDisable(true);
        dismiss.setDisable(true);
        note.textProperty().addListener((o, a, b) -> {
            boolean ok = b != null && b.trim().length() >= 10;
            resolve.setDisable(!ok);
            dismiss.setDisable(!ok);
        });
        resolve.setOnAction(e -> decide("RESOLVED", note.getText().trim()));
        dismiss.setOnAction(e -> decide("DISMISSED", note.getText().trim()));
        card.getChildren().addAll(
                Ui.wrap("Resolve when the complaint is upheld or settled; dismiss when it is not supported. Payments stay "
                        + "as the workflow already set them – the decision is recorded and sent to every party.", "muted"),
                note, Ui.row(10, resolve, dismiss));
        return card;
    }

    private void decide(String outcome, String text) {
        String verb = "RESOLVED".equals(outcome) ? "Resolve" : "Dismiss";
        if (!Ui.confirm(verb + " this case?", "Everyone on the order is notified with your decision. This cannot be undone.", verb)) return;
        feedback.getChildren().setAll(Ui.loading("Saving decision…"));
        Ui.async(() -> ApiClient.post("/api/officer/disputes/" + ApiClient.enc(disputeId) + "/resolve", Map.of("outcome", outcome, "note", text)),
                detail -> {
                    render(detail);
                    feedback.getChildren().setAll(Ui.banner("Case " + ("RESOLVED".equals(outcome) ? "resolved" : "dismissed")
                            + ". The parties have been notified.", "ok"));
                    Shell.current().refreshCounts();
                }, err -> feedback.getChildren().setAll(Ui.banner(err, "error")));
    }
}

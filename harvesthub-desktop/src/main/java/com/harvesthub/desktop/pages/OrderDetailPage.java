package com.harvesthub.desktop.pages;

import com.fasterxml.jackson.databind.JsonNode;
import com.harvesthub.desktop.PortalApp;
import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.ui.Page;
import com.harvesthub.desktop.ui.Shell;
import com.harvesthub.desktop.ui.Ui;
import com.harvesthub.desktop.util.Fmt;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.*;

import java.util.Locale;

/** Full order picture for an officer: parties, money (fees and escrow), timeline, loading evidence and disputes. */
public class OrderDetailPage implements Page {

    private final String orderId;
    private final Shell.Nav returnTo;
    private final VBox root = new VBox(16);

    public OrderDetailPage(String orderId, Shell.Nav returnTo) {
        this.orderId = orderId;
        this.returnTo = returnTo == null ? Shell.Nav.ORDERS : returnTo;
    }

    @Override
    public String title() {
        return "Order details";
    }

    @Override
    public String subtitle() {
        return orderId;
    }

    @Override
    public Node view() {
        refresh();
        return Ui.scroll(root);
    }

    @Override
    public void refresh() {
        root.getChildren().setAll(Ui.loading("Loading order…"));
        Ui.async(() -> ApiClient.get("/api/officer/orders/" + ApiClient.enc(orderId)), this::render,
                err -> root.getChildren().setAll(back(), Ui.banner(err, "error")));
    }

    private Node back() {
        Button b = Ui.button("Back", "fas-arrow-left", "btn-small");
        b.setOnAction(e -> Shell.current().go(returnTo));
        return b;
    }

    private void render(JsonNode d) {
        JsonNode o = d.get("order");
        Shell.current().setSubtitle(Fmt.textOr(o, "cropName", "Order") + " · " + orderId);
        HBox head = Ui.row(10, back(), Ui.grow(), Ui.pill(Fmt.orderStatus(Fmt.text(o, "status")), Ui.toneForStage(Fmt.text(o, "stage"))));
        if (Fmt.bool(o, "disputeOpen")) head.getChildren().add(Ui.pill("Dispute open", "red"));

        VBox summary = Ui.card(Ui.label("Order", "h2"), Ui.details(
                "Crop", Fmt.text(o, "cropName"),
                "Quantity", Fmt.kg(Fmt.num(o, "quantityKg")),
                "Agreed price", Fmt.perKg(Fmt.num(d, "pricePerKg")),
                "Created", Fmt.dateTime(Fmt.text(o, "createdDate")),
                "Delivery date", Fmt.date(Fmt.text(d, "deliveryDate")),
                "Pickup", Fmt.text(d, "pickupAddress"),
                "Drop-off", Fmt.text(d, "dropAddress"),
                "Distance", Fmt.num(d, "distanceKm") == null ? null : String.format(Locale.US, "%.1f km", Fmt.num(d, "distanceKm")),
                "Vehicle", join(Fmt.text(d, "vehicleLabel"), Fmt.text(d, "vehiclePlate"))));
        if (Fmt.num(d, "rating") != null) {
            summary.getChildren().add(Ui.wrap("Buyer rating: " + Math.round(Fmt.num(d, "rating")) + " ★"
                    + (Fmt.text(d, "ratingComment") != null ? " – “" + Fmt.text(d, "ratingComment") + "”" : ""), "muted"));
        }

        VBox parties = Ui.card(Ui.label("People", "h2"), party("Farmer", d.get("farmer")), party("Buyer", d.get("buyer")),
                party("Transporter", d.get("transporter")));

        GridPane top = new GridPane();
        top.setHgap(16);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(34);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(33);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(33);
        top.getColumnConstraints().addAll(c1, c2, c3);
        top.add(summary, 0, 0);
        top.add(moneyCard(d), 1, 0);
        top.add(parties, 2, 0);
        top.getChildren().forEach(n -> GridPane.setValignment(n, VPos.TOP));

        root.getChildren().setAll(head, top, timelineCard(d));
        JsonNode evidence = d.get("loadingEvidenceUrls");
        if (evidence != null && evidence.size() > 0) {
            HBox photos = new HBox(10);
            for (JsonNode u : evidence) {
                StackPane img = Ui.remoteImage(u.asText(), 180, 130);
                img.setOnMouseClicked(e -> PortalApp.openInBrowser(ApiClient.absolute(u.asText())));
                photos.getChildren().add(img);
            }
            root.getChildren().add(Ui.card(Ui.row(8, Ui.label("Loading photos (taken by the driver)", "h2"), Ui.grow(),
                    Ui.label(Fmt.dateTime(Fmt.text(d, "loadingEvidenceAt")), "muted")), photos));
        }
        JsonNode disputes = d.get("disputes");
        if (disputes != null && disputes.size() > 0) {
            VBox card = Ui.card(Ui.label("Disputes and appeals", "h2"));
            for (JsonNode x : disputes) card.getChildren().add(DisputesPage.disputeRow(x));
            root.getChildren().add(card);
        }
    }

    private Node party(String label, JsonNode p) {
        if (p == null || p.isNull()) return Ui.row(10, Ui.label(label, "muted"), Ui.grow(), Ui.label("Not assigned", "muted"));
        HBox row = Ui.row(10, Ui.avatar(Fmt.text(p, "fullName"), 34, true),
                Ui.twoLine(Fmt.textOr(p, "fullName", label), label + " · " + Fmt.textOr(p, "mobile", "no mobile")), Ui.grow(),
                Fmt.bool(p, "accountSuspended") ? Ui.pill("Suspended", "red") : new Region());
        row.setStyle("-fx-cursor: hand;");
        row.setOnMouseClicked(e -> Shell.current().open(new UserDetailPage(Fmt.text(p, "id"), Shell.Nav.ORDERS)));
        return row;
    }

    private Node moneyCard(JsonNode d) {
        JsonNode p = d.get("payment");
        VBox card = Ui.card(Ui.label("Money (mock wallet)", "h2"));
        if (p == null || p.isNull()) {
            card.getChildren().add(Ui.wrap("No price has been agreed yet, so there are no payments.", "muted"));
            return card;
        }
        card.getChildren().add(Ui.details(
                "Crop value", Fmt.money(Fmt.num(p, "cropValue")),
                "Transport fee", Fmt.money(Fmt.num(p, "transportFee")),
                "Buyer fee (3%)", Fmt.money(Fmt.num(p, "buyerFee")),
                "Advance (20% + fees)", Fmt.money(Fmt.num(p, "advanceDue")) + (Fmt.bool(p, "advancePaid") ? "  · paid" : "  · not paid")
                        + (Fmt.text(d, "advancePaymentMethod") != null ? " by " + Fmt.title(Fmt.text(d, "advancePaymentMethod")) : ""),
                "Balance (80%)", Fmt.money(Fmt.num(p, "balanceDue")) + (Fmt.bool(p, "balancePaid") ? "  · paid" : "  · not paid")
                        + (Fmt.text(d, "balancePaymentMethod") != null ? " by " + Fmt.title(Fmt.text(d, "balancePaymentMethod")) : ""),
                "Driver payout (95%)", Fmt.money(Fmt.num(p, "transporterPayout")),
                "Empty trip fee", Fmt.num(p, "emptyTripFee") == null || Fmt.num(p, "emptyTripFee") == 0 ? null : Fmt.money(Fmt.num(p, "emptyTripFee")),
                "Settled", Fmt.bool(p, "settled") ? "Yes" : "Not yet"));
        if (Fmt.text(p, "settlementNote") != null) card.getChildren().add(Ui.wrap(Fmt.text(p, "settlementNote"), "muted", "small"));
        return card;
    }

    private Node timelineCard(JsonNode d) {
        VBox card = Ui.card(Ui.label("Timeline", "h2"));
        JsonNode history = d.get("statusHistory");
        JsonNode events = d.get("transportEvents");
        boolean any = false;
        if (history != null) {
            for (JsonNode h : history) {
                any = true;
                card.getChildren().add(step(Fmt.orderStatus(Fmt.text(h, "status")), Fmt.text(h, "note"), Fmt.text(h, "timestamp")));
            }
        }
        if (events != null && events.size() > 0) {
            card.getChildren().add(Ui.label("Driver events", "caps"));
            for (JsonNode e : events) {
                any = true;
                String note = Fmt.text(e, "note");
                Double lat = Fmt.num(e, "latitude");
                Double lng = Fmt.num(e, "longitude");
                String gps = lat == null || lng == null ? null : String.format(Locale.US, "GPS %.5f, %.5f", lat, lng);
                String reason = Fmt.text(e, "rejectionReason");
                card.getChildren().add(step(Fmt.title(Fmt.text(e, "eventType")),
                        join(note, reason == null ? null : "Reason: " + reason, gps), Fmt.text(e, "timestamp")));
            }
        }
        if (!any) card.getChildren().add(Ui.wrap("No status changes recorded yet.", "muted"));
        return card;
    }

    private Node step(String title, String note, String at) {
        Region dot = new Region();
        dot.getStyleClass().add("timeline-dot");
        VBox text = new VBox(1, Ui.label(title, "strong"));
        if (note != null) text.getChildren().add(Ui.wrap(note, "muted", "small"));
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(12, dot, text, Ui.label(Fmt.dateTime(at), "muted", "small"));
        row.setAlignment(Pos.TOP_LEFT);
        dot.setTranslateY(5);
        return row;
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(p);
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}

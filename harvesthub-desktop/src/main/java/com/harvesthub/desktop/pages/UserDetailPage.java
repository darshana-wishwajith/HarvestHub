package com.harvesthub.desktop.pages;

import com.fasterxml.jackson.databind.JsonNode;
import com.harvesthub.desktop.PortalApp;
import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.api.Session;
import com.harvesthub.desktop.ui.Page;
import com.harvesthub.desktop.ui.Shell;
import com.harvesthub.desktop.ui.Ui;
import com.harvesthub.desktop.util.Fmt;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** One user's account: profile, wallet, listings, orders and disputes; suspend or reactivate with a reason. */
public class UserDetailPage implements Page {

    private final String userId;
    private final Shell.Nav returnTo;
    private final VBox root = new VBox(16);
    private final VBox feedback = new VBox();

    public UserDetailPage(String userId, Shell.Nav returnTo) {
        this.userId = userId;
        this.returnTo = returnTo;
    }

    @Override
    public String title() {
        return "User account";
    }

    @Override
    public String subtitle() {
        return "Profile, activity and account status";
    }

    @Override
    public Node view() {
        refresh();
        return Ui.scroll(root);
    }

    @Override
    public void refresh() {
        root.getChildren().setAll(Ui.loading("Loading account…"));
        Ui.async(() -> ApiClient.get("/api/officer/users/" + ApiClient.enc(userId)), this::render,
                err -> root.getChildren().setAll(back(), Ui.banner(err, "error")));
    }

    private Node back() {
        Shell.Nav target = returnTo == null ? Shell.Nav.USERS : returnTo;
        Button b = Ui.button(returnTo == null ? "Back" : "Back to " + target.name().toLowerCase(Locale.ROOT), "fas-arrow-left", "btn-small");
        b.setOnAction(e -> Shell.current().go(target));
        return b;
    }

    private void render(JsonNode d) {
        JsonNode u = d.get("user");
        String role = Fmt.text(u, "role");
        boolean suspended = Fmt.bool(u, "accountSuspended");
        Shell.current().setSubtitle(Fmt.textOr(u, "fullName", "User") + " · " + Fmt.role(role));

        VBox profile = Ui.card(
                Ui.row(14, Ui.avatar(Fmt.text(u, "fullName"), 56, true),
                        new VBox(4, Ui.label(Fmt.textOr(u, "fullName", "No name"), "h2"), Ui.row(8, Ui.pill(Fmt.role(role), "grey"),
                                suspended ? Ui.pill("Suspended", "red") : Ui.pill("Active", "green")))),
                Ui.details("Email", Fmt.text(u, "email"), "Mobile", Fmt.text(u, "mobile"), "NIC", Fmt.text(d, "nic"),
                        "TRANSPORTER".equals(role) ? "Vehicle base" : "BUYER".equals(role) ? "Delivery location" : "Farm / location",
                        Fmt.text(u, "address")));
        Double lat = Fmt.num(d, "latitude");
        Double lng = Fmt.num(d, "longitude");
        if (lat != null && lng != null) {
            profile.getChildren().add(Ui.link(String.format(Locale.US, "Open %.5f, %.5f in Google Maps", lat, lng),
                    () -> PortalApp.openInBrowser("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng)));
        }

        boolean staff = "OFFICER".equals(role) || "ADMIN".equals(role);
        VBox numbers = Ui.card(Ui.label("Trust and money", "h2"));
        if (staff) {
            numbers.getChildren().add(Ui.wrap("Officer accounts have no wallet or trust score.", "muted"));
        } else {
            Double bal = Fmt.num(u, "walletBalance");
            Double rating = Fmt.num(u, "ratingAverage");
            numbers.getChildren().add(Ui.details(
                    "Trust score", Fmt.num(u, "trustScore") == null ? null : Math.round(Fmt.num(u, "trustScore")) + " %",
                    "Rating", rating == null ? "Not rated yet" : String.format(Locale.US, "%.1f ★", rating),
                    "Completed orders", String.valueOf(Fmt.count(u, "completedOrders")),
                    "Failed orders", String.valueOf(Fmt.count(d, "failedOrders")),
                    "Wallet balance", bal == null ? "No wallet yet" : Fmt.money(bal),
                    "Penalties paid", Fmt.money(Fmt.num(d, "walletPenalties"))));
            if (Fmt.bool(u, "walletSuspended")) {
                numbers.getChildren().add(Ui.banner("The wallet is negative (Empty Trip Fee or damage charge), so the app blocks new "
                        + ("TRANSPORTER".equals(role) ? "jobs" : "listings") + " until it is paid back (workflow 5.3).", "warn"));
            }
        }

        VBox status = statusCard(d, u, staff);
        GridPane top = new GridPane();
        top.setHgap(16);
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(36);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(32);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(32);
        top.getColumnConstraints().addAll(c, c2, c3);
        top.add(profile, 0, 0);
        top.add(numbers, 1, 0);
        top.add(status, 2, 0);
        for (Node n : top.getChildren()) GridPane.setValignment(n, VPos.TOP);

        root.getChildren().setAll(back(), feedback, top);
        if ("TRANSPORTER".equals(role)) root.getChildren().add(vehicleCard(d, u));
        if ("FARMER".equals(role)) root.getChildren().add(listingsCard(d.get("listings")));
        if (!staff) root.getChildren().add(ordersCard(d.get("orders")));
        JsonNode disputes = d.get("disputes");
        if (disputes != null && disputes.size() > 0) root.getChildren().add(disputesCard(disputes));
    }

    private VBox statusCard(JsonNode d, JsonNode u, boolean staff) {
        VBox card = Ui.card(Ui.label("Account status", "h2"));
        if (Fmt.bool(u, "accountSuspended")) {
            card.getChildren().addAll(
                    Ui.banner("Suspended by " + Fmt.textOr(d, "suspendedByName", "an officer") + " on "
                            + Fmt.dateTime(Fmt.text(d, "suspendedAt")) + ". The user cannot sign in.", "error"),
                    Ui.label("Reason", "caps"), Ui.wrap(Fmt.text(u, "suspensionReason")));
            Button re = Ui.button("Reactivate account", "fas-user-check", "btn-primary");
            re.setOnAction(e -> Ui.askText("Reactivate " + Fmt.textOr(u, "fullName", "this account") + "?",
                            "The user can sign in again and is notified. Add a short note (optional, sent to the user).",
                            "e.g. Spoke with the user and the issue is settled.", 0, "Reactivate")
                    .ifPresent(note -> act("/reactivate", Map.of("note", note), "Account reactivated. The user has been notified.")));
            card.getChildren().add(re);
            return card;
        }
        if (Fmt.text(d, "reactivatedAt") != null) {
            card.getChildren().add(Ui.wrap("Reactivated on " + Fmt.dateTime(Fmt.text(d, "reactivatedAt")) + ".", "muted"));
        }
        if (staff) {
            card.getChildren().add(Ui.wrap("Agricultural Officer accounts cannot be suspended from the portal.", "muted"));
            return card;
        }
        long active = 0;
        JsonNode orders = d.get("orders");
        if (orders != null) for (JsonNode o : orders) if ("ACTIVE".equals(Fmt.text(o, "stage"))) active++;
        card.getChildren().add(Ui.wrap("Suspending blocks sign-in and every app action at once"
                + ("TRANSPORTER".equals(Fmt.text(u, "role")) ? ", and the driver gets no new job requests." : ".") , "muted"));
        if (active > 0) card.getChildren().add(Ui.banner(active + " order" + (active == 1 ? " is" : "s are")
                + " still in progress with this user. They stay as they are and cannot move on while the account is suspended.", "warn"));
        Button sus = Ui.button("Suspend account", "fas-user-slash", "btn-danger");
        sus.setDisable(Fmt.text(u, "id") != null && Fmt.text(u, "id").equals(Session.officerId()));
        sus.setOnAction(e -> Ui.askText("Suspend " + Fmt.textOr(u, "fullName", "this account") + "?",
                        "Write the reason. The user sees it in the app and when trying to sign in.",
                        "e.g. Repeated reports of listing crops that do not match the photos.", 10, "Suspend")
                .ifPresent(reason -> act("/suspend", Map.of("reason", reason), "Account suspended. The user has been notified.")));
        card.getChildren().add(sus);
        return card;
    }

    private void act(String path, Map<String, String> body, String done) {
        feedback.getChildren().setAll(Ui.loading("Saving…"));
        Ui.async(() -> ApiClient.post("/api/officer/users/" + ApiClient.enc(userId) + path, body), d -> {
            render(d);
            feedback.getChildren().setAll(Ui.banner(done, "ok"));
            Shell.current().refreshCounts();
        }, err -> feedback.getChildren().setAll(Ui.banner(err, "error")));
    }

    private Node vehicleCard(JsonNode d, JsonNode u) {
        VBox card = Ui.card(Ui.label("Vehicle", "h2"), Ui.details(
                "Type", Fmt.title(Fmt.text(u, "vehicleType")),
                "Plate", Fmt.text(u, "vehiclePlate"),
                "Payload", Fmt.kg(Fmt.num(d, "vehicleCapacityKg")),
                "Body", Fmt.title(Fmt.text(d, "vehicleBodyType"))));
        String licence = Fmt.text(d, "licenceUrl");
        if (licence == null) {
            card.getChildren().add(Ui.wrap("No driving-licence photo uploaded.", "muted"));
        } else {
            javafx.scene.layout.StackPane img = Ui.remoteImage(licence, 260, 170);
            img.setOnMouseClicked(e -> PortalApp.openInBrowser(ApiClient.absolute(licence)));
            card.getChildren().addAll(Ui.label("Driving licence (click to open)", "caps"), img);
        }
        return card;
    }

    private Node listingsCard(JsonNode listings) {
        TableView<JsonNode> t = Ui.table("No listings.");
        t.getColumns().setAll(List.of(
                Ui.textCol("CROP", 160, n -> Fmt.text(n, "cropName")),
                Ui.textCol("QUANTITY", 110, n -> Fmt.kg(Fmt.num(n, "quantityKg"))),
                Ui.textCol("PRICE", 120, n -> Fmt.perKg(Fmt.num(n, "pricePerKg"))),
                Ui.nodeCol("STATUS", 150, n -> Ui.pill(Fmt.title(Fmt.text(n, "status")), Ui.toneForListing(Fmt.text(n, "status")))),
                Ui.nodeCol("GRADE", 90, n -> Fmt.text(n, "grade") == null ? Ui.label(Fmt.NONE, "muted") : Ui.gradePill(Fmt.text(n, "grade"))),
                Ui.textCol("CREATED", 140, n -> Fmt.date(Fmt.text(n, "createdDate")))));
        Ui.onOpen(t, n -> Shell.current().open(new ReviewPage(Fmt.text(n, "id"), Shell.Nav.USERS)));
        List<JsonNode> rows = new ArrayList<>();
        if (listings != null) listings.forEach(rows::add);
        Ui.fill(t, rows);
        t.setPrefHeight(Math.min(8, Math.max(2, rows.size())) * 54 + 44);
        return Ui.card(Ui.row(8, Ui.label("Listings", "h2"), Ui.label(rows.size() + "", "muted"), Ui.grow(),
                Ui.label("Double-click a listing to open it", "muted", "small")), t);
    }

    static Node ordersTable(JsonNode orders, String empty) {
        TableView<JsonNode> t = Ui.table(empty);
        t.getColumns().setAll(List.of(
                Ui.nodeCol("ORDER", 200, n -> Ui.twoLine(Fmt.textOr(n, "cropName", "Crop") + " · " + Fmt.kg(Fmt.num(n, "quantityKg")),
                        Fmt.textOr(n, "orderId", ""))),
                Ui.textCol("FARMER", 140, n -> Fmt.text(n, "farmerName")),
                Ui.textCol("BUYER", 140, n -> Fmt.text(n, "buyerName")),
                Ui.textCol("DRIVER", 140, n -> Fmt.text(n, "transporterName")),
                Ui.nodeCol("STATUS", 210, n -> Ui.row(6, Ui.pill(Fmt.orderStatus(Fmt.text(n, "status")), Ui.toneForStage(Fmt.text(n, "stage"))),
                        Fmt.bool(n, "disputeOpen") ? Ui.pill("Dispute", "red") : new Region())),
                Ui.textCol("CROP VALUE", 110, n -> Fmt.money(Fmt.num(n, "cropValue"))),
                Ui.textCol("CREATED", 110, n -> Fmt.date(Fmt.text(n, "createdDate")))));
        Ui.onOpen(t, n -> Shell.current().open(new OrderDetailPage(Fmt.text(n, "orderId"), Shell.current().active())));
        List<JsonNode> rows = new ArrayList<>();
        if (orders != null) orders.forEach(rows::add);
        Ui.fill(t, rows);
        t.setPrefHeight(Math.min(8, Math.max(2, rows.size())) * 54 + 44);
        return t;
    }

    private Node ordersCard(JsonNode orders) {
        return Ui.card(Ui.row(8, Ui.label("Orders", "h2"), Ui.label((orders == null ? 0 : orders.size()) + "", "muted"), Ui.grow(),
                Ui.label("Double-click an order to open it", "muted", "small")), ordersTable(orders, "No orders yet."));
    }

    private Node disputesCard(JsonNode disputes) {
        VBox card = Ui.card(Ui.label("Disputes and appeals", "h2"));
        for (JsonNode x : disputes) card.getChildren().add(DisputesPage.disputeRow(x));
        return card;
    }
}

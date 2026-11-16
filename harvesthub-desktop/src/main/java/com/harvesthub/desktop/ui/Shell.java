package com.harvesthub.desktop.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.harvesthub.desktop.PortalApp;
import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.api.OfficerDto;
import com.harvesthub.desktop.api.Session;
import com.harvesthub.desktop.pages.*;
import com.harvesthub.desktop.util.Fmt;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Portal frame: sidebar navigation (verification + monitoring), top bar with page title, refresh, notifications
 * and the signed-in officer. Counts in the sidebar come from GET /api/dashboard/stats every minute.
 */
public final class Shell {

    public enum Nav { DASHBOARD, QUEUE, HISTORY, USERS, ORDERS, DISPUTES, PROFILE }

    private static Shell current;
    private static Timeline poller;

    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final Label pageTitle = Ui.label("", "h1");
    private final Label pageSubtitle = Ui.label("", "muted");
    private final Map<Nav, HBox> navItems = new EnumMap<>(Nav.class);
    private final Map<Nav, Label> badges = new EnumMap<>(Nav.class);
    private final Label bellBadge = Ui.label("", "bell-badge");
    private Page page;
    private Nav active;

    private Shell() {
    }

    public static Shell create() {
        stopTimers();
        current = new Shell();
        current.build();
        current.go(Nav.DASHBOARD);
        poller = new Timeline(new KeyFrame(Duration.seconds(60), e -> current.refreshCounts()));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
        return current;
    }

    public static Shell current() {
        return current;
    }

    public static void stopTimers() {
        if (poller != null) poller.stop();
        poller = null;
    }

    public BorderPane view() {
        return root;
    }

    private void build() {
        root.getStyleClass().add("page");
        root.setLeft(sidebar());
        root.setTop(topbar());
        content.setPadding(new Insets(16, 20, 16, 20));
        root.setCenter(content);
    }

    // ---- sidebar ---------------------------------------------------------------------------------

    private Node sidebar() {
        ImageView logo = new ImageView(Ui.resource("logo-full.png"));
        logo.setFitWidth(96);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        VBox brand = new VBox(4, logo, Ui.label("OFFICER PORTAL", "sidebar-tag"));
        brand.setAlignment(Pos.CENTER);
        brand.setPadding(new Insets(6, 0, 10, 0));

        VBox nav = new VBox(2,
                section("VERIFICATION"),
                navItem(Nav.DASHBOARD, "fas-th-large", "Dashboard"),
                navItem(Nav.QUEUE, "fas-clipboard-list", "Verification Queue"),
                navItem(Nav.HISTORY, "fas-history", "Review History"),
                section("MONITORING"),
                navItem(Nav.USERS, "fas-users", "Users"),
                navItem(Nav.ORDERS, "fas-truck", "Orders"),
                navItem(Nav.DISPUTES, "fas-balance-scale", "Disputes & Appeals"),
                section("ACCOUNT"),
                navItem(Nav.PROFILE, "fas-user-circle", "My Profile"));

        Button logout = Ui.button("Log out", "fas-sign-out-alt", "btn-ghost");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setAlignment(Pos.CENTER_LEFT);
        logout.setOnAction(e -> {
            if (Ui.confirm("Log out?", "You will need your password to sign in again.", "Log out")) {
                Session.clear();
                PortalApp.showLogin(null);
            }
        });
        Label server = Ui.wrap("Connected to " + ApiClient.getBaseUrl().replace("https://", ""), "muted", "small");

        VBox side = new VBox(10, brand, nav, Ui.grow(), new Separator(), logout, server);
        side.getStyleClass().add("sidebar");
        side.setPadding(new Insets(12, 12, 12, 12));
        side.setPrefWidth(246);
        side.setMinWidth(246);
        return side;
    }

    private Label section(String text) {
        return Ui.label(text, "nav-section");
    }

    private HBox navItem(Nav nav, String icon, String text) {
        Label badge = Ui.label("", "nav-badge");
        badge.setVisible(false);
        badge.setManaged(false);
        badges.put(nav, badge);
        Label name = Ui.label(text);
        name.setMinWidth(Region.USE_PREF_SIZE); // never cut to "Verification Qu…"
        HBox item = new HBox(10, Ui.icon(icon, 15), name, Ui.grow(), badge);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("nav-item");
        item.setOnMouseClicked(e -> go(nav));
        navItems.put(nav, item);
        return item;
    }

    private void highlight(Nav nav) {
        navItems.values().forEach(n -> n.getStyleClass().remove("nav-item-active"));
        if (nav != null) navItems.get(nav).getStyleClass().add("nav-item-active");
    }

    // ---- top bar ---------------------------------------------------------------------------------

    private Node topbar() {
        VBox titles = new VBox(2, pageTitle, pageSubtitle);
        Button refresh = Ui.button("Refresh", "fas-sync-alt", "btn-small");
        refresh.setOnAction(e -> {
            if (page != null) page.refresh();
            refreshCounts();
        });
        Button bell = Ui.button(null, "fas-bell", "btn-ghost");
        bell.setTooltip(new Tooltip("Notifications"));
        bellBadge.setVisible(false);
        StackPane bellBox = new StackPane(bell, bellBadge);
        StackPane.setAlignment(bellBadge, Pos.TOP_RIGHT);
        bellBadge.setMouseTransparent(true);
        bell.setOnAction(e -> NotificationsDialog.show(this::refreshCounts));

        OfficerDto me = Session.getOfficer();
        String name = me == null ? "Officer" : Fmt.orDash(me.fullName);
        HBox chip = new HBox(8, Ui.avatar(name, 30, false),
                new VBox(0, Ui.label(name, "strong"), Ui.label("Agricultural Officer", "muted", "small")));
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("officer-chip");
        chip.setOnMouseClicked(e -> go(Nav.PROFILE));

        HBox bar = new HBox(14, titles, Ui.grow(), refresh, bellBox, chip);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        bar.setPadding(new Insets(9, 20, 9, 20));
        return bar;
    }

    // ---- navigation ------------------------------------------------------------------------------

    public void go(Nav nav) {
        Page p = switch (nav) {
            case DASHBOARD -> new DashboardPage();
            case QUEUE -> new QueuePage();
            case HISTORY -> new HistoryPage();
            case USERS -> new UsersPage();
            case ORDERS -> new OrdersPage();
            case DISPUTES -> new DisputesPage();
            case PROFILE -> new ProfilePage();
        };
        active = nav;
        highlight(nav);
        show(p);
        refreshCounts();
    }

    /** Detail screens (listing review, user, order, dispute) keep the current sidebar highlight. */
    public void open(Page p) {
        show(p);
    }

    public Nav active() {
        return active;
    }

    private void show(Page p) {
        page = p;
        pageTitle.setText(p.title());
        pageSubtitle.setText(p.subtitle());
        content.getChildren().setAll(p.view());
        PortalApp.setPageTitle(p.title());
    }

    public void setSubtitle(String text) {
        pageSubtitle.setText(text);
    }

    // ---- counts ----------------------------------------------------------------------------------

    public void refreshCounts() {
        Ui.async(() -> ApiClient.get("/api/dashboard/stats"), this::applyCounts, err -> { });
    }

    private void applyCounts(JsonNode s) {
        setBadge(Nav.QUEUE, Fmt.count(s, "unclaimedCount"));
        setBadge(Nav.DISPUTES, Fmt.count(s, "openDisputes"));
        long unread = Fmt.count(s, "unreadNotifications");
        bellBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
        bellBadge.setVisible(unread > 0);
    }

    private void setBadge(Nav nav, long n) {
        Label b = badges.get(nav);
        b.setText(n > 99 ? "99+" : String.valueOf(n));
        b.setVisible(n > 0);
        b.setManaged(n > 0);
    }
}

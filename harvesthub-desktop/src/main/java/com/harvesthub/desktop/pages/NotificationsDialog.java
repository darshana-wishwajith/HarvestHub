package com.harvesthub.desktop.pages;

import com.fasterxml.jackson.databind.JsonNode;
import com.harvesthub.desktop.PortalApp;
import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.api.Session;
import com.harvesthub.desktop.ui.Shell;
import com.harvesthub.desktop.ui.Ui;
import com.harvesthub.desktop.util.Fmt;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** The officer's own notifications (new disputes, appeals, …) from GET /notifications/{officerId}. */
public final class NotificationsDialog {

    private NotificationsDialog() {
    }

    public static void show(Runnable afterClose) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("HarvestHub Officer Portal");
        d.setHeaderText("Notifications");
        d.initOwner(PortalApp.stage());
        d.getDialogPane().getStylesheets().add(PortalApp.css());
        ButtonType markAll = new ButtonType("Mark all as read", ButtonBar.ButtonData.LEFT);
        d.getDialogPane().getButtonTypes().addAll(markAll, ButtonType.CLOSE);
        VBox list = new VBox(6, Ui.loading("Loading notifications…"));
        ScrollPane sp = Ui.scroll(list);
        sp.setPrefSize(520, 460);
        d.getDialogPane().setContent(sp);
        String me = Session.officerId();
        load(list, me);
        Button mark = (Button) d.getDialogPane().lookupButton(markAll);
        mark.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            mark.setDisable(true);
            Ui.async(() -> ApiClient.post(ApiClient.query("/notifications/mark-all-read", "farmerId", me), null),
                    ok -> {
                        mark.setDisable(false);
                        load(list, me);
                    }, err -> {
                        mark.setDisable(false);
                        list.getChildren().add(0, Ui.banner(err, "error"));
                    });
        });
        d.showAndWait();
        if (afterClose != null) afterClose.run();
    }

    private static void load(VBox list, String me) {
        Ui.async(() -> ApiClient.get("/notifications/" + ApiClient.enc(me)), arr -> {
            list.getChildren().clear();
            if (arr == null || arr.isEmpty()) {
                list.getChildren().add(Ui.empty("fas-bell-slash", "No notifications", "New disputes and appeals will appear here."));
                return;
            }
            for (JsonNode n : arr) list.getChildren().add(item(n));
        }, err -> list.getChildren().setAll(Ui.banner(err, "error")));
    }

    private static Node item(JsonNode n) {
        boolean read = Fmt.bool(n, "read") || Fmt.bool(n, "isRead");
        String type = Fmt.textOr(n, "type", "");
        String icon = type.startsWith("DISPUTE") ? "fas-balance-scale" : type.startsWith("ACCOUNT") ? "fas-user-shield" : "fas-bell";
        Label title = Ui.wrap(Fmt.textOr(n, "title", "Notification"), "strong");
        Label body = Ui.wrap(Fmt.text(n, "body"), "muted");
        Label when = Ui.label(Fmt.ago(Fmt.text(n, "timestamp")), "muted", "small");
        VBox text = new VBox(3, title, body, when);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(12, Ui.icon(icon, 16, read ? "#9AA8A0" : "#1B4332"), text);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("notif-item");
        if (!read) row.getStyleClass().add("notif-unread");
        if (type.startsWith("DISPUTE")) {
            row.setOnMouseClicked(e -> {
                row.getScene().getWindow().hide();
                Shell.current().go(Shell.Nav.DISPUTES);
            });
            row.setStyle("-fx-cursor: hand;");
        }
        return row;
    }
}

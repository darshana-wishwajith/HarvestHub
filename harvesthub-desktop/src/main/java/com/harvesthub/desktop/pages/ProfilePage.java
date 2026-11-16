package com.harvesthub.desktop.pages;

import com.fasterxml.jackson.databind.JsonNode;
import com.harvesthub.desktop.PortalApp;
import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.api.Session;
import com.harvesthub.desktop.ui.Page;
import com.harvesthub.desktop.ui.Ui;
import com.harvesthub.desktop.util.Fmt;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

/** The signed-in officer: details from the server, own review numbers, and change password. */
public class ProfilePage implements Page {

    private final VBox root = new VBox(16);

    @Override
    public String title() {
        return "My Profile";
    }

    @Override
    public String subtitle() {
        return "Your officer account";
    }

    @Override
    public Node view() {
        refresh();
        return Ui.scroll(root);
    }

    @Override
    public void refresh() {
        root.getChildren().setAll(Ui.loading("Loading profile…"));
        Ui.async(() -> List.of(ApiClient.get("/api/officers/me"), ApiClient.get("/api/dashboard/stats"),
                ApiClient.get("/api/review-history?scope=mine")), r -> render(r.get(0), r.get(1), r.get(2)),
                err -> root.getChildren().setAll(Ui.banner(err, "error")));
    }

    private void render(JsonNode me, JsonNode stats, JsonNode history) {
        VBox profile = Ui.card(
                Ui.row(14, Ui.avatar(Fmt.text(me, "fullName"), 64, false),
                        new VBox(4, Ui.label(Fmt.textOr(me, "fullName", "Officer"), "h2"), Ui.pill("Agricultural Officer", "green"))),
                Ui.details("Officer code", Fmt.text(me, "officerIdCode"), "Email", Fmt.text(me, "email"),
                        "Mobile", Fmt.text(me, "phone"), "Region", Fmt.text(me, "region")),
                Ui.wrap("To change these details, contact the HarvestHub team.", "muted", "small"));

        long approved = 0;
        long retake = 0;
        long rejected = 0;
        long[] grades = new long[3];
        for (JsonNode h : history) {
            switch (Fmt.textOr(h, "decision", "")) {
                case "APPROVED" -> {
                    approved++;
                    String g = Fmt.textOr(h, "grade", "");
                    if (g.equals("A")) grades[0]++;
                    if (g.equals("B")) grades[1]++;
                    if (g.equals("C")) grades[2]++;
                }
                case "RETAKE" -> retake++;
                case "REJECTED" -> rejected++;
                default -> { }
            }
        }
        VBox work = Ui.card(Ui.label("My verification work", "h2"), Ui.details(
                "Decisions", String.valueOf(history.size()),
                "Today", String.valueOf(Fmt.count(stats, "myReviewedToday")),
                "Approved", approved + "  (Grade A " + grades[0] + " · B " + grades[1] + " · C " + grades[2] + ")",
                "Retakes requested", String.valueOf(retake),
                "Rejected", String.valueOf(rejected),
                "Approval rate", history.size() == 0 ? null : Fmt.count(stats, "approvalRate") + " %",
                "Claimed right now", String.valueOf(Fmt.count(stats, "inReviewByMe"))));

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c, c2);
        grid.add(profile, 0, 0);
        grid.add(work, 1, 0);
        grid.add(passwordCard(), 0, 1);
        grid.add(sessionCard(), 1, 1);
        grid.getChildren().forEach(n -> GridPane.setValignment(n, VPos.TOP));
        root.getChildren().setAll(grid);
    }

    private Node passwordCard() {
        PasswordField current = new PasswordField();
        current.setPromptText("Current password");
        PasswordField p1 = new PasswordField();
        p1.setPromptText("New password");
        PasswordField p2 = new PasswordField();
        p2.setPromptText("Confirm new password");
        VBox feedback = new VBox();
        Button save = Ui.button("Change password", "fas-key", "btn-primary");
        save.setOnAction(e -> {
            if (current.getText().isEmpty()) {
                feedback.getChildren().setAll(Ui.banner("Enter your current password.", "error"));
                return;
            }
            String problem = LoginView.passwordProblem(p1.getText(), p2.getText());
            if (problem != null) {
                feedback.getChildren().setAll(Ui.banner(problem, "error"));
                return;
            }
            save.setDisable(true);
            Ui.async(() -> ApiClient.post("/api/me/change-password", Map.of("currentPassword", current.getText(), "newPassword", p1.getText())),
                    ok -> {
                        save.setDisable(false);
                        current.clear();
                        p1.clear();
                        p2.clear();
                        feedback.getChildren().setAll(Ui.banner("Password changed. Use the new password next time you sign in.", "ok"));
                    }, err -> {
                        save.setDisable(false);
                        feedback.getChildren().setAll(Ui.banner(err, "error"));
                    });
        });
        return Ui.card(Ui.label("Change password", "h2"), current, p1, p2,
                Ui.wrap("At least 8 characters, with letters and numbers.", "muted", "small"), feedback, save);
    }

    private Node sessionCard() {
        Button logout = Ui.button("Log out", "fas-sign-out-alt", "btn-danger");
        logout.setOnAction(e -> {
            Session.clear();
            PortalApp.showLogin("You have logged out.");
        });
        return Ui.card(Ui.label("This computer", "h2"), Ui.details(
                        "Server", ApiClient.getBaseUrl(),
                        "Remembered email", com.harvesthub.desktop.util.Prefs.rememberedLogin().isBlank() ? "None" : com.harvesthub.desktop.util.Prefs.rememberedLogin()),
                Ui.wrap("Your password is never stored on this computer. Log out when you leave the desk.", "muted", "small"), logout);
    }
}

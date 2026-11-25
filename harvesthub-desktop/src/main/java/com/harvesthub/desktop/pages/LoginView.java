package com.harvesthub.desktop.pages;

import com.harvesthub.desktop.PortalApp;
import com.harvesthub.desktop.api.ApiClient;
import com.harvesthub.desktop.api.ApiException;
import com.harvesthub.desktop.api.LoginResponseDto;
import com.harvesthub.desktop.api.Session;
import com.harvesthub.desktop.ui.Ui;
import com.harvesthub.desktop.util.Prefs;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.regex.Pattern;

/**
 * Sign-in for Agricultural Officers, plus the real forgot-password flow (same email-code flow as the
 * mobile app – email → 6-digit code → new password).
 */
public class LoginView {

    static final Pattern CODE = Pattern.compile("\\d{6}");

    private final String startMessage;
    private final StackPane formHost = new StackPane();
    private Timeline resendTimer;

    public LoginView(String startMessage) {
        this.startMessage = startMessage;
    }

    public Parent view() {
        GridPane grid = new GridPane();
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(44);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(56);
        grid.getColumnConstraints().addAll(left, right);
        RowConstraints rc = new RowConstraints();
        rc.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().add(rc);
        grid.add(brandPanel(), 0, 0);
        formHost.getStyleClass().add("login-form-area");
        formHost.setPadding(new Insets(30));
        grid.add(formHost, 1, 0);
        showSignIn(null, startMessage, "warn");
        return grid;
    }

    // ---- left: brand -----------------------------------------------------------------------------

    private Node brandPanel() {
        // Logo straight on the green panel (no white box): light leaf mark + two-colour wordmark
        ImageView mark = new ImageView(Ui.resource("logo-mark-light.png"));
        mark.setFitWidth(52);
        mark.setPreserveRatio(true);
        mark.setSmooth(true);
        Label harvest = Ui.label("Harvest", "login-wordmark");
        Label hub = Ui.label("Hub", "login-wordmark", "login-wordmark-accent");
        HBox logoBox = new HBox(12, mark, new HBox(0, harvest, hub));
        logoBox.setAlignment(Pos.CENTER_LEFT);
        ((HBox) logoBox.getChildren().get(1)).setAlignment(Pos.CENTER_LEFT);

        Label hero = Ui.wrap("Agricultural Officer Portal", "login-hero");
        Label copy = Ui.wrap("Check the photos, video and GPS that farmers capture at their farms, give each crop "
                + "Grade A, B or C, and keep watch over users, orders and disputes on HarvestHub.", "login-copy");
        VBox points = new VBox(12,
                point("fas-clipboard-check", "One shared queue – a claimed listing is locked to you"),
                point("fas-medal", "Grades A, B and C follow the Department of Agriculture guide"),
                point("fas-balance-scale", "Resolve buyer issues and driver appeals with a written decision"));
        VBox box = new VBox(26, logoBox, Ui.grow(), hero, copy, points, Ui.grow(),
                Ui.label("ICT 2212 · Group 12 CodeX · Rajarata University of Sri Lanka", "login-copy", "small"));
        box.getStyleClass().add("login-brand");
        box.setPadding(new Insets(40, 48, 32, 48));
        return box;
    }

    private Node point(String icon, String text) {
        Label l = Ui.wrap(text, "login-point");
        HBox h = new HBox(12, Ui.icon(icon, 16, "#95D5B2"), l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    // ---- sign in ---------------------------------------------------------------------------------------

    private void showSignIn(String prefill, String message, String tone) {
        stopTimer();
        TextField login = new TextField(prefill != null ? prefill : Prefs.rememberedLogin());
        login.setPromptText("Email or mobile number");
        PasswordField pwd = new PasswordField();
        pwd.setPromptText("Password");
        TextField pwdShown = new TextField();
        pwdShown.setPromptText("Password");
        pwdShown.textProperty().bindBidirectional(pwd.textProperty());
        pwdShown.setVisible(false);
        pwdShown.setManaged(false);
        Button eye = Ui.button(null, "fas-eye", "btn-ghost");
        eye.setFocusTraversable(false);
        eye.setTooltip(new Tooltip("Show password"));
        eye.setOnAction(e -> {
            boolean show = !pwdShown.isVisible();
            pwdShown.setVisible(show);
            pwdShown.setManaged(show);
            pwd.setVisible(!show);
            pwd.setManaged(!show);
            ((org.kordamp.ikonli.javafx.FontIcon) eye.getGraphic()).setIconLiteral(show ? "fas-eye-slash" : "fas-eye");
            (show ? pwdShown : pwd).requestFocus();
        });
        HBox.setHgrow(pwd, Priority.ALWAYS);
        HBox.setHgrow(pwdShown, Priority.ALWAYS);
        HBox pwdRow = new HBox(6, pwd, pwdShown, eye);
        pwdRow.setAlignment(Pos.CENTER_LEFT);

        CheckBox remember = new CheckBox("Remember my email on this computer");
        remember.setSelected(!Prefs.rememberedLogin().isBlank());
        Hyperlink forgot = Ui.link("Forgot password?", () -> showForgotEmail(login.getText()));

        VBox feedback = new VBox();
        if (message != null) feedback.getChildren().setAll(Ui.banner(message, tone));
        Button signIn = Ui.button("Sign in", "fas-arrow-right", "btn-primary", "btn-wide");
        signIn.setContentDisplay(ContentDisplay.RIGHT);
        signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setDefaultButton(true);
        signIn.setOnAction(e -> {
            String id = login.getText() == null ? "" : login.getText().trim();
            String pw = pwd.getText() == null ? "" : pwd.getText();
            if (id.isEmpty() || pw.isEmpty()) {
                feedback.getChildren().setAll(Ui.banner("Enter your email (or mobile number) and password.", "error"));
                return;
            }
            signIn.setDisable(true);
            signIn.setText("Signing in…");
            feedback.getChildren().clear();
            Ui.async(() -> ApiClient.login(id, pw), (LoginResponseDto r) -> {
                if (r == null || r.token == null || r.officer == null || !Session.mayUsePortal(r.officer.role)) {
                    Session.clear();
                    resetButton(signIn);
                    feedback.getChildren().setAll(Ui.banner("This portal is only for Agricultural Officers. Farmers, buyers and "
                            + "transporters sign in on the HarvestHub mobile app.", "error"));
                    return;
                }
                Prefs.rememberLogin(remember.isSelected() ? id : null);
                Session.set(r.token, r.officer);
                PortalApp.showPortal();
            }, err -> {
                resetButton(signIn);
                feedback.getChildren().setAll(Ui.banner("Invalid credentials".equalsIgnoreCase(err)
                        ? "The email/mobile or password is not correct." : err, "error"));
                pwd.clear();
                pwd.requestFocus();
            });
        });

        VBox form = form(
                Ui.label("AGRICULTURAL OFFICER PORTAL", "login-eyebrow"),
                Ui.label("Sign in", "h1"),
                Ui.wrap("Use the email or mobile number registered for your officer account.", "muted"),
                gap(6),
                field("Email or mobile number", login),
                field("Password", pwdRow),
                Ui.row(8, remember, Ui.grow(), forgot),
                feedback,
                signIn,
                gap(10),
                new Separator(),
                Ui.wrap("For authorised Agricultural Officers only. Farmers, buyers and transporters use the HarvestHub "
                        + "mobile app.", "footer-note"));
        formHost.getChildren().setAll(form);
        javafx.application.Platform.runLater(() -> (login.getText().isBlank() ? login : pwd).requestFocus());
    }

    private static void resetButton(Button b) {
        b.setDisable(false);
        b.setText("Sign in");
    }

    // ---- forgot password: step 1 email -------------------------------------------------------------

    private void showForgotEmail(String prefill) {
        stopTimer();
        TextField email = new TextField(prefill != null && prefill.contains("@") ? prefill.trim() : "");
        email.setPromptText("you@example.com");
        VBox feedback = new VBox();
        Button send = Ui.button("Send verification code", "fas-paper-plane", "btn-primary", "btn-wide");
        send.setMaxWidth(Double.MAX_VALUE);
        send.setDefaultButton(true);
        send.setOnAction(e -> {
            String value = email.getText() == null ? "" : email.getText().trim();
            if (!value.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
                feedback.getChildren().setAll(Ui.banner("Enter the email address of your officer account.", "error"));
                return;
            }
            send.setDisable(true);
            send.setText("Sending…");
            Ui.async(() -> {
                ApiClient.forgotPassword(value);
                return true;
            }, ok -> showForgotCode(value, null), err -> {
                send.setDisable(false);
                send.setText("Send verification code");
                feedback.getChildren().setAll(Ui.banner(err, "error"));
            });
        });
        formHost.getChildren().setAll(form(
                backLink(),
                Ui.label("Reset your password", "h1"),
                Ui.wrap("We will email a 6-digit verification code to your officer account. The code is valid for 15 minutes.", "muted"),
                gap(6),
                field("Email address", email),
                feedback,
                send));
        javafx.application.Platform.runLater(email::requestFocus);
    }

    // ---- step 2 code --------------------------------------------------------------------------------------

    private void showForgotCode(String email, String message) {
        stopTimer();
        TextField code = new TextField();
        code.setPromptText("6-digit code");
        code.setStyle("-fx-font-size: 20px; -fx-letter-spacing: 4;");
        code.textProperty().addListener((o, a, b) -> {
            if (b != null && !b.matches("\\d{0,6}")) code.setText(b.replaceAll("\\D", "").substring(0, Math.min(6, b.replaceAll("\\D", "").length())));
        });
        VBox feedback = new VBox(Ui.banner(message != null ? message
                : "If " + email + " belongs to an officer account, a code is on its way. Check the inbox and spam folder.", "info"));
        Button verify = Ui.button("Verify code", "fas-check", "btn-primary", "btn-wide");
        verify.setMaxWidth(Double.MAX_VALUE);
        verify.setDefaultButton(true);
        verify.setOnAction(e -> {
            String c = code.getText() == null ? "" : code.getText().trim();
            if (!CODE.matcher(c).matches()) {
                feedback.getChildren().setAll(Ui.banner("Enter the 6 digits from the email.", "error"));
                return;
            }
            verify.setDisable(true);
            Ui.async(() -> {
                ApiClient.verifyResetCode(email, c);
                return true;
            }, ok -> showNewPassword(email, c), err -> {
                verify.setDisable(false);
                feedback.getChildren().setAll(Ui.banner(err, "error"));
            });
        });
        Button resend = Ui.button("Resend code", "fas-redo", "btn-small");
        Label wait = Ui.label("", "muted", "small");
        startResendTimer(resend, wait);
        resend.setOnAction(e -> {
            resend.setDisable(true);
            Ui.async(() -> {
                ApiClient.forgotPassword(email);
                return true;
            }, ok -> showForgotCode(email, "A new code was sent to " + email + "."),
                    err -> {
                        feedback.getChildren().setAll(Ui.banner(err, "error"));
                        resend.setDisable(false);
                    });
        });
        formHost.getChildren().setAll(form(
                backLink(),
                Ui.label("Enter verification code", "h1"),
                Ui.wrap("Sent to " + email, "muted"),
                gap(6),
                field("Verification code", code),
                feedback,
                verify,
                Ui.row(10, resend, wait, Ui.grow(), Ui.link("Use a different email", () -> showForgotEmail(email)))));
        javafx.application.Platform.runLater(code::requestFocus);
    }

    private void startResendTimer(Button resend, Label wait) {
        final int[] left = {60};
        resend.setDisable(true);
        wait.setText("Resend in 60 s");
        resendTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            left[0]--;
            if (left[0] <= 0) {
                resend.setDisable(false);
                wait.setText("");
                stopTimer();
            } else {
                wait.setText("Resend in " + left[0] + " s");
            }
        }));
        resendTimer.setCycleCount(60);
        resendTimer.play();
    }

    private void stopTimer() {
        if (resendTimer != null) resendTimer.stop();
        resendTimer = null;
    }

    // ---- step 3 new password -------------------------------------------------------------------------------

    private void showNewPassword(String email, String code) {
        stopTimer();
        PasswordField p1 = new PasswordField();
        p1.setPromptText("New password");
        PasswordField p2 = new PasswordField();
        p2.setPromptText("Type it again");
        Label rules = Ui.wrap("At least 8 characters, with letters and numbers.", "muted", "small");
        VBox feedback = new VBox();
        Button save = Ui.button("Save new password", "fas-lock", "btn-primary", "btn-wide");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setDefaultButton(true);
        save.setOnAction(e -> {
            String problem = passwordProblem(p1.getText(), p2.getText());
            if (problem != null) {
                feedback.getChildren().setAll(Ui.banner(problem, "error"));
                return;
            }
            save.setDisable(true);
            Ui.async(() -> {
                ApiClient.resetPassword(email, code, p1.getText());
                return true;
            }, ok -> showSignIn(email, "Your password was changed. Sign in with the new password.", "ok"), err -> {
                save.setDisable(false);
                feedback.getChildren().setAll(Ui.banner(err, "error"));
            });
        });
        formHost.getChildren().setAll(form(
                backLink(),
                Ui.label("Choose a new password", "h1"),
                Ui.wrap("For " + email, "muted"),
                gap(6),
                field("New password", p1),
                field("Confirm new password", p2),
                rules,
                feedback,
                save));
        javafx.application.Platform.runLater(p1::requestFocus);
    }

    /** Same rule as the server: 8–128 characters with at least one letter and one digit. */
    public static String passwordProblem(String p1, String p2) {
        if (p1 == null || p1.length() < 8) return "Use at least 8 characters.";
        if (p1.length() > 128) return "Use at most 128 characters.";
        if (!p1.matches(".*[A-Za-z].*") || !p1.matches(".*\\d.*")) return "Use both letters and numbers.";
        if (!p1.equals(p2)) return "The two passwords do not match.";
        return null;
    }

    // ---- helpers ---------------------------------------------------------------------------------------------

    private Node backLink() {
        Hyperlink back = Ui.link("Back to sign in", () -> showSignIn(null, null, "info"));
        back.setGraphic(Ui.icon("fas-arrow-left", 12, "#2D6A4F"));
        return back;
    }

    private static VBox form(Node... children) {
        VBox v = new VBox(14, children);
        v.setMaxWidth(430);
        v.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(v, Pos.CENTER);
        return v;
    }

    private static VBox field(String label, Node input) {
        return new VBox(6, Ui.label(label, "field-label"), input);
    }

    private static Region gap(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    static boolean isNetworkError(ApiException e) {
        return e.getStatusCode() < 0;
    }
}

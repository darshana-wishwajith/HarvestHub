package com.harvesthub.mobile.ui.common;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.harvesthub.mobile.MainActivity;
import com.harvesthub.mobile.model.LoginRequest;
import com.harvesthub.mobile.model.LoginResponse;
import com.harvesthub.mobile.model.User;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Where each role lives in the app, and the shared "sign in, save the session, open home" step. */
public final class RoleHome {

    private RoleHome() {
    }

    /** Home screen per role; null for roles the mobile app does not serve (officer / admin). */
    @Nullable
    public static Class<?> homeFor(String role) {
        if ("FARMER".equalsIgnoreCase(role)) return MainActivity.class;
        if ("BUYER".equalsIgnoreCase(role)) return com.harvesthub.mobile.ui.buyer.BuyerDashboardActivity.class;
        if ("TRANSPORTER".equalsIgnoreCase(role)) return com.harvesthub.mobile.ui.transporter.DeliveryDashboardActivity.class;
        return null;
    }

    public static Class<?> loginFor(String role) {
        if ("BUYER".equalsIgnoreCase(role)) return com.harvesthub.mobile.ui.buyer.BuyerLoginActivity.class;
        if ("TRANSPORTER".equalsIgnoreCase(role)) return com.harvesthub.mobile.ui.transporter.TransporterLoginActivity.class;
        return com.harvesthub.mobile.ui.farmer.FarmerLoginActivity.class;
    }

    /** Saves the signed-in user and opens their home screen, clearing the back stack. */
    public static void openHome(Activity activity, LoginResponse body) {
        User user = body.getUser();
        String role = user.getRole().toUpperCase(Locale.US);
        SessionManager session = new SessionManager(activity);
        session.saveAuthToken(body.getToken());
        session.saveUser(user);
        session.setLogin(true, role);
        session.setStaySignedIn(true);
        open(activity, homeFor(role));
    }

    /**
     * Right after registration: signs the new account in and opens its home screen.
     * {@code afterSignIn} (optional) runs first with the new session, then must call the given Runnable to go home
     * (used to upload the transporter's licence photo). If sign-in fails the account still exists, so the user
     * is sent to that role's login screen.
     */
    public static void signInAfterRegistration(Activity activity, String email, String password, String role,
                                               @Nullable AfterSignIn afterSignIn) {
        ApiClient.getApiService().login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> r) {
                LoginResponse body = r.body();
                if (!r.isSuccessful() || body == null || body.getUser() == null || homeFor(body.getUser().getRole()) == null) {
                    goToLogin(activity, role);
                    return;
                }
                if (afterSignIn == null) {
                    Toast.makeText(activity, "Welcome to HarvestHub!", Toast.LENGTH_SHORT).show();
                    openHome(activity, body);
                    return;
                }
                // Save the session first so the extra step can call authenticated endpoints.
                SessionManager session = new SessionManager(activity);
                session.saveAuthToken(body.getToken());
                session.saveUser(body.getUser());
                session.setLogin(true, body.getUser().getRole().toUpperCase(Locale.US));
                session.setStaySignedIn(true);
                afterSignIn.run(() -> openHome(activity, body));
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                goToLogin(activity, role);
            }
        });
    }

    public interface AfterSignIn {
        void run(Runnable goHome);
    }

    private static void goToLogin(Activity activity, String role) {
        Toast.makeText(activity, "Account created. Please sign in.", Toast.LENGTH_LONG).show();
        open(activity, loginFor(role));
    }

    private static void open(Activity activity, Class<?> target) {
        Intent i = new Intent(activity, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(i);
        activity.finish();
    }
}

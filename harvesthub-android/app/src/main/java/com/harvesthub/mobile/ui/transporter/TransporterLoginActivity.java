package com.harvesthub.mobile.ui.transporter;

import android.view.View;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.ui.buyer.SignInBuyerActivity;
import com.harvesthub.mobile.utils.AuthInputUtils;

/**
 * Transporter login: the same screen and behaviour as the buyer sign-in,
 * with transporter wording; signs in to the delivery dashboard. Registration opens the shared Create Account
 * form with TARGET_ROLE=TRANSPORTER; forgot password uses the shared reset flow.
 */
public class TransporterLoginActivity extends SignInBuyerActivity {

    @Override
    protected String getExpectedRole() {
        return "TRANSPORTER";
    }

    @Override
    protected Class<?> homeScreen() {
        return DeliveryDashboardActivity.class;
    }

    @Override
    protected void applyRoleTexts() {
        setText(R.id.tvTagline, "Empowering Local Harvest Logistics");
        setText(R.id.tvWelcome, "Welcome Transporter");
        setText(R.id.tvWelcomeSubtitle, "Log in or register your agricultural transport\nvehicle to access fresh daily delivery manifests.");
        setText(R.id.btnSignIn, "Sign In as Transporter");
        setText(R.id.tvRegisterBuyer, "Register as Transporter");
        View footer = findViewById(R.id.layoutFooterCard);
        if (footer != null) footer.setVisibility(View.GONE);
    }

    /** Kept for the existing unit test: same rule as {@link AuthInputUtils#normaliseMobile(String)}. */
    static String normaliseMobile(String raw) {
        return AuthInputUtils.normaliseMobile(raw);
    }
}

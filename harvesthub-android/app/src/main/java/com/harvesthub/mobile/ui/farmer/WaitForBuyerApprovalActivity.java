package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.google.android.material.button.MaterialButton;

public class WaitForBuyerApprovalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_for_buyer_approval);

        TextView tvProTip = findViewById(R.id.tvProTipContent);
        String proTipText = getString(R.string.pro_tip_content);
        SpannableString spannableString = new SpannableString(proTipText);
        
        int start = proTipText.indexOf("Do not harvest");
        if (start != -1) {
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), start, start + 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvProTip.setText(spannableString);

        MaterialButton btnMessage = findViewById(R.id.btnMessage);
        MaterialButton btnViewPost = findViewById(R.id.btnViewPost);

        btnMessage.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            startActivity(intent);
        });

        btnViewPost.setOnClickListener(v -> {
            Toast.makeText(this, "Opening original listing...", Toast.LENGTH_SHORT).show();
        });
    }
}
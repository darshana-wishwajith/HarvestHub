package com.harvesthub.mobile;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    // REPLACE THIS WITH YOUR PC's IP ADDRESS FOUND IN IPCONFIG
    private static final String BASE_URL = "http://10.30.5.209:8080/";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnLoad = findViewById(R.id.btnLoad);
        TextView txtResult = findViewById(R.id.txtResult);

        // 1. Setup Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        // 2. Button Click Listener
        btnLoad.setOnClickListener(v -> {
            txtResult.setText("Loading...");

            // 3. Make API Call
            apiService.getAllUsers().enqueue(new Callback<List<User>>() {
                @Override
                public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        StringBuilder sb = new StringBuilder();
                        for (User u : response.body()) {
                            sb.append("• ").append(u.getName())
                                    .append(" (").append(u.getRole()).append(")\n");
                        }
                        txtResult.setText(sb.toString());
                    } else {
                        txtResult.setText("Error: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<User>> call, Throwable t) {
                    txtResult.setText("Failed: " + t.getMessage());
                }
            });
        });
    }
}
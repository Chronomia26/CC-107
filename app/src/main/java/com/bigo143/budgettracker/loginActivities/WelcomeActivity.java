package com.bigo143.budgettracker.loginActivities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.bigo143.budgettracker.MainActivity;
import com.bigo143.budgettracker.R;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1️⃣ Check if a user is already logged in
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String savedUser = prefs.getString("logged_in_user", null);

        if (savedUser != null) {
            // User is already logged in → go to MainActivity / Dashboard
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // close WelcomeActivity
            return; // prevent executing the rest
        }

        // 2️⃣ If no saved user, show welcome screen
        setContentView(R.layout.activity_welcome);

        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnCreateAccount.setOnClickListener(v -> {
            Intent i = new Intent(WelcomeActivity.this, SignupActivity.class);
            finish();
            startActivity(i);
        });

        btnLogin.setOnClickListener(v -> {
            Intent i = new Intent(WelcomeActivity.this, LoginActivity.class);
            finish();
            startActivity(i);
        });
    }
}


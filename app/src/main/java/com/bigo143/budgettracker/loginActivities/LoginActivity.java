package com.bigo143.budgettracker.loginActivities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bigo143.budgettracker.DatabaseHelper;
import com.bigo143.budgettracker.MainActivity;
//import com.bigo143.budgettracker.MgaMatatanggalNaFiles.BudgetActivity;
import com.bigo143.budgettracker.R;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        DatabaseHelper dbHelper = new DatabaseHelper(this);

        // Match these IDs with your XML file
        Button btnLogin = findViewById(R.id.btnLogin);
        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);
        TextView tvSignupRedirect = findViewById(R.id.tvSignupRedirect);

        // Handle login
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter your credentials", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean valid = dbHelper.loginUser(username, password);
            if (valid) {
                // Save the logged-in user in SharedPreferences
                getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("logged_in_user", username)
                        .apply();
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish(); // close login so user can’t go back
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            }
        });

        // Redirect to signup
        tvSignupRedirect.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }
}

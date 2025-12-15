package com.bigo143.budgettracker;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bigo143.budgettracker.calcu_add;
import com.bigo143.budgettracker.fragments.AccountFragment;
import com.bigo143.budgettracker.fragments.BudgetFragment;
import com.bigo143.budgettracker.fragments.CategoriesFragment;
import com.bigo143.budgettracker.fragments.ChartsFragment;
import com.bigo143.budgettracker.fragments.ExpenseFragment;
import com.bigo143.budgettracker.fragments.IncomeFragment;
import com.bigo143.budgettracker.fragments.RecordsFragment;
import com.bigo143.budgettracker.loginActivities.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private RecordsFragment recordsFragment;
    private AccountFragment accountFragment;
    private IncomeFragment incomeFragment;
    private ExpenseFragment expenseFragment;

    private ChartsFragment chartsFragment;
    private BudgetFragment budgetFragment;
    private CategoriesFragment categoriesFragment;

    public static OnTransactionSavedListener staticListener;

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fab;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private ActivityResultLauncher<Intent> transactionLauncher;

    // ✅ Theme preferences
    private SharedPreferences themePrefs;
    private static final String THEME_PREFS = "ThemePrefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ✅ Load saved theme before calling super.onCreate()
        themePrefs = getSharedPreferences(THEME_PREFS, MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fab = findViewById(R.id.fab);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        // Status bar color
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // ✅ Setup navigation drawer item click listener
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                handleNavigationItemSelected(item);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        recordsFragment = new RecordsFragment();
        accountFragment = new AccountFragment();
        incomeFragment = new IncomeFragment();
        expenseFragment = new ExpenseFragment();
        categoriesFragment = new CategoriesFragment();

        chartsFragment = new ChartsFragment();
        budgetFragment = new BudgetFragment();

        // Default fragment
        if (savedInstanceState == null) {
            replaceFragment(recordsFragment); // use the persistent instance
        }

        setupBottomNavigationView();

        // Launcher for calcu_add
        transactionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        refreshAllData();
                    }
                }
        );

        fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, calcu_add.class);
            transactionLauncher.launch(intent);
        });
    }

    // ✅ Handle navigation drawer menu items
    private void handleNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle main navigation items
        if (id == R.id.nav_home) {
            replaceFragment(recordsFragment);
            bottomNavigationView.setSelectedItemId(R.id.records);
        } else if (id == R.id.nav_charts) {
            replaceFragment(chartsFragment);
            bottomNavigationView.setSelectedItemId(R.id.charts);
        } else if (id == R.id.nav_budget) {
            replaceFragment(budgetFragment);
            bottomNavigationView.setSelectedItemId(R.id.budget);
        } else if (id == R.id.nav_categories) {
            replaceFragment(categoriesFragment);
            bottomNavigationView.setSelectedItemId(R.id.categories);
        }
        // Handle preferences items
        else if (id == R.id.nav_theme_toggle) {
            toggleTheme();
        } else if (id == R.id.nav_share) {
            shareApp();
        } else if (id == R.id.nav_about) {
            showAboutDialog();
        } else if (id == R.id.nav_logout) {
            showLogoutDialog();
        }
    }

    // ✅ Toggle Theme
    private void toggleTheme() {
        boolean isDarkMode = themePrefs.getBoolean(KEY_DARK_MODE, false);
        boolean newMode = !isDarkMode;

        // Save preference
        themePrefs.edit().putBoolean(KEY_DARK_MODE, newMode).apply();

        // Apply theme
        AppCompatDelegate.setDefaultNightMode(
                newMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        // Recreate activity to apply theme
        recreate();
    }

    // ✅ Share App
    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareMessage = "Check out Budget Tracker App!\n\n" +
                "Download it here: [Your app link or play store link]";
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    // ✅ About Dialog
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About Budget Tracker")
                .setMessage("Budget Tracker v1.0\n\n" +
                        "A simple and efficient way to manage your finances.\n\n" +
                        "Features:\n" +
                        "• Track income and expenses\n" +
                        "• Set budgets for categories\n" +
                        "• View spending charts\n" +
                        "• Manage multiple local accounts\n\n" +
                        "Developed by: Amihan, Austero, Carnain, Morales\n" +
                        "© 2025 All rights reserved")
                .setPositiveButton("OK", null)
                .setNeutralButton("Rate App", (dialog, which) -> {
                    openPlayStore();
                })
                .show();
    }

    // ✅ Logout Dialog
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ✅ Perform Logout
    private void performLogout() {
        // Clear user session
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        prefs.edit()
                .remove("logged_in_user")
                .putBoolean("is_logged_in", false)
                .apply();

        // Navigate to login screen
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    // ✅ Open Play Store
    private void openPlayStore() {
        try {
            String packageName = getPackageName();
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setBackground(null);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selected = getSelectedFragment(item.getItemId());
            if (selected != null) replaceFragment(selected);
            return true;
        });
    }

    private Fragment getSelectedFragment(int id) {
        if (id == R.id.records) return recordsFragment;
        if (id == R.id.charts) return chartsFragment;
        if (id == R.id.budget) return budgetFragment;
        if (id == R.id.categories) return categoriesFragment;
        return null;
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
    }

    private void showBottomDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        LinearLayout addRecord = dialog.findViewById(R.id.addRecord);

        addRecord.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, calcu_add.class);
            transactionLauncher.launch(intent);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }
    }

    private void refreshAllData() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
        if(currentFragment == null || !currentFragment.isAdded()) return;

        if (currentFragment instanceof AccountFragment)
            ((AccountFragment) currentFragment).reloadData();
        else if (currentFragment instanceof IncomeFragment)
            ((IncomeFragment) currentFragment).reloadData();
        else if (currentFragment instanceof ExpenseFragment)
            ((ExpenseFragment) currentFragment).reloadData();
        else if (currentFragment instanceof CategoriesFragment)
            ((CategoriesFragment) currentFragment).reloadData();
        else if (currentFragment instanceof RecordsFragment)
            ((RecordsFragment) currentFragment).reloadData();
        else if (currentFragment instanceof BudgetFragment)
            ((BudgetFragment) currentFragment).reloadData();
        else if (currentFragment instanceof ChartsFragment)
            ((ChartsFragment) currentFragment).reloadData();
    }

    // ✅ Handle back button to close drawer if open

}
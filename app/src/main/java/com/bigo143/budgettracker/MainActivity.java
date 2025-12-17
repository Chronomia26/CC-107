package com.bigo143.budgettracker;

import android.accounts.Account;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import com.bigo143.budgettracker.models.CategoryModel;
import com.bigo143.budgettracker.models.Record;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private long backPressedTime;
    private Toast backToast;

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
            exportRecords();

        } else if (id == R.id.nav_charts) {
            backupAndRestore();

        } else if (id == R.id.nav_budget) {
            deleteAndReset();

        }
        // Handle preferences items
        if (id == R.id.nav_theme_toggle) {
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
    private void exportRecords() {
        // Database helper
        DatabaseHelper db = new DatabaseHelper(this);
        SharedPreferences prefs = this.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String currentUser = prefs.getString("logged_in_user", null);
        List<Record> records = db.getAllTransactions(currentUser); // Returns List<Record>

        if (records == null || records.isEmpty()) {
            Toast.makeText(this, "No records to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Category,Account,Amount,Type,Date,Note\n");

        for (Record r : records) {
            String typeStr;
            switch (r.getType()) {
                case Record.TYPE_INCOME: typeStr = "Income"; break;
                case Record.TYPE_EXPENSE: typeStr = "Expense"; break;
                case Record.TYPE_TRANSFER_IN: typeStr = "Transfer In"; break;
                case Record.TYPE_TRANSFER_OUT: typeStr = "Transfer Out"; break;
                default: typeStr = "Unknown"; break;
            }

            csv.append(r.getId()).append(",")
                    .append(r.getCategory()).append(",")
                    .append(r.getAccount()).append(",")
                    .append(r.getAmount()).append(",")
                    .append(typeStr).append(",")
                    .append(r.getDate()).append(",")
                    .append(r.getNote()).append("\n");
        }

        try {
            // Get Downloads folder
            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsFolder.exists()) {
                downloadsFolder.mkdirs();
            }

            File file = new File(downloadsFolder, "records.csv");
            FileWriter writer = new FileWriter(file);
            writer.write(csv.toString());
            writer.close();

            Toast.makeText(this, "Records exported to Downloads", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting records", Toast.LENGTH_SHORT).show();
        }
    }



    private void backupAndRestore() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Backup & Restore")
                .setMessage("Choose an action")
                .setPositiveButton("Backup", (dialog, which) -> performBackup())
                .setNegativeButton("Restore", (dialog, which) -> performRestore())
                .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Optional: customize button colors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.primary));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.secondary));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setTextColor(ContextCompat.getColor(this, R.color.black));
    }

    private void performBackup() {
        DatabaseHelper db = new DatabaseHelper(this);
        BackupManager backupManager = new BackupManager(db);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String currentUser = prefs.getString("logged_in_user", null);

        if (currentUser == null) {
            Toast.makeText(this, "No logged-in user", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Export all data (accounts, categories, budgets, records)
        Map<String, Object> allData = backupManager.exportAllData(currentUser);

        if (allData.isEmpty()) {
            Toast.makeText(this, "No data to backup", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ IMPROVED: Add timestamp to filename
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                .format(new java.util.Date());
        String fileName = "backup_" + currentUser + "_" + timestamp + ".json";

        // Save complete backup as JSON
        try {
            JSONObject jsonObject = new JSONObject(allData);

            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsFolder.exists()) downloadsFolder.mkdirs();

            File file = new File(downloadsFolder, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(jsonObject.toString(4));
            writer.close();

            Toast.makeText(this, "✅ Backup saved!\n" + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Backup failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void performRestore() {
        DatabaseHelper db = new DatabaseHelper(this);
        BackupManager backupManager = new BackupManager(db);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String currentUser = prefs.getString("logged_in_user", null);

        if (currentUser == null) {
            Toast.makeText(this, "No logged-in user", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ IMPROVED: Show list of available backups
        File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] allFiles = downloadsFolder.listFiles();

        if (allFiles == null || allFiles.length == 0) {
            Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find all backup files for current user
        final List<File> backupFiles = new ArrayList<>();
        for (File f : allFiles) {
            if (f.getName().startsWith("backup_" + currentUser + "_") && f.getName().endsWith(".json")) {
                backupFiles.add(f);
            }
        }

        if (backupFiles.isEmpty()) {
            Toast.makeText(this, "No backup files found for user: " + currentUser, Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ IMPROVED: Sort by date (newest first)
        java.util.Collections.sort(backupFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        // ✅ IMPROVED: Show selection dialog
        String[] fileNames = new String[backupFiles.size()];
        for (int i = 0; i < backupFiles.size(); i++) {
            String name = backupFiles.get(i).getName();
            // Extract timestamp from filename
            String displayName = name;
            try {
                // Convert filename like "backup_user_20241217_093045.json" to readable format
                String[] parts = name.replace("backup_", "").replace(".json", "").split("_");
                if (parts.length >= 3) {
                    String date = parts[1]; // 20241217
                    String time = parts[2]; // 093045
                    String formattedDate = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
                    String formattedTime = time.substring(0, 2) + ":" + time.substring(2, 4) + ":" + time.substring(4, 6);
                    displayName = formattedDate + " " + formattedTime;
                }
            } catch (Exception e) {
                // If parsing fails, use original name
            }
            fileNames[i] = displayName;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Backup to Restore")
                .setItems(fileNames, (dialog, which) -> {
                    File selectedFile = backupFiles.get(which);
                    restoreFromFile(selectedFile, backupManager, currentUser);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ✅ ADDED: Separate method to restore from selected file
    private void restoreFromFile(File file, BackupManager backupManager, String currentUser) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject jsonObject = new JSONObject(sb.toString());

            // Convert JSONObject to Map
            Map<String, Object> backupData = new HashMap<>();
            backupData.put("username", jsonObject.getString("username"));
            backupData.put("backup_version", jsonObject.getString("backup_version"));

            // Convert accounts
            JSONArray accountsArray = jsonObject.getJSONArray("accounts");
            List<Map<String, Object>> accounts = new ArrayList<>();
            for (int i = 0; i < accountsArray.length(); i++) {
                JSONObject obj = accountsArray.getJSONObject(i);
                Map<String, Object> map = new HashMap<>();
                map.put("name", obj.getString("name"));
                map.put("icon", obj.getInt("icon"));
                accounts.add(map);
            }
            backupData.put("accounts", accounts);

            // Convert categories
            JSONArray categoriesArray = jsonObject.getJSONArray("categories");
            List<Map<String, Object>> categories = new ArrayList<>();
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject obj = categoriesArray.getJSONObject(i);
                Map<String, Object> map = new HashMap<>();
                map.put("name", obj.getString("name"));
                map.put("type", obj.getString("type"));
                map.put("icon", obj.getInt("icon"));
                categories.add(map);
            }
            backupData.put("categories", categories);

            // Convert budgets
            if (jsonObject.has("budgets")) {
                JSONArray budgetsArray = jsonObject.getJSONArray("budgets");
                List<Map<String, Object>> budgets = new ArrayList<>();
                for (int i = 0; i < budgetsArray.length(); i++) {
                    JSONObject obj = budgetsArray.getJSONObject(i);
                    Map<String, Object> map = new HashMap<>();
                    map.put("category_name", obj.getString("category_name"));
                    map.put("category_type", obj.getString("category_type"));
                    map.put("amount", obj.getDouble("amount"));
                    budgets.add(map);
                }
                backupData.put("budgets", budgets);
            }

            // Convert records
            JSONArray recordsArray = jsonObject.getJSONArray("records");
            List<Map<String, Object>> records = new ArrayList<>();
            for (int i = 0; i < recordsArray.length(); i++) {
                JSONObject obj = recordsArray.getJSONObject(i);
                Map<String, Object> map = new HashMap<>();
                map.put("category", obj.getString("category"));
                map.put("account", obj.getString("account"));
                map.put("type", obj.getString("type"));
                map.put("amount", obj.getDouble("amount"));
                map.put("date", obj.getString("date"));
                map.put("note", obj.optString("note", ""));
                records.add(map);
            }
            backupData.put("records", records);

            // Import all data
            boolean success = backupManager.importAllData(currentUser, backupData);

            if (success) {
                int budgetCount = jsonObject.has("budgets") ? jsonObject.getJSONArray("budgets").length() : 0;
                Toast.makeText(this, "✅ Restore completed successfully!\n" +
                        accounts.size() + " accounts, " +
                        categories.size() + " categories, " +
                        budgetCount + " budgets, " +
                        records.size() + " records restored", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "⚠️ Restore completed with errors. Check Logcat for details.", Toast.LENGTH_LONG).show();
            }

            refreshAllData();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }







    private void deleteAndReset() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String currentUser = prefs.getString("logged_in_user", null);

        new AlertDialog.Builder(this)
                .setTitle("Delete & Reset")
                .setMessage("Are you sure you want to delete ALL data for user " + currentUser + "? This cannot be undone unless you have a backup!")
                .setPositiveButton("Yes, Delete Everything", (dialog, which) -> {
                    DatabaseHelper db = new DatabaseHelper(this);
                    android.database.sqlite.SQLiteDatabase database = db.getWritableDatabase();

                    // Delete only the current user's data
                    database.delete("records", "username = ?", new String[]{currentUser});
                    database.delete("budgets", "username = ?", new String[]{currentUser});
                    database.delete("categories", "username = ?", new String[]{currentUser});

                    Toast.makeText(this, "All data deleted for " + currentUser, Toast.LENGTH_LONG).show();
                    refreshAllData();
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    @Override
    //this works, palitan pag nakahanap ng better
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                if (backToast != null) {
                    backToast.cancel();
                }
                super.onBackPressed();
                return;
            }
            backToast = Toast.makeText(this, "Back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
            backPressedTime = System.currentTimeMillis();
        }
    }
}

package com.bigo143.budgettracker;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bigo143.budgettracker.fragments.BudgetFragment;
import com.bigo143.budgettracker.fragments.CategoriesFragment;
import com.bigo143.budgettracker.fragments.ChartsFragment;
import com.bigo143.budgettracker.fragments.RecordsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

//MainActivity is for fragments, navigation bar
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fab;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fab = findViewById(R.id.fab); // Correctly assign to the class field
        drawerLayout = findViewById(R.id.drawer_layout); // Correctly assign
        navigationView = findViewById(R.id.nav_view); // Correctly assign
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.second));

        // Set up navigation drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Load default fragment on first launch
        if (savedInstanceState == null) {
            replaceFragment(new RecordsFragment()); // Default fragment
//            navigationView.setCheckedItem(R.id.nav_home);
        }

        // Set up bottom navigation view
        setupBottomNavigationView();

        // Floating Action Button click listener to show bottom sheet
        fab.setOnClickListener(view -> showBottomDialog());
    }

    // Set up bottom navigation behavior
    private void setupBottomNavigationView() {
        bottomNavigationView.setBackground(null); // Optional styling

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = getSelectedFragment(item.getItemId());
            if (selectedFragment != null) {
                replaceFragment(selectedFragment);
            }
            return true;
        });
    }

    // --- FIX 2: Enable the BudgetFragment logic ---
    private Fragment getSelectedFragment(int itemId) {
        if (itemId == R.id.records) {
            return new RecordsFragment();
        } else if (itemId == R.id.charts) {
            return new ChartsFragment();
        } else if (itemId == R.id.budget) {
            return new BudgetFragment();
        } else if (itemId == R.id.categories) {
            return new CategoriesFragment();
        }
        else {
            return null;
        }
    }

    // Replace current fragment with new one
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frame_layout, fragment);
        transaction.commit();
    }

    // Display bottom sheet dialog for upload options
    private void showBottomDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        LinearLayout videoLayout = dialog.findViewById(R.id.addRecord);
        LinearLayout shortsLayout = dialog.findViewById(R.id.layoutShorts);
        LinearLayout liveLayout = dialog.findViewById(R.id.layoutLive);

        videoLayout.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "Setting Values", Toast.LENGTH_SHORT).show();
            // Note: Make sure calcu_add.class exists
            startActivity(new Intent(MainActivity.this, calcu_add.class));
        });

        shortsLayout.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "Editting Budget", Toast.LENGTH_SHORT).show();
        });

        liveLayout.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(MainActivity.this, "Adding Record", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.BOTTOM);
        }
    }
}

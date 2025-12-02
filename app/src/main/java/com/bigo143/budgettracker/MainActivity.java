package com.bigo143.budgettracker;
import android.app.Dialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fab = findViewById(R.id.fab);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        // Status bar color
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.second));

        // Drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        recordsFragment = new RecordsFragment();
        accountFragment = new AccountFragment(new ArrayList<>());
        incomeFragment = new IncomeFragment(new ArrayList<>());
        expenseFragment = new ExpenseFragment(new ArrayList<>());
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






}

package com.bigo143.budgettracker.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bigo143.budgettracker.BudgetedAdapter;
import com.bigo143.budgettracker.DatabaseHelper;
import com.bigo143.budgettracker.NotBudgetedAdapter;
import com.bigo143.budgettracker.R;
import com.bigo143.budgettracker.databinding.FragmentBudgetBinding;
import com.bigo143.budgettracker.models.CategoryModel;

import java.util.ArrayList;

public class BudgetFragment extends Fragment {

    private FragmentBudgetBinding binding;
    private BudgetedAdapter budgetedAdapter;
    private NotBudgetedAdapter notBudgetedAdapter;

    private ArrayList<CategoryModel> budgetedList = new ArrayList<>();
    private ArrayList<CategoryModel> notBudgetedList = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private String currentUser ; // TODO: replace with actual logged-in username

    public BudgetFragment() {
        // Required empty public constructor
        setHasOptionsMenu(true); // enables toolbar menu
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUser = prefs.getString("logged_in_user", null);

        binding = FragmentBudgetBinding.inflate(inflater, container, false);
        dbHelper = new DatabaseHelper(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadBudgetedData();
        loadNotBudgetedData();
        setupRecyclerViews();
    }

    // --------------------------
    // MENU (Calendar / Filter / Search)
    // --------------------------
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_normal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_search) {
            // open search UI
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadBudgetedData() {
        budgetedList.clear();
        Cursor cursor = dbHelper.getBudgetedCategories(currentUser);

        if (cursor != null && cursor.moveToFirst()) {
            do {

                // 🔥 FILTER ONLY EXPENSE CATEGORIES
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                if (!type.equals("expense")) continue;

                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                double limit = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                double spent = dbHelper.getTotalExpenseForCategory(currentUser, name);
                int icon = getIconForCategory(name);

                budgetedList.add(new CategoryModel(name, limit, spent, icon));

            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private void loadNotBudgetedData() {
        notBudgetedList.clear();
        Cursor cursor = dbHelper.getUnbudgetedCategories(currentUser);

        if (cursor != null && cursor.moveToFirst()) {
            do {

                // 🔥 FILTER ONLY EXPENSE CATEGORIES
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                if (!type.equals("expense")) continue;

                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int icon = getIconForCategory(name);

                notBudgetedList.add(new CategoryModel(name, 0, 0, icon));

            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private void setupRecyclerViews() {
        // Budgeted List
        budgetedAdapter = new BudgetedAdapter(budgetedList, requireContext());
        binding.rvBudgeted.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBudgeted.setAdapter(budgetedAdapter);

        // Not Budgeted List with OnBudgetSetListener
        notBudgetedAdapter = new NotBudgetedAdapter(notBudgetedList, requireContext(),
                new NotBudgetedAdapter.OnBudgetSetListener() {
                    @Override
                    public void onBudgetSet(String categoryName, double amount) {
                        // Insert budget for this category in the database
                        boolean success = dbHelper.insertBudget(currentUser,
                                dbHelper.getCategoryIdByName(currentUser, categoryName, "expense"),
                                amount);

                        if (success) {
                            // Reload data
                            loadBudgetedData();
                            loadNotBudgetedData();

                            // Notify adapters
                            budgetedAdapter.notifyDataSetChanged();
                            notBudgetedAdapter.notifyDataSetChanged();
                        }
                    }
                });
        binding.rvNotBudgeted.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotBudgeted.setAdapter(notBudgetedAdapter);
    }


    private int getIconForCategory(String name) {
        switch (name.toLowerCase()) {
            case "food":
                return R.drawable.ic_food;
            case "transport":
                return R.drawable.ic_transport;
            case "bills":
                return R.drawable.ic_bills;
            case "shopping":
                return R.drawable.ic_shopping;
            case "snacks":
                return R.drawable.ic_snacks;
        }
        return 0;
    }
}

package com.bigo143.budgettracker.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bigo143.budgettracker.BudgetedAdapter;
import com.bigo143.budgettracker.DatabaseHelper;
import com.bigo143.budgettracker.NotBudgetedAdapter;
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
    private String currentUser;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        dbHelper = new DatabaseHelper(context);
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUser = prefs.getString("logged_in_user", null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBudgetBinding.inflate(inflater, container, false);
        setupRecyclerViews();
        reloadData(); // Load initially
        return binding.getRoot();
    }

    private void setupRecyclerViews() {
        // Budgeted List
        budgetedAdapter = new BudgetedAdapter(budgetedList, requireContext());
        binding.rvBudgeted.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBudgeted.setAdapter(budgetedAdapter);

        // Not Budgeted List
        notBudgetedAdapter = new NotBudgetedAdapter(notBudgetedList, requireContext(), new NotBudgetedAdapter.OnBudgetSetListener() {
            @Override
            public void onBudgetSet(String categoryName, double amount) {
                boolean success = dbHelper.insertBudget(currentUser,
                        dbHelper.getCategoryIdByName(currentUser, categoryName, "expense"),
                        amount);
                if(success) reloadData();
            }
        });
        binding.rvNotBudgeted.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotBudgeted.setAdapter(notBudgetedAdapter);
    }

    // 🔹 Dynamic reload method
    public void reloadData() {
        loadBudgetedData();
        loadNotBudgetedData();

        if(budgetedAdapter != null) budgetedAdapter.updateData(budgetedList);
        if(notBudgetedAdapter != null) notBudgetedAdapter.updateData(notBudgetedList);

        updateBudgetSummary();
    }

    private void loadBudgetedData() {
        budgetedList.clear();
        Cursor cursor = dbHelper.getBudgetedCategories(currentUser);

        if(cursor != null && cursor.moveToFirst()) {
            do {
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                if(!type.equals("expense")) continue;

                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                double limit = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
                double spent = dbHelper.getTotalExpenseForCategory(currentUser, name);
                int icon = cursor.getInt(cursor.getColumnIndexOrThrow("icon"));

                budgetedList.add(new CategoryModel(name, limit, spent, icon));
            } while(cursor.moveToNext());
            cursor.close();
        }
    }

    private void loadNotBudgetedData() {
        notBudgetedList.clear();
        Cursor cursor = dbHelper.getUnbudgetedCategories(currentUser);

        if(cursor != null && cursor.moveToFirst()) {
            do {
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                if(!type.equals("expense")) continue;

                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int icon = cursor.getInt(cursor.getColumnIndexOrThrow("icon"));

                notBudgetedList.add(new CategoryModel(name, 0, 0, icon));
            } while(cursor.moveToNext());
            cursor.close();
        }
    }

    private void updateBudgetSummary() {
        double totalBudget = 0;
        double totalSpent = 0;

        for(CategoryModel c : budgetedList) {
            totalBudget += c.getLimit();
            totalSpent += c.getSpent();
        }

        binding.editTotalBudget.setText("₱ " + String.format("%.2f", totalBudget));
        binding.editTotalSpent.setText("₱ " + String.format("%.2f", totalSpent));
    }
}

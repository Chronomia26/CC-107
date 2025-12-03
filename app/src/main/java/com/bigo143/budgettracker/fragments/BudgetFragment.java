package com.bigo143.budgettracker.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

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

public class BudgetFragment extends Fragment implements BudgetedAdapter.OnBudgetActionListener {

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
        // Budgeted List - ✅ Pass 'this' as the listener
        budgetedAdapter = new BudgetedAdapter(budgetedList, requireContext(), this);
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

    // ✅ Implement OnBudgetActionListener - Reset Budget
    @Override
    public void onResetBudget(CategoryModel category, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Reset Budget")
                .setMessage("Are you sure you want to reset the budget for " + category.getName() + "?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    int categoryId = dbHelper.getCategoryIdByName(currentUser, category.getName(), "expense");
                    boolean success = dbHelper.deleteBudget(currentUser, categoryId);

                    if (success) {
                        Toast.makeText(getContext(), "Budget reset successfully", Toast.LENGTH_SHORT).show();
                        reloadData();
                    } else {
                        Toast.makeText(getContext(), "Failed to reset budget", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ✅ Implement OnBudgetActionListener - Edit Budget
    @Override
    public void onEditBudget(CategoryModel category, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Budget for " + category.getName());

        // Create EditText for input
        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter new budget amount");
        input.setText(String.valueOf(category.getLimit()));

        // Add padding to EditText
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        builder.setView(input);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String amountStr = input.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                try {
                    double newAmount = Double.parseDouble(amountStr);
                    if (newAmount <= 0) {
                        Toast.makeText(getContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int categoryId = dbHelper.getCategoryIdByName(currentUser, category.getName(), "expense");
                    boolean success = dbHelper.updateBudget(currentUser, categoryId, newAmount);

                    if (success) {
                        Toast.makeText(getContext(), "Budget updated successfully", Toast.LENGTH_SHORT).show();
                        reloadData();
                    } else {
                        Toast.makeText(getContext(), "Failed to update budget", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid amount entered", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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
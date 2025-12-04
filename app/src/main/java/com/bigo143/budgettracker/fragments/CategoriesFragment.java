package com.bigo143.budgettracker.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.accounts.Account;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bigo143.budgettracker.DatabaseHelper;
import com.bigo143.budgettracker.R;
import com.bigo143.budgettracker.adapters.IconAdapter;
import com.bigo143.budgettracker.models.CategoryModel;

import java.util.ArrayList;
import java.util.List;

public class CategoriesFragment extends Fragment implements OnCategoriesUpdatedListener {

    private Button btnIncome, btnAccount, btnExpense, btnAdd;
    private DatabaseHelper dbHelper;
    //private String currentUser = "userOne";

    // Replace with actual user logic
    private String currentType = "account";

    private String currentUser ;

    private IncomeFragment incomeFragment;
    private AccountFragment accountFragment;
    private ExpenseFragment expenseFragment;
    private TextView tvIncomeValue, tvExpenseValue, tvAllAccounts;



    private List<Account> accounts = new ArrayList<>();

    private int selectedIconResource = R.drawable.ic_default;

    private int[] availableIcons = new int[]{
            R.drawable.ic_salary,
            R.drawable.ic_income,
            R.drawable.ic_wallet,
            R.drawable.ic_bank,
            R.drawable.ic_food,
            R.drawable.ic_transport,
            R.drawable.ic_expense,
            R.drawable.ic_default
    };

    public CategoriesFragment() {}
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        dbHelper = new DatabaseHelper(context);

        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        currentUser = prefs.getString("logged_in_user", null);
        if (currentUser == null) {
            throw new IllegalStateException("No logged in user found in SharedPreferences");
        }

    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_categories, container, false);
        FragmentUpdateListenerHolder.listener = this;

        btnIncome = v.findViewById(R.id.btnIncome);
        btnAccount = v.findViewById(R.id.btnAccount);
        btnExpense = v.findViewById(R.id.btnExpense);
        btnAdd = v.findViewById(R.id.btnOpenCategoryAdd);

        tvIncomeValue = v.findViewById(R.id.tvIncomeValue);
        tvExpenseValue = v.findViewById(R.id.tvExpenseValue);
        tvAllAccounts = v.findViewById(R.id.tvAllAccounts);

        // ✅ FIXED: Create fragments without parameters
        incomeFragment = new IncomeFragment();
        accountFragment = new AccountFragment();
        expenseFragment = new ExpenseFragment();

        showFragment(accountFragment);
        currentType = "account";

        loadAccountData();

        btnIncome.setOnClickListener(view -> {
            showFragment(incomeFragment);
            currentType = "income";
            highlightTab(btnIncome, btnAccount, btnExpense);
        });

        btnAccount.setOnClickListener(view -> {
            showFragment(accountFragment);
            currentType = "account";
            highlightTab(btnAccount, btnIncome, btnExpense);
        });

        btnExpense.setOnClickListener(view -> {
            showFragment(expenseFragment);
            currentType = "expense";
            highlightTab(btnExpense, btnIncome, btnAccount);
        });

        btnAdd.setOnClickListener(view -> showAddDialog());
        updateIncomeExpenseSummary();

        return v;
    }

    private ArrayList<CategoryModel> loadCategoriesFromDB(String type){
        ArrayList<CategoryModel> list = new ArrayList<>();
        var cursor = dbHelper.getCategoriesForUserAndType(currentUser, type);

        if(cursor != null && cursor.moveToFirst()){
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORY_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORY_NAME));
                int icon = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORY_ICON));

                double amount = 0;
                String subtitle = "";

                // ✅ If type is "account", calculate current balance from records
                if(type.equals("account")) {
                    double income = dbHelper.getTotalIncomeForAccount(currentUser, id);
                    double expense = dbHelper.getTotalExpenseForAccount(currentUser, id);
                    amount = income - expense;
                    subtitle = "Balance: ₱ " + String.format("%.2f", amount);
                }

                list.add(new CategoryModel(name, icon, subtitle, amount));
            } while(cursor.moveToNext());
            cursor.close();
        }
        return list;
    }


    private void showFragment(Fragment fragment){
        FragmentTransaction t = getChildFragmentManager().beginTransaction();
        t.replace(R.id.categoryContentContainer, fragment);
        t.commit();
    }


    private void showIconSelectionDialog(Button targetButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Icon");

        View gridViewLayout = getLayoutInflater().inflate(R.layout.dialog_select_icon, null);
        GridView grid = gridViewLayout.findViewById(R.id.gridIcons);
        grid.setAdapter(new IconAdapter(requireContext(), availableIcons));
        grid.setOnItemClickListener((parent, view, position, id) -> {
            selectedIconResource = availableIcons[position];
            targetButton.setText("Icon Selected");
            Toast.makeText(getContext(), "Icon selected", Toast.LENGTH_SHORT).show();
        });

        builder.setView(gridViewLayout);
        builder.setPositiveButton("Done", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add " + currentType + " category");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
        EditText input = dialogView.findViewById(R.id.etCategoryName);
        Button btnSelectIcon = dialogView.findViewById(R.id.btnSelectIcon);

        // Reset to default icon
        selectedIconResource = R.drawable.ic_default;

        btnSelectIcon.setOnClickListener(v -> showIconSelectionDialog(btnSelectIcon));

        builder.setView(dialogView);

        builder.setPositiveButton("Add", null); // Set null to override default behavior
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override positive button to prevent auto-dismiss on validation error
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Please enter category name", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean ok = dbHelper.insertCategory(currentUser, currentType, name, selectedIconResource);

            if (ok) {
                CategoryModel newCat = new CategoryModel(name, selectedIconResource);
                Fragment current = getChildFragmentManager()
                        .findFragmentById(R.id.categoryContentContainer);

                if (current instanceof IncomeFragment)
                    ((IncomeFragment) current).addCategory(newCat);
                else if (current instanceof AccountFragment)
                    ((AccountFragment) current).addCategory(newCat);
                else if (current instanceof ExpenseFragment)
                    ((ExpenseFragment) current).addCategory(newCat);

                Toast.makeText(getContext(), "Category added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Failed to add", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showIconSelectionDialog(int[] selectedIcon){
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Icon");

        View gridViewLayout = getLayoutInflater().inflate(R.layout.dialog_select_icon, null);
        GridView grid = gridViewLayout.findViewById(R.id.gridIcons);
        grid.setAdapter(new IconAdapter(requireContext(), availableIcons));
        grid.setOnItemClickListener((parent, view, position, id) -> {
            selectedIcon[0] = availableIcons[position];
            Toast.makeText(getContext(), "Icon selected", Toast.LENGTH_SHORT).show();
        });

        builder.setView(gridViewLayout);
        builder.setPositiveButton("Done", null);
        builder.show();
    }

    private void highlightTab(Button selected, Button... others) {
        selected.setBackgroundResource(R.drawable.bg_button_glow);
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

        for (Button b : others) {
            b.setBackgroundResource(R.drawable.bg_button_outline);
            b.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
        }
    }

    private void updateIncomeExpenseSummary() {
        double totalIncome = dbHelper.getTotalIncome(currentUser);
        double totalExpense = dbHelper.getTotalExpense(currentUser);

        tvIncomeValue.setText("₱ " + String.format("%.2f", totalIncome));
        tvExpenseValue.setText("₱ " + String.format("%.2f", totalExpense));
    }
    @Override
    public void onCategoriesUpdated() {
        // Reload category lists
        incomeFragment.updateList(loadCategoriesFromDB("income"));
        accountFragment.updateList(loadCategoriesFromDB("account"));
        expenseFragment.updateList(loadCategoriesFromDB("expense"));

        // Reload totals
        updateIncomeExpenseSummary();
    }
    // ✅ NEW METHOD: Load all account data
    private void loadAccountData() {
        // Get total balance of all accounts
        double totalBalance = dbHelper.getTotalAccountsBalance(currentUser);
        tvAllAccounts.setText(String.format("[ All Accounts ₱ %.2f ]", totalBalance));

        // Update income and expense summary
        updateIncomeExpenseSummary();
    }

    public void reloadData() {
        // Safety check
        if (!isAdded()) return;  // CategoriesFragment itself must be attached

        // Reload child fragments only if attached
        Fragment current = getChildFragmentManager().findFragmentById(R.id.categoryContentContainer);

        if (current instanceof IncomeFragment) {
            ((IncomeFragment) current).reloadData();
        } else if (current instanceof AccountFragment) {
            ((AccountFragment) current).reloadData();
        } else if (current instanceof ExpenseFragment) {
            ((ExpenseFragment) current).reloadData();
        }

        // Reload totals regardless
        updateIncomeExpenseSummary();
        loadAccountData();
    }
    public void deleteCategoryDialog(int categoryId, String categoryName) {

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete " + categoryName)
                .setMessage("Are you sure you want to delete this? All records linked to this will also be removed.")
                .setPositiveButton("Delete", (dialog, which) -> {

                    boolean ok = dbHelper.deleteCategory(categoryId, currentUser);

                    if (ok) {
                        Toast.makeText(getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
                        reloadData();   // refresh ALL fragments and totals
                    } else {
                        Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }








}

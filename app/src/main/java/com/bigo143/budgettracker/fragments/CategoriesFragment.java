package com.bigo143.budgettracker.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
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

import com.bigo143.budgettracker.DatabaseHelper;
import com.bigo143.budgettracker.R;
import com.bigo143.budgettracker.adapters.IconAdapter;
import com.bigo143.budgettracker.models.CategoryModel;

import java.util.ArrayList;

public class CategoriesFragment extends Fragment {

    private Button btnIncome, btnAccount, btnExpense, btnAdd;
    private DatabaseHelper dbHelper;
    //private String currentUser = "userOne";

    // Replace with actual user logic
    private String currentType = "account";

    private String currentUser ;

    private IncomeFragment incomeFragment;
    private AccountFragment accountFragment;
    private ExpenseFragment expenseFragment;
    private TextView tvIncomeValue, tvExpenseValue;



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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUser = prefs.getString("logged_in_user", null);

        View v = inflater.inflate(R.layout.fragment_categories, container, false);

        dbHelper = new DatabaseHelper(requireContext());

        btnIncome = v.findViewById(R.id.btnIncome);
        btnAccount = v.findViewById(R.id.btnAccount);
        btnExpense = v.findViewById(R.id.btnExpense);
        btnAdd = v.findViewById(R.id.btnOpenCategoryAdd);

        tvIncomeValue = v.findViewById(R.id.tvIncomeValue);
        tvExpenseValue = v.findViewById(R.id.tvExpenseValue);

        incomeFragment = new IncomeFragment(loadCategoriesFromDB("income"));
        accountFragment = new AccountFragment(loadCategoriesFromDB("account"));
        expenseFragment = new ExpenseFragment(loadCategoriesFromDB("expense"));

        showFragment(accountFragment);
        currentType = "account";

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

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add " + currentType + " category");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
        EditText input = dialogView.findViewById(R.id.editCategoryName);
        Button btnSelectIcon = dialogView.findViewById(R.id.btnSelectIcon);

        final int[] selectedIcon = {R.drawable.ic_default};

        btnSelectIcon.setOnClickListener(v -> showIconSelectionDialog(selectedIcon));

        builder.setView(dialogView);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if(!name.isEmpty()){
                boolean ok = dbHelper.insertCategory(currentUser, currentType, name, selectedIcon[0]);
                if(ok){
                    CategoryModel newCat = new CategoryModel(name, selectedIcon[0]);
                    Fragment current = getChildFragmentManager()
                            .findFragmentById(R.id.categoryContentContainer);
                    if(current instanceof IncomeFragment)
                        ((IncomeFragment) current).addCategory(newCat);
                    else if(current instanceof AccountFragment)
                        ((AccountFragment) current).addCategory(newCat);
                    else if(current instanceof ExpenseFragment)
                        ((ExpenseFragment) current).addCategory(newCat);

                    Toast.makeText(getContext(), "Category added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to add", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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
}

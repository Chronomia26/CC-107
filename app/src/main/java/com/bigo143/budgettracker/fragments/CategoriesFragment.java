package com.bigo143.budgettracker.fragments;

import android.accounts.Account;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.material.button.MaterialButton;

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
            R.drawable.ic_default,
            R.drawable.airplane_travel,
            R.drawable.baseball,
            R.drawable.bachelor_hat_svgrepo_com,
            R.drawable.basketball,
            R.drawable.cash,
            R.drawable.card_holder_svgrepo_com,
            R.drawable.coins,
            R.drawable.coffee_svgrepo_com,
            R.drawable.coupons_svgrepo_com,
            R.drawable.electricity,
            R.drawable.gift_svgrepo_com,
            R.drawable.healthcare_medical_,
            R.drawable.house_svgrepo_com,
            R.drawable.second_hand_housing_svgrepo_com,
            R.drawable.ic_calendar,
            R.drawable.ic_shopping,
            R.drawable.medicines,
            R.drawable.resource_package,
            R.drawable.savings_svgrepo_com,
            R.drawable.tips_svgrepo_com,
            R.drawable.transportationvec,
            R.drawable.volleyball,
            R.drawable.wifi,
            R.drawable.water_faucet
    };

    public CategoriesFragment() {
        setHasOptionsMenu(true); // enables toolbar menu
    }
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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_normal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();  // GOOD — Java allows this

//        if (id == R.id.action_calendar) {
//            // open calendar modal
//            return true;
//
//        } else if (id == R.id.action_filter) {
//            // open filter modal
//            return true;         } else
        return super.onOptionsItemSelected(item);
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

        // Load account data and highlight default active tab
        loadAccountData();
        highlightTab((MaterialButton) btnAccount, (MaterialButton) btnIncome, (MaterialButton) btnExpense);

// INCOME button click
        btnIncome.setOnClickListener(view -> {
            showFragment(incomeFragment);
            currentType = "income";
            highlightTab((MaterialButton) btnIncome, (MaterialButton) btnAccount, (MaterialButton) btnExpense);

            // Update Add button text
            btnAdd.setText("Add Income");
        });

// ACCOUNT button click
        btnAccount.setOnClickListener(view -> {
            showFragment(accountFragment);
            currentType = "account";
            highlightTab((MaterialButton) btnAccount, (MaterialButton) btnIncome, (MaterialButton) btnExpense);

            // Update Add button text
            btnAdd.setText("Add Account");
        });

// EXPENSE button click
        btnExpense.setOnClickListener(view -> {
            showFragment(expenseFragment);
            currentType = "expense";
            highlightTab((MaterialButton) btnExpense, (MaterialButton) btnIncome, (MaterialButton) btnAccount);

            // Update Add button text
            btnAdd.setText("Add Expense");
        });
        btnAdd.setOnClickListener(view -> {
            if ("account".equals(currentType)) {
                showAddAccountDialog(); // Show the new account dialog
            } else {
                showAddDialog(); // Generic category dialog for income/expense
            }
        });



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

        // ✅ HIDE the radio button group completely
        android.widget.RadioGroup rgType = dialogView.findViewById(R.id.rgCategoryType);
        rgType.setVisibility(View.GONE);

        // ✅ HIDE the "Category Type" label as well
        TextView tvCategoryTypeLabel = dialogView.findViewById(R.id.CategoryType);
        if (tvCategoryTypeLabel != null) {
            tvCategoryTypeLabel.setVisibility(View.GONE);
        }

        // Reset to default icon
        selectedIconResource = R.drawable.ic_default;

        btnSelectIcon.setOnClickListener(v -> showIconSelectionDialog(btnSelectIcon));

        builder.setView(dialogView);

        builder.setPositiveButton("Add", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Please enter category name", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Simply use currentType (no need to read radio buttons)
            boolean ok = dbHelper.insertCategory(currentUser, currentType, name, selectedIconResource);

            if (ok) {
                CategoryModel newCat = new CategoryModel(name, selectedIconResource);
                Fragment current = getChildFragmentManager()
                        .findFragmentById(R.id.categoryContentContainer);

                if (current instanceof IncomeFragment)
                    ((IncomeFragment) current).addCategory(newCat);
                else if (current instanceof ExpenseFragment)
                    ((ExpenseFragment) current).addCategory(newCat);

                Toast.makeText(getContext(), "Category added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Failed to add", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showAddAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_account, null);

        EditText etAccountName = dialogView.findViewById(R.id.etAccountName);
        EditText etInitialBalance = dialogView.findViewById(R.id.etInitialBalance);
        Button btnSelectIcon = dialogView.findViewById(R.id.btnSelectIcon);

        // Reset icon to default
        selectedIconResource = R.drawable.ic_default;

        btnSelectIcon.setOnClickListener(v -> showIconSelectionDialog(btnSelectIcon));

        builder.setView(dialogView)
                .setTitle("Add New Account")
                .setPositiveButton("Add", null) // override default
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override positive button to handle validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String accountName = etAccountName.getText().toString().trim();
            String balanceStr = etInitialBalance.getText().toString().trim();

            if (accountName.isEmpty()) {
                Toast.makeText(getContext(), "Please enter account name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (balanceStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter initial balance", Toast.LENGTH_SHORT).show();
                return;
            }

            double initialBalance;
            try {
                initialBalance = Double.parseDouble(balanceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid balance amount", Toast.LENGTH_SHORT).show();
                return;
            }

            // Insert account into DB
            boolean success = dbHelper.insertCategory(currentUser, "account", accountName, selectedIconResource);

            if (success) {
                // If initial balance > 0, add as income record
                if (initialBalance > 0) {
                    int accountId = dbHelper.getCategoryIdByName(currentUser, accountName, "account");
                    String currentDate = new java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Calendar.getInstance().getTime());
                    dbHelper.insertRecord(currentUser, accountId, accountId, "income", initialBalance, currentDate, "Initial Balance");
                }

                // Refresh data and UI
                reloadData();

                Toast.makeText(getContext(), "Account added successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Failed to add account", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Updated showIconSelectionDialog to work with Button reference



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

    private void highlightTab(MaterialButton selected, MaterialButton... others) {
        // Set selected button as filled (active)
        selected.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary))); // Fill color
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        selected.setStrokeWidth(0); // No border

        // Set other buttons as outlined
        for (MaterialButton b : others) {
            b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.transparent))); // transparent fill
            b.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
            b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary))); // outline color
            b.setStrokeWidth(2); // outline width
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
    // Inside your Activity or Fragment
    public void showEditCategoryDialog(int categoryId, String currentName, int currentIcon) {
        Context context = requireContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit " + currentName);

        // Create a vertical layout for EditText + Icon selection
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // --- Name input ---
        final EditText input = new EditText(context);
        input.setText(currentName);
        input.setSelection(currentName.length());
        layout.addView(input);

        // --- Icon selection label ---
        TextView iconLabel = new TextView(context);
        iconLabel.setText("Select an icon:");
        iconLabel.setPadding(0, 30, 0, 10);
        layout.addView(iconLabel);

        // --- Icon GridView ---
        GridView gridView = new GridView(context);
        gridView.setNumColumns(5);
        gridView.setHorizontalSpacing(10);
        gridView.setVerticalSpacing(10);



        final int[] selectedIcon = {currentIcon}; // store selected icon

        gridView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return availableIcons.length;
            }

            @Override
            public Object getItem(int position) {
                return availableIcons[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ImageView imageView;
                if (convertView == null) {
                    imageView = new ImageView(context);
                    imageView.setLayoutParams(new GridView.LayoutParams(100, 100));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setPadding(8, 8, 8, 8);
                } else {
                    imageView = (ImageView) convertView;
                }
                imageView.setImageResource(availableIcons[position]);
                if (availableIcons[position] == selectedIcon[0]) {
                    imageView.setBackgroundColor(Color.LTGRAY); // highlight selected
                } else {
                    imageView.setBackgroundColor(Color.TRANSPARENT);
                }
                return imageView;
            }
        });

        gridView.setOnItemClickListener((parentGrid, view, position, id) -> {
            selectedIcon[0] = availableIcons[position];
            ((BaseAdapter) gridView.getAdapter()).notifyDataSetChanged(); // refresh highlight
        });

        layout.addView(gridView);

        builder.setView(layout);

        // --- Save button ---
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = dbHelper.updateCategory(categoryId, newName, selectedIcon[0]);
            if (success) {
                Toast.makeText(context, "Category updated", Toast.LENGTH_SHORT).show();
                reloadData(); // refresh ALL fragments and totals
            } else {
                Toast.makeText(context, "Failed to update category", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }














}

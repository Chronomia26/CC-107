package com.bigo143.budgettracker;

import android.util.Log;

import com.bigo143.budgettracker.models.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupManager {

    private static final String TAG = "BackupManager";
    private final DatabaseHelper dbHelper;

    public BackupManager(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /* ---------------------------------------------------------
     * EXPORT - Complete backup including accounts, categories, budgets, and records
     * --------------------------------------------------------- */

    /**
     * Export all data for a user: accounts, categories, budgets, and records
     */
    public Map<String, Object> exportAllData(String username) {
        Map<String, Object> backup = new HashMap<>();

        // Export accounts
        backup.put("accounts", exportAccounts(username));

        // Export categories (income and expense)
        backup.put("categories", exportCategories(username));

        // ✅ Export budgets
        backup.put("budgets", exportBudgets(username));

        // Export records
        backup.put("records", exportRecords(username));

        backup.put("username", username);
        backup.put("backup_version", "1.0");

        return backup;
    }

    /**
     * Export all accounts for a user
     */
    private List<Map<String, Object>> exportAccounts(String username) {
        List<Map<String, Object>> accounts = new ArrayList<>();
        android.database.Cursor cursor = dbHelper.getAccounts(username);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Map<String, Object> account = new HashMap<>();
                account.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")));
                account.put("icon", cursor.getInt(cursor.getColumnIndexOrThrow("icon")));
                accounts.add(account);
            } while (cursor.moveToNext());
            cursor.close();
        }

        Log.d(TAG, "Exported " + accounts.size() + " accounts");
        return accounts;
    }

    /**
     * Export all categories (income and expense) for a user
     */
    private List<Map<String, Object>> exportCategories(String username) {
        List<Map<String, Object>> categories = new ArrayList<>();

        // Export income categories
        android.database.Cursor incomeCursor = dbHelper.getCategoriesByType(username, "income");
        if (incomeCursor != null && incomeCursor.moveToFirst()) {
            do {
                Map<String, Object> category = new HashMap<>();
                category.put("name", incomeCursor.getString(incomeCursor.getColumnIndexOrThrow("name")));
                category.put("type", "income");
                category.put("icon", incomeCursor.getInt(incomeCursor.getColumnIndexOrThrow("icon")));
                categories.add(category);
            } while (incomeCursor.moveToNext());
            incomeCursor.close();
        }

        // Export expense categories
        android.database.Cursor expenseCursor = dbHelper.getCategoriesByType(username, "expense");
        if (expenseCursor != null && expenseCursor.moveToFirst()) {
            do {
                Map<String, Object> category = new HashMap<>();
                category.put("name", expenseCursor.getString(expenseCursor.getColumnIndexOrThrow("name")));
                category.put("type", "expense");
                category.put("icon", expenseCursor.getInt(expenseCursor.getColumnIndexOrThrow("icon")));
                categories.add(category);
            } while (expenseCursor.moveToNext());
            expenseCursor.close();
        }

        Log.d(TAG, "Exported " + categories.size() + " categories");
        return categories;
    }

    /**
     * ✅ ADDED: Export all budgets for a user
     */
    private List<Map<String, Object>> exportBudgets(String username) {
        List<Map<String, Object>> budgets = new ArrayList<>();
        android.database.Cursor cursor = dbHelper.getBudgetedCategories(username);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Map<String, Object> budget = new HashMap<>();
                budget.put("category_name", cursor.getString(cursor.getColumnIndexOrThrow("name")));
                budget.put("category_type", cursor.getString(cursor.getColumnIndexOrThrow("type")));
                budget.put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow("amount")));
                budgets.add(budget);
            } while (cursor.moveToNext());
            cursor.close();
        }

        Log.d(TAG, "Exported " + budgets.size() + " budgets");
        return budgets;
    }

    /**
     * Export all records for a user
     */
    public List<Map<String, Object>> exportRecords(String username) {
        // Get ALL records including transfer_out
        android.database.Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT r.category_id, c.name AS categoryName, r.account_id, acc.name AS accountName, " +
                        "r.type, r.amount, r.date, r.note " +
                        "FROM records r " +
                        "LEFT JOIN categories c ON r.category_id = c.id " +
                        "LEFT JOIN categories acc ON r.account_id = acc.id " +
                        "WHERE r.username = ? " +
                        "ORDER BY r.date ASC",
                new String[]{username}
        );

        List<Map<String, Object>> result = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Map<String, Object> row = new HashMap<>();
                row.put("category", cursor.getString(cursor.getColumnIndexOrThrow("categoryName")));
                row.put("account", cursor.getString(cursor.getColumnIndexOrThrow("accountName")));
                row.put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow("amount")));
                row.put("type", cursor.getString(cursor.getColumnIndexOrThrow("type")));
                row.put("date", cursor.getString(cursor.getColumnIndexOrThrow("date")));
                row.put("note", cursor.getString(cursor.getColumnIndexOrThrow("note")));

                result.add(row);
            } while (cursor.moveToNext());
            cursor.close();
        }

        Log.d(TAG, "Exported " + result.size() + " records");
        return result;
    }

    /* ---------------------------------------------------------
     * IMPORT - Complete restore including accounts, categories, budgets, and records
     * --------------------------------------------------------- */

    /**
     * Import complete backup data
     */
    public boolean importAllData(String username, Map<String, Object> backupData) {
        boolean success = true;
        int accountsImported = 0, categoriesImported = 0, budgetsImported = 0, recordsImported = 0;

        try {
            // 1. Restore accounts first
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> accounts = (List<Map<String, Object>>) backupData.get("accounts");
            if (accounts != null) {
                accountsImported = importAccounts(username, accounts);
                Log.d(TAG, "Imported " + accountsImported + "/" + accounts.size() + " accounts");
            }

            // 2. Restore categories
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> categories = (List<Map<String, Object>>) backupData.get("categories");
            if (categories != null) {
                categoriesImported = importCategories(username, categories);
                Log.d(TAG, "Imported " + categoriesImported + "/" + categories.size() + " categories");
            }

            // ✅ 3. Restore budgets (must be after categories)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> budgets = (List<Map<String, Object>>) backupData.get("budgets");
            if (budgets != null) {
                budgetsImported = importBudgets(username, budgets);
                Log.d(TAG, "Imported " + budgetsImported + "/" + budgets.size() + " budgets");
            }

            // 4. Restore records
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) backupData.get("records");
            if (records != null) {
                recordsImported = importRecords(username, records);
                Log.d(TAG, "Imported " + recordsImported + "/" + records.size() + " records");
                if (recordsImported < records.size()) {
                    success = false;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during import: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return success;
    }

    /**
     * Import accounts
     */
    private int importAccounts(String username, List<Map<String, Object>> accounts) {
        int imported = 0;

        for (Map<String, Object> account : accounts) {
            try {
                String name = (String) account.get("name");
                int icon = ((Number) account.get("icon")).intValue();

                boolean inserted = dbHelper.insertCategory(username, "account", name, icon);
                if (inserted) imported++;
                else Log.e(TAG, "Failed to import account: " + name);

            } catch (Exception e) {
                Log.e(TAG, "Error importing account: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return imported;
    }

    /**
     * Import categories
     */
    private int importCategories(String username, List<Map<String, Object>> categories) {
        int imported = 0;

        for (Map<String, Object> category : categories) {
            try {
                String name = (String) category.get("name");
                String type = (String) category.get("type");
                int icon = ((Number) category.get("icon")).intValue();

                boolean inserted = dbHelper.insertCategory(username, type, name, icon);
                if (inserted) imported++;
                else Log.e(TAG, "Failed to import category: " + name + " (" + type + ")");

            } catch (Exception e) {
                Log.e(TAG, "Error importing category: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return imported;
    }

    /**
     * ✅ ADDED: Import budgets
     */
    private int importBudgets(String username, List<Map<String, Object>> budgets) {
        int imported = 0;

        for (Map<String, Object> budget : budgets) {
            try {
                String categoryName = (String) budget.get("category_name");
                String categoryType = (String) budget.get("category_type");
                double amount = ((Number) budget.get("amount")).doubleValue();

                // Get category ID
                int categoryId = dbHelper.getCategoryIdByName(username, categoryName, categoryType);

                if (categoryId == -1) {
                    Log.e(TAG, "Category not found for budget: " + categoryName + " (" + categoryType + ")");
                    continue;
                }

                // Insert budget
                boolean inserted = dbHelper.insertBudget(username, categoryId, amount);
                if (inserted) {
                    imported++;
                    Log.d(TAG, "✅ Imported budget: " + categoryName + " - ₱" + amount);
                } else {
                    Log.e(TAG, "Failed to import budget: " + categoryName);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error importing budget: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return imported;
    }

    /**
     * Import records
     */
    public int importRecords(String username, List<Map<String, Object>> data) {
        int imported = 0;

        for (Map<String, Object> row : data) {
            try {
                String categoryName = (String) row.get("category");
                String accountName = (String) row.get("account");
                String type = (String) row.get("type");
                String date = (String) row.get("date");
                String note = (String) row.get("note");
                double amount = ((Number) row.get("amount")).doubleValue();

                // Get account ID
                int accountId = dbHelper.getAccountIdByName(username, accountName);
                if (accountId == -1) {
                    Log.e(TAG, "Account not found: " + accountName + " for type: " + type);
                    continue;
                }

                // Get category ID based on type
                int categoryId = -1;

                // ✅ SPECIAL CASE: Initial Balance (category == account for income)
                if ("income".equals(type) && categoryName.equals(accountName)) {
                    // This is an initial balance - category is the account itself
                    categoryId = accountId;
                    Log.d(TAG, "Detected initial balance for account: " + accountName);
                }
                else if ("income".equals(type)) {
                    categoryId = dbHelper.getCategoryIdByName(username, categoryName, "income");
                    if (categoryId == -1) {
                        Log.e(TAG, "Income category not found: " + categoryName);
                        continue;
                    }
                } else if ("expense".equals(type)) {
                    categoryId = dbHelper.getCategoryIdByName(username, categoryName, "expense");
                    if (categoryId == -1) {
                        Log.e(TAG, "Expense category not found: " + categoryName);
                        continue;
                    }
                } else if ("transfer_in".equals(type) || "transfer_out".equals(type)) {
                    // For transfers, category is also an account
                    categoryId = dbHelper.getAccountIdByName(username, categoryName);
                    if (categoryId == -1) {
                        Log.e(TAG, "Transfer account not found: " + categoryName);
                        continue;
                    }
                }

                if (categoryId == -1) {
                    Log.e(TAG, "Category ID not found for: " + categoryName + " type: " + type);
                    continue;
                }

                // Insert record
                boolean inserted = dbHelper.insertRecord(
                        username,
                        categoryId,
                        accountId,
                        type,
                        amount,
                        date,
                        note != null ? note : ""
                );

                if (inserted) {
                    imported++;
                    Log.d(TAG, "✅ Imported: " + type + " - " + categoryName + " - ₱" + amount);
                } else {
                    Log.e(TAG, "Failed to insert record: " + categoryName + " / " + accountName + " / " + amount);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error importing record: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return imported;
    }

    /* ---------------------------------------------------------
     * HELPERS
     * --------------------------------------------------------- */

    private String typeIntToString(int type) {
        switch (type) {
            case Record.TYPE_INCOME:
                return "income";
            case Record.TYPE_TRANSFER_OUT:
                return "transfer_out";
            case Record.TYPE_TRANSFER_IN:
                return "transfer_in";
            case Record.TYPE_EXPENSE:
            default:
                return "expense";
        }
    }
}
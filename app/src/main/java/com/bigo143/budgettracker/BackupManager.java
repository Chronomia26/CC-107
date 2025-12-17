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
     * EXPORT
     * --------------------------------------------------------- */

    public Map<String, Object> exportAllData(String username) {
        Map<String, Object> backup = new HashMap<>();

        backup.put("accounts", exportAccounts(username));
        backup.put("categories", exportCategories(username));
        backup.put("budgets", exportBudgets(username));
        backup.put("records", exportRecords(username));

        //backup.put("username", username);
        backup.put("backup_version", "1.0");

        return backup;
    }

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

    private List<Map<String, Object>> exportCategories(String username) {
        List<Map<String, Object>> categories = new ArrayList<>();
        exportCategoryType(username, "income", categories);
        exportCategoryType(username, "expense", categories);
        return categories;
    }

    private void exportCategoryType(String username, String type, List<Map<String, Object>> out) {
        android.database.Cursor cursor = dbHelper.getCategoriesByType(username, type);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Map<String, Object> category = new HashMap<>();
                category.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")));
                category.put("type", type);
                category.put("icon", cursor.getInt(cursor.getColumnIndexOrThrow("icon")));
                out.add(category);
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

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
        return budgets;
    }

    private List<Map<String, Object>> exportRecords(String username) {
        List<Map<String, Object>> result = new ArrayList<>();
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

        return result;
    }

    /* ---------------------------------------------------------
     * IMPORT
     * --------------------------------------------------------- */

    public boolean importAllData(String currentUser, Map<String, Object> backupData) {
        boolean success = true;

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> accounts = (List<Map<String, Object>>) backupData.get("accounts");
            if (accounts != null) importAccounts(currentUser, accounts);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> categories = (List<Map<String, Object>>) backupData.get("categories");
            if (categories != null) importCategories(currentUser, categories);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> budgets = (List<Map<String, Object>>) backupData.get("budgets");
            if (budgets != null) importBudgets(currentUser, budgets);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) backupData.get("records");
            if (records != null) importRecords(currentUser, records);

        } catch (Exception e) {
            Log.e(TAG, "Error during import: " + e.getMessage(), e);
            success = false;
        }

        return success;
    }


    private void importAccounts(String username, List<Map<String, Object>> accounts) {
        for (Map<String, Object> account : accounts) {
            try {
                String name = (String) account.get("name");
                int icon = ((Number) account.get("icon")).intValue();
                dbHelper.insertCategory(username, "account", name, icon);
            } catch (Exception e) {
                Log.e(TAG, "Error importing account: " + e.getMessage(), e);
            }
        }
    }

    private void importCategories(String username, List<Map<String, Object>> categories) {
        for (Map<String, Object> category : categories) {
            try {
                String name = (String) category.get("name");
                String type = (String) category.get("type");
                int icon = ((Number) category.get("icon")).intValue();
                dbHelper.insertCategory(username, type, name, icon);
            } catch (Exception e) {
                Log.e(TAG, "Error importing category: " + e.getMessage(), e);
            }
        }
    }

    private void importBudgets(String username, List<Map<String, Object>> budgets) {
        for (Map<String, Object> budget : budgets) {
            try {
                String categoryName = (String) budget.get("category_name");
                String categoryType = (String) budget.get("category_type");
                double amount = ((Number) budget.get("amount")).doubleValue();
                int categoryId = dbHelper.getCategoryIdByName(username, categoryName, categoryType);
                if (categoryId != -1) dbHelper.insertBudget(username, categoryId, amount);
            } catch (Exception e) {
                Log.e(TAG, "Error importing budget: " + e.getMessage(), e);
            }
        }
    }

    private void importRecords(String username, List<Map<String, Object>> records) {
        for (Map<String, Object> row : records) {
            try {
                String categoryName = (String) row.get("category");
                String accountName = (String) row.get("account");
                String type = (String) row.get("type");
                String date = (String) row.get("date");
                String note = (String) row.get("note");
                double amount = ((Number) row.get("amount")).doubleValue();

                int accountId = dbHelper.getAccountIdByName(username, accountName);
                int categoryId;

                if ("income".equals(type) && categoryName.equals(accountName)) categoryId = accountId;
                else if ("income".equals(type)) categoryId = dbHelper.getCategoryIdByName(username, categoryName, "income");
                else if ("expense".equals(type)) categoryId = dbHelper.getCategoryIdByName(username, categoryName, "expense");
                else categoryId = dbHelper.getAccountIdByName(username, categoryName); // for transfers

                if (accountId != -1 && categoryId != -1)
                    dbHelper.insertRecord(username, categoryId, accountId, type, amount, date, note != null ? note : "");

            } catch (Exception e) {
                Log.e(TAG, "Error importing record: " + e.getMessage(), e);
            }
        }
    }
}

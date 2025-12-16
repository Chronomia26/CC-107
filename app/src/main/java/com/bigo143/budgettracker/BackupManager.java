package com.bigo143.budgettracker;

import com.bigo143.budgettracker.models.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupManager {

    private final DatabaseHelper dbHelper;

    public BackupManager(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /* ---------------------------------------------------------
     * EXPORT
     * --------------------------------------------------------- */

    /**
     * Export all records for a user into a serializable structure
     * suitable for JSON / CSV conversion.
     */
    public List<Map<String, Object>> exportRecords(String username) {
        List<Record> records = dbHelper.getAllTransactions(username);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Record r : records) {
            // Skip headers if they are used only for UI
            if (r.isHeader()) continue;

            Map<String, Object> row = new HashMap<>();
            row.put("category", r.getCategory());
            row.put("account", r.getAccount());
            row.put("amount", r.getAmount());
            row.put("type", typeIntToString(r.getType()));
            row.put("date", r.getDate());
            row.put("note", r.getNote());

            result.add(row);
        }

        return result;
    }

    /* ---------------------------------------------------------
     * IMPORT
     * --------------------------------------------------------- */

    /**
     * Import records previously exported by {@link #exportRecords}.
     */
    public boolean importRecords(String username, List<Map<String, Object>> data) {
        boolean success = true;

        for (Map<String, Object> row : data) {

            String categoryName = (String) row.get("category");
            String accountName  = (String) row.get("account");
            String type         = (String) row.get("type");
            String date         = (String) row.get("date");
            String note         = (String) row.get("note");

            double amount;
            try {
                amount = ((Number) row.get("amount")).doubleValue();
            } catch (Exception e) {
                success = false;
                continue;
            }

            // Normalize Initial Balance
            if ("income".equals(type) && categoryName.equals(accountName)) {
                categoryName = "Initial Balance";
            }

// Create missing rows
            int accountId = dbHelper.createAccountIfNotExists(username, accountName);
            int categoryId = dbHelper.createCategoryIfNotExists(username, categoryName, type);

// Hard fail only if insertion failed
            if (accountId == -1 || categoryId == -1) {
                success = false;
                continue;
            }




            boolean inserted = dbHelper.insertRecord(
                    username,
                    categoryId,
                    accountId,
                    type,
                    amount,
                    date,
                    note
            );

            if (!inserted) success = false;
        }

        return success;
    }

    /* ---------------------------------------------------------
     * HELPERS
     * --------------------------------------------------------- */

    /**
     * Convert Record.TYPE_* int constants to database string values.
     */
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

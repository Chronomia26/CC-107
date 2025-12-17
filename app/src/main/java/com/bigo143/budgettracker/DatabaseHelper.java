package com.bigo143.budgettracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.bigo143.budgettracker.models.CategoryModel;
import com.bigo143.budgettracker.models.Record;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "budget_tracker.db";
    private static final int DATABASE_VERSION = 10; // Increment to force upgrade

    // --- Users table ---
    private static final String TABLE_USERS = "users";
    public static final String COL_ID = "id";
    public static final String COL_USERNAME = "username";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";

    // --- Categories table ---
    private static final String TABLE_CATEGORIES = "categories";
    public static final String COL_CATEGORY_ID = "id";
    public static final String COL_CATEGORY_USER = "username";
    public static final String COL_CATEGORY_TYPE = "type";
    public static final String COL_CATEGORY_NAME = "name";
    public static final String COL_CATEGORY_ICON = "icon"; // New column

    // --- Budgets table ---
    private static final String TABLE_BUDGETS = "budgets";
    public static final String COL_BUDGET_ID = "id";
    public static final String COL_BUDGET_USER = "username";
    public static final String COL_BUDGET_CATEGORY = "category_id";
    public static final String COL_BUDGET_AMOUNT = "amount";

    // Records table
    private static final String TABLE_RECORDS = "records";
    public static final String COL_RECORD_ID = "id";
    public static final String COL_RECORD_USER = "username";
    public static final String COL_RECORD_CATEGORY = "category_id";
    public static final String COL_RECORD_TYPE = "type";
    public static final String COL_RECORD_AMOUNT = "amount";
    public static final String COL_RECORD_DATE = "date";
    public static final String COL_RECORD_NOTE = "note";
    public static final String COL_RECORD_ACCOUNT = "account_id";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Users table
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT UNIQUE, " +
                COL_EMAIL + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT)");

        // Categories table with icon column
        db.execSQL("CREATE TABLE " + TABLE_CATEGORIES + " (" +
                COL_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CATEGORY_USER + " TEXT, " +
                COL_CATEGORY_TYPE + " TEXT, " +
                COL_CATEGORY_NAME + " TEXT, " +
                COL_CATEGORY_ICON + " INTEGER DEFAULT " + R.drawable.ic_default + ")");

        // Budgets table
        db.execSQL("CREATE TABLE " + TABLE_BUDGETS + " (" +
                COL_BUDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_BUDGET_USER + " TEXT, " +
                COL_BUDGET_CATEGORY + " INTEGER, " +
                COL_BUDGET_AMOUNT + " REAL)");

        // Records
        db.execSQL("CREATE TABLE " + TABLE_RECORDS + " (" +
                COL_RECORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RECORD_USER + " TEXT, " +
                COL_RECORD_CATEGORY + " INTEGER, " +
                COL_RECORD_ACCOUNT + " INTEGER DEFAULT 0, " +
                COL_RECORD_TYPE + " TEXT, " +
                COL_RECORD_AMOUNT + " REAL, " +
                COL_RECORD_DATE + " TEXT, " +
                COL_RECORD_NOTE + " TEXT)");




    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        // Add account_id column if upgrading from older versions
        if (oldVersion < 10) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COL_RECORD_ACCOUNT + " INTEGER DEFAULT 0");
            } catch (Exception e) {
                Log.e(TAG, "Error adding account_id column: " + e.getMessage());
            }
        }



    }

    // ---------------- User management ----------------
    public boolean registerUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);

        try {
            long result = db.insertOrThrow(TABLE_USERS, null, values);
            return result != -1;
        } catch (Exception e) {
            Log.e(TAG, "Error registering user: " + e.getMessage());
            return false;
        }
    }

    public boolean loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;

        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE username = ? AND password = ?",
                    new String[]{username, password});
            if (cursor != null && cursor.moveToFirst()) {
                exists = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging in: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        return exists;
    }

    public boolean userExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;

        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE email = ?",
                    new String[]{email});
            if (cursor != null && cursor.moveToFirst()) {
                exists = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking user exists: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        return exists;
    }

    // ---------------- Category management ----------------
    public boolean insertCategory(String username, String type, String name, int icon) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CATEGORY_USER, username);
        values.put(COL_CATEGORY_TYPE, type);
        values.put(COL_CATEGORY_NAME, name);
        values.put(COL_CATEGORY_ICON, icon);

        try {
            long result = db.insert(TABLE_CATEGORIES, null, values);
            return result != -1;
        } catch (Exception e) {
            Log.e(TAG, "Error inserting category: " + e.getMessage());
            return false;
        }
    }

    public Cursor getCategoriesForUserAndType(String username, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            return db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES + " WHERE username = ? AND type = ?",
                    new String[]{username, type});
        } catch (Exception e) {
            Log.e(TAG, "Error fetching categories: " + e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------



    public boolean updateCategory(int id, String newName, int newIcon) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CATEGORY_NAME, newName);
        values.put(COL_CATEGORY_ICON, newIcon);

        try {
            int rows = db.update(TABLE_CATEGORIES, values, COL_CATEGORY_ID + " = ?", new String[]{String.valueOf(id)});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating category: " + e.getMessage());
            return false;
        }
    }


    public boolean deleteCategory(int id) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            int rows = db.delete(TABLE_CATEGORIES, COL_CATEGORY_ID + " = ?", new String[]{String.valueOf(id)});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting category: " + e.getMessage());
            return false;
        }
    }

    // ---------------- Budget management ----------------



    // ---------------- Records management ----------------






    // Calculate total spent for a category for a user
    public double getTotalExpenseForCategory(String username, String categoryName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        double total = 0;

        try {
            // First, get category id from name
            cursor = db.rawQuery("SELECT id FROM " + TABLE_CATEGORIES + " WHERE username = ? AND name = ?",
                    new String[]{username, categoryName});

            int categoryId = -1;
            if (cursor != null && cursor.moveToFirst()) {
                categoryId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                cursor.close();
            }

            if (categoryId != -1) {
                cursor = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_RECORDS + " WHERE username = ? AND category_id = ? AND type = 'expense'",
                        new String[]{username, java.lang.String.valueOf(categoryId)});
                if (cursor != null && cursor.moveToFirst()) {
                    total = cursor.getDouble(0);
                    cursor.close();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting total expense for category: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        return total;
    }



    // Get total spent for a category
    public double getTotalSpentForCategory(String username, int categoryId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT SUM(amount) FROM records WHERE username = ? AND category_id = ?",
                    new String[]{username, java.lang.String.valueOf(categoryId)});
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching total spent: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return total;
    }

    // Get categories without budget
    public Cursor getNotBudgetedCategories(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT c.id, c.name FROM categories c " +
                        "LEFT JOIN budgets b ON c.id = b.category_id " +
                        "WHERE c.username = ? AND b.amount IS NULL",
                new String[]{username});
    }

    // ---------------- Account management ----------------
//    public Cursor getAccounts(String username) {
//        SQLiteDatabase db = this.getReadableDatabase();
//        return db.rawQuery(
//                "SELECT id, name FROM " + TABLE_CATEGORIES + " WHERE username = ? AND type = 'account'",
//                new String[]{username}
//        );
//    }


    // ---------------- Helper methods for calcu_add ----------------
    public int getCategoryIdByName(String username, String categoryName, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int categoryId = -1;
        try {
            cursor = db.rawQuery("SELECT id FROM " + TABLE_CATEGORIES + " WHERE username = ? AND name = ? AND type = ?",
                    new String[]{username, categoryName, type});
            if (cursor != null && cursor.moveToFirst()) {
                categoryId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting category ID: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return categoryId;
    }

    public int getAccountIdByName(String username, String accountName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int accountId = -1;
        try {
            cursor = db.rawQuery("SELECT id FROM " + TABLE_CATEGORIES + " WHERE username = ? AND name = ?",
                    new String[]{username, accountName});
            if (cursor != null && cursor.moveToFirst()) {
                accountId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting account ID: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return accountId;
    }

    // ---------------- Transfer management ----------------
    // ---------------- Transfer management ----------------
    // ---------------- Transfer management ----------------
    public boolean insertTransfer(String username, int fromAccountId, int toAccountId, double amount, String timestamp, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // --- 1. Check if source account has enough balance ---
            double currentBalance = getTotalIncomeForAccount(username, fromAccountId)
                    - getTotalExpenseForAccount(username, fromAccountId);
            if (currentBalance < amount) {
                return false; // insufficient funds
            }

            // --- 2. Insert transfer_out record for source account ---
            ContentValues cvOut = new ContentValues();
            cvOut.put(COL_RECORD_USER, username);
            cvOut.put(COL_RECORD_CATEGORY, toAccountId);    // ✅ CHANGED: Store destination account in category
            cvOut.put(COL_RECORD_ACCOUNT, fromAccountId);   // ✅ Store source account in account_id
            cvOut.put(COL_RECORD_TYPE, "transfer_out");
            cvOut.put(COL_RECORD_AMOUNT, amount);
            cvOut.put(COL_RECORD_DATE, timestamp);
            cvOut.put(COL_RECORD_NOTE, note);
            db.insert(TABLE_RECORDS, null, cvOut);

            // --- 3. Insert transfer_in record for target account ---
            ContentValues cvIn = new ContentValues();
            cvIn.put(COL_RECORD_USER, username);
            cvIn.put(COL_RECORD_CATEGORY, fromAccountId);   // ✅ CHANGED: Store source account in category
            cvIn.put(COL_RECORD_ACCOUNT, toAccountId);      // ✅ Store destination account in account_id
            cvIn.put(COL_RECORD_TYPE, "transfer_in");
            cvIn.put(COL_RECORD_AMOUNT, amount);
            cvIn.put(COL_RECORD_DATE, timestamp);
            cvIn.put(COL_RECORD_NOTE, note);
            db.insert(TABLE_RECORDS, null, cvIn);

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error inserting transfer: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
        }
    }


    // ---------------- Get categories by type ----------------
    public Cursor getCategoriesByType(String username, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM categories WHERE username = ? AND type = ?",
                new String[]{username, type}
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Log.d("DB_CHECK", "DB row: username=" + cursor.getString(cursor.getColumnIndexOrThrow("username"))
                        + ", type=" + cursor.getString(cursor.getColumnIndexOrThrow("type"))
                        + ", name=" + cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
        }

        return cursor;
    }
    public List<Record> getAllTransactions(String username) {
        List<Record> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT r." + COL_RECORD_ID + ", r." + COL_RECORD_CATEGORY + ", c." + COL_CATEGORY_NAME +
                        ", r." + COL_RECORD_TYPE + ", r." + COL_RECORD_AMOUNT + ", r." + COL_RECORD_DATE +
                        ", r." + COL_RECORD_NOTE + ", c." + COL_CATEGORY_ICON +
                        ", r." + COL_RECORD_ACCOUNT + ", acc." + COL_CATEGORY_NAME + " AS accountName" +
                        " FROM " + TABLE_RECORDS + " r " +
                        "JOIN " + TABLE_CATEGORIES + " c " +
                        "ON r." + COL_RECORD_CATEGORY + " = c." + COL_CATEGORY_ID +
                        " LEFT JOIN " + TABLE_CATEGORIES + " acc " +
                        "ON r." + COL_RECORD_ACCOUNT + " = acc." + COL_CATEGORY_ID +
                        " WHERE r." + COL_RECORD_USER + " = ? AND r." + COL_RECORD_TYPE + " != 'transfer_out' " + // ✅ EXCLUDE transfer_out
                        "ORDER BY r." + COL_RECORD_DATE + " DESC",
                new String[]{username}
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORD_ID));
                int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORD_CATEGORY));
                String categoryName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY_NAME));
                String typeStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_RECORD_TYPE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_RECORD_AMOUNT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_RECORD_DATE));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(COL_RECORD_NOTE));
                int icon = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CATEGORY_ICON));

                // Get account name (will be null if no account assigned)
                String accountName = cursor.getString(cursor.getColumnIndexOrThrow("accountName"));
                if (accountName == null || accountName.isEmpty()) {
                    accountName = "No Account";
                }

                list.add(new Record(id, categoryId, categoryName, typeStr, amount, date, note, icon, accountName));

            } while (cursor.moveToNext());

            cursor.close();
        }

        return list;
    }



    public double getIncomeForLastDays(String username, int days) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT SUM(amount) FROM records WHERE username=? AND type='income' AND date >= datetime('now', ?)";
        Cursor cursor = db.rawQuery(query, new String[]{username, "-" + days + " days"});
        double sum = 0;
        if(cursor.moveToFirst()) sum = cursor.getDouble(0);
        cursor.close();
        return sum;
    }

    public double getExpenseForLastDays(String username, int days) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT SUM(amount) FROM records WHERE username=? AND type='expense' AND date >= datetime('now', ?)";
        Cursor cursor = db.rawQuery(query, new String[]{username, "-" + days + " days"});
        double sum = 0;
        if(cursor.moveToFirst()) sum = cursor.getDouble(0);
        cursor.close();
        return sum;
    }

// Similarly implement getIncomeForLastMonth, getExpenseForLastMonth
// and getIncomeForLastYear, getExpenseForLastYear using strftime('%Y-%m', date) or '%Y'


    // --- Monthly ---
    public double getIncomeForLastMonth(String username) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT SUM(amount) FROM records " +
                "WHERE username=? AND type='income' AND strftime('%Y-%m', date) = strftime('%Y-%m', 'now')";
        Cursor cursor = db.rawQuery(query, new String[]{username});
        double sum = 0;
        if (cursor.moveToFirst()) sum = cursor.getDouble(0);
        cursor.close();
        return sum;
    }

    public double getExpenseForLastMonth(String username) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT SUM(amount) FROM records " +
                "WHERE username=? AND type='expense' AND strftime('%Y-%m', date) = strftime('%Y-%m', 'now')";
        Cursor cursor = db.rawQuery(query, new String[]{username});
        double sum = 0;
        if (cursor.moveToFirst()) sum = cursor.getDouble(0);
        cursor.close();
        return sum;
    }

    // --- Yearly ---
    public double getIncomeForLastYear(String username) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT SUM(amount) FROM records " +
                "WHERE username=? AND type='income' AND strftime('%Y', date) = strftime('%Y', 'now')";
        Cursor cursor = db.rawQuery(query, new String[]{username});
        double sum = 0;
        if (cursor.moveToFirst()) sum = cursor.getDouble(0);
        cursor.close();
        return sum;
    }

    public double getExpenseForLastYear(String username) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT SUM(amount) FROM records " +
                "WHERE username=? AND type='expense' AND strftime('%Y', date) = strftime('%Y', 'now')";
        Cursor cursor = db.rawQuery(query, new String[]{username});
        double sum = 0;
        if (cursor.moveToFirst()) sum = cursor.getDouble(0);
        cursor.close();
        return sum;
    }

    public Map<String, Double> getExpensePercentageByCategory(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, Double> result = new HashMap<>();

        // 1. Total expenses
        double totalExpense = getTotalExpense(username);
        if (totalExpense == 0) return result;

        // 2. Get sum per category
        String query =
                "SELECT c.name, SUM(r.amount) AS total " +
                        "FROM records r " +
                        "LEFT JOIN categories c ON r.category_id = c.id " +
                        "WHERE r.username = ? AND r.type = 'expense' " +
                        "GROUP BY r.category_id";

        Cursor cursor = db.rawQuery(query, new String[]{username});

        if (cursor.moveToFirst()) {
            do {
                String categoryName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                double totalInCategory = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));

                // compute percentage
                double percent = (totalInCategory / totalExpense) * 100;

                result.put(categoryName, percent);

            } while (cursor.moveToNext());
        }

        cursor.close();
        return result;
    }
    public Map<String, Double> getExpensePercentageByCategoryLastDays(String username, int days) {
        Map<String, Double> percentages = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Total expense in last X days
        String totalQuery = "SELECT SUM(" + COL_RECORD_AMOUNT + ") FROM " + TABLE_RECORDS +
                " WHERE " + COL_RECORD_USER + " = ? AND " + COL_RECORD_TYPE + " = 'expense' AND " +
                COL_RECORD_DATE + " >= date('now', ? || ' days')";
        Cursor totalCursor = db.rawQuery(totalQuery, new String[]{username, "-" + days});
        double total = 0;
        if (totalCursor.moveToFirst()) total = totalCursor.getDouble(0);
        totalCursor.close();
        if (total == 0) return percentages;

        // Sum per category
        String query = "SELECT c." + COL_CATEGORY_NAME + ", SUM(r." + COL_RECORD_AMOUNT + ") AS total " +
                "FROM " + TABLE_RECORDS + " r " +
                "LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + COL_RECORD_CATEGORY + " = c." + COL_CATEGORY_ID + " " +
                "WHERE r." + COL_RECORD_USER + " = ? AND r." + COL_RECORD_TYPE + " = 'expense' AND r." + COL_RECORD_DATE + " >= date('now', ? || ' days') " +
                "GROUP BY r." + COL_RECORD_CATEGORY;

        Cursor cursor = db.rawQuery(query, new String[]{username, "-" + days});
        while (cursor.moveToNext()) {
            String categoryName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY_NAME));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
            percentages.put(categoryName, (amount / total) * 100);
        }
        cursor.close();

        return percentages;
    }

    public Map<String, Double> getExpensePercentageByCategoryLastMonth(String username) {
        Map<String, Double> percentages = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String totalQuery = "SELECT SUM(" + COL_RECORD_AMOUNT + ") FROM " + TABLE_RECORDS +
                " WHERE " + COL_RECORD_USER + " = ? AND " + COL_RECORD_TYPE + " = 'expense' AND strftime('%Y-%m', " + COL_RECORD_DATE + ") = strftime('%Y-%m', 'now')";
        Cursor totalCursor = db.rawQuery(totalQuery, new String[]{username});
        double total = 0;
        if (totalCursor.moveToFirst()) total = totalCursor.getDouble(0);
        totalCursor.close();
        if (total == 0) return percentages;

        String query = "SELECT c." + COL_CATEGORY_NAME + ", SUM(r." + COL_RECORD_AMOUNT + ") AS total " +
                "FROM " + TABLE_RECORDS + " r " +
                "LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + COL_RECORD_CATEGORY + " = c." + COL_CATEGORY_ID + " " +
                "WHERE r." + COL_RECORD_USER + " = ? AND r." + COL_RECORD_TYPE + " = 'expense' AND strftime('%Y-%m', r." + COL_RECORD_DATE + ") = strftime('%Y-%m', 'now') " +
                "GROUP BY r." + COL_RECORD_CATEGORY;

        Cursor cursor = db.rawQuery(query, new String[]{username});
        while (cursor.moveToNext()) {
            String categoryName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY_NAME));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
            percentages.put(categoryName, (amount / total) * 100);
        }
        cursor.close();

        return percentages;
    }

    public Map<String, Double> getExpensePercentageByCategoryLastYear(String username) {
        Map<String, Double> percentages = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String totalQuery = "SELECT SUM(" + COL_RECORD_AMOUNT + ") FROM " + TABLE_RECORDS +
                " WHERE " + COL_RECORD_USER + " = ? AND " + COL_RECORD_TYPE + " = 'expense' AND strftime('%Y', " + COL_RECORD_DATE + ") = strftime('%Y', 'now')";
        Cursor totalCursor = db.rawQuery(totalQuery, new String[]{username});
        double total = 0;
        if (totalCursor.moveToFirst()) total = totalCursor.getDouble(0);
        totalCursor.close();
        if (total == 0) return percentages;

        String query = "SELECT c." + COL_CATEGORY_NAME + ", SUM(r." + COL_RECORD_AMOUNT + ") AS total " +
                "FROM " + TABLE_RECORDS + " r " +
                "LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + COL_RECORD_CATEGORY + " = c." + COL_CATEGORY_ID + " " +
                "WHERE r." + COL_RECORD_USER + " = ? AND r." + COL_RECORD_TYPE + " = 'expense' AND strftime('%Y', r." + COL_RECORD_DATE + ") = strftime('%Y', 'now') " +
                "GROUP BY r." + COL_RECORD_CATEGORY;

        Cursor cursor = db.rawQuery(query, new String[]{username});
        while (cursor.moveToNext()) {
            String categoryName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY_NAME));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
            percentages.put(categoryName, (amount / total) * 100);
        }
        cursor.close();

        return percentages;
    }


    // Get all accounts (categories of type "account")

    public boolean deductFromAccount(int accountId, String username, double amount, String date, String note) {
        double balance = getAccountBalance(accountId, username);
        if (balance < amount) return false; // insufficient funds

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_RECORD_USER, username);
        values.put(COL_RECORD_CATEGORY, accountId);
        values.put(COL_RECORD_TYPE, "expense");
        values.put(COL_RECORD_AMOUNT, amount);
        values.put(COL_RECORD_DATE, date);
        values.put(COL_RECORD_NOTE, note);

        long result = db.insert(TABLE_RECORDS, null, values);
        return result != -1;
    }
    public boolean addToAccount(int accountId, String username, double amount, String date, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_RECORD_USER, username);
        values.put(COL_RECORD_CATEGORY, accountId);
        values.put(COL_RECORD_TYPE, "income");
        values.put(COL_RECORD_AMOUNT, amount);
        values.put(COL_RECORD_DATE, date);
        values.put(COL_RECORD_NOTE, note);

        long result = db.insert(TABLE_RECORDS, null, values);
        return result != -1;
    }
    public boolean transferBetweenAccounts(int fromAccountId, int toAccountId, String username, double amount, String date, String note) {
        double fromBalance = getAccountBalance(fromAccountId, username);
        if (fromBalance < amount) return false; // insufficient funds

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Deduct from source
            ContentValues outValues = new ContentValues();
            outValues.put(COL_RECORD_USER, username);
            outValues.put(COL_RECORD_CATEGORY, fromAccountId);
            outValues.put(COL_RECORD_TYPE, "transfer_out");
            outValues.put(COL_RECORD_AMOUNT, amount);
            outValues.put(COL_RECORD_DATE, date);
            outValues.put(COL_RECORD_NOTE, note);
            db.insert(TABLE_RECORDS, null, outValues);

            // Add to destination
            ContentValues inValues = new ContentValues();
            inValues.put(COL_RECORD_USER, username);
            inValues.put(COL_RECORD_CATEGORY, toAccountId);
            inValues.put(COL_RECORD_TYPE, "transfer_in");
            inValues.put(COL_RECORD_AMOUNT, amount);
            inValues.put(COL_RECORD_DATE, date);
            inValues.put(COL_RECORD_NOTE, note);
            db.insert(TABLE_RECORDS, null, inValues);

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // Get total income for a specific account
    public double getTotalIncomeForAccount(String username, int accountId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor cursor = db.rawQuery(
                "SELECT SUM(amount) FROM " + TABLE_RECORDS +
                        " WHERE username = ? AND account_id = ? AND type = 'income'",
                new String[]{username, String.valueOf(accountId)}
        );
        if(cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    public double getTotalExpenseForAccount(String username, int accountId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor cursor = db.rawQuery(
                "SELECT SUM(amount) FROM " + TABLE_RECORDS +
                        " WHERE username = ? AND account_id = ? AND (type = 'expense' OR type='transfer_out')",
                new String[]{username, String.valueOf(accountId)}
        );
        if(cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }
    // ---------------- Budgets ----------------
    public boolean insertBudget(String username, int categoryId, double amount) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_BUDGET_USER, username);
        values.put(COL_BUDGET_CATEGORY, categoryId);
        values.put(COL_BUDGET_AMOUNT, amount);
        return db.insert(TABLE_BUDGETS, null, values) != -1;
    }

    public Cursor getBudgetedCategories(String username) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT c.name, b.amount, c.icon, c.type FROM " + TABLE_BUDGETS + " b " +
                        "JOIN " + TABLE_CATEGORIES + " c ON b.category_id=c.id " +
                        "WHERE b.username=?",
                new String[]{username});
    }

    public Cursor getUnbudgetedCategories(String username) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT c.name, c.type, c.icon FROM " + TABLE_CATEGORIES + " c " +
                        "LEFT JOIN " + TABLE_BUDGETS + " b " +
                        "ON c.id = b.category_id AND b.username = ? " +
                        "WHERE c.username = ? AND b.category_id IS NULL",
                new String[]{username, username});
    }


    public double getTotalBudget(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_BUDGETS + " WHERE username=?", new String[]{username});
        double total = cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        cursor.close();
        return total;
    }

    // ---------------- Records ----------------
    public boolean insertRecord(String username, int categoryId, int accountId, String type, double amount, String date, String note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_RECORD_USER, username);
        values.put(COL_RECORD_CATEGORY, categoryId);
        values.put(COL_RECORD_ACCOUNT, accountId);
        values.put(COL_RECORD_TYPE, type);
        values.put(COL_RECORD_AMOUNT, amount);
        values.put(COL_RECORD_DATE, date);
        values.put(COL_RECORD_NOTE, note);

        return db.insert(TABLE_RECORDS, null, values) != -1;
    }

    public double getTotalExpense(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_RECORDS + " WHERE username=? AND type='expense'", new String[]{username});
        double total = cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        cursor.close();
        return total;
    }

    public double getTotalIncome(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM " + TABLE_RECORDS + " WHERE username=? AND type='income'", new String[]{username});
        double total = cursor.moveToFirst() ? cursor.getDouble(0) : 0;
        cursor.close();
        return total;
    }

    public Cursor getRecordsForUser(String username) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT r.id, c.name, r.type, r.amount, r.date, r.note FROM " + TABLE_RECORDS + " r " +
                        "LEFT JOIN " + TABLE_CATEGORIES + " c ON r.category_id=c.id " +
                        "WHERE r.username=? ORDER BY r.date DESC",
                new String[]{username});
    }

    // ---------------- Account Balances ----------------
    public Cursor getAccounts(String username) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT id, name, icon FROM " + TABLE_CATEGORIES + " WHERE username=? AND type='account'", new String[]{username});
    }

    public double getAccountBalance(int accountId, String username) {
        SQLiteDatabase db = getReadableDatabase();
        double income = 0, expense = 0;

        Cursor cursorIncome = db.rawQuery(
                "SELECT SUM(amount) FROM " + TABLE_RECORDS +
                        " WHERE username=? AND account_id=? AND (type='income' OR type='transfer_in')",
                new String[]{username, String.valueOf(accountId)}
        );
        if (cursorIncome.moveToFirst()) income = cursorIncome.getDouble(0);
        cursorIncome.close();

        Cursor cursorExpense = db.rawQuery(
                "SELECT SUM(amount) FROM " + TABLE_RECORDS +
                        " WHERE username=? AND account_id=? AND (type='expense' OR type='transfer_out')",
                new String[]{username, String.valueOf(accountId)}
        );
        if (cursorExpense.moveToFirst()) expense = cursorExpense.getDouble(0);
        cursorExpense.close();

        return income - expense;
    }
    // Returns the budgeted amount for a specific category for the current user
    public double getBudgetedAmount(String user, int categoryId) {
        double budget = 0;
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT amount FROM " + TABLE_BUDGETS + " WHERE username = ? AND category_id = ?",
                new String[]{user, String.valueOf(categoryId)}
        );

        if (cursor != null && cursor.moveToFirst()) {
            budget = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"));
            cursor.close();
        }

        return budget;
    }

    // Returns total spent for that category

    public String getAccountNameById(int accountId, String user) {
        String name = "Unknown";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT name FROM " + TABLE_CATEGORIES + " WHERE id = ? AND username = ? AND type='account'",
                new String[]{String.valueOf(accountId), user}
        );
        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            cursor.close();
        }
        return name;
    }

    public ArrayList<Record> getTransactions(String user) {
        var list = new ArrayList<Record>();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_RECORDS + " WHERE " + COL_RECORD_USER + " = ?",
                new String[]{user});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORD_CATEGORY));
                int accountId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORD_ACCOUNT));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COL_RECORD_TYPE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_RECORD_AMOUNT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_RECORD_DATE));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(COL_RECORD_NOTE));

                String categoryName = getCategoryNameById(categoryId, user);
                String accountName = getAccountNameById(accountId, user);

                list.add(new Record(categoryName, accountName, amount, date, note));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }
    public String getCategoryNameById(int categoryId, String user) {
        String name = "Unknown";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT name FROM " + TABLE_CATEGORIES + " WHERE id = ? AND username = ?",
                new String[]{String.valueOf(categoryId), user}
        );
        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            cursor.close();
        }
        return name;
    }
    // Get total balance across all accounts for a user
    public double getTotalAccountsBalance(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        double totalIncome = 0, totalExpense = 0;

        // Get all income and transfer_in for all accounts
        Cursor cursorIncome = db.rawQuery(
                "SELECT SUM(r." + COL_RECORD_AMOUNT + ") FROM " + TABLE_RECORDS + " r " +
                        "JOIN " + TABLE_CATEGORIES + " c ON r." + COL_RECORD_ACCOUNT + " = c." + COL_CATEGORY_ID + " " +
                        "WHERE r." + COL_RECORD_USER + " = ? AND c." + COL_CATEGORY_TYPE + " = 'account' " +
                        "AND (r." + COL_RECORD_TYPE + " = 'income' OR r." + COL_RECORD_TYPE + " = 'transfer_in')",
                new String[]{username}
        );
        if (cursorIncome != null && cursorIncome.moveToFirst()) {
            totalIncome = cursorIncome.getDouble(0);
            cursorIncome.close();
        }

        // Get all expense and transfer_out for all accounts
        Cursor cursorExpense = db.rawQuery(
                "SELECT SUM(r." + COL_RECORD_AMOUNT + ") FROM " + TABLE_RECORDS + " r " +
                        "JOIN " + TABLE_CATEGORIES + " c ON r." + COL_RECORD_ACCOUNT + " = c." + COL_CATEGORY_ID + " " +
                        "WHERE r." + COL_RECORD_USER + " = ? AND c." + COL_CATEGORY_TYPE + " = 'account' " +
                        "AND (r." + COL_RECORD_TYPE + " = 'expense' OR r." + COL_RECORD_TYPE + " = 'transfer_out')",
                new String[]{username}
        );
        if (cursorExpense != null && cursorExpense.moveToFirst()) {
            totalExpense = cursorExpense.getDouble(0);
            cursorExpense.close();
        }

        return totalIncome - totalExpense;
    }
    // Delete a budget for a specific category
    public boolean deleteBudget(String username, int categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int rows = db.delete(TABLE_BUDGETS,
                    COL_BUDGET_USER + " = ? AND " + COL_BUDGET_CATEGORY + " = ?",
                    new String[]{username, String.valueOf(categoryId)});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting budget: " + e.getMessage());
            return false;
        }
    }

    // Update budget amount
    public boolean updateBudget(String username, int categoryId, double newAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_BUDGET_AMOUNT, newAmount);

        try {
            int rows = db.update(TABLE_BUDGETS, values,
                    COL_BUDGET_USER + " = ? AND " + COL_BUDGET_CATEGORY + " = ?",
                    new String[]{username, String.valueOf(categoryId)});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating budget: " + e.getMessage());
            return false;
        }
    }


    public boolean deleteCategory(int categoryId, String username) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            // Delete related records first
            db.delete(TABLE_RECORDS, "category_id=? AND username=?", new String[]{
                    String.valueOf(categoryId), username});

            // Delete category
            int rows = db.delete(TABLE_CATEGORIES, "id=? AND username=?", new String[]{
                    String.valueOf(categoryId), username});

            return rows > 0;

        } catch (Exception e) {
            Log.e("DB", "Delete category error: " + e.getMessage());
            return false;
        }
    }
    // Get full category details by ID using your CategoryModel
    public CategoryModel getCategoryById(int categoryId, String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        CategoryModel category = null;

        Cursor cursor = db.rawQuery(
                "SELECT name, icon FROM " + TABLE_CATEGORIES + " WHERE id = ? AND username = ?",
                new String[]{String.valueOf(categoryId), username}
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY_NAME));
                int icon = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CATEGORY_ICON));

                category = new CategoryModel(name, icon); // use the simple constructor
            }
            cursor.close();
        }

        return category;
    }// Add these methods to DatabaseHelper.java

    // Update a record
    public boolean updateRecord(int recordId, double newAmount, String newNote) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_RECORD_AMOUNT, newAmount);
        values.put(COL_RECORD_NOTE, newNote);

        try {
            int rows = db.update(TABLE_RECORDS, values, COL_RECORD_ID + " = ?",
                    new String[]{String.valueOf(recordId)});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating record: " + e.getMessage());
            return false;
        }
    }

    // Delete a record
    public boolean deleteRecord(int recordId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int rows = db.delete(TABLE_RECORDS, COL_RECORD_ID + " = ?",
                    new String[]{String.valueOf(recordId)});
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting record: " + e.getMessage());
            return false;
        }
    }

    // Get record type by ID (to check if it's a transfer)
    public String getRecordType(int recordId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String type = null;
        try {
            cursor = db.rawQuery("SELECT " + COL_RECORD_TYPE + " FROM " + TABLE_RECORDS +
                            " WHERE " + COL_RECORD_ID + " = ?",
                    new String[]{String.valueOf(recordId)});
            if (cursor != null && cursor.moveToFirst()) {
                type = cursor.getString(cursor.getColumnIndexOrThrow(COL_RECORD_TYPE));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting record type: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return type;
    }

    // Update transfer (both transfer_in and corresponding transfer_out)
    public boolean updateTransfer(int transferInId, double newAmount, String newNote) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Get the transfer_in record details
            Cursor cursor = db.rawQuery(
                    "SELECT " + COL_RECORD_DATE + ", " + COL_RECORD_CATEGORY + ", " + COL_RECORD_ACCOUNT +
                            " FROM " + TABLE_RECORDS + " WHERE " + COL_RECORD_ID + " = ?",
                    new String[]{String.valueOf(transferInId)}
            );

            if (cursor == null || !cursor.moveToFirst()) {
                cursor.close();
                return false;
            }

            String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_RECORD_DATE));
            int fromAccountId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORD_CATEGORY));
            int toAccountId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORD_ACCOUNT));
            cursor.close();

            // Update the transfer_in record
            ContentValues cvIn = new ContentValues();
            cvIn.put(COL_RECORD_AMOUNT, newAmount);
            cvIn.put(COL_RECORD_NOTE, newNote);
            db.update(TABLE_RECORDS, cvIn, COL_RECORD_ID + " = ?",
                    new String[]{String.valueOf(transferInId)});

            // Find and update the corresponding transfer_out record
            cursor = db.rawQuery(
                    "SELECT " + COL_RECORD_ID + " FROM " + TABLE_RECORDS +
                            " WHERE " + COL_RECORD_TYPE + " = 'transfer_out' AND " +
                            COL_RECORD_DATE + " = ? AND " +
                            COL_RECORD_CATEGORY + " = ? AND " +
                            COL_RECORD_ACCOUNT + " = ?",
                    new String[]{date, String.valueOf(toAccountId), String.valueOf(fromAccountId)}
            );

            if (cursor != null && cursor.moveToFirst()) {
                int transferOutId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORD_ID));
                ContentValues cvOut = new ContentValues();
                cvOut.put(COL_RECORD_AMOUNT, newAmount);
                cvOut.put(COL_RECORD_NOTE, newNote);
                db.update(TABLE_RECORDS, cvOut, COL_RECORD_ID + " = ?",
                        new String[]{String.valueOf(transferOutId)});
            }
            cursor.close();

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error updating transfer: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // Delete transfer (both transfer_in and corresponding transfer_out)
    public boolean deleteTransfer(int transferInId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Get the transfer_in record details
            Cursor cursor = db.rawQuery(
                    "SELECT " + COL_RECORD_DATE + ", " + COL_RECORD_CATEGORY + ", " + COL_RECORD_ACCOUNT +
                            " FROM " + TABLE_RECORDS + " WHERE " + COL_RECORD_ID + " = ?",
                    new String[]{String.valueOf(transferInId)}
            );

            if (cursor == null || !cursor.moveToFirst()) {
                cursor.close();
                return false;
            }

            String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_RECORD_DATE));
            int fromAccountId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORD_CATEGORY));
            int toAccountId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORD_ACCOUNT));
            cursor.close();

            // Delete the transfer_in record
            db.delete(TABLE_RECORDS, COL_RECORD_ID + " = ?",
                    new String[]{String.valueOf(transferInId)});

            // Find and delete the corresponding transfer_out record
            cursor = db.rawQuery(
                    "SELECT " + COL_RECORD_ID + " FROM " + TABLE_RECORDS +
                            " WHERE " + COL_RECORD_TYPE + " = 'transfer_out' AND " +
                            COL_RECORD_DATE + " = ? AND " +
                            COL_RECORD_CATEGORY + " = ? AND " +
                            COL_RECORD_ACCOUNT + " = ?",
                    new String[]{date, String.valueOf(toAccountId), String.valueOf(fromAccountId)}
            );

            if (cursor != null && cursor.moveToFirst()) {
                int transferOutId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECORD_ID));
                db.delete(TABLE_RECORDS, COL_RECORD_ID + " = ?",
                        new String[]{String.valueOf(transferOutId)});
            }
            cursor.close();

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting transfer: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }
//    public int ensureAccountExists(String username, String accountName) {
//        int id = getAccountIdByName(username, accountName);
//        if (id != -1) return id;
//
//        // Default values for restored accounts
//        insertAccount(username, accountName, 0.0);
//        return getAccountIdByName(username, accountName);
//    }
//
//    public int ensureCategoryExists(String username, String categoryName, String type) {
//        int id = getCategoryIdByName(username, categoryName, type);
//        if (id != -1) return id;
//
//        insertCategory(username, categoryName, type);
//        return getCategoryIdByName(username, categoryName, type);
//    }



    public boolean upsertBudget(String username, int categoryId, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 1. Check if budget already exists
        Cursor cursor = db.rawQuery(
                "SELECT id FROM budgets WHERE username = ? AND category_id = ?",
                new String[]{username, String.valueOf(categoryId)}
        );

        boolean exists = cursor.moveToFirst();
        cursor.close();

        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("category_id", categoryId);
        values.put("amount", amount);

        if (exists) {
            // 2. Update existing budget
            int rows = db.update(
                    "budgets",
                    values,
                    "username = ? AND category_id = ?",
                    new String[]{username, String.valueOf(categoryId)}
            );
            return rows > 0;
        } else {
            // 3. Insert new budget
            long id = db.insert("budgets", null, values);
            return id != -1;
        }
    }





}

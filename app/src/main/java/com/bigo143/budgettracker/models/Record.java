package com.bigo143.budgettracker.models;

public class Record {

    public static final int TYPE_HEADER = -1;
    public static final int TYPE_EXPENSE = 0;
    public static final int TYPE_INCOME = 1;
    public static final int TYPE_TRANSFER_OUT = 2;
    public static final int TYPE_TRANSFER_IN = 3;

    private boolean isHeader;
    private String headerTitle;

    private String category;
    private String account;
    private double amount;
    private int type;
    private int icon;
    private String date;
    private String note;

    public Record() { }

    public static Record header(String title) {
        Record r = new Record();
        r.isHeader = true;
        r.headerTitle = title;
        r.type = TYPE_HEADER;
        return r;
    }

    // ✅ UPDATED: Item constructor with accountName parameter
    public Record(int id, int categoryId, String categoryName, String typeStr,
                  double amount, String date, String note, int icon, String accountName) {

        this.isHeader = false;
        this.category = categoryName;
        this.account = accountName;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.icon = icon;

        // ✅ UPDATED: Handle transfer types
        if (typeStr.equalsIgnoreCase("income")) {
            this.type = TYPE_INCOME;
        } else if (typeStr.equalsIgnoreCase("expense")) {
            this.type = TYPE_EXPENSE;
        } else if (typeStr.equalsIgnoreCase("transfer_out")) {
            this.type = TYPE_TRANSFER_OUT;
        } else if (typeStr.equalsIgnoreCase("transfer_in")) {
            this.type = TYPE_TRANSFER_IN;
        } else {
            this.type = TYPE_INCOME; // default
        }
    }

    // ✅ ADDED: Simple constructor for basic transactions (if you need it elsewhere)
    public Record(String categoryName, String accountName, double amount, String date, String note) {
        this.category = categoryName;
        this.account = accountName;
        this.amount = amount;
        this.date = date;
        this.note = note;
    }

    // Getters
    public boolean isHeader() { return isHeader; }
    public String getHeaderTitle() { return headerTitle; }
    public String getCategory() { return category; }
    public String getAccount() { return account; }
    public double getAmount() { return amount; }
    public int getType() { return type; }
    public int getIcon() { return icon; }
    public String getDate() { return date; }
    public String getNote() { return note; }
}
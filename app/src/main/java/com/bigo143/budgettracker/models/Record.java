package com.bigo143.budgettracker.models;

public class Record {

    public static final int TYPE_HEADER = -1;
    public static final int TYPE_EXPENSE = 0;
    public static final int TYPE_INCOME = 1;
    public static final int TYPE_TRANSFER = 2;

    private boolean isHeader;
    private String headerTitle;

    private String category;
    private String account;
    private double amount;
    private int type;
    private int icon;  // changed name to match adapter usage
    private String date;  // store as string
    private String note;

    public Record() { }

    public static Record header(String title) {
        Record r = new Record();
        r.isHeader = true;
        r.headerTitle = title;
        r.type = TYPE_HEADER;
        return r;
    }

    // Item constructor
    public Record(int id, int categoryId, String categoryName, String typeStr,
                  double amount, String date, String note, int icon) {

        this.isHeader = false;
        this.category = categoryName;
        this.account = ""; // you can set account if needed
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.icon = icon; // now using the actual icon from DB

        if (typeStr.equalsIgnoreCase("income")) this.type = TYPE_INCOME;
        else if (typeStr.equalsIgnoreCase("expense")) this.type = TYPE_EXPENSE;
        else this.type = TYPE_TRANSFER;
    }

    // Getters
    public boolean isHeader() { return isHeader; }
    public String getHeaderTitle() { return headerTitle; }
    public String getCategory() { return category; }
    public String getAccount() { return account; }
    public double getAmount() { return amount; }
    public int getType() { return type; }
    public int getIcon() { return icon; } // updated getter to match adapter
    public String getDate() { return date; }
    public String getNote() { return note; }
}

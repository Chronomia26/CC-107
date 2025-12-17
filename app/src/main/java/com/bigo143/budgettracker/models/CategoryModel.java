package com.bigo143.budgettracker.models;

public class CategoryModel {

    private String name;

    private int icon;        // <- THE ONLY ICON FIELD
    private String subtitle;
    private double amount;

    private double limit;
    private double spent;

    // Constructor for simple category list (name + icon)
    public CategoryModel(String name, int icon) {
        this.name = name;
        this.icon = icon;
        this.subtitle = "";
        this.amount = 0;
    }

    // Constructor for home page categories (subtitle, amount)
    public CategoryModel(String name, int icon, String subtitle, double amount) {
        this.name = name;
        this.icon = icon;
        this.subtitle = subtitle;
        this.amount = amount;
    }

    // Constructor for budgeted categories
    public CategoryModel(String name, double limit, double spent, int icon) {
        this.name = name;
        this.limit = limit;
        this.spent = spent;
        this.icon = icon;
    }

    public String getName() { return name; }

    public int getIcon() { return icon; }

    public String getSubtitle() { return subtitle; }

    public double getAmount() { return amount; }

    public double getLimit() { return limit; }

    public double getSpent() { return spent; }
}

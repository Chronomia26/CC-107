package com.bigo143.budgettracker.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bigo143.budgettracker.DatabaseHelper;
import com.bigo143.budgettracker.R;
import com.bigo143.budgettracker.models.CategoryModel;

import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_INCOME = 1;
    public static final int TYPE_ACCOUNT = 2;
    public static final int TYPE_EXPENSE = 3;

    private ArrayList<CategoryModel> list;
    private int viewType;
    private OnItemClickListener listener;
    private OnCategoryActionListener actionListener;

    public interface OnItemClickListener {
        void onItemClick(CategoryModel model, int position);
        void onMoreClick(CategoryModel model, int position);
    }

    // ✅ NEW: Interface for Edit/Delete actions
    public interface OnCategoryActionListener {
        void onDeleteCategory(String categoryName, int categoryId);
        void onEditCategory(String categoryName, int categoryId);
    }

    public CategoryAdapter(ArrayList<CategoryModel> list, int viewType){
        this.list = list;
        this.viewType = viewType;
    }

    public void setOnItemClickListener(OnItemClickListener l){
        this.listener = l;
    }

    // ✅ NEW: Set the action listener
    public void setOnCategoryActionListener(OnCategoryActionListener l){
        this.actionListener = l;
    }

    @Override
    public int getItemCount(){ return list.size(); }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int vType) {
        View v;
        if(viewType == TYPE_INCOME){
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_income_category, parent, false);
            return new IncomeVH(v);

        } else if(viewType == TYPE_ACCOUNT){
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account_category, parent, false);
            return new AccountVH(v);

        } else {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense_category, parent, false);
            return new ExpenseVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        CategoryModel m = list.get(position);

        // INCOME
        if(holder instanceof IncomeVH){
            IncomeVH h = (IncomeVH) holder;

            h.name.setText(m.getName());
            h.icon.setImageResource(m.getIcon());

            // ✅ Modified: Show Edit/Delete options
            h.more.setOnClickListener(v -> {
                showOptionsDialog(v.getContext(), m, position);
            });

            h.itemView.setOnClickListener(v -> {
                if(listener != null) listener.onItemClick(m, position);
            });
        }

        // ACCOUNT
        else if(holder instanceof AccountVH){
            AccountVH h = (AccountVH) holder;

            h.name.setText(m.getName());
            h.icon.setImageResource(m.getIcon());
            h.balance.setText(m.getSubtitle());

            // ✅ Modified: Show Edit/Delete options
            h.more.setOnClickListener(v -> {
                showOptionsDialog(v.getContext(), m, position);
            });

            h.itemView.setOnClickListener(v -> {
                if(listener != null) listener.onItemClick(m, position);
            });
        }

        // EXPENSE
        else if(holder instanceof ExpenseVH){
            ExpenseVH h = (ExpenseVH) holder;

            h.name.setText(m.getName());
            h.icon.setImageResource(m.getIcon());

            // ✅ Modified: Show Edit/Delete options
            h.more.setOnClickListener(v -> {
                showOptionsDialog(v.getContext(), m, position);
            });

            h.itemView.setOnClickListener(v -> {
                if(listener != null) listener.onItemClick(m, position);
            });
        }
    }

    // ✅ NEW: Show options dialog when "more" button is clicked
    private void showOptionsDialog(Context context, CategoryModel model, int position) {
        // Get category ID from database
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String currentUser = prefs.getString("logged_in_user", null);

        if (currentUser == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(context);

        // Determine type based on viewType
        String type = "";
        if (viewType == TYPE_INCOME) type = "income";
        else if (viewType == TYPE_EXPENSE) type = "expense";
        else if (viewType == TYPE_ACCOUNT) type = "account";

        int categoryId = dbHelper.getCategoryIdByName(currentUser, model.getName(), type);

        if (categoryId == -1) {
            Toast.makeText(context, "Category not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show dialog with Edit/Delete options
        new AlertDialog.Builder(context)
                .setTitle(model.getName())
                .setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        // Edit
                        if (actionListener != null) {
                            actionListener.onEditCategory(model.getName(), categoryId);
                        } else {
                            Toast.makeText(context, "Edit functionality coming soon", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Delete
                        if (actionListener != null) {
                            actionListener.onDeleteCategory(model.getName(), categoryId);
                        } else {
                            Toast.makeText(context, "Delete listener not set", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    // VIEW HOLDERS
    static class IncomeVH extends RecyclerView.ViewHolder {
        ImageView icon, more;
        TextView name;
        IncomeVH(View v){
            super(v);
            icon = v.findViewById(R.id.iconIncome);
            name = v.findViewById(R.id.nameIncome);
            more = v.findViewById(R.id.btnIncomeMore);
        }
    }

    static class AccountVH extends RecyclerView.ViewHolder {
        ImageView icon, more;
        TextView name, balance;
        AccountVH(View v){
            super(v);
            icon = v.findViewById(R.id.iconAccount);
            name = v.findViewById(R.id.nameAccount);
            balance = v.findViewById(R.id.balanceAccount);
            more = v.findViewById(R.id.btnAccountMore);
        }
    }

    static class ExpenseVH extends RecyclerView.ViewHolder {
        ImageView icon, more;
        TextView name;
        ExpenseVH(View v){
            super(v);
            icon = v.findViewById(R.id.iconExpense);
            name = v.findViewById(R.id.nameExpense);
            more = v.findViewById(R.id.btnExpenseMore);
        }
    }

    public void updateData(ArrayList<CategoryModel> newData){
        this.list = newData;
        notifyDataSetChanged();
    }
}
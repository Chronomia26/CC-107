package com.bigo143.budgettracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bigo143.budgettracker.models.CategoryModel;

import java.util.ArrayList;

public class BudgetedAdapter extends RecyclerView.Adapter<BudgetedAdapter.ViewHolder> {

    private ArrayList<CategoryModel> list;
    private Context context;
    private OnBudgetActionListener listener;

    // ✅ Interface for budget actions
    public interface OnBudgetActionListener {
        void onResetBudget(CategoryModel category, int position);
        void onEditBudget(CategoryModel category, int position);
    }

    public BudgetedAdapter(ArrayList<CategoryModel> list, Context context, OnBudgetActionListener listener) {
        this.list = list;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_budgeted_category, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryModel model = list.get(position);

        holder.name.setText(model.getName());
        holder.limit.setText("Limit: ₱" + String.format("%.2f", model.getLimit()));
        holder.spent.setText("Spent: ₱" + String.format("%.2f", model.getSpent()));
        holder.icon.setImageResource(model.getIcon());

        int percentage = 0;
        if (model.getLimit() > 0) {
            percentage = (int) ((model.getSpent() / model.getLimit()) * 100);
            if (percentage > 100) percentage = 100;
        }
        holder.progressBar.setProgress(percentage);

        // ✅ Three-dots menu click listener
        holder.btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMore);
            popup.inflate(R.menu.menu_budget_item);

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_reset_budget) {
                    if (listener != null) {
                        listener.onResetBudget(model, holder.getAdapterPosition());
                    }
                    return true;
                } else if (id == R.id.action_edit_budget) {
                    if (listener != null) {
                        listener.onEditBudget(model, holder.getAdapterPosition());
                    }
                    return true;
                }
                return false;
            });

            popup.show();
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void updateData(ArrayList<CategoryModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, limit, spent;
        ImageView icon;
        ImageButton btnMore;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.catName);
            limit = itemView.findViewById(R.id.catLimit);
            spent = itemView.findViewById(R.id.catSpent);
            icon = itemView.findViewById(R.id.catIcon);
            progressBar = itemView.findViewById(R.id.catProgress);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}
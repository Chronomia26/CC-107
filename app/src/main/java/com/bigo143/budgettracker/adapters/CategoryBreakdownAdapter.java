package com.bigo143.budgettracker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bigo143.budgettracker.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryBreakdownAdapter extends RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder> {

    private final Context context;
    private final List<CategoryBreakdownItem> items;
    private final int[] colors;

    public CategoryBreakdownAdapter(Context context, Map<String, Double> categoryData, double total, int[] colors) {
        this.context = context;
        this.colors = colors;
        this.items = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            double percentage = (total > 0) ? (entry.getValue() / total) * 100 : 0;
            items.add(new CategoryBreakdownItem(
                    entry.getKey(),
                    entry.getValue(),
                    percentage,
                    colors[index % colors.length]
            ));
            index++;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
                R.layout.item_category_breakdown, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryBreakdownItem item = items.get(position);

        holder.tvCategoryName.setText(item.categoryName);
        holder.tvAmount.setText("₱" + String.format("%.2f", item.amount));
        holder.tvPercentage.setText(String.format("%.1f%%", item.percentage));
        holder.progressBar.setProgress((int) item.percentage);
        holder.progressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(item.color));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvAmount, tvPercentage;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPercentage = itemView.findViewById(R.id.tvPercentage);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }

    static class CategoryBreakdownItem {
        String categoryName;
        double amount;
        double percentage;
        int color;

        public CategoryBreakdownItem(String categoryName, double amount, double percentage, int color) {
            this.categoryName = categoryName;
            this.amount = amount;
            this.percentage = percentage;
            this.color = color;
        }
    }
}
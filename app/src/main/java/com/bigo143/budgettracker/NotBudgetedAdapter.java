package com.bigo143.budgettracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bigo143.budgettracker.models.CategoryModel;

import java.util.ArrayList;

public class NotBudgetedAdapter extends RecyclerView.Adapter<NotBudgetedAdapter.ViewHolder> {

    private ArrayList<CategoryModel> list;
    private Context context;
    private OnBudgetSetListener listener;

    public NotBudgetedAdapter(ArrayList<CategoryModel> list, Context context, OnBudgetSetListener listener) {
        this.list = list;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_not_budgeted_category, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryModel model = list.get(position);

        holder.title.setText(model.getName());
        holder.icon.setImageResource(model.getIcon());
        holder.limit.setText("Not set");

        holder.btnSetBudget.setOnClickListener(v -> {
            String input = holder.editBudgetAmount.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(input);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Trigger the listener
            if (listener != null) {
                listener.onBudgetSet(model.getName(), amount);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView title, limit;
        EditText editBudgetAmount;
        Button btnSetBudget;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.imgCategory);
            title = itemView.findViewById(R.id.tvNotBudgetTitle);
            limit = itemView.findViewById(R.id.tvNotBudgetLimit);
            editBudgetAmount = itemView.findViewById(R.id.editBudgetAmount); // make sure this exists in XML
            btnSetBudget = itemView.findViewById(R.id.btnSetBudget); // make sure this exists in XML
        }
    }

    public interface OnBudgetSetListener {
        void onBudgetSet(String categoryName, double amount);
    }

}

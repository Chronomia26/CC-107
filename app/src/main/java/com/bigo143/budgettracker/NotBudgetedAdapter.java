package com.bigo143.budgettracker;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
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
            // Open a dialog for user input
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Set Budget for " + model.getName());

            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint("Enter budget amount");
            int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
            input.setPadding(padding, padding, padding, padding);

            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String text = input.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Invalid number", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (listener != null) {
                    listener.onBudgetSet(model.getName(), amount);
                }
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
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
        ImageView icon;
        TextView title, limit;
        Button btnSetBudget;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.imgCategory);
            title = itemView.findViewById(R.id.tvNotBudgetTitle);
            limit = itemView.findViewById(R.id.tvNotBudgetLimit);
            btnSetBudget = itemView.findViewById(R.id.btnSetBudget);
        }
    }

    public interface OnBudgetSetListener {
        void onBudgetSet(String categoryName, double amount);
    }
}

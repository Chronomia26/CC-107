package com.bigo143.budgettracker.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

    public interface OnItemClickListener {
        void onItemClick(CategoryModel model, int position);
        void onMoreClick(CategoryModel model, int position);
    }

    public CategoryAdapter(ArrayList<CategoryModel> list, int viewType){
        this.list = list;
        this.viewType = viewType;
    }

    public void setOnItemClickListener(OnItemClickListener l){
        this.listener = l;
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

            h.more.setOnClickListener(v -> {
                if(listener != null) listener.onMoreClick(m, position);
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

            h.more.setOnClickListener(v -> {
                if(listener != null) listener.onMoreClick(m, position);
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

            h.more.setOnClickListener(v -> {
                if(listener != null) listener.onMoreClick(m, position);
            });

            h.itemView.setOnClickListener(v -> {
                if(listener != null) listener.onItemClick(m, position);
            });
        }
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

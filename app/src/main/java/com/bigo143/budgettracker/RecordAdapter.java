package com.bigo143.budgettracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bigo143.budgettracker.models.Record;

import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<Record> list;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public RecordAdapter(Context context, List<Record> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).isHeader() ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ------------------------------
    // CREATE VIEW HOLDERS
    // ------------------------------
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        if (viewType == VIEW_TYPE_HEADER) {
            View v = LayoutInflater.from(context)
                    .inflate(R.layout.item_date_header, parent, false);
            return new HeaderHolder(v);
        }

        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_transaction, parent, false);
        return new ItemHolder(v);
    }

    // ------------------------------
    // BIND VALUES
    // ------------------------------
    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position) {

        Record r = list.get(position);

        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).tvHeader.setText(r.getHeaderTitle());
            return;
        }

        ItemHolder item = (ItemHolder) holder;

        // ✅ HANDLE TRANSFERS DIFFERENTLY
        if (r.getType() == Record.TYPE_TRANSFER_OUT) {
            // Transfer OUT: "From AccountA → To AccountB"
            item.tvCategory.setText("Transfer to " + r.getCategory());
            item.tvAccount.setText("From: " + r.getAccount());
            item.tvAmount.setText("-₱" + String.format("%.2f", r.getAmount()));
            item.tvAmount.setTextColor(item.tvAmount.getResources().getColor(R.color.expenseRed));
            item.iconCategory.setImageResource(R.drawable.transfer_icon); // ✅ CHANGED to ic_transfer

        } else if (r.getType() == Record.TYPE_TRANSFER_IN) {
            // Transfer IN: "From AccountA → To AccountB"
            item.tvCategory.setText("Transfer from " + r.getCategory());
            item.tvAccount.setText("To: " + r.getAccount());
            item.tvAmount.setText("+₱" + String.format("%.2f", r.getAmount()));
            item.tvAmount.setTextColor(item.tvAmount.getResources().getColor(R.color.incomeValue));
            item.iconCategory.setImageResource(R.drawable.transfer_icon); // ✅ CHANGED to ic_transfer

        } else if (r.getType() == Record.TYPE_EXPENSE) {
            // Regular expense
            item.tvCategory.setText(r.getCategory());
            item.tvAccount.setText(r.getAccount());
            item.tvAmount.setText("-₱" + String.format("%.2f", r.getAmount()));
            item.tvAmount.setTextColor(item.tvAmount.getResources().getColor(R.color.expenseRed));
            item.iconCategory.setImageResource(r.getIcon());

        } else {
            // Regular income
            item.tvCategory.setText(r.getCategory());
            item.tvAccount.setText(r.getAccount());
            item.tvAmount.setText("+₱" + String.format("%.2f", r.getAmount()));
            item.tvAmount.setTextColor(item.tvAmount.getResources().getColor(R.color.incomeValue));
            item.iconCategory.setImageResource(r.getIcon());
        }
    }


    // ------------------------------
    // HEADER HOLDER
    // ------------------------------
    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;

        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvDateHeader);
        }
    }

    // ------------------------------
    // ITEM HOLDER
    // ------------------------------
    static class ItemHolder extends RecyclerView.ViewHolder {

        ImageView iconCategory;
        TextView tvCategory, tvAccount, tvAmount;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);

            iconCategory = itemView.findViewById(R.id.iconCategory);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAccount = itemView.findViewById(R.id.tvAccount);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
package com.bigo143.budgettracker.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bigo143.budgettracker.DatabaseHelper;
import com.bigo143.budgettracker.R;
import com.bigo143.budgettracker.RecordAdapter;
import com.bigo143.budgettracker.calcu_add;
import com.bigo143.budgettracker.models.Record;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordsFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecordAdapter adapter;
    private List<Record> fullList = new ArrayList<>();

    private TextView tvIncome, tvExpense, tvTotal;
    private DatabaseHelper db;
    private String currentUser;

    public RecordsFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        db = new DatabaseHelper(context);

        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        currentUser = prefs.getString("logged_in_user", null);
        if (currentUser == null) {
            throw new IllegalStateException("No logged in user found in SharedPreferences");
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_records, container, false);

        recyclerView = view.findViewById(R.id.recyclerRecords);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new RecordAdapter(requireContext(), fullList);
        recyclerView.setAdapter(adapter);

        // ✅ CHANGED: Set regular click listener
        adapter.setOnRecordClickListener(this::showTransactionDetailDialog);

        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        tvTotal = view.findViewById(R.id.tvTotal);

        db = new DatabaseHelper(requireContext());

        loadTransactions();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_records, menu);
    }

    private void loadTransactions() {
        List<Record> records = db.getAllTransactions(currentUser);

        fullList.clear();

        String lastDate = "";
        for (Record r : records) {
            String recordDate = r.getDate().split(" ")[0];
            if (!recordDate.equals(lastDate)) {
                fullList.add(Record.header(recordDate));
                lastDate = recordDate;
            }
            fullList.add(r);
        }

        adapter.notifyDataSetChanged();

        double totalIncome = db.getTotalIncome(currentUser);
        double totalExpense = db.getTotalExpense(currentUser);
        double total = totalIncome - totalExpense;

        tvIncome.setText("₱" + totalIncome);
        tvExpense.setText("₱" + totalExpense);
        tvTotal.setText("₱" + total);
    }

    public void reloadData() {
        if (!isAdded()) return;
        loadTransactions();
    }

    // ✅ NEW: Show transaction detail dialog
    private void showTransactionDetailDialog(Record record) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_transaction_detail, null);

        // Header views
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);
        ImageView btnDelete = dialogView.findViewById(R.id.btnDelete);
        ImageView btnEdit = dialogView.findViewById(R.id.btnEdit);
        TextView tvTransactionType = dialogView.findViewById(R.id.tvTransactionType);
        TextView tvAmount = dialogView.findViewById(R.id.tvAmount);
        TextView tvDateTime = dialogView.findViewById(R.id.tvDateTime);

        // Content views
        ImageView imgAccountIcon = dialogView.findViewById(R.id.imgAccountIcon);
        TextView tvAccountName = dialogView.findViewById(R.id.tvAccountName);
        LinearLayout layoutCategory = dialogView.findViewById(R.id.layoutCategory);
        ImageView imgCategoryIcon = dialogView.findViewById(R.id.imgCategoryIcon);
        TextView tvCategoryName = dialogView.findViewById(R.id.tvCategoryName);
        TextView tvNote = dialogView.findViewById(R.id.tvNote);

        // Set transaction type and header color
        int headerColor;
        String typeText;
        String amountText;

        if (record.getType() == Record.TYPE_INCOME) {
            typeText = "INCOME";
            amountText = "+₱" + String.format("%.2f", record.getAmount());
            headerColor = ContextCompat.getColor(requireContext(), R.color.incomeValue);
        } else if (record.getType() == Record.TYPE_EXPENSE) {
            typeText = "EXPENSE";
            amountText = "-₱" + String.format("%.2f", record.getAmount());
            headerColor = ContextCompat.getColor(requireContext(), R.color.expenseRed);
        } else if (record.getType() == Record.TYPE_TRANSFER_IN) {
            typeText = "TRANSFER";
            amountText = "₱" + String.format("%.2f", record.getAmount());
            headerColor = ContextCompat.getColor(requireContext(), R.color.primaryBlue);
        } else {
            typeText = "TRANSACTION";
            amountText = "₱" + String.format("%.2f", record.getAmount());
            headerColor = ContextCompat.getColor(requireContext(), R.color.primary);
        }

        tvTransactionType.setText(typeText);
        tvAmount.setText(amountText);

        // Set header background color
        ViewParent parent = dialogView.findViewById(R.id.btnClose).getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).setBackgroundColor(headerColor);
        }

        // Format date and time
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US);
            Date date = inputFormat.parse(record.getDate());
            if (date != null) {
                tvDateTime.setText(outputFormat.format(date));
            } else {
                tvDateTime.setText(record.getDate());
            }
        } catch (Exception e) {
            tvDateTime.setText(record.getDate());
        }

        // Set account info
        tvAccountName.setText(record.getAccount());
        imgAccountIcon.setImageResource(R.drawable.ic_wallet); // You can get actual icon from DB if needed

        // Set category info (hide for transfers)
        if (record.getType() == Record.TYPE_TRANSFER_IN) {
            layoutCategory.setVisibility(View.GONE);
        } else {
            tvCategoryName.setText(record.getCategory());
            imgCategoryIcon.setImageResource(record.getIcon());
        }

        // Set note
        if (record.getNote() == null || record.getNote().trim().isEmpty()) {
            tvNote.setText("No note");
            tvNote.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary));
        } else {
            tvNote.setText(record.getNote());
            tvNote.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
        }

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        // Button click listeners
        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteDialog(record);
        });

        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            showEditDialog(record);
        });
    }

    // ✅ CHANGED: Use startActivityForResult to reload data when returning
    private static final int EDIT_REQUEST_CODE = 100;

    private void showEditDialog(Record record) {
        Intent intent = new Intent(requireActivity(), calcu_add.class);
        intent.putExtra("EDIT_MODE", true);
        intent.putExtra("RECORD_ID", record.getId());
        intent.putExtra("RECORD_TYPE", getRecordTypeString(record.getType()));
        intent.putExtra("AMOUNT", record.getAmount());
        intent.putExtra("ACCOUNT_NAME", record.getAccount());
        intent.putExtra("CATEGORY_NAME", record.getCategory());
        intent.putExtra("NOTE", record.getNote());
        intent.putExtra("DATE", record.getDate());

        startActivityForResult(intent, EDIT_REQUEST_CODE);
    }

    // ✅ ADDED: Handle result when returning from edit
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_REQUEST_CODE && resultCode == android.app.Activity.RESULT_OK) {
            reloadData(); // Refresh the list immediately
        }
    }

    private String getRecordTypeString(int type) {
        switch (type) {
            case Record.TYPE_INCOME:
                return "income";
            case Record.TYPE_EXPENSE:
                return "expense";
            case Record.TYPE_TRANSFER_IN:
                return "transfer_in";
            default:
                return "income";
        }
    }

    // Delete dialog
    private void showDeleteDialog(Record record) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean success;

                    if (record.getType() == Record.TYPE_TRANSFER_IN) {
                        success = db.deleteTransfer(record.getId());
                    } else {
                        success = db.deleteRecord(record.getId());
                    }

                    if (success) {
                        Toast.makeText(getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
                        reloadData();
                    } else {
                        Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
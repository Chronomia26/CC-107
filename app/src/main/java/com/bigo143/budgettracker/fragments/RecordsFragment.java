package com.bigo143.budgettracker.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bigo143.budgettracker.DatabaseHelper;
import com.bigo143.budgettracker.R;
import com.bigo143.budgettracker.RecordAdapter;
import com.bigo143.budgettracker.models.Record;

import java.util.ArrayList;
import java.util.List;

public class RecordsFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecordAdapter adapter;
    private List<Record> fullList = new ArrayList<>();


    private TextView tvIncome, tvExpense, tvTotal; // move TextViews here
    private DatabaseHelper db;
    private String currentUser ;

    public RecordsFragment() {
        setHasOptionsMenu(true); // enables toolbar menu
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_records, container, false);
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUser = prefs.getString("logged_in_user", null);

        // RecyclerView
        recyclerView = view.findViewById(R.id.recyclerRecords);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Adapter
        adapter = new RecordAdapter(requireContext(), fullList);
        recyclerView.setAdapter(adapter);

        // Summary TextViews
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        tvTotal = view.findViewById(R.id.tvTotal);

        // Database
        db = new DatabaseHelper(requireContext());

        // Load transactions safely after views are initialized
        loadTransactions();

        return view;
    }


    // --------------------------
    // MENU (Calendar / Filter / Search)
    // --------------------------
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_records, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();  // GOOD — Java allows this

        if (id == R.id.action_calendar) {
            // open calendar modal
            return true;

        } else if (id == R.id.action_filter) {
            // open filter modal
            return true;

        } else if (id == R.id.action_search) {
            // open search UI
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // --------------------------
    // Sample Data
    // --------------------------


    private void loadTransactions() {

        List<Record> records = db.getAllTransactions(currentUser);

        fullList.clear();

        String lastDate = "";
        for (Record r : records) {
            String recordDate = r.getDate().split(" ")[0]; // YYYY-MM-DD
            if (!recordDate.equals(lastDate)) {
                fullList.add(Record.header(recordDate));
                lastDate = recordDate;
            }
            fullList.add(r);
        }

        adapter.notifyDataSetChanged();

        // Update summary
        double totalIncome = db.getTotalIncome(currentUser);
        double totalExpense = db.getTotalExpense(currentUser);
        double total = totalIncome - totalExpense;

        tvIncome.setText("₱" + totalIncome);
        tvExpense.setText("₱" + totalExpense);
        tvTotal.setText("₱" + total);
    }

}

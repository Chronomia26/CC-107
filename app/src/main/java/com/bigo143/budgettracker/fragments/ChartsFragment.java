package com.bigo143.budgettracker.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.graphics.Color;
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

import com.bigo143.budgettracker.DatabaseHelper;
import com.bigo143.budgettracker.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ChartsFragment extends Fragment {
    private DatabaseHelper db;
    private String currentUser; // replace with actual logged-in user
    private TextView tvIncome, tvExpense, tvTotal; // move TextViews here



    public ChartsFragment() {
        // Required empty public constructor
        setHasOptionsMenu(true); // enables toolbar menu
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charts, container, false);
        db = new DatabaseHelper(requireContext());

        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        currentUser = prefs.getString("logged_in_user", null);

        // Summary TextViews
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        tvTotal = view.findViewById(R.id.tvTotal);

        TextView segWeekly = view.findViewById(R.id.segWeekly);
        TextView segMonthly = view.findViewById(R.id.segMonthly);
        TextView segYearly = view.findViewById(R.id.segYearly);

        setupPie(view);
        setupBar(view);
        segWeekly.setOnClickListener(v -> {
            currentPeriod = ChartPeriod.WEEKLY;
            updateSegmentUI(segWeekly, segMonthly, segYearly);
            updateCharts();
        });

        segMonthly.setOnClickListener(v -> {
            currentPeriod = ChartPeriod.MONTHLY;
            updateSegmentUI(segWeekly, segMonthly, segYearly);
            updateCharts();
        });

        segYearly.setOnClickListener(v -> {
            currentPeriod = ChartPeriod.YEARLY;
            updateSegmentUI(segWeekly, segMonthly, segYearly);
            updateCharts();
        });

        return view;
    }

    // --------------------------
    // MENU (Calendar / Filter / Search)
    // --------------------------
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_normal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();  // GOOD — Java allows this

//        if (id == R.id.action_calendar) {
//            // open calendar modal
//            return true;
//
//        } else if (id == R.id.action_filter) {
//            // open filter modal
//            return true;         } else

        if (id == R.id.action_search) {
            // open search UI
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void updateCharts() {
        // Fetch filtered data based on currentPeriod
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<PieEntry> pieEntries = new ArrayList<>();

        double totalIncome = 0;
        double totalExpense = 0;

        switch (currentPeriod) {
            case WEEKLY:
                totalIncome = db.getIncomeForLastDays(currentUser, 7);
                totalExpense = db.getExpenseForLastDays(currentUser, 7);
                break;
            case MONTHLY:
                totalIncome = db.getIncomeForLastMonth(currentUser);
                totalExpense = db.getExpenseForLastMonth(currentUser);
                break;
            case YEARLY:
                totalIncome = db.getIncomeForLastYear(currentUser);
                totalExpense = db.getExpenseForLastYear(currentUser);
                break;
        }

        // --- Bar chart ---
        BarChart bar = requireView().findViewById(R.id.barChart);
        barEntries.add(new BarEntry(0, (float) totalIncome));
        barEntries.add(new BarEntry(1, (float) totalExpense));

        BarDataSet barDataSet = new BarDataSet(barEntries, "Income vs Expense");
        barDataSet.setColors(new int[]{Color.parseColor("#4CAF50"), Color.parseColor("#F44336")});
        BarData barData = new BarData(barDataSet);
        bar.setData(barData);
        bar.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(new String[]{"Income","Expense"}));
        bar.getXAxis().setGranularity(1f);
        bar.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        bar.getAxisRight().setEnabled(false);
        bar.getDescription().setEnabled(false);
        bar.invalidate();

        updateCategoryPieChart();

    }
    private void updateCategoryPieChart() {

        PieChart pieChart = requireView().findViewById(R.id.pieChart);

        Map<String, Double> percentages;

        switch (currentPeriod) {
            case WEEKLY:
                percentages = db.getExpensePercentageByCategoryLastDays(currentUser, 7);
                break;
            case MONTHLY:
                percentages = db.getExpensePercentageByCategoryLastMonth(currentUser);
                break;
            case YEARLY:
                percentages = db.getExpensePercentageByCategoryLastYear(currentUser);
                break;
            default:
                percentages = new HashMap<>();
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        Map<String, Integer> categoryColors = new HashMap<>();
        categoryColors.put("Food", Color.parseColor("#FF7043"));
        categoryColors.put("Bills", Color.parseColor("#42A5F5"));
        categoryColors.put("Transport", Color.parseColor("#66BB6A"));
        categoryColors.put("Shopping", Color.parseColor("#AB47BC"));
        categoryColors.put("Other", Color.parseColor("#FFA726"));

        for (Map.Entry<String, Double> e : percentages.entrySet()) {
            entries.add(new PieEntry(e.getValue().floatValue(), e.getKey()));
            colors.add(categoryColors.getOrDefault(e.getKey(), Color.GRAY));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Expense Categories");
        dataSet.setValueTextSize(12f);
        dataSet.setColors(colors);

        pieChart.setData(new PieData(dataSet));
        pieChart.setUsePercentValues(true);
        pieChart.invalidate();
    }






    private void setupPie(View view) {
        PieChart pieChart = view.findViewById(R.id.pieChart);
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        Map<String, Double> percentages = db.getExpensePercentageByCategory("userOne");

        Map<String, Integer> categoryColors = new HashMap<>();
        categoryColors.put("Food", Color.parseColor("#FF7043"));
        categoryColors.put("Bills", Color.parseColor("#42A5F5"));
        categoryColors.put("Transport", Color.parseColor("#66BB6A"));
        categoryColors.put("Shopping", Color.parseColor("#AB47BC"));
        categoryColors.put("Other", Color.parseColor("#FFA726"));

        for (Map.Entry<String, Double> e : percentages.entrySet()) {
            entries.add(new PieEntry(e.getValue().floatValue(), e.getKey()));

            if (categoryColors.containsKey(e.getKey()))
                colors.add(categoryColors.get(e.getKey()));
            else
                colors.add(Color.GRAY);
        }

        PieDataSet dataSet = new PieDataSet(entries, "Expense Categories");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);

        pieChart.setData(new PieData(dataSet));
        pieChart.setUsePercentValues(true);
        pieChart.invalidate();


    }


    private void setupBar(View view) {
        BarChart bar = view.findViewById(R.id.barChart);

        double totalIncome = db.getTotalIncome(currentUser);
        double totalExpense = db.getTotalExpense(currentUser);

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) totalIncome));
        entries.add(new BarEntry(1, (float) totalExpense));

        BarDataSet dataSet = new BarDataSet(entries, "Income vs Expense");
        dataSet.setColors(new int[]{
                Color.parseColor("#4CAF50"), // Green
                Color.parseColor("#F44336")  // Red
        });

        BarData data = new BarData(dataSet);
        bar.setData(data);

        String[] labels = new String[]{"Income", "Expense"};
        bar.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels));
        bar.getXAxis().setGranularity(1f);
        bar.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        bar.getAxisRight().setEnabled(false);
        bar.getDescription().setEnabled(false);
        bar.invalidate();

        double total = totalIncome - totalExpense;
        tvIncome.setText("₱" + totalIncome);
        tvExpense.setText("₱" + totalExpense);
        tvTotal.setText("₱" + total);
    }
    private enum ChartPeriod {
        WEEKLY, MONTHLY, YEARLY
    }

    private ChartPeriod currentPeriod = ChartPeriod.MONTHLY; // default

    private void updateSegmentUI(TextView weekly, TextView monthly, TextView yearly) {
        weekly.setBackgroundResource(currentPeriod == ChartPeriod.WEEKLY ? R.drawable.segment_selected : R.drawable.segment_unselected);
        monthly.setBackgroundResource(currentPeriod == ChartPeriod.MONTHLY ? R.drawable.segment_selected : R.drawable.segment_unselected);
        yearly.setBackgroundResource(currentPeriod == ChartPeriod.YEARLY ? R.drawable.segment_selected : R.drawable.segment_unselected);

        int selectedColor = getResources().getColor(R.color.primaryBlue);
        int defaultColor = getResources().getColor(R.color.text_main);

        weekly.setTextColor(currentPeriod == ChartPeriod.WEEKLY ? selectedColor : defaultColor);
        monthly.setTextColor(currentPeriod == ChartPeriod.MONTHLY ? selectedColor : defaultColor);
        yearly.setTextColor(currentPeriod == ChartPeriod.YEARLY ? selectedColor : defaultColor);
    }





}

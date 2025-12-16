package com.bigo143.budgettracker.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ChartsFragment extends Fragment {
    private DatabaseHelper db;
    private String currentUser; // replace with actual logged-in user
    private TextView tvIncome, tvExpense, tvTotal; // move TextViews here
    private final int[] CATEGORY_COLORS = new int[] {
            Color.parseColor("#FF7043"), // Food
            Color.parseColor("#42A5F5"), // Bills
            Color.parseColor("#66BB6A"), // Transport
            Color.parseColor("#AB47BC"), // Shopping
            Color.parseColor("#FFA726"), // Other
            Color.parseColor("#26C6DA"), // Health
            Color.parseColor("#EC407A"), // Entertainment
            Color.parseColor("#8D6E63"), // Education
            Color.parseColor("#9CCC65"), // Gifts
            Color.parseColor("#FFCA28"), // Travel
            Color.parseColor("#5C6BC0"), // Insurance
            Color.parseColor("#FF8A65")  // Misc
    };




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

        //setupBar(view);
        MaterialButton segWeekly = view.findViewById(R.id.segWeekly);
        MaterialButton segMonthly = view.findViewById(R.id.segMonthly);
        MaterialButton segYearly = view.findViewById(R.id.segYearly);

// Initial highlight
        highlightTab(segMonthly, segWeekly, segYearly); // default monthly

        segWeekly.setOnClickListener(v -> {
            currentPeriod = ChartPeriod.WEEKLY;
            highlightTab(segWeekly, segMonthly, segYearly);
            updateCharts();
        });

        segMonthly.setOnClickListener(v -> {
            currentPeriod = ChartPeriod.MONTHLY;
            highlightTab(segMonthly, segWeekly, segYearly);
            updateCharts();
        });

        segYearly.setOnClickListener(v -> {
            currentPeriod = ChartPeriod.YEARLY;
            highlightTab(segYearly, segWeekly, segMonthly);
            updateCharts();
        });


        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateCharts();
    }

    // --------------------------
    // MENU (Calendar / Filter / Search)
    // --------------------------
//    @Override
//    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
//        inflater.inflate(R.menu.menu_normal, menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//
//        int id = item.getItemId();  // GOOD — Java allows this

//        if (id == R.id.action_calendar) {
//            // open calendar modal
//            return true;
//
//        } else if (id == R.id.action_filter) {
//            // open filter modal
//            return true;
//
//        } else if (id == R.id.action_search) {
//            // open search UI
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }


    private void updateCharts() {
        // Fetch filtered data based on currentPeriod
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

        // --- Update Bar Chart ---
//        BarChart bar = requireView().findViewById(R.id.barChart);
//        ArrayList<BarEntry> barEntries = new ArrayList<>();
//        barEntries.add(new BarEntry(0, (float) totalIncome));
//        barEntries.add(new BarEntry(1, (float) totalExpense));
//
//        BarDataSet barDataSet = new BarDataSet(barEntries, "Income vs Expense");
//        barDataSet.setColors(new int[]{Color.parseColor("#4CAF50"), Color.parseColor("#F44336")});
//        BarData barData = new BarData(barDataSet);
//        bar.setData(barData);
//        bar.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(new String[]{"Income","Expense"}));
//        bar.getXAxis().setGranularity(1f);
//        bar.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
//        bar.getAxisRight().setEnabled(false);
//        bar.getDescription().setEnabled(false);
//        bar.invalidate();

        // --- Update Pie Chart ---
        updateCategoryPieChart();

        // --- Update Summary TextViews dynamically ---
        double total = totalIncome - totalExpense;
        tvIncome.setText("₱" + String.format("%.2f", totalIncome));
        tvExpense.setText("₱" + String.format("%.2f", totalExpense));
        tvTotal.setText("₱" + String.format("%.2f", total));
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

        ArrayList<String> categoryNames = new ArrayList<>(percentages.keySet());

        for (String cat : categoryNames) {
            entries.add(new PieEntry(percentages.get(cat).floatValue(), cat));
            colors.add(getColorForCategory(cat, categoryNames));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Expense Categories");
        dataSet.setValueTextSize(12f);
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.invalidate();
    }







    private void setupPie(View view) {
        PieChart pieChart = view.findViewById(R.id.pieChart);
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        // fetch percentages for current user
        Map<String, Double> percentages = db.getExpensePercentageByCategory(currentUser);

        ArrayList<String> categoryNames = new ArrayList<>(percentages.keySet());

        for (String cat : categoryNames) {
            double value = percentages.get(cat);
            entries.add(new PieEntry((float) value, cat));
            colors.add(getColorForCategory(cat, categoryNames));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Expense Categories");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        //dataSet.setValueTextColor(getResources().getColor(R.color.secondary)); // <-- set secondary color

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        // Set legend text color
        pieChart.getLegend().setTextColor(getResources().getColor(R.color.secondary));
        pieChart.invalidate();
    }



//    private void setupBar(View view) {
//        BarChart bar = view.findViewById(R.id.barChart);
//
//        double totalIncome = db.getTotalIncome(currentUser);
//        double totalExpense = db.getTotalExpense(currentUser);
//
//        ArrayList<BarEntry> entries = new ArrayList<>();
//        entries.add(new BarEntry(0, (float) totalIncome));
//        entries.add(new BarEntry(1, (float) totalExpense));
//
//        BarDataSet dataSet = new BarDataSet(entries, "Income vs Expense");
//        dataSet.setColors(new int[]{
//                Color.parseColor("#66BB6A"), // Income green
//                Color.parseColor("#EF5350")  // Expense red
//        });
//        dataSet.setValueTextColor(getResources().getColor(R.color.secondary)); // Values on top of bars
//
//        BarData data = new BarData(dataSet);
//        data.setBarWidth(0.5f); // optional: bar width
//        bar.setData(data);
//
//        String[] labels = new String[]{"Income", "Expense"};
//        bar.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels));
//        bar.getXAxis().setGranularity(1f);
//        bar.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
//        bar.getXAxis().setTextColor(getResources().getColor(R.color.secondary)); // X-axis labels color
//
//        bar.getAxisLeft().setTextColor(getResources().getColor(R.color.secondary)); // Y-axis labels color
//        bar.getAxisRight().setEnabled(false); // disable right axis
//        bar.getLegend().setTextColor(getResources().getColor(R.color.secondary)); // Legend text color
//        bar.getDescription().setEnabled(false); // remove description
//
//        bar.invalidate(); // refresh chart
//
//        double total = totalIncome - totalExpense;
//        tvIncome.setText("₱" + totalIncome);
//        tvExpense.setText("₱" + totalExpense);
//        tvTotal.setText("₱" + total);
//    }

    private enum ChartPeriod {
        WEEKLY, MONTHLY, YEARLY
    }

    private ChartPeriod currentPeriod = ChartPeriod.MONTHLY; // default

    private void updateSegmentUI(TextView weekly, TextView monthly, TextView yearly) {
        weekly.setBackgroundResource(currentPeriod == ChartPeriod.WEEKLY ? R.drawable.bg_button_outline : R.drawable.segment_unselected);
        monthly.setBackgroundResource(currentPeriod == ChartPeriod.MONTHLY ? R.drawable.bg_button_outline : R.drawable.segment_unselected);
        yearly.setBackgroundResource(currentPeriod == ChartPeriod.YEARLY ? R.drawable.bg_button_outline : R.drawable.segment_unselected);

        int selectedColor = getResources().getColor(R.color.primary);
        int defaultColor = getResources().getColor(R.color.textPrimary);

        weekly.setTextColor(currentPeriod == ChartPeriod.WEEKLY ? selectedColor :getResources().getColor(R.color.secondary ));
        monthly.setTextColor(currentPeriod == ChartPeriod.MONTHLY ? selectedColor : getResources().getColor(R.color.secondary ));
        yearly.setTextColor(currentPeriod == ChartPeriod.YEARLY ? selectedColor : getResources().getColor(R.color.secondary ));
    }

    private int getColorForCategory(String categoryName, ArrayList<String> categoryList) {
        int index = categoryList.indexOf(categoryName);
        if (index == -1) index = 0; // fallback
        return CATEGORY_COLORS[index % CATEGORY_COLORS.length];
    }


    public void reloadData() {
        // Recalculate summary and refresh charts
        updateCharts();
    }
    private void highlightTab(MaterialButton selected, MaterialButton... others) {
        // Selected button: filled
        selected.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        selected.setStrokeWidth(0);

        // Other buttons: outlined
        for (MaterialButton b : others) {
            b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.transparent)));
            b.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
            b.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
            b.setStrokeWidth(2);
        }
    }







}

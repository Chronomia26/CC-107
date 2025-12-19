package com.bigo143.budgettracker.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bigo143.budgettracker.DatabaseHelper;
import com.bigo143.budgettracker.R;
import com.bigo143.budgettracker.adapters.CategoryBreakdownAdapter;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;
import android.widget.PopupMenu;


public class ChartsFragment extends Fragment {
    private DatabaseHelper db;
    private String currentUser;
    private TextView tvIncome, tvExpense, tvTotal, tvMonthYear;
    private ImageView btnPrevMonth, btnNextMonth;
    private MaterialButton btnSelectOverview;
    private RecyclerView recyclerCategoryBreakdown;


    private Calendar selectedDate;

    private OverviewType currentOverview = OverviewType.EXPENSE;

    private final int[] CATEGORY_COLORS = new int[] {
            Color.parseColor("#FF7043"), Color.parseColor("#42A5F5"),
            Color.parseColor("#66BB6A"), Color.parseColor("#AB47BC"),
            Color.parseColor("#FFA726"), Color.parseColor("#26C6DA"),
            Color.parseColor("#EC407A"), Color.parseColor("#8D6E63"),
            Color.parseColor("#9CCC65"), Color.parseColor("#FFCA28"),
            Color.parseColor("#5C6BC0"), Color.parseColor("#FF8A65")
    };

    private enum ChartPeriod {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }

    private enum OverviewType {
        INCOME, EXPENSE
    }

    private ChartPeriod currentPeriod = ChartPeriod.MONTHLY;

    public ChartsFragment() {
        setHasOptionsMenu(true);
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

        // Initialize selected month to current month
        selectedDate = Calendar.getInstance();


        initViews(view);
        setupNavigation();
        setupOverviewSelector();
        setupPeriodSelector(view);
        //setupPeriodButtons(view);

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        updateCharts(); // ✅ FIX: refresh when chart tab is clicked
    }

    private void initViews(View view) {
        tvIncome = view.findViewById(R.id.tvIncome);
        tvExpense = view.findViewById(R.id.tvExpense);
        tvTotal = view.findViewById(R.id.tvTotal);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        btnSelectOverview = view.findViewById(R.id.btnSelectOverview);
        recyclerCategoryBreakdown = view.findViewById(R.id.recyclerCategoryBreakdown);

        recyclerCategoryBreakdown.setLayoutManager(new LinearLayoutManager(requireContext()));

    }
    private void setupNavigation() {
        updateDateLabel();

        btnPrevMonth.setOnClickListener(v -> {
            moveDate(-1);
            updateCharts(); // ✅ FIX
        });

        btnNextMonth.setOnClickListener(v -> {
            moveDate(1);
            updateCharts(); // ✅ FIX
        });
    }

    private void setupMonthNavigation() {
        updateMonthYearDisplay();

        btnPrevMonth.setOnClickListener(v -> {
            selectedDate.add(Calendar.MONTH, -1);
            updateMonthYearDisplay();
            //updateCharts();
        });

        btnNextMonth.setOnClickListener(v -> {
            selectedDate.add(Calendar.MONTH, 1);
            updateMonthYearDisplay();
            //updateCharts();
        });
    }

    private void moveDate(int delta) {
        switch (currentPeriod) {
            case DAILY:
                selectedDate.add(Calendar.DAY_OF_MONTH, delta);
                break;
            case WEEKLY:
                selectedDate.add(Calendar.WEEK_OF_YEAR, delta);
                break;
            case MONTHLY:
                selectedDate.add(Calendar.MONTH, delta);
                break;
            case YEARLY:
                selectedDate.add(Calendar.YEAR, delta);
                break;
        }
        updateDateLabel();
    }
    private void updateDateLabel() {
        SimpleDateFormat sdf;

        switch (currentPeriod) {
            case DAILY:
                sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                break;
            case WEEKLY:
                sdf = new SimpleDateFormat("'Week of' MMM dd, yyyy", Locale.US);
                break;
            case YEARLY:
                sdf = new SimpleDateFormat("yyyy", Locale.US);
                break;
            default:
                sdf = new SimpleDateFormat("MMMM yyyy", Locale.US);
        }

        tvMonthYear.setText(sdf.format(selectedDate.getTime()));
    }

    // ---------------- PERIOD SELECTOR ----------------

    private void setupPeriodSelector(View view) {
        view.findViewById(R.id.btnPeriodMenu).setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.getMenu().add("Daily");
            menu.getMenu().add("Weekly");
            menu.getMenu().add("Monthly");
            menu.getMenu().add("Yearly");

            menu.setOnMenuItemClickListener(item -> {
                currentPeriod = ChartPeriod.valueOf(
                        item.getTitle().toString().toUpperCase(Locale.US));
                updateDateLabel();
                updateCharts();
                return true;
            });

            menu.show();
        });
    }
    private void setupOverviewSelector() {
        btnSelectOverview.setOnClickListener(v -> {
            String[] options = {"Expense Overview", "Income Overview"};

            new AlertDialog.Builder(requireContext())
                    .setTitle("Overview Type")
                    .setSingleChoiceItems(options,
                            currentOverview == OverviewType.EXPENSE ? 0 : 1,
                            (dialog, which) -> {
                                currentOverview = (which == 0)
                                        ? OverviewType.EXPENSE
                                        : OverviewType.INCOME;
                                updateCharts();
                                updateOverviewButton();
                                dialog.dismiss();
                            })
                    .show();
        });
    }
    private void updateCharts() {

        double income = 0;
        double expense = 0;

        int year = selectedDate.get(Calendar.YEAR);
        int month = selectedDate.get(Calendar.MONTH) + 1;

        switch (currentPeriod) {
            case DAILY: {
                int daysAgo = daysFromToday(selectedDate) + 1;

                income = db.getIncomeForLastDays(currentUser, daysAgo);
                expense = db.getExpenseForLastDays(currentUser, daysAgo);
                break;
            }


            case WEEKLY:
                income = db.getIncomeForLastDays(currentUser, 7);
                expense = db.getExpenseForLastDays(currentUser, 7);
                break;

            case MONTHLY:
                income = db.getIncomeForMonth(currentUser, year, month);
                expense = db.getExpenseForMonth(currentUser, year, month);
                break;

            case YEARLY:
                income = db.getIncomeForYear(currentUser, year);
                expense = db.getExpenseForYear(currentUser, year);
                break;
        }

        tvIncome.setText("₱" + String.format("%.2f", income));
        tvExpense.setText("₱" + String.format("%.2f", expense));
        tvTotal.setText("₱" + String.format("%.2f", income - expense));

        updateOverviewData();
        updateCategoryPieChart();
    }

    public void reloadData() {
        updateCharts(); // ✅ FIX
    }
    private void updateMonthYearDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM, yyyy", Locale.US);
        tvMonthYear.setText(sdf.format(selectedDate.getTime()));
    }



    private void showOverviewSelectionDialog() {
        String[] options = {"Expense Overview", "Income Overview"};
        int selectedIndex = (currentOverview == OverviewType.EXPENSE) ? 0 : 1;

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Overview Type")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    currentOverview = (which == 0) ? OverviewType.EXPENSE : OverviewType.INCOME;
                    updateOverviewButton();
                    updateOverviewData();
                    updateCategoryPieChart(); // ✅ ADDED: Update pie chart when overview type changes
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateOverviewButton() {
        if (currentOverview == OverviewType.EXPENSE) {
            btnSelectOverview.setText("EXPENSE OVERVIEW");
            btnSelectOverview.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.expenseRed)));
        } else {
            btnSelectOverview.setText("INCOME OVERVIEW");
            btnSelectOverview.setIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.incomeValue)));
        }
    }

    private void updateOverviewData() {
        Map<String, Double> categoryData;
        double total = 0;

        if (currentOverview == OverviewType.EXPENSE) {
            switch (currentPeriod) {

                case DAILY: {
                    int daysAgo = daysFromToday(selectedDate) + 1;

                    categoryData = db.getExpenseAmountByCategoryLastDays(currentUser, daysAgo);
                    total = db.getExpenseForLastDays(currentUser, daysAgo);
                    break;
                }

                case WEEKLY:
                    Calendar[] weekRange = getWeekStartEnd(selectedDate);
                    if (currentOverview == OverviewType.EXPENSE) {
                        categoryData = db.getExpenseAmountByCategoryBetweenDates(
                                currentUser,
                                weekRange[0].getTimeInMillis(),
                                weekRange[1].getTimeInMillis()
                        );
                        total = db.getExpenseBetweenDates(
                                currentUser,
                                weekRange[0].getTimeInMillis(),
                                weekRange[1].getTimeInMillis()
                        );
                    } else {
                        categoryData = db.getIncomeAmountByCategoryBetweenDates(
                                currentUser,
                                weekRange[0].getTimeInMillis(),
                                weekRange[1].getTimeInMillis()
                        );
                        total = db.getIncomeBetweenDates(
                                currentUser,
                                weekRange[0].getTimeInMillis(),
                                weekRange[1].getTimeInMillis()
                        );
                    }
                    break;



                case MONTHLY:
                    categoryData = db.getExpenseAmountByCategoryForMonth(
                            currentUser,
                            selectedDate.get(Calendar.YEAR),
                            selectedDate.get(Calendar.MONTH) + 1
                    );
                    total = db.getExpenseForMonth(
                            currentUser,
                            selectedDate.get(Calendar.YEAR),
                            selectedDate.get(Calendar.MONTH) + 1
                    );
                    break;

                case YEARLY:
                    categoryData = db.getExpenseAmountByCategoryForYear(
                            currentUser,
                            selectedDate.get(Calendar.YEAR)
                    );
                    total = db.getExpenseForYear(
                            currentUser,
                            selectedDate.get(Calendar.YEAR)
                    );
                    break;

                default:
                    categoryData = new HashMap<>();
            }
        }
        else {
            switch (currentPeriod) {

                case DAILY: {
                    int daysAgo = daysFromToday(selectedDate) + 1;

                    categoryData = db.getIncomeAmountByCategoryLastDays(currentUser, daysAgo);
                    total = db.getIncomeForLastDays(currentUser, daysAgo);
                    break;
                }

                case WEEKLY:
                    categoryData = db.getIncomeAmountByCategoryLastDays(currentUser, 7);
                    total = db.getIncomeForLastDays(currentUser, 7);
                    break;

                case MONTHLY:
                    categoryData = db.getIncomeAmountByCategoryForMonth(
                            currentUser,
                            selectedDate.get(Calendar.YEAR),
                            selectedDate.get(Calendar.MONTH) + 1
                    );
                    total = db.getIncomeForMonth(
                            currentUser,
                            selectedDate.get(Calendar.YEAR),
                            selectedDate.get(Calendar.MONTH) + 1
                    );
                    break;

                case YEARLY:
                    categoryData = db.getIncomeAmountByCategoryForYear(
                            currentUser,
                            selectedDate.get(Calendar.YEAR)
                    );
                    total = db.getIncomeForYear(
                            currentUser,
                            selectedDate.get(Calendar.YEAR)
                    );
                    break;

                default:
                    categoryData = new HashMap<>();
            }
        }


        // Show/hide based on whether there's data
        if (categoryData.isEmpty()) {
            recyclerCategoryBreakdown.setVisibility(View.GONE);
        } else {
            recyclerCategoryBreakdown.setVisibility(View.VISIBLE);
            CategoryBreakdownAdapter adapter = new CategoryBreakdownAdapter(
                    requireContext(), categoryData, total, CATEGORY_COLORS);
            recyclerCategoryBreakdown.setAdapter(adapter);
        }
    }

//    private void setupPeriodButtons(View view) {
//        MaterialButton segWeekly = view.findViewById(R.id.segWeekly);
//        MaterialButton segMonthly = view.findViewById(R.id.segMonthly);
//        MaterialButton segYearly = view.findViewById(R.id.segYearly);
//
//        highlightTab(segMonthly, segWeekly, segYearly);
//
//        segWeekly.setOnClickListener(v -> {
//            currentPeriod = ChartPeriod.WEEKLY;
//            highlightTab(segWeekly, segMonthly, segYearly);
//            updateCharts();
//        });
//
//        segMonthly.setOnClickListener(v -> {
//            currentPeriod = ChartPeriod.MONTHLY;
//            highlightTab(segMonthly, segWeekly, segYearly);
//            updateCharts();
//        });
//
//        segYearly.setOnClickListener(v -> {
//            currentPeriod = ChartPeriod.YEARLY;
//            highlightTab(segYearly, segWeekly, segMonthly);
//            updateCharts();
//        });
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        updateCharts();
//    }
//
//    private void updateCharts() {
//        double totalIncome = 0;
//        double totalExpense = 0;
//
//        switch (currentPeriod) {
//            case WEEKLY:
//                totalIncome = db.getIncomeForLastDays(currentUser, 7);
//                totalExpense = db.getExpenseForLastDays(currentUser, 7);
//                break;
//            case MONTHLY:
//                totalIncome = db.getIncomeForMonth(currentUser,
//                        selectedMonth.get(Calendar.YEAR),
//                        selectedMonth.get(Calendar.MONTH) + 1);
//                totalExpense = db.getExpenseForMonth(currentUser,
//                        selectedMonth.get(Calendar.YEAR),
//                        selectedMonth.get(Calendar.MONTH) + 1);
//                break;
//            case YEARLY:
//                totalIncome = db.getIncomeForYear(currentUser,
//                        selectedMonth.get(Calendar.YEAR));
//                totalExpense = db.getExpenseForYear(currentUser,
//                        selectedMonth.get(Calendar.YEAR));
//                break;
//        }
//
//        updateCategoryPieChart();
//        updateOverviewData(); // Always update overview data
//
//        double total = totalIncome - totalExpense;
//        tvIncome.setText("₱" + String.format("%.2f", totalIncome));
//        tvExpense.setText("₱" + String.format("%.2f", totalExpense));
//        tvTotal.setText("₱" + String.format("%.2f", total));
//    }

    private void updateCategoryPieChart() {
        PieChart pieChart = requireView().findViewById(R.id.pieChart);
        Map<String, Double> percentages;

        // Get percentages based on current overview type and period
        if (currentOverview == OverviewType.EXPENSE) {
            switch (currentPeriod) {
                case DAILY: {
                    int daysAgo = daysFromToday(selectedDate) + 1;
                    percentages = db.getExpensePercentageByCategoryLastDays(currentUser, daysAgo);
                    break;
                }

                case WEEKLY:
                    Calendar[] weekRange = getWeekStartEnd(selectedDate);
                    percentages = db.getExpensePercentageByCategoryBetweenDates(
                            currentUser,
                            weekRange[0].getTimeInMillis(),
                            weekRange[1].getTimeInMillis()
                    );
                    break;

                case MONTHLY:
                    percentages = db.getExpensePercentageByCategoryForMonth(currentUser,
                            selectedDate.get(Calendar.YEAR),
                            selectedDate.get(Calendar.MONTH) + 1);
                    break;

                case YEARLY:
                    percentages = db.getExpensePercentageByCategoryForYear(currentUser,
                            selectedDate.get(Calendar.YEAR));
                    break;

                default:
                    percentages = new HashMap<>();
            }
        } else {
            // Income overview
            switch (currentPeriod) {
                case DAILY: {
                    int daysAgo = daysFromToday(selectedDate) + 1;
                    percentages = db.getIncomePercentageByCategoryLastDays(currentUser, daysAgo);
                    break;
                }

                case WEEKLY:
                    Calendar[] weekRange = getWeekStartEnd(selectedDate);
                    percentages = db.getIncomePercentageByCategoryBetweenDates(
                            currentUser,
                            weekRange[0].getTimeInMillis(),
                            weekRange[1].getTimeInMillis()
                    );
                    break;

                case MONTHLY:
                    percentages = db.getIncomePercentageByCategoryForMonth(currentUser,
                            selectedDate.get(Calendar.YEAR),
                            selectedDate.get(Calendar.MONTH) + 1);
                    break;

                case YEARLY:
                    percentages = db.getIncomePercentageByCategoryForYear(currentUser,
                            selectedDate.get(Calendar.YEAR));
                    break;

                default:
                    percentages = new HashMap<>();
            }
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        ArrayList<String> categoryNames = new ArrayList<>(percentages.keySet());

        for (String cat : categoryNames) {
            entries.add(new PieEntry(percentages.get(cat).floatValue(), cat));
            colors.add(getColorForCategory(cat, categoryNames));
        }

        // Update chart title based on overview type
        String chartTitle = (currentOverview == OverviewType.EXPENSE) ?
                "Expense Categories" : "Income Categories";

        PieDataSet dataSet = new PieDataSet(entries, chartTitle);
        dataSet.setValueTextSize(12f);
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.invalidate();
    }

    private int getColorForCategory(String categoryName, ArrayList<String> categoryList) {
        int index = categoryList.indexOf(categoryName);
        if (index == -1) index = 0;
        return CATEGORY_COLORS[index % CATEGORY_COLORS.length];
    }

    private int daysFromToday(Calendar targetDate) {
        Calendar today = Calendar.getInstance();

        // Zero out time to avoid partial-day errors
        for (Calendar c : new Calendar[]{today, targetDate}) {
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        long diffMillis = today.getTimeInMillis() - targetDate.getTimeInMillis();
        return (int) (diffMillis / (1000 * 60 * 60 * 24));
    }

    private Calendar[] getWeekStartEnd(Calendar date) {
        Calendar start = (Calendar) date.clone();
        start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // Always start on Monday
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 6); // Monday + 6 = Sunday

        return new Calendar[]{start, end};
    }




    private void highlightTab(MaterialButton selected, MaterialButton... others) {
        selected.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primary)));
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        selected.setStrokeWidth(0);

        for (MaterialButton b : others) {
            b.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), android.R.color.transparent)));
            b.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
            b.setStrokeColor(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.primary)));
            b.setStrokeWidth(2);
        }
    }
}
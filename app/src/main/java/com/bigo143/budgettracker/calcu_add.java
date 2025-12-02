package com.bigo143.budgettracker;

import static com.bigo143.budgettracker.MainActivity.staticListener;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

public class calcu_add extends AppCompatActivity {

    private TextView tvResult, tvIncome, tvTransfer, tvExpense;
    private EditText etNotes;
    private AutoCompleteTextView ddAccount, ddCategory;
    private TextInputLayout lyAccount, lyCategory;

    private TextView tvDatePicker, tvTimePicker;

    private enum TxType { INCOME, TRANSFER, EXPENSE }
    private TxType currentType = TxType.EXPENSE;

    private final StringBuilder expression = new StringBuilder();
    private final Calendar calendar = Calendar.getInstance();

    private DatabaseHelper db;
    private String loggedInUser;

    private final List<String> accounts = new ArrayList<>();
    private final List<String> incomeCats = new ArrayList<>();
    private final List<String> expenseCats = new ArrayList<>();

    public static OnTransactionSavedListener staticListener;
    public interface OnTransactionSavedListener {
        void onTransactionSaved();
    }

    private OnTransactionSavedListener listener;

    public void setOnTransactionSavedListener(OnTransactionSavedListener listener) {
        this.listener = listener;
    }




    // ***********************************************
    //  ACTIVITIES
    // ***********************************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.calcu_add_expenseincome);

        // Get logged in username
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        loggedInUser = prefs.getString("logged_in_user", null);

        db = new DatabaseHelper(this);

        initViews();
        setupInsets();
        loadData();
        setupDropdowns();
        setupTypeButtons();
        setupCalculatorButtons();
        setupSaveCancel();
        setupDateTimePickers();
        addInputWatchers();
    }

    private void initViews() {
        tvResult = findViewById(R.id.result);
        etNotes = findViewById(R.id.notes_edittext);

        ddAccount = findViewById(R.id.account_dropdown);
        ddCategory = findViewById(R.id.category_dropdown);

        lyAccount = findViewById(R.id.account_dropdown_layout);
        lyCategory = findViewById(R.id.category_dropdown_layout);

        tvIncome = findViewById(R.id.text_income);
        tvTransfer = findViewById(R.id.text_transfer);
        tvExpense = findViewById(R.id.text_Expense);

        tvDatePicker = findViewById(R.id.date_picker_text);
        tvTimePicker = findViewById(R.id.time_picker_text);
    }

    private void setupInsets() {
        ConstraintLayout main = findViewById(R.id.main_layout);
        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });
    }

    // ***********************************************
    //  LOAD DB DATA
    // ***********************************************
    private void loadData() {
        accounts.clear();
        incomeCats.clear();
        expenseCats.clear();

        Cursor a = db.getAccounts(loggedInUser);
        if (a != null && a.moveToFirst()) {
            do accounts.add(a.getString(a.getColumnIndexOrThrow("name")));
            while (a.moveToNext());
            a.close();
        }

        Cursor in = db.getCategoriesByType(loggedInUser, "income");
        if (in != null && in.moveToFirst()) {
            do incomeCats.add(in.getString(in.getColumnIndexOrThrow("name")));
            while (in.moveToNext());
            in.close();
        }

        Cursor ex = db.getCategoriesByType(loggedInUser, "expense");
        if (ex != null && ex.moveToFirst()) {
            do expenseCats.add(ex.getString(ex.getColumnIndexOrThrow("name")));
            while (ex.moveToNext());
            ex.close();
        }
    }

    private void setupDropdowns() {
        setAdapter(ddAccount, accounts);
        setAdapter(ddCategory, expenseCats); // default type = EXPENSE
    }

    private void setAdapter(AutoCompleteTextView view, List<String> list) {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, list);
        view.setAdapter(adapter);
    }

    // ***********************************************
    //  TRANSACTION TYPE UI
    // ***********************************************
    private void setupTypeButtons() {
        tvIncome.setOnClickListener(v -> switchType(TxType.INCOME));
        tvTransfer.setOnClickListener(v -> switchType(TxType.TRANSFER));
        tvExpense.setOnClickListener(v -> switchType(TxType.EXPENSE));

        switchType(TxType.EXPENSE);
    }

    private void switchType(TxType type) {
        currentType = type;

        // reset highlights
        tvIncome.setBackgroundColor(0);
        tvTransfer.setBackgroundColor(0);
        tvExpense.setBackgroundColor(0);

        int hl = ContextCompat.getColor(this, R.color.third);

        switch (type) {
            case INCOME:
                tvIncome.setBackgroundColor(hl);
                lyCategory.setHint("Select Category");
                setAdapter(ddCategory, incomeCats);
                setAdapter(ddAccount, accounts);
                break;

            case TRANSFER:
                tvTransfer.setBackgroundColor(hl);
                lyCategory.setHint("To Account");
                setAdapter(ddCategory, accounts);
                setAdapter(ddAccount, accounts);
                break;

            case EXPENSE:
                tvExpense.setBackgroundColor(hl);
                lyCategory.setHint("Select Category");
                setAdapter(ddCategory, expenseCats);
                setAdapter(ddAccount, accounts);
                break;
        }
    }

    // ***********************************************
    //  SAVE TRANSACTION
    // ***********************************************
    private void setupSaveCancel() {
        findViewById(R.id.save_button).setOnClickListener(v -> save());
        findViewById(R.id.cancel_button).setOnClickListener(v -> finish());
    }

    private void save() {
        if (!validate()) return;

        double amount = Double.parseDouble(tvResult.getText().toString());
        String fromAcc = ddAccount.getText().toString();
        String categoryOrTarget = ddCategory.getText().toString();
        String note = etNotes.getText().toString();

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(calendar.getTime());

        boolean ok;

        switch (currentType) {
            case INCOME:
                int incId = db.getCategoryIdByName(loggedInUser, categoryOrTarget, "income");
                int incomeAccId = db.getAccountIdByName(loggedInUser, fromAcc); // account to add income
                if (incId == -1 || incomeAccId == -1) {
                    Toast.makeText(this, "Income category or account not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                ok = db.insertRecord(loggedInUser, incId, incomeAccId, "income", amount, timestamp, note);
                break;

            case EXPENSE:
                int expId = db.getCategoryIdByName(loggedInUser, categoryOrTarget, "expense");
                int expenseAccId = db.getAccountIdByName(loggedInUser, fromAcc); // account to deduct from
                if (expId == -1 || expenseAccId == -1) {
                    Toast.makeText(this, "Expense category or account not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                double balance = db.getAccountBalance(expenseAccId, loggedInUser); // correct balance check

                if (balance < amount) {
                    Toast.makeText(this, "Insufficient funds in " + fromAcc, Toast.LENGTH_SHORT).show();
                    return;
                }

                ok = db.insertRecord(loggedInUser, expId, expenseAccId, "expense", amount, timestamp, note);
                break;

            case TRANSFER:
                int aFrom = db.getAccountIdByName(loggedInUser, fromAcc);
                int aTo = db.getAccountIdByName(loggedInUser, categoryOrTarget);

                if (aFrom == -1 || aTo == -1) {
                    Toast.makeText(this, "Account not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                double fromBalance = db.getAccountBalance(aFrom, loggedInUser);

                if (fromBalance < amount) {
                    Toast.makeText(this, "Insufficient funds in " + fromAcc, Toast.LENGTH_SHORT).show();
                    return;
                }

                ok = db.insertTransfer(loggedInUser, aFrom, aTo, amount, timestamp, note);
                break;

            default:
                ok = false;
        }


        if (!ok) {
            Toast.makeText(this, "Saving failed", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Transaction Saved!", Toast.LENGTH_SHORT).show();
        // Notify listener
        if (listener != null) listener.onTransactionSaved();
        if (staticListener != null) staticListener.onTransactionSaved();
        finishWithUpdate();
        //finish();


    }


    private boolean validate() {
        lyAccount.setError(null);
        lyCategory.setError(null);

        String acc = ddAccount.getText().toString();
        String cat = ddCategory.getText().toString();
        String amount = tvResult.getText().toString();

        if (acc.isEmpty()) {
            lyAccount.setError("Please select an account");
            return false;
        }

        if (currentType == TxType.TRANSFER) {
            if (cat.isEmpty()) {
                lyCategory.setError("Select target account");
                return false;
            }
            if (acc.equals(cat)) {
                lyCategory.setError("Cannot transfer to same account");
                return false;
            }
        } else {
            if (cat.isEmpty()) {
                lyCategory.setError("Please select category");
                return false;
            }
        }

        if (amount.equals("0") || amount.isEmpty()) {
            Toast.makeText(this, "Amount cannot be zero", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // ***********************************************
    //  DATE & TIME PICKERS
    // ***********************************************
    private void setupDateTimePickers() {
        updateDateTimeLabels();

        tvDatePicker.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, y, m, d) -> {
                calendar.set(y, m, d);
                updateDateTimeLabels();
            }, calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        tvTimePicker.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, h, m) -> {
                calendar.set(Calendar.HOUR_OF_DAY, h);
                calendar.set(Calendar.MINUTE, m);
                updateDateTimeLabels();
            }, calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false).show();
        });
    }

    private void updateDateTimeLabels() {
        tvDatePicker.setText(new SimpleDateFormat("MM/dd/yyyy", Locale.US)
                .format(calendar.getTime()));

        tvTimePicker.setText(new SimpleDateFormat("hh:mm a", Locale.US)
                .format(calendar.getTime()));
    }

    // ***********************************************
    //  CALCULATOR LOGIC
    // ***********************************************
    private void setupCalculatorButtons() {
        int[] numbers = {
                R.id.button_0, R.id.button_1, R.id.button_2, R.id.button_3,
                R.id.button_4, R.id.button_5, R.id.button_6, R.id.button_7,
                R.id.button_8, R.id.button_9, R.id.button_00, R.id.button_dot
        };
        for (int id : numbers)
            findViewById(id).setOnClickListener(this::numClick);

        int[] ops = {R.id.button_add, R.id.button_subtract, R.id.button_multiply, R.id.button_divide};
        for (int id : ops)
            findViewById(id).setOnClickListener(this::opClick);

        findViewById(R.id.button_equal).setOnClickListener(this::equalClick);
        findViewById(R.id.clear_button).setOnClickListener(v -> { expression.setLength(0); updateResult(); });
        findViewById(R.id.back_button).setOnClickListener(v -> {
            if (expression.length() > 0) expression.deleteCharAt(expression.length() - 1);
            updateResult();
        });
    }

    private void numClick(View v) {
        expression.append(((Button)v).getText());
        updateResult();
    }

    private void opClick(View v) {
        char op = ((Button)v).getText().charAt(0);

        if (expression.length() == 0) {
            if (op == '-') expression.append(op);
            updateResult();
            return;
        }

        char last = expression.charAt(expression.length() - 1);
        if (isOp(last)) expression.setCharAt(expression.length() - 1, op);
        else expression.append(op);

        updateResult();
    }

    private void equalClick(View v) {
        try {
            double r = evaluate(expression.toString());
            String displayed = (r == (long)r) ? String.valueOf((long)r) : String.format("%.2f", r);

            tvResult.setText(displayed);
            expression.setLength(0);
            expression.append(displayed);

        } catch (Exception e) {
            tvResult.setText("Error");
            expression.setLength(0);
        }
    }

    private void updateResult() {
        tvResult.setText(expression.length() == 0 ? "0" : expression.toString());
    }

    private boolean isOp(char c) { return c=='+'||c=='-'||c=='*'||c=='/'; }

    // Expression evaluator (same but formatted)
    public double evaluate(String exp) {
        char[] arr = exp.toCharArray();
        Stack<Double> vals = new Stack<>();
        Stack<Character> ops = new Stack<>();

        for (int i = 0; i < arr.length; i++) {
            if ((arr[i] >= '0' && arr[i] <= '9') || arr[i] == '.') {
                StringBuilder sb = new StringBuilder();
                while (i < arr.length && ((arr[i]>='0'&&arr[i]<='9')||arr[i]=='.'))
                    sb.append(arr[i++]);
                i--;
                vals.push(Double.parseDouble(sb.toString()));
            }
            else if (isOp(arr[i])) {
                if (i == 0 && arr[i] == '-') {
                    StringBuilder sb = new StringBuilder("-");
                    i++;
                    while (i < arr.length && ((arr[i]>='0'&&arr[i]<='9')||arr[i]=='.'))
                        sb.append(arr[i++]);
                    i--;
                    vals.push(Double.parseDouble(sb.toString()));
                    continue;
                }

                while (!ops.empty() && hasPrec(arr[i], ops.peek()))
                    vals.push(applyOp(ops.pop(), vals.pop(), vals.pop()));

                ops.push(arr[i]);
            }
        }

        while (!ops.empty())
            vals.push(applyOp(ops.pop(), vals.pop(), vals.pop()));

        return vals.pop();
    }

    private boolean hasPrec(char a, char b) {
        return (b!='('&&b!=')') && ((a!='*'&&a!='/') || (b!='+'&&b!='-'));
    }

    private double applyOp(char op, double b, double a) {
        switch (op) {
            case '+': return a+b;
            case '-': return a-b;
            case '*': return a*b;
            case '/': if (b==0) throw new RuntimeException(); return a/b;
        }
        return 0;
    }

    // ***********************************************
    //  INPUT ERROR CLEAR
    // ***********************************************
    private void addInputWatchers() {
        ddAccount.addTextChangedListener(simpleWatcher(() -> lyAccount.setError(null)));
        ddCategory.addTextChangedListener(simpleWatcher(() -> lyCategory.setError(null)));
    }

    private TextWatcher simpleWatcher(Runnable r) {
        return new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){ r.run(); }
            public void afterTextChanged(Editable s){}
        };
    }
    private void finishWithUpdate() {
        setResult(RESULT_OK); // signal MainActivity that a change happened
        finish();
    }








}

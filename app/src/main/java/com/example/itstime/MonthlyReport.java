package com.example.itstime;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MonthlyReport extends AppCompatActivity {

    private TextView monthYearText, totalReminders, completedReminders, pendingReminders, completionPercentage;
    private Button btnPreviousMonth, btnNextMonth;
    private LinearLayout remindersTableLayout;
    private ProgressBar completionProgressBar;

    private AppDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private Calendar currentMonth;
    private List<Reminder> monthlyReminders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monthly_report);

        initializeViews();
        initializeMonth();
        setupClickListeners();
        loadMonthlyData();
    }

    private void initializeViews() {
        monthYearText = findViewById(R.id.monthYearText);
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        remindersTableLayout = findViewById(R.id.remindersTableLayout);
        totalReminders = findViewById(R.id.totalReminders);
        completedReminders = findViewById(R.id.completedReminders);
        pendingReminders = findViewById(R.id.pendingReminders);
        completionPercentage = findViewById(R.id.completionPercentage);
        completionProgressBar = findViewById(R.id.completionProgressBar);

        db = AppDatabase.getInstance(this);
    }

    private void initializeMonth() {
        // Get current date (August 2025)
        currentMonth = Calendar.getInstance();

        // Keep the current month and year, just reset day/time for consistency
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        currentMonth.set(Calendar.HOUR_OF_DAY, 0);
        currentMonth.set(Calendar.MINUTE, 0);
        currentMonth.set(Calendar.SECOND, 0);
        currentMonth.set(Calendar.MILLISECOND, 0);

        updateMonthDisplay();
    }

    private void setupClickListeners() {
        btnPreviousMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadMonthlyData();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadMonthlyData();
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String monthYear = monthYearFormat.format(currentMonth.getTime());
        monthYearText.setText(monthYear);
    }

    private void loadMonthlyData() {
        executor.execute(() -> {
            // Get all reminders from database
            List<Reminder> allReminders = db.reminderDao().getAllReminders();
            List<Reminder> completedRemindersList = db.reminderDao().getCompletedReminders();

            // Combine both lists
            List<Reminder> allCombined = new ArrayList<>();
            allCombined.addAll(allReminders);
            allCombined.addAll(completedRemindersList);

            monthlyReminders.clear();

            // Filter reminders for current month
            for (Reminder reminder : allCombined) {
                // Check if reminder is in the same month and year
                if (reminder.year == currentMonth.get(Calendar.YEAR) &&
                        reminder.month == currentMonth.get(Calendar.MONTH)) {
                    monthlyReminders.add(reminder);
                }
            }

            runOnUiThread(() -> {
                populateTable();
                updateSummary();
            });
        });
    }

    private void populateTable() {
        remindersTableLayout.removeAllViews();

        if (monthlyReminders.isEmpty()) {
            // Show empty state
            LinearLayout emptyRow = new LinearLayout(this);
            emptyRow.setOrientation(LinearLayout.HORIZONTAL);
            emptyRow.setPadding(16, 32, 16, 32);
            emptyRow.setGravity(android.view.Gravity.CENTER);

            TextView emptyView = new TextView(this);
            emptyView.setText("No reminders found for this month");
            emptyView.setTextSize(16);
            emptyView.setTextColor(Color.parseColor("#6C757D"));
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyRow.addView(emptyView);

            remindersTableLayout.addView(emptyRow);
            return;
        }

        for (int i = 0; i < monthlyReminders.size(); i++) {
            Reminder reminder = monthlyReminders.get(i);
            addTableRow(i + 1, reminder);
        }
    }

    private void addTableRow(int number, Reminder reminder) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 16, 16, 16);
        row.setBackgroundColor(Color.parseColor("#FFFFFF"));

        // Add subtle divider line between rows
        if (number > 1) {
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(Color.parseColor("#F1F3F4"));
            remindersTableLayout.addView(divider);
        }

        // Number column
        TextView numberView = new TextView(this);
        numberView.setText(String.valueOf(number));
        numberView.setTextSize(14);
        numberView.setTextColor(Color.parseColor("#495057"));
        numberView.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f);
        numberView.setLayoutParams(numberParams);
        row.addView(numberView);

        // Title column
        TextView titleView = new TextView(this);
        titleView.setText(reminder.title != null ? reminder.title : "No Title");
        titleView.setTextSize(14);
        titleView.setTextColor(Color.parseColor("#212529"));
        titleView.setPadding(8, 0, 8, 0);
        titleView.setMaxLines(2);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 4.2f);
        titleView.setLayoutParams(titleParams);
        row.addView(titleView);

        // Status column with professional badges
        LinearLayout statusContainer = new LinearLayout(this);
        statusContainer.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams statusContainerParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 2.0f);
        statusContainer.setLayoutParams(statusContainerParams);

        TextView statusView = new TextView(this);
        statusView.setPadding(12, 6, 12, 6);
        statusView.setTextSize(12);

        if (reminder.completed) {
            statusView.setText("Completed");
            statusView.setTextColor(Color.parseColor("#155724"));
            statusView.setBackgroundColor(Color.parseColor("#D4EDDA"));
        } else {
            statusView.setText("Pending");
            statusView.setTextColor(Color.parseColor("#856404"));
            statusView.setBackgroundColor(Color.parseColor("#FFF3CD"));
        }

        // Create rounded background programmatically
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(12f);
        if (reminder.completed) {
            drawable.setColor(Color.parseColor("#D4EDDA"));
        } else {
            drawable.setColor(Color.parseColor("#FFF3CD"));
        }
        statusView.setBackground(drawable);

        statusContainer.addView(statusView);
        row.addView(statusContainer);

        remindersTableLayout.addView(row);
    }

    private void updateSummary() {
        int total = monthlyReminders.size();
        int completed = 0;

        for (Reminder reminder : monthlyReminders) {
            if (reminder.completed) {
                completed++;
            }
        }

        int pending = total - completed;
        double completionRate = total > 0 ? (completed * 100.0) / total : 0;

        totalReminders.setText(String.valueOf(total));
        completedReminders.setText(String.valueOf(completed));
        pendingReminders.setText(String.valueOf(pending));

        completionProgressBar.setMax(100);
        completionProgressBar.setProgress((int) completionRate);

        completionPercentage.setText(String.format(Locale.getDefault(),
                "%.0f%%", completionRate));

        // Professional color scheme for completion rate
        if (completionRate >= 90) {
            completionPercentage.setTextColor(Color.parseColor("#28A745")); // Success green
        } else if (completionRate >= 70) {
            completionPercentage.setTextColor(Color.parseColor("#17A2B8")); // Info blue
        } else if (completionRate >= 40) {
            completionPercentage.setTextColor(Color.parseColor("#FFC107")); // Warning yellow
        } else {
            completionPercentage.setTextColor(Color.parseColor("#DC3545")); // Danger red
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
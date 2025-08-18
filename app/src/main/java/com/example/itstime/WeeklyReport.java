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

public class WeeklyReport extends AppCompatActivity {

    private TextView weekDateRange, totalReminders, completedReminders, pendingReminders, completionPercentage;
    private Button btnPreviousWeek, btnNextWeek;
    private LinearLayout remindersTableLayout;
    private ProgressBar completionProgressBar;

    private AppDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private Calendar currentWeekStart;
    private List<Reminder> weeklyReminders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weekly_report);

        initializeViews();
        initializeWeek();
        setupClickListeners();
        loadWeeklyData();
    }

    private void initializeViews() {
        weekDateRange = findViewById(R.id.weekDateRange);
        btnPreviousWeek = findViewById(R.id.btnPreviousWeek);
        btnNextWeek = findViewById(R.id.btnNextWeek);
        remindersTableLayout = findViewById(R.id.remindersTableLayout);
        totalReminders = findViewById(R.id.totalReminders);
        completedReminders = findViewById(R.id.completedReminders);
        pendingReminders = findViewById(R.id.pendingReminders);
        completionPercentage = findViewById(R.id.completionPercentage);
        completionProgressBar = findViewById(R.id.completionProgressBar);

        db = AppDatabase.getInstance(this);
    }

    private void initializeWeek() {
        currentWeekStart = Calendar.getInstance();

        // Get current date to determine which week of the month we're in
        Calendar today = Calendar.getInstance();
        int currentMonth = today.get(Calendar.MONTH);
        int currentYear = today.get(Calendar.YEAR);

        // Set to first day of current month
        currentWeekStart.set(Calendar.YEAR, currentYear);
        currentWeekStart.set(Calendar.MONTH, currentMonth);
        currentWeekStart.set(Calendar.DAY_OF_MONTH, 1);
        currentWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        currentWeekStart.set(Calendar.MINUTE, 0);
        currentWeekStart.set(Calendar.SECOND, 0);
        currentWeekStart.set(Calendar.MILLISECOND, 0);

        // Calculate which week of the month today falls into
        int todayDayOfMonth = today.get(Calendar.DAY_OF_MONTH);
        int currentWeekNumber = ((todayDayOfMonth - 1) / 7) + 1;

        // Set to start of current week within the month
        int daysToAdd = (currentWeekNumber - 1) * 7;
        currentWeekStart.add(Calendar.DAY_OF_MONTH, daysToAdd);

        updateWeekDisplay();
    }

    private void setupClickListeners() {
        btnPreviousWeek.setOnClickListener(v -> {
            // Move to previous week within the month
            int currentWeekStart = this.currentWeekStart.get(Calendar.DAY_OF_MONTH);

            if (currentWeekStart > 7) {
                // Go to previous week in same month
                this.currentWeekStart.add(Calendar.DAY_OF_MONTH, -7);
            } else {
                // Go to previous month, last week
                this.currentWeekStart.add(Calendar.MONTH, -1);
                int maxDay = this.currentWeekStart.getActualMaximum(Calendar.DAY_OF_MONTH);
                int lastWeekStart = ((maxDay - 1) / 7) * 7 + 1;
                this.currentWeekStart.set(Calendar.DAY_OF_MONTH, lastWeekStart);
            }

            updateWeekDisplay();
            loadWeeklyData();
        });

        btnNextWeek.setOnClickListener(v -> {
            // Move to next week within the month
            int currentWeekStart = this.currentWeekStart.get(Calendar.DAY_OF_MONTH);
            int maxDayOfMonth = this.currentWeekStart.getActualMaximum(Calendar.DAY_OF_MONTH);

            if (currentWeekStart + 7 <= maxDayOfMonth) {
                // Go to next week in same month
                this.currentWeekStart.add(Calendar.DAY_OF_MONTH, 7);
            } else {
                // Go to next month, first week
                this.currentWeekStart.add(Calendar.MONTH, 1);
                this.currentWeekStart.set(Calendar.DAY_OF_MONTH, 1);
            }

            updateWeekDisplay();
            loadWeeklyData();
        });
    }

    private void updateWeekDisplay() {
        Calendar weekEnd = (Calendar) currentWeekStart.clone();

        // Calculate week end (6 days after start, but don't exceed month end)
        int maxDayOfMonth = currentWeekStart.getActualMaximum(Calendar.DAY_OF_MONTH);
        int weekStartDay = currentWeekStart.get(Calendar.DAY_OF_MONTH);
        int weekEndDay = Math.min(weekStartDay + 6, maxDayOfMonth);

        weekEnd.set(Calendar.DAY_OF_MONTH, weekEndDay);

        // Calculate week number within the month
        int weekNumber = ((weekStartDay - 1) / 7) + 1;

        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String monthYear = monthYearFormat.format(currentWeekStart.getTime());

        String weekText = String.format(Locale.getDefault(),
                "Week %d (%d-%d %s)",
                weekNumber,
                weekStartDay,
                weekEndDay,
                monthYear);

        weekDateRange.setText(weekText);
    }

    private void loadWeeklyData() {
        executor.execute(() -> {
            // Get all reminders from database
            List<Reminder> allReminders = db.reminderDao().getAllReminders();
            List<Reminder> completedRemindersList = db.reminderDao().getCompletedReminders();

            // Combine both lists
            List<Reminder> allCombined = new ArrayList<>();
            allCombined.addAll(allReminders);
            allCombined.addAll(completedRemindersList);

            weeklyReminders.clear();

            // Calculate week end (6 days after start, but don't exceed month end)
            Calendar weekEnd = (Calendar) currentWeekStart.clone();
            int maxDayOfMonth = currentWeekStart.getActualMaximum(Calendar.DAY_OF_MONTH);
            int weekStartDay = currentWeekStart.get(Calendar.DAY_OF_MONTH);
            int weekEndDay = Math.min(weekStartDay + 6, maxDayOfMonth);
            weekEnd.set(Calendar.DAY_OF_MONTH, weekEndDay);

            // Filter reminders for current week
            for (Reminder reminder : allCombined) {
                // Check if reminder is in the same month and year
                if (reminder.year == currentWeekStart.get(Calendar.YEAR) &&
                        reminder.month == currentWeekStart.get(Calendar.MONTH)) {

                    // Check if reminder day falls within the week range
                    if (reminder.day >= weekStartDay && reminder.day <= weekEndDay) {
                        weeklyReminders.add(reminder);
                    }
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

        if (weeklyReminders.isEmpty()) {
            // Show empty state
            LinearLayout emptyRow = new LinearLayout(this);
            emptyRow.setOrientation(LinearLayout.HORIZONTAL);
            emptyRow.setPadding(16, 32, 16, 32);
            emptyRow.setGravity(android.view.Gravity.CENTER);

            TextView emptyView = new TextView(this);
            emptyView.setText("No reminders found for this week");
            emptyView.setTextSize(16);
            emptyView.setTextColor(Color.parseColor("#6C757D"));
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyRow.addView(emptyView);

            remindersTableLayout.addView(emptyRow);
            return;
        }

        for (int i = 0; i < weeklyReminders.size(); i++) {
            Reminder reminder = weeklyReminders.get(i);
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
                LinearLayout.LayoutParams.WRAP_CONTENT, 3.5f);
        titleView.setLayoutParams(titleParams);
        row.addView(titleView);

        // Date column
        TextView dateView = new TextView(this);
        dateView.setText(String.format(Locale.getDefault(), "%d/%d",
                reminder.day, reminder.month + 1));
        dateView.setTextSize(13);
        dateView.setTextColor(Color.parseColor("#6C757D"));
        dateView.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.8f);
        dateView.setLayoutParams(dateParams);
        row.addView(dateView);

        // Status column with professional badges
        LinearLayout statusContainer = new LinearLayout(this);
        statusContainer.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams statusContainerParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.9f);
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
        int total = weeklyReminders.size();
        int completed = 0;

        for (Reminder reminder : weeklyReminders) {
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
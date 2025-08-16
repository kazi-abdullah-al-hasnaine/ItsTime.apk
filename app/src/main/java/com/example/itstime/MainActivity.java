package com.example.itstime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    TextView todayCount, scheduledCount, allCount, completedCount;
    Button addReminderButton;
    AppDatabase db;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        todayCount = findViewById(R.id.today_count);
        scheduledCount = findViewById(R.id.scheduled_count);
        allCount = findViewById(R.id.all_count);
        completedCount = findViewById(R.id.completed_count);
        addReminderButton = findViewById(R.id.add_reminder_button);

        db = AppDatabase.getInstance(this);

        // Open AddReminderActivity
        addReminderButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddReminderActivity.class);
            startActivity(intent);
        });

        // Open filtered reminder pages
        findViewById(R.id.cardToday).setOnClickListener(v -> openReminderList("Today"));
        findViewById(R.id.cardScheduled).setOnClickListener(v -> openReminderList("Scheduled"));
        findViewById(R.id.cardAll).setOnClickListener(v -> openReminderList("All"));
        findViewById(R.id.cardCompleted).setOnClickListener(v -> openReminderList("Completed"));

        // Load counts initially
        loadReminderCounts();
    }

    private void openReminderList(String filter) {
        Intent intent = new Intent(MainActivity.this, ReminderListActivity.class);
        intent.putExtra("filter", filter);
        startActivity(intent);
    }

    /**
     * Updated: Proper counts excluding completed from "All" and separating Today/Scheduled
     */
    private void loadReminderCounts() {
        executor.execute(() -> {
            List<Reminder> allReminders = db.reminderDao().getAllReminders(); // Only not completed
            List<Reminder> completedReminders = db.reminderDao().getCompletedReminders();

            int today = 0;
            int scheduled = 0;
            int all = 0;
            int completed = completedReminders.size();

            Calendar now = Calendar.getInstance();
            int currentDay = now.get(Calendar.DAY_OF_MONTH);
            int currentMonth = now.get(Calendar.MONTH);
            int currentYear = now.get(Calendar.YEAR);

            for (Reminder r : allReminders) {
                // Count Today
                if (r.day == currentDay && r.month == currentMonth && r.year == currentYear) {
                    today++;
                }
                // Count Scheduled (future or not today)
                else {
                    scheduled++;
                }
            }

            // All = Today + Scheduled (excluding completed)
            all = today + scheduled;

            int finalToday = today;
            int finalScheduled = scheduled;
            int finalAll = all;
            int finalCompleted = completed;

            // Update UI on main thread
            runOnUiThread(() -> {
                todayCount.setText(String.valueOf(finalToday));
                scheduledCount.setText(String.valueOf(finalScheduled));
                allCount.setText(String.valueOf(finalAll));
                completedCount.setText(String.valueOf(finalCompleted));
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh counts every time user returns to main page
        loadReminderCounts();
    }
}

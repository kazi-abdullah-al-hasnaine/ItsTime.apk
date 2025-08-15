package com.example.itstime;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderListActivity extends AppCompatActivity {

    RecyclerView reminderRecyclerView;
    ReminderAdapter reminderAdapter;
    TextView filterTitle;
    AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_list);

        reminderRecyclerView = findViewById(R.id.reminderRecyclerView);
        filterTitle = findViewById(R.id.filterTitle);
        db = AppDatabase.getInstance(this); // get database instance

        // Get filter from Intent
        String filter = getIntent().getStringExtra("filter");
        filterTitle.setText(filter);

        loadReminders(filter); // Load reminders from DB
    }

    /**
     * Load reminders from database based on filter
     */
    private void loadReminders(String filter) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Reminder> reminders;

            switch (filter) {
                case "Completed":
                    reminders = db.reminderDao().getCompletedReminders();
                    break;
                case "Today":
                case "Scheduled":
                case "All":
                default:
                    reminders = db.reminderDao().getAllReminders(); // Only uncompleted
                    break;
            }

            // Filter Today or Scheduled if needed
            if ("Today".equals(filter)) {
                reminders.removeIf(r -> !isToday(r));
            } else if ("Scheduled".equals(filter)) {
                reminders.removeIf(r -> isToday(r));
            }

            runOnUiThread(() -> {
                reminderAdapter = new ReminderAdapter(this, reminders, filter);
                reminderRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                reminderRecyclerView.setAdapter(reminderAdapter);
            });
        });
        executor.shutdown();
    }

    /**
     * Check if reminder is today
     */
    private boolean isToday(Reminder r) {
        java.util.Calendar today = java.util.Calendar.getInstance();
        return r.day == today.get(java.util.Calendar.DAY_OF_MONTH) &&
                r.month == today.get(java.util.Calendar.MONTH) &&
                r.year == today.get(java.util.Calendar.YEAR);
    }
}

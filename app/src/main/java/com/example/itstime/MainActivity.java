package com.example.itstime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView todayCount, scheduledCount, allCount, completedCount;
    Button addReminderButton;

    private List<Reminder> reminderList = new ArrayList<>();
    private static final int ADD_REMINDER_REQUEST = 1;

    // CardViews for Today, Scheduled, All
    CardView cardToday, cardScheduled, cardAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Link UI elements
        todayCount = findViewById(R.id.today_count);
        scheduledCount = findViewById(R.id.scheduled_count);
        allCount = findViewById(R.id.all_count);
        completedCount = findViewById(R.id.completed_count);
        addReminderButton = findViewById(R.id.add_reminder_button);

        cardToday = findViewById(R.id.cardToday);
        cardScheduled = findViewById(R.id.cardScheduled);
        cardAll = findViewById(R.id.cardAll);

        // Load reminders from database
        loadReminders();

        // Click listener for Add Reminder button
        addReminderButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddReminderActivity.class);
            startActivityForResult(intent, ADD_REMINDER_REQUEST);
        });

        // --- NEW: Click listeners for cards ---
        cardToday.setOnClickListener(v -> openReminderList("Today"));
        cardScheduled.setOnClickListener(v -> openReminderList("Scheduled"));
        cardAll.setOnClickListener(v -> openReminderList("All"));
    }

    // Method to start ReminderListActivity with a filter
    private void openReminderList(String filter) {
        Intent intent = new Intent(MainActivity.this, ReminderListActivity.class);
        intent.putExtra("filter", filter); // pass the filter
        startActivity(intent);
    }

    // Load reminders from database in background
    private void loadReminders() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            reminderList = db.reminderDao().getAllReminders();

            runOnUiThread(this::updateCounts);
        }).start();
    }

    // Calculate counts for Today, Scheduled, All
    private void updateCounts() {
        Calendar today = Calendar.getInstance();
        int todayYear = today.get(Calendar.YEAR);
        int todayMonth = today.get(Calendar.MONTH);
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        int todayCountValue = 0;
        int scheduledCountValue = 0;
        int allCountValue = reminderList.size();

        for (Reminder r : reminderList) {
            Calendar reminderDate = Calendar.getInstance();
            reminderDate.set(r.year, r.month, r.day);

            if (r.year == todayYear && r.month == todayMonth && r.day == todayDay) {
                todayCountValue++;
                scheduledCountValue++;
            } else if (reminderDate.after(today)) {
                scheduledCountValue++;
            }
        }

        // Update UI counts
        todayCount.setText(String.valueOf(todayCountValue));
        scheduledCount.setText(String.valueOf(scheduledCountValue));
        allCount.setText(String.valueOf(allCountValue));
        completedCount.setText("0"); // placeholder
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_REMINDER_REQUEST) {
            loadReminders(); // reload after adding reminder
        }
    }
}

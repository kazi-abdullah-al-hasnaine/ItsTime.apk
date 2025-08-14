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

    CardView cardToday, cardScheduled, cardAll, cardCompleted;

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
        cardCompleted = findViewById(R.id.cardCompleted);

        // Load reminders from database
        loadReminders();

        // Add Reminder button
        addReminderButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddReminderActivity.class);
            startActivityForResult(intent, ADD_REMINDER_REQUEST);
        });

        // Card click listeners
        cardToday.setOnClickListener(v -> openReminderList("Today"));
        cardScheduled.setOnClickListener(v -> openReminderList("Scheduled"));
        cardAll.setOnClickListener(v -> openReminderList("All"));
        cardCompleted.setOnClickListener(v -> openReminderList("Completed")); // new
    }

    private void openReminderList(String filter) {
        Intent intent = new Intent(MainActivity.this, ReminderListActivity.class);
        intent.putExtra("filter", filter);
        startActivity(intent);
    }

    private void loadReminders() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            reminderList = db.reminderDao().getAllReminders(); // not completed
            List<Reminder> completedList = db.reminderDao().getCompletedReminders();

            runOnUiThread(() -> updateCounts(completedList));
        }).start();
    }

    private void updateCounts(List<Reminder> completedList) {
        Calendar today = Calendar.getInstance();
        int todayYear = today.get(Calendar.YEAR);
        int todayMonth = today.get(Calendar.MONTH);
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        int todayCountValue = 0;
        int scheduledCountValue = 0;
        int allCountValue = reminderList.size();
        int completedCountValue = completedList.size();

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

        todayCount.setText(String.valueOf(todayCountValue));
        scheduledCount.setText(String.valueOf(scheduledCountValue));
        allCount.setText(String.valueOf(allCountValue));
        completedCount.setText(String.valueOf(completedCountValue)); // updated
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_REMINDER_REQUEST) {
            loadReminders();
        }
    }
}

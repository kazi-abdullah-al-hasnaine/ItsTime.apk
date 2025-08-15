package com.example.itstime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    TextView todayCount, scheduledCount, allCount, completedCount;
    Button addReminderButton;
    AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        todayCount = findViewById(R.id.today_count);
        scheduledCount = findViewById(R.id.scheduled_count);
        allCount = findViewById(R.id.all_count);
        completedCount = findViewById(R.id.completed_count);

        addReminderButton = findViewById(R.id.add_reminder_button);

        db = AppDatabase.getInstance(this);

        loadCounts();

        addReminderButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddReminderActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.cardToday).setOnClickListener(v -> openReminderList("Today"));
        findViewById(R.id.cardScheduled).setOnClickListener(v -> openReminderList("Scheduled"));
        findViewById(R.id.cardAll).setOnClickListener(v -> openReminderList("All"));
        findViewById(R.id.cardCompleted).setOnClickListener(v -> openReminderList("Completed"));
    }

    private void openReminderList(String filter) {
        Intent intent = new Intent(MainActivity.this, ReminderListActivity.class);
        intent.putExtra("filter", filter);
        startActivity(intent);
    }

    private void loadCounts() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                List<Reminder> allReminders = db.reminderDao().getAllReminders(); // uncompleted
                List<Reminder> completedReminders = db.reminderDao().getCompletedReminders();

                int allCountNum = allReminders.size() + completedReminders.size();
                int completedCountNum = completedReminders.size();

                final int[] todayCountNum = {0};
                final int[] scheduledCountNum = {0};

                Calendar today = Calendar.getInstance();

                for (Reminder r : allReminders) {
                    boolean isToday = r.day == today.get(Calendar.DAY_OF_MONTH)
                            && r.month == today.get(Calendar.MONTH)
                            && r.year == today.get(Calendar.YEAR);
                    if (isToday) todayCountNum[0]++;
                    else scheduledCountNum[0]++;
                }

                runOnUiThread(() -> {
                    todayCount.setText(String.valueOf(todayCountNum[0]));
                    scheduledCount.setText(String.valueOf(scheduledCountNum[0]));
                    allCount.setText(String.valueOf(allCountNum));
                    completedCount.setText(String.valueOf(completedCountNum));
                });
            } finally {
                executor.shutdown(); // Close executor to avoid memory leaks
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCounts();
    }
}
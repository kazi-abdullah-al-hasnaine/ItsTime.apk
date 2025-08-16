package com.example.itstime;

import android.app.AlarmManager;
import android.app.PendingIntent;
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

        // ------------------------------
        // 1. Create Notification Channel
        // ------------------------------
        NotificationHelper.createNotificationChannel(this);

        // ------------------------------
        // 2. Initialize views
        // ------------------------------
        todayCount = findViewById(R.id.today_count);
        scheduledCount = findViewById(R.id.scheduled_count);
        allCount = findViewById(R.id.all_count);
        completedCount = findViewById(R.id.completed_count);
        addReminderButton = findViewById(R.id.add_reminder_button);

        db = AppDatabase.getInstance(this);

        // ------------------------------
        // 3. Button click to open AddReminderActivity
        // ------------------------------
        addReminderButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddReminderActivity.class);
            startActivity(intent);
        });

        // ------------------------------
        // 4. Card click events
        // ------------------------------
        findViewById(R.id.cardToday).setOnClickListener(v -> openReminderList("Today"));
        findViewById(R.id.cardScheduled).setOnClickListener(v -> openReminderList("Scheduled"));
        findViewById(R.id.cardAll).setOnClickListener(v -> openReminderList("All"));
        findViewById(R.id.cardCompleted).setOnClickListener(v -> openReminderList("Completed"));

        // ------------------------------
        // 5. Load counts initially
        // ------------------------------
        loadReminderCounts();

        // ------------------------------
        // 6. Schedule notifications for all future reminders
        // ------------------------------
        executor.execute(() -> {
            List<Reminder> reminders = db.reminderDao().getAllReminders();
            for (Reminder r : reminders) {
                Calendar cal = Calendar.getInstance();
                cal.set(r.year, r.month, r.day, r.hour, r.minute, 0);

                if (cal.getTimeInMillis() > System.currentTimeMillis()) {
                    scheduleNotificationForReminder(r, cal.getTimeInMillis());
                }
            }
        });
    }

    // ------------------------------
    // Open filtered reminder list
    // ------------------------------
    private void openReminderList(String filter) {
        Intent intent = new Intent(MainActivity.this, ReminderListActivity.class);
        intent.putExtra("filter", filter);
        startActivity(intent);
    }

    // ------------------------------
    // Load counts for Today, Scheduled, All, Completed reminders
    // ------------------------------
    private void loadReminderCounts() {
        executor.execute(() -> {
            List<Reminder> allReminders = db.reminderDao().getAllReminders();
            List<Reminder> completedReminders = db.reminderDao().getCompletedReminders();

            int today = 0, scheduled = 0, all = 0, completed = completedReminders.size();

            Calendar now = Calendar.getInstance();
            int currentDay = now.get(Calendar.DAY_OF_MONTH);
            int currentMonth = now.get(Calendar.MONTH);
            int currentYear = now.get(Calendar.YEAR);

            for (Reminder r : allReminders) {
                if (r.day == currentDay && r.month == currentMonth && r.year == currentYear) {
                    today++;
                } else {
                    scheduled++;
                }
            }

            all = today + scheduled;

            int finalToday = today;
            int finalScheduled = scheduled;
            int finalAll = all;
            int finalCompleted = completed;

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
        // Refresh counts smoothly
        loadReminderCounts();
    }

    // ------------------------------
    // Schedule notification for a single reminder
    // ------------------------------
    private void scheduleNotificationForReminder(Reminder r, long triggerTime) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", r.title);
        intent.putExtra("message", r.notes);
        intent.putExtra("notificationId", r.id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                r.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        }
    }
}

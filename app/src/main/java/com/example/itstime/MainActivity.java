package com.example.itstime;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    TextView todayCount, scheduledCount, allCount, completedCount;
    Button addReminderButton;
    AppDatabase db;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final int REQUEST_CODE_ADD_REMINDER = 1;
    private static final int REQUEST_CODE_VIEW_REMINDERS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermission();
        NotificationHelper.createNotificationChannel(this);

        todayCount = findViewById(R.id.today_count);
        scheduledCount = findViewById(R.id.scheduled_count);
        allCount = findViewById(R.id.all_count);
        completedCount = findViewById(R.id.completed_count);
        addReminderButton = findViewById(R.id.add_reminder_button);

        db = AppDatabase.getInstance(this);

        addReminderButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddReminderActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_REMINDER);
        });

        findViewById(R.id.cardToday).setOnClickListener(v -> openReminderList("Today"));
        findViewById(R.id.cardScheduled).setOnClickListener(v -> openReminderList("Scheduled"));
        findViewById(R.id.cardAll).setOnClickListener(v -> openReminderList("All"));
        findViewById(R.id.cardCompleted).setOnClickListener(v -> openReminderList("Completed"));

        loadReminderCounts();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void openReminderList(String filter) {
        Intent intent = new Intent(MainActivity.this, ReminderListActivity.class);
        intent.putExtra("filter", filter);
        startActivityForResult(intent, REQUEST_CODE_VIEW_REMINDERS);
    }

    public void loadReminderCounts() {
        executor.execute(() -> {
            List<Reminder> allReminders = db.reminderDao().getAllReminders();
            List<Reminder> completedReminders = db.reminderDao().getCompletedReminders();

            int today = 0, scheduled = 0, all = allReminders.size(), completed = completedReminders.size();

            Calendar now = Calendar.getInstance();
            for (Reminder r : allReminders) {
                if (!r.completed) {
                    if (r.day == now.get(Calendar.DAY_OF_MONTH) &&
                            r.month == now.get(Calendar.MONTH) &&
                            r.year == now.get(Calendar.YEAR)) {
                        today++;
                    } else {
                        scheduled++;
                    }
                }
            }

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
        loadReminderCounts();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadReminderCounts();
        }
    }
}
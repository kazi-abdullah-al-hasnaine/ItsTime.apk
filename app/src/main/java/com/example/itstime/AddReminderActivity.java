package com.example.itstime;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

public class AddReminderActivity extends AppCompatActivity {

    EditText titleEditText, notesEditText;
    TextView dateTextView, timeTextView;
    Button saveButton;

    int year, month, day, hour, minute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        titleEditText = findViewById(R.id.titleEditText);
        notesEditText = findViewById(R.id.notesEditText);
        dateTextView = findViewById(R.id.dateTextView);
        timeTextView = findViewById(R.id.timeTextView);
        saveButton = findViewById(R.id.saveButton);

        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);

        updateDateText();
        updateTimeText();

        // Date picker
        dateTextView.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(AddReminderActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        year = selectedYear;
                        month = selectedMonth;
                        day = selectedDay;
                        updateDateText();
                    }, year, month, day);
            datePickerDialog.show();
        });

        // Time picker
        timeTextView.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(AddReminderActivity.this,
                    (view, selectedHour, selectedMinute) -> {
                        hour = selectedHour;
                        minute = selectedMinute;
                        updateTimeText();
                    }, hour, minute, false);
            timePickerDialog.show();
        });

        // Save button
        saveButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String notes = notesEditText.getText().toString().trim();

            if (title.isEmpty()) {
                titleEditText.setError("Title required");
                return;
            }

            Reminder reminder = new Reminder(title, notes, year, month, day, hour, minute);

            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                db.reminderDao().insert(reminder);

                int notificationId = (int) System.currentTimeMillis();
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, day, hour, minute, 0);

                scheduleNotification(notificationId, title, notes, cal.getTimeInMillis());

                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            }).start();
        });
    }

    private void updateDateText() {
        dateTextView.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year));
    }

    private void updateTimeText() {
        String amPm = (hour >= 12) ? "PM" : "AM";
        int hour12 = hour % 12;
        if (hour12 == 0) hour12 = 12;
        timeTextView.setText(String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm));
    }

    private void scheduleNotification(int notificationId, String title, String message, long triggerAtMillis) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        }
    }
}

package com.example.itstime;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddReminderActivity extends AppCompatActivity {

    EditText titleInput, notesInput;
    TextView dateTextView, timeTextView;
    Button saveButton;
    Calendar calendar;
    AppDatabase db;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        titleInput = findViewById(R.id.titleEditText);
        notesInput = findViewById(R.id.notesEditText);
        dateTextView = findViewById(R.id.dateTextView);
        timeTextView = findViewById(R.id.timeTextView);
        saveButton = findViewById(R.id.saveButton);

        calendar = Calendar.getInstance();
        db = AppDatabase.getInstance(this);

        // Date picker
        dateTextView.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(this,
                    (DatePicker view, int year, int month, int dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dateTextView.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        // Time picker
        timeTextView.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(this,
                    (TimePicker view, int hourOfDay, int minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        calendar.set(Calendar.SECOND, 0);
                        timeTextView.setText(String.format("%02d:%02d", hourOfDay, minute));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true);
            timePicker.show();
        });

        // Save button
        saveButton.setOnClickListener(v -> {
            String title = titleInput.getText().toString();
            String notes = notesInput.getText().toString();

            Reminder reminder = new Reminder();
            reminder.title = title;
            reminder.notes = notes;
            reminder.year = calendar.get(Calendar.YEAR);
            reminder.month = calendar.get(Calendar.MONTH);
            reminder.day = calendar.get(Calendar.DAY_OF_MONTH);
            reminder.hour = calendar.get(Calendar.HOUR_OF_DAY);
            reminder.minute = calendar.get(Calendar.MINUTE);
            reminder.completed = false;

            executor.execute(() -> {
                // ✅ Insert returns long ID
                long id = db.reminderDao().insert(reminder);
                reminder.id = (int) id;

                // Schedule alarm
                scheduleAlarm(reminder);

                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            });
        });
    }

    private void scheduleAlarm(Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", reminder.title);
        intent.putExtra("message", reminder.notes);
        intent.putExtra("notificationId", reminder.id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                reminder.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            try {
                // ✅ Handle exact alarms properly
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }
}

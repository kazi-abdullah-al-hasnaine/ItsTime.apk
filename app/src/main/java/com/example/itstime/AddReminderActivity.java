package com.example.itstime;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddReminderActivity extends AppCompatActivity {

    EditText titleInput, notesInput;
    TextView dateTextView, timeTextView;
    CardView saveButton;
    LinearLayout dateContainer, timeContainer;
    ImageView backButton;
    Calendar calendar;
    AppDatabase db;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        initializeViews();
        setupClickListeners();

        calendar = Calendar.getInstance();
        db = AppDatabase.getInstance(this);
    }

    private void initializeViews() {
        titleInput = findViewById(R.id.titleEditText);
        notesInput = findViewById(R.id.notesEditText);
        dateTextView = findViewById(R.id.dateTextView);
        timeTextView = findViewById(R.id.timeTextView);
        saveButton = findViewById(R.id.saveButton);
        dateContainer = findViewById(R.id.dateContainer);
        timeContainer = findViewById(R.id.timeContainer);
        backButton = findViewById(R.id.backButton);
    }

    private void setupClickListeners() {
        // Back button - simplified
        backButton.setOnClickListener(v -> finish());

        // Date picker - click on entire date container
        dateContainer.setOnClickListener(v -> showDatePicker());

        // Time picker - click on entire time container
        timeContainer.setOnClickListener(v -> showTimePicker());

        // Save button
        saveButton.setOnClickListener(v -> saveReminder());
    }

    private void showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Format date nicely
                    String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                    String formattedDate = dayOfMonth + " " + months[month] + " " + year;
                    dateTextView.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePicker.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePicker = new TimePickerDialog(this,
                (TimePicker view, int hourOfDay, int minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);

                    // Format time in 12-hour format
                    String amPm = hourOfDay >= 12 ? "PM" : "AM";
                    int displayHour = hourOfDay % 12;
                    if (displayHour == 0) displayHour = 12;

                    String formattedTime = String.format(Locale.getDefault(),
                            "%d:%02d %s", displayHour, minute, amPm);
                    timeTextView.setText(formattedTime);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false); // Use 12-hour format

        timePicker.show();
    }

    private void saveReminder() {
        String title = titleInput.getText().toString().trim();
        String notes = notesInput.getText().toString().trim();

        // Validate input
        if (title.isEmpty()) {
            titleInput.setError("Title is required");
            titleInput.requestFocus();
            return;
        }

        // Check if date and time are selected
        if (dateTextView.getText().toString().equals("Select Date")) {
            // Use current date as default
            calendar.setTimeInMillis(System.currentTimeMillis());
        }

        if (timeTextView.getText().toString().equals("Select Time")) {
            // Use current time + 1 hour as default
            calendar.add(Calendar.HOUR_OF_DAY, 1);
        }

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
            // Insert returns long ID
            long id = db.reminderDao().insert(reminder);
            reminder.id = (int) id;

            // Schedule alarm
            scheduleAlarm(reminder);

            runOnUiThread(() -> finish()); // Simply finish - MainActivity will refresh in onResume
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
                // Handle exact alarms properly
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
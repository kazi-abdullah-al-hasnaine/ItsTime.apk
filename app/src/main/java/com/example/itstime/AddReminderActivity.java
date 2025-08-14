package com.example.itstime;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AddReminderActivity extends AppCompatActivity {

    EditText titleEditText, notesEditText;
    TextView dateTextView, timeTextView;
    Button saveButton;

    // Variables to hold date and time parts
    int year, month, day, hour, minute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        // Link UI elements with XML views
        titleEditText = findViewById(R.id.titleEditText);
        notesEditText = findViewById(R.id.notesEditText);
        dateTextView = findViewById(R.id.dateTextView);
        timeTextView = findViewById(R.id.timeTextView);
        saveButton = findViewById(R.id.saveButton);

        // Initialize date and time with current date/time
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);

        // Show the default date and time on the TextViews
        updateDateText();
        updateTimeText();

        // When date TextView is clicked, open DatePickerDialog to select date
        dateTextView.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(AddReminderActivity.this,
                    (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                        year = selectedYear;
                        month = selectedMonth;
                        day = selectedDay;
                        updateDateText(); // update UI after selection
                    }, year, month, day);
            datePickerDialog.show();
        });

        // When time TextView is clicked, open TimePickerDialog to select time
        timeTextView.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(AddReminderActivity.this,
                    (TimePicker view, int selectedHour, int selectedMinute) -> {
                        hour = selectedHour;
                        minute = selectedMinute;
                        updateTimeText(); // update UI after selection
                    }, hour, minute, false);
            timePickerDialog.show();
        });

        // When Save button is clicked, create Reminder object and save to Room database
        saveButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString();
            String notes = notesEditText.getText().toString();

            // Create a Reminder object with entered data
            Reminder reminder = new Reminder(title, notes, year, month, day, hour, minute);

            // Run database insertion on a background thread
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                db.reminderDao().insert(reminder);

                // Notify MainActivity that save was successful, then close this activity
                runOnUiThread(() -> {
                    setResult(RESULT_OK);  // Important: send success result back
                    finish();             // Close AddReminderActivity
                });
            }).start();
        });
    }

    // Update date TextView in format dd/MM/yyyy
    private void updateDateText() {
        dateTextView.setText(String.format("%02d/%02d/%04d", day, month + 1, year));
    }

    // Update time TextView in 12-hour format with AM/PM
    private void updateTimeText() {
        String amPm = (hour >= 12) ? "PM" : "AM";
        int hour12 = hour % 12;
        if (hour12 == 0) hour12 = 12;
        timeTextView.setText(String.format("%02d:%02d %s", hour12, minute, amPm));
    }
}

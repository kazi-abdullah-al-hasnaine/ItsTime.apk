package com.example.itstime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderListActivity extends AppCompatActivity {

    RecyclerView reminderRecyclerView;
    ReminderAdapter reminderAdapter;
    TextView filterTitle;
    AppDatabase db;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    List<Reminder> allReminders = new ArrayList<>();
    List<Reminder> filteredReminders = new ArrayList<>();
    String filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_list);

        reminderRecyclerView = findViewById(R.id.reminderRecyclerView);
        filterTitle = findViewById(R.id.filterTitle);
        db = AppDatabase.getInstance(this);

        filter = getIntent().getStringExtra("filter");
        filterTitle.setText(filter);

        reminderRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reminderAdapter = new ReminderAdapter(this, filteredReminders, filter, allReminders);
        reminderRecyclerView.setAdapter(reminderAdapter);
        loadReminders();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setResult(RESULT_OK); // ✅ Notify MainActivity
                finish();
            }
        });
    }

    public void loadReminders() {
        executor.execute(() -> {
            List<Reminder> all = db.reminderDao().getAllReminders();       // completed = 0
            List<Reminder> completed = db.reminderDao().getCompletedReminders(); // completed = 1

            allReminders.clear();
            allReminders.addAll(all);        // only active reminders
            allReminders.addAll(completed);  // add completed if needed for Completed page

            filteredReminders.clear();
            for (Reminder r : allReminders) {
                switch (filter) {
                    case "Today":
                        if (isToday(r) && !r.completed) filteredReminders.add(r);
                        break;
                    case "Scheduled":
                        if (!isToday(r) && !r.completed) filteredReminders.add(r);
                        break;
                    case "All":
                        if (!r.completed) filteredReminders.add(r); // ❌ Only show active reminders
                        break;
                    case "Completed":
                        if (r.completed) filteredReminders.add(r);
                        break;
                }
            }

            runOnUiThread(() -> reminderAdapter.notifyDataSetChanged());
        });
    }


    private boolean isToday(Reminder r) {
        Calendar today = Calendar.getInstance();
        return r.day == today.get(Calendar.DAY_OF_MONTH)
                && r.month == today.get(Calendar.MONTH)
                && r.year == today.get(Calendar.YEAR);
    }
}
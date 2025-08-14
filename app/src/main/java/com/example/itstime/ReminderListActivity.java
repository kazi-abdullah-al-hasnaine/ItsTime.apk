package com.example.itstime;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReminderListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ReminderAdapter adapter;
    List<Reminder> reminderList = new ArrayList<>();
    TextView filterTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_list);

        recyclerView = findViewById(R.id.reminderRecyclerView);
        filterTitle = findViewById(R.id.filterTitle);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReminderAdapter(reminderList);
        recyclerView.setAdapter(adapter);

        String filter = getIntent().getStringExtra("filter");
        filterTitle.setText(filter + " Reminders");

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<Reminder> allReminders;

            if ("Completed".equals(filter)) {
                allReminders = db.reminderDao().getCompletedReminders();
            } else {
                allReminders = db.reminderDao().getAllReminders();
            }

            List<Reminder> filteredReminders = new ArrayList<>();
            Calendar today = Calendar.getInstance();

            for (Reminder r : allReminders) {
                Calendar reminderDate = Calendar.getInstance();
                reminderDate.set(r.year, r.month, r.day);

                switch (filter) {
                    case "Today":
                        if (r.year == today.get(Calendar.YEAR) &&
                                r.month == today.get(Calendar.MONTH) &&
                                r.day == today.get(Calendar.DAY_OF_MONTH)) {
                            filteredReminders.add(r);
                        }
                        break;
                    case "Scheduled":
                        if (reminderDate.after(today)) {
                            filteredReminders.add(r);
                        }
                        break;
                    case "All":
                        filteredReminders.add(r);
                        break;
                    case "Completed":
                        if (r.completed) {
                            filteredReminders.add(r);
                        }
                        break;
                }
            }

            runOnUiThread(() -> {
                reminderList.clear();
                reminderList.addAll(filteredReminders);
                adapter.notifyDataSetChanged();
            });

        }).start();
    }
}

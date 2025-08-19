package com.example.itstime;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    TextView todayCount, scheduledCount, allCount, completedCount;
    ImageView addReminderButton, searchCancelButton;
    EditText searchEditText;
    RecyclerView searchRecyclerView;
    LinearLayout cardsLayout;

    AppDatabase db;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    private SearchAdapter searchAdapter;
    private final List<Reminder> searchResults = new ArrayList<>();

    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final int REQUEST_CODE_ADD_REMINDER = 1;
    private static final int REQUEST_CODE_VIEW_REMINDERS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermission();
        NotificationHelper.createNotificationChannel(this);

        initializeViews();
        setupSearchFunctionality();
        setupClickListeners();

        db = AppDatabase.getInstance(this);
        loadReminderCounts();

        // Handle back press using AndroidX OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // If search results are showing, hide them first
                if (searchRecyclerView.getVisibility() == View.VISIBLE) {
                    searchEditText.setText("");
                    hideSearchResults();
                    searchCancelButton.setVisibility(View.GONE);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void initializeViews() {
        todayCount = findViewById(R.id.today_count);
        scheduledCount = findViewById(R.id.scheduled_count);
        allCount = findViewById(R.id.all_count);
        completedCount = findViewById(R.id.completed_count);
        addReminderButton = findViewById(R.id.add_reminder_button);
        searchEditText = findViewById(R.id.searchEditText);
        searchCancelButton = findViewById(R.id.searchCancelButton);
        searchRecyclerView = findViewById(R.id.searchRecyclerView);
        cardsLayout = findViewById(R.id.cardsLayout);
    }

    private void setupSearchFunctionality() {
        // Setup RecyclerView for search results
        searchRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new SearchAdapter(this, searchResults, this::onSearchResultClick);
        searchRecyclerView.setAdapter(searchAdapter);

        // Initially hide search results
        searchRecyclerView.setVisibility(View.GONE);

        // Add text watcher for search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    hideSearchResults();
                    searchCancelButton.setVisibility(View.GONE);
                } else {
                    searchReminders(query);
                    searchCancelButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Search cancel button click
        searchCancelButton.setOnClickListener(v -> {
            searchEditText.setText("");
            hideSearchResults();
            searchCancelButton.setVisibility(View.GONE);
        });
    }

    private void setupClickListeners() {
        addReminderButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddReminderActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_REMINDER);
        });

        findViewById(R.id.cardToday).setOnClickListener(v -> openReminderList("Today"));
        findViewById(R.id.cardScheduled).setOnClickListener(v -> openReminderList("Scheduled"));
        findViewById(R.id.cardAll).setOnClickListener(v -> openReminderList("All"));
        findViewById(R.id.cardCompleted).setOnClickListener(v -> openReminderList("Completed"));

        findViewById(R.id.cardReport).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportHistory1.class);
            startActivity(intent);
        });
    }

    private void searchReminders(String query) {
        executor.execute(() -> {
            List<Reminder> allReminders = db.reminderDao().getAllReminders();
            List<Reminder> completedReminders = db.reminderDao().getCompletedReminders();

            // Combine all reminders
            List<Reminder> combinedReminders = new ArrayList<>();
            combinedReminders.addAll(allReminders);
            combinedReminders.addAll(completedReminders);

            // Filter reminders based on search query
            List<Reminder> filteredResults = new ArrayList<>();
            for (Reminder reminder : combinedReminders) {
                if (reminder.title != null &&
                        reminder.title.toLowerCase().contains(query.toLowerCase())) {
                    filteredResults.add(reminder);
                }
            }

            runOnUiThread(() -> {
                searchResults.clear();
                searchResults.addAll(filteredResults);
                searchAdapter.notifyDataSetChanged();
                showSearchResults();
            });
        });
    }

    private void showSearchResults() {
        cardsLayout.setVisibility(View.GONE);
        searchRecyclerView.setVisibility(View.VISIBLE);
    }

    private void hideSearchResults() {
        searchRecyclerView.setVisibility(View.GONE);
        cardsLayout.setVisibility(View.VISIBLE);
    }

    private void onSearchResultClick(Reminder reminder) {
        // Clear search
        searchEditText.setText("");
        hideSearchResults();
        searchCancelButton.setVisibility(View.GONE);

        // Determine which page this reminder belongs to
        String targetPage = determineReminderPage(reminder);
        openReminderList(targetPage);
    }

    private String determineReminderPage(Reminder reminder) {
        if (reminder.completed) {
            return "Completed";
        }

        Calendar today = Calendar.getInstance();
        boolean isToday = reminder.day == today.get(Calendar.DAY_OF_MONTH) &&
                reminder.month == today.get(Calendar.MONTH) &&
                reminder.year == today.get(Calendar.YEAR);

        return isToday ? "Today" : "Scheduled";
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
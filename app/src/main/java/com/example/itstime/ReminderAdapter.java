package com.example.itstime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private final List<Reminder> reminderList;
    private final Context context;
    private final String filter; // Page: Today, Scheduled, All, Completed
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Reminder> allReminders; // reference from ReminderListActivity

    public ReminderAdapter(Context context, List<Reminder> reminderList, String filter, List<Reminder> allReminders) {
        this.context = context;
        this.reminderList = reminderList;
        this.filter = filter;
        this.allReminders = allReminders; // keep reference for All page
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.reminder_item, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);

        // Set reminder details
        holder.reminderTitle.setText(reminder.title);
        holder.reminderNotes.setText(reminder.notes);
        holder.reminderDateTime.setText(reminder.day + "/" + (reminder.month + 1) + "/" + reminder.year +
                " " + reminder.hour + ":" + reminder.minute);

        // Only show Done button if not Completed page
        holder.doneButton.setVisibility("Completed".equals(filter) ? View.GONE : View.VISIBLE);

        // DELETE button click with Snackbar Undo
        holder.deleteButton.setOnClickListener(v -> {
            Reminder removedReminder = reminderList.get(position);
            reminderList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, reminderList.size());

            // Delete from DB
            executor.execute(() -> AppDatabase.getInstance(context).reminderDao().delete(removedReminder));

            // Snackbar Undo
            Snackbar.make(holder.itemView, "Reminder deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo", undoView -> {
                        reminderList.add(position, removedReminder);
                        notifyItemInserted(position);
                        executor.execute(() -> AppDatabase.getInstance(context).reminderDao().insert(removedReminder));
                    }).show();
        });

        // DONE button click
        holder.doneButton.setOnClickListener(v -> {
            reminder.completed = true;

            // Update in database
            executor.execute(() -> AppDatabase.getInstance(context).reminderDao().update(reminder));

            // Remove from current list if filter no longer matches
            boolean shouldRemove = !"Completed".equals(filter); // Remove from Today/Scheduled/All
            if (shouldRemove) {
                reminderList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, reminderList.size());

                // Also remove from main allReminders if filter is All
                if ("All".equals(filter) && allReminders != null) {
                    allReminders.remove(reminder);
                }
            } else {
                notifyItemChanged(position); // Completed page just updates
            }
        });
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView reminderTitle, reminderNotes, reminderDateTime;
        MaterialButton doneButton, deleteButton;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            reminderTitle = itemView.findViewById(R.id.reminderTitle);
            reminderNotes = itemView.findViewById(R.id.reminderNotes);
            reminderDateTime = itemView.findViewById(R.id.reminderDateTime);
            doneButton = itemView.findViewById(R.id.doneButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}

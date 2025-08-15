package com.example.itstime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private final List<Reminder> reminderList;
    private final Context context;
    private final String filter; // Page type: "Today", "Scheduled", "All", "Completed"
    private final AppDatabase db;

    public ReminderAdapter(Context context, List<Reminder> reminderList, String filter) {
        this.context = context;
        this.reminderList = reminderList;
        this.filter = filter;
        db = AppDatabase.getInstance(context); // Get DB instance
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

        // Only show Delete button in Completed page; hide Done button there
        if ("Completed".equals(filter)) {
            holder.doneButton.setVisibility(View.GONE);
        } else {
            holder.doneButton.setVisibility(View.VISIBLE);
        }

        // DELETE button click - live delete
        holder.deleteButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                Reminder r = reminderList.get(pos);

                // Delete from DB on background thread
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> db.reminderDao().delete(r));
                executor.shutdown();

                // Remove from current list and update RecyclerView immediately
                reminderList.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, reminderList.size());
            }
        });

        // DONE button click - mark completed and remove from page if needed
        holder.doneButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                Reminder r = reminderList.get(pos);
                r.completed = true; // mark completed

                // Update DB in background thread
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> db.reminderDao().update(r));
                executor.shutdown();

                // Remove from list immediately if page is not "All" (Today/Scheduled)
                if (!"All".equals(filter)) {
                    reminderList.remove(pos);
                    notifyItemRemoved(pos);
                    notifyItemRangeChanged(pos, reminderList.size());
                } else {
                    // Just refresh the item for "All" page
                    notifyItemChanged(pos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    // ViewHolder class
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

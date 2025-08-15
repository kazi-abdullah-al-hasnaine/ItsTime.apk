package com.example.itstime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private final List<Reminder> reminderList;
    private final Context context;

    public ReminderAdapter(Context context, List<Reminder> reminderList) {
        this.context = context;
        this.reminderList = reminderList;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.reminder_item, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);

        holder.title.setText(reminder.title);
        holder.notes.setText(reminder.notes);
        holder.dateTime.setText(
                String.format("%02d/%02d/%04d %02d:%02d",
                        reminder.day, reminder.month + 1, reminder.year,
                        reminder.hour, reminder.minute)
        );

        // DONE BUTTON
        holder.doneButton.setOnClickListener(v -> {
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                reminder.completed = true;
                db.reminderDao().update(reminder);

                ((ReminderListActivity) context).runOnUiThread(() -> {
                    reminderList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, reminderList.size());
                });
            }).start();
        });

        // DELETE BUTTON
        holder.deleteButton.setOnClickListener(v -> {
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                db.reminderDao().delete(reminder);

                ((ReminderListActivity) context).runOnUiThread(() -> {
                    reminderList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, reminderList.size());
                });
            }).start();
        });
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView title, notes, dateTime;
        MaterialButton doneButton, deleteButton;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.reminderTitle);
            notes = itemView.findViewById(R.id.reminderNotes);
            dateTime = itemView.findViewById(R.id.reminderDateTime);
            doneButton = itemView.findViewById(R.id.doneButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}

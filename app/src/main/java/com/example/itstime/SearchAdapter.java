package com.example.itstime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {

    private final Context context;
    private final List<Reminder> searchResults;
    private final OnSearchResultClickListener listener;

    public interface OnSearchResultClickListener {
        void onSearchResultClick(Reminder reminder);
    }

    public SearchAdapter(Context context, List<Reminder> searchResults, OnSearchResultClickListener listener) {
        this.context = context;
        this.searchResults = searchResults;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_result_item, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        Reminder reminder = searchResults.get(position);

        holder.titleTextView.setText(reminder.title);

        // Format time
        String timeString = String.format(Locale.getDefault(), "%02d:%02d",
                reminder.hour, reminder.minute);
        holder.timeTextView.setText(timeString);

        // Format date
        String dateString = String.format(Locale.getDefault(), "%d/%d/%d",
                reminder.day, reminder.month + 1, reminder.year);
        holder.dateTextView.setText(dateString);

        // Set status indicator
        if (reminder.completed) {
            holder.statusIndicator.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light));
            holder.statusTextView.setText("Completed");
        } else {
            holder.statusIndicator.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));
            holder.statusTextView.setText("Pending");
        }

        holder.itemView.setOnClickListener(v -> listener.onSearchResultClick(reminder));
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, timeTextView, dateTextView, statusTextView;
        View statusIndicator;

        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.searchResultTitle);
            timeTextView = itemView.findViewById(R.id.searchResultTime);
            dateTextView = itemView.findViewById(R.id.searchResultDate);
            statusTextView = itemView.findViewById(R.id.searchResultStatus);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
        }
    }
}
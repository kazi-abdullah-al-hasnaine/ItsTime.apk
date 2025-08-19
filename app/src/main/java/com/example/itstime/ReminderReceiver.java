package com.example.itstime;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String notes = intent.getStringExtra("message");
        int notificationId = intent.getIntExtra("notificationId", 0);

        // Create intent to open MainActivity directly
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create formatted notification text with time
        String formattedMessage = createNotificationMessage(title, notes);

        // Build the notification with improved content
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_completed) // your notification icon
                .setContentTitle("It's Time!")
                .setContentText(formattedMessage)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(formattedMessage)
                        .setBigContentTitle("It's Time!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL); // Add sound, vibration, lights

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }

    private String createNotificationMessage(String title, String notes) {
        Calendar now = Calendar.getInstance();

        // Format current time
        String amPm = now.get(Calendar.HOUR_OF_DAY) >= 12 ? "PM" : "AM";
        int displayHour = now.get(Calendar.HOUR_OF_DAY) % 12;
        if (displayHour == 0) displayHour = 12;

        String currentTime = String.format(Locale.getDefault(),
                "%d:%02d %s", displayHour, now.get(Calendar.MINUTE), amPm);

        // Build the message
        StringBuilder message = new StringBuilder();
        message.append(title);
        message.append(" • Today • ");
        message.append(currentTime);

        // Add notes if they exist
        if (notes != null && !notes.trim().isEmpty()) {
            message.append("\n").append(notes);
        }

        return message.toString();
    }
}
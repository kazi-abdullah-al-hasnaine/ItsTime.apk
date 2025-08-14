package com.example.itstime;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders")
public class Reminder {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String notes;
    public int year;
    public int month;
    public int day;
    public int hour;
    public int minute;

    public boolean completed;

    // Constructor matching AddReminderActivity usage
    public Reminder(String title, String notes, int year, int month, int day, int hour, int minute) {
        this.title = title;
        this.notes = notes;
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.completed = false;
    }

    // Default constructor (required by Room)
    public Reminder() {}
}

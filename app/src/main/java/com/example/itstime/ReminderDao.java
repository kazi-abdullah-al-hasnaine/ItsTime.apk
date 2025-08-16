package com.example.itstime;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReminderDao {

    // Get all reminders that are NOT completed
    @Query("SELECT * FROM reminders WHERE completed = 0")
    List<Reminder> getAllReminders();

    // Get all completed reminders
    @Query("SELECT * FROM reminders WHERE completed = 1")
    List<Reminder> getCompletedReminders();

    // ✅ Return the row ID of inserted reminder
    @Insert
    long insert(Reminder reminder);

    @Update
    void update(Reminder reminder);

    @Delete
    void delete(Reminder reminder);
}

package com.example.appseguimiento.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MediaDao {
    @Query("SELECT * FROM media_items")
    List<MediaItem> getAllItems();

    @Insert
    void insertItem(MediaItem item);

    @Update
    void updateItem(MediaItem item);

    @Delete
    void deleteItem(MediaItem item);
}

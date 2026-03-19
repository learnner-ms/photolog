package com.example.photolog_front.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.photolog_front.db.entity.ChatSessionEntity;

@Dao
public interface ChatSessionDao {

    @Insert
    long insert(ChatSessionEntity session);

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    ChatSessionEntity findById(long sessionId);

    @Update
    void update(ChatSessionEntity session);
}
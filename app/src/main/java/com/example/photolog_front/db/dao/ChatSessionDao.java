package com.example.photolog_front.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.photolog_front.db.entity.ChatSessionEntity;

import java.util.List;

@Dao
public interface ChatSessionDao {

    @Insert
    long insert(ChatSessionEntity session);

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    ChatSessionEntity findById(long sessionId);

    @Query("SELECT * FROM chat_sessions WHERE userId = :userId ORDER BY createdAt DESC")
    List<ChatSessionEntity> findByUserId(long userId);

    @Update
    void update(ChatSessionEntity session);

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    void deleteById(long sessionId);

    @Query("DELETE FROM chat_sessions WHERE userId = :userId")
    void deleteAllByUserId(long userId);
}
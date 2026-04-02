package com.example.photolog_front.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.photolog_front.db.entity.ChatMessageEntity;

import java.util.List;

@Dao
public interface ChatMessageDao {

    @Insert
    long insert(ChatMessageEntity message);

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY id ASC")
    List<ChatMessageEntity> findBySessionId(long sessionId);

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    void deleteBySessionId(long sessionId);

    @Query("DELETE FROM chat_messages WHERE sessionId IN (SELECT id FROM chat_sessions WHERE userId = :userId)")
    void deleteAllByUserId(long userId);
}
package com.example.photolog_front.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_sessions")
public class ChatSessionEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;
    public String photoUri;
    public int stepIndex;      // 현재 질문 단계
    public boolean isCompleted;
    public long createdAt;
}
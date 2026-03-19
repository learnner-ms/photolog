package com.example.photolog_front.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessageEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long sessionId;
    public String sender;   // "BOT" or "USER"
    public String message;
    public long createdAt;
}
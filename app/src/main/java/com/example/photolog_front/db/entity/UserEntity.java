package com.example.photolog_front.db.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "users",
        indices = {@Index(value = {"username"}, unique = true)}
)

public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String username;
    //비밀번호 평문 저장 X (해시값)
    public String passwordHash;
    public long createdAt;
}

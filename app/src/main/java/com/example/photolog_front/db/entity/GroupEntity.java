package com.example.photolog_front.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "family_groups",
        indices = {
                @Index(value = {"inviteCode"}, unique = true)
        }
)
public class GroupEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String groupName;

    @NonNull
    public String inviteCode;

    public int maxMember;

    public long ownerUserId;

    public GroupEntity(@NonNull String groupName, @NonNull String inviteCode, int maxMember, long ownerUserId) {
        this.groupName = groupName;
        this.inviteCode = inviteCode;
        this.maxMember = maxMember;
        this.ownerUserId = ownerUserId;
    }
}
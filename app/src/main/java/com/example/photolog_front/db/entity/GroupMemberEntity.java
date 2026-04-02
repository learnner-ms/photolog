package com.example.photolog_front.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "group_members",
        foreignKeys = {
                @ForeignKey(
                        entity = GroupEntity.class,
                        parentColumns = "id",
                        childColumns = "groupId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("groupId"),
                @Index("userId")
        }
)
public class GroupMemberEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long groupId;
    public long userId;

    @NonNull
    public String role; // OWNER / MEMBER

    public GroupMemberEntity(long groupId, long userId, @NonNull String role) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
    }
}
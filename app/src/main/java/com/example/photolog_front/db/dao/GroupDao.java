package com.example.photolog_front.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.photolog_front.db.entity.GroupEntity;
import com.example.photolog_front.db.entity.GroupMemberEntity;

import java.util.List;

@Dao
public interface GroupDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertGroup(GroupEntity group);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertGroupMember(GroupMemberEntity member);

    @Query("SELECT * FROM family_groups WHERE inviteCode = :inviteCode LIMIT 1")
    GroupEntity findByInviteCode(String inviteCode);

    @Query("SELECT * FROM family_groups WHERE ownerUserId = :userId LIMIT 1")
    GroupEntity findGroupByOwnerUserId(long userId);

    @Query("SELECT * FROM group_members WHERE userId = :userId LIMIT 1")
    GroupMemberEntity findMembershipByUserId(long userId);

    @Query("SELECT family_groups.* FROM family_groups " +
            "INNER JOIN group_members ON family_groups.id = group_members.groupId " +
            "WHERE group_members.userId = :userId LIMIT 1")
    GroupEntity findGroupByUserId(long userId);

    @Query("SELECT COUNT(*) FROM group_members WHERE groupId = :groupId")
    int countMembers(long groupId);

    @Query("SELECT COUNT(*) FROM group_members WHERE groupId = :groupId AND userId = :userId")
    int existsMember(long groupId, long userId);

    @Query("SELECT userId FROM group_members WHERE groupId = :groupId ORDER BY id ASC")
    List<Long> getMemberUserIds(long groupId);

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    void deleteMember(long groupId, long userId);

    @Query("DELETE FROM family_groups WHERE id = :groupId")
    void deleteGroupById(long groupId);
}
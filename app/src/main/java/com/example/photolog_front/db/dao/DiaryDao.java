package com.example.photolog_front.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.photolog_front.db.entity.DiaryEntity;

import java.util.List;

@Dao
public interface DiaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DiaryEntity diary);

    @Update
    void update(DiaryEntity diary);

    @Query("SELECT * FROM diaries WHERE id = :id LIMIT 1")
    DiaryEntity getDiaryById(long id);

    @Query("SELECT * FROM diaries WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    DiaryEntity getLatestDiary(long userId);

    @Query("SELECT * FROM diaries WHERE userId = :userId ORDER BY createdAt DESC")
    List<DiaryEntity> getAllDiaries(long userId);

    @Query("DELETE FROM diaries WHERE userId = :userId")
    void deleteAllByUser(long userId);

    @Query("SELECT COUNT(*) FROM diaries WHERE userId = :userId")
    int countByUserId(long userId);
}
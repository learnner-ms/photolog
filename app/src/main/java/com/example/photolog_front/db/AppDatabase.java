package com.example.photolog_front.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;

import com.example.photolog_front.db.dao.ChatMessageDao;
import com.example.photolog_front.db.dao.ChatSessionDao;
import com.example.photolog_front.db.dao.DiaryDao;
import com.example.photolog_front.db.dao.UserDao;
import com.example.photolog_front.db.entity.ChatMessageEntity;
import com.example.photolog_front.db.entity.ChatSessionEntity;
import com.example.photolog_front.db.entity.DiaryEntity;
import com.example.photolog_front.db.entity.UserEntity;

@Database(
        entities = {
                UserEntity.class,
                DiaryEntity.class,
                ChatSessionEntity.class,
                ChatMessageEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract DiaryDao diaryDao();
    public abstract ChatSessionDao chatSessionDao();
    public abstract ChatMessageDao chatMessageDao();

    @Transaction
    public void clearUserPersonalData(long userId) {
        chatMessageDao().deleteAllByUserId(userId);
        chatSessionDao().deleteAllByUserId(userId);
        diaryDao().deleteAllByUser(userId);
    }

    @Transaction
    public void deleteUserAccountCompletely(long userId) {
        chatMessageDao().deleteAllByUserId(userId);
        chatSessionDao().deleteAllByUserId(userId);
        diaryDao().deleteAllByUser(userId);
        userDao().deleteById(userId);
    }

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "photolog.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
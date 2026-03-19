package com.example.photolog_front.db.entity;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "diaries",
        indices = {@Index(value = {"userId", "createdAt"})}
)

public class DiaryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long userId; //로그인 사용자 식별
    public String title;
    public String content;
    public String dateText; //표시용 날짜
    public String photoUri; //로컬 Uri 문자열 저장
    public long createdAt; //정렬용
}

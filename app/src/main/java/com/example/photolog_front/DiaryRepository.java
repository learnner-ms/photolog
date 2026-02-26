package com.example.photolog_front;

import java.util.ArrayList;
import java.util.List;

public class DiaryRepository {
    private static DiaryRepository instance;
    private final List<Diary> diaryList = new ArrayList<>();

    private DiaryRepository() {}

    public static DiaryRepository getInstance() {
        if (instance == null) instance = new DiaryRepository();
        return instance;
    }

    public void addDiary(Diary diary) {
        diaryList.add(0, diary); // 최신순으로 위에 추가
    }

    public List<Diary> getAll() {
        return diaryList;
    }
}

package com.example.photolog_front;

import android.net.Uri;

public class DiaryItem {
    private Uri photoUri;
    private String title;
    private String date;
    private String content;

    public DiaryItem(Uri photoUri, String title, String date, String content) {
        this.photoUri = photoUri;
        this.title = title;
        this.date = date;
        this.content = content;
    }

    public Uri getPhotoUri() { return photoUri; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getContent() { return content; }
}

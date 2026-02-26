package com.example.photolog_front;

public class FamilyDiary {
    private String author;
    private String title;
    private String content;
    private String date;
    private int imageResId;

    public FamilyDiary(String author, String title, String content, String date, int imageResId) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.date = date;
        this.imageResId = imageResId;
    }

    public String getAuthor() { return author; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getDate() { return date; }
    public int getImageResId() { return imageResId; }
}

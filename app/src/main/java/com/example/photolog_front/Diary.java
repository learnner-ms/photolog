package com.example.photolog_front;

import java.io.Serializable;

public class Diary implements Serializable {
    private String author;
    private String title;
    private String content;
    private String date;
    private int imageRes; // drawable용 리소스 ID (임시)
    private String imageUri; // 실제 저장 시 URI를 문자열로 저장할 수 있음

    // 기본 생성자 (Firebase 등 DB 연동 시 필요)
    public Diary() {}

    // 이미지 리소스를 사용할 때
    public Diary(String author, String title, String content, int imageRes, String date) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.imageRes = imageRes;
        this.date = date;
    }

    // 이미지 URI를 사용할 때 (DB나 갤러리 이미지)
    public Diary(String author, String title, String content, String imageUri, String date) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.imageUri = imageUri;
        this.date = date;
    }

    //제목 미리보기용 (이미지/내용/날짜 없음)
    public Diary(String author, String title) {
        this.author = author;
        this.title = title;
        this.content = "";
        this.date = "";
        this.imageRes = 0;
        this.imageUri = null;
    }

    // Getter & Setter
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getImageRes() { return imageRes; }
    public void setImageRes(int imageRes) { this.imageRes = imageRes; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }
}

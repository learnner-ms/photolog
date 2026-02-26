package com.example.photolog_front.model;

public class SignupResponse {

    private int id;
    private String name;
    private String username;

    // 응답 JSON에 있는 필드 중 관심 있는 것만 적어도 됨
    // created_at, families, diaries_count, last_diary 등은 필요하면 나중에 추가

    public int getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
}
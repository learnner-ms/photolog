package com.example.photolog_front.model;

public class FamilyCreateRequest {

    private String name;

    public FamilyCreateRequest() {}  // 기본 생성자

    public FamilyCreateRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
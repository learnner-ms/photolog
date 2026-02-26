package com.example.photolog_front.model;

public class LoginRequest {
    private String username;
    private String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getter
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
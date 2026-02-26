package com.example.photolog_front.model;

import java.util.List;

public class UserResponse {
    public int id;
    public String name;
    public String username;
    public int diaries_count;
    public List<FamilyInfo> families;

    public static class FamilyInfo {
        public int id;
        public String name;
        public String created_at;
        public String invite_code;
        public String role; // member 또는 admin
    }
}

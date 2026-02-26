package com.example.photolog_front.model;

import java.util.List;

public class DiaryStartResponse {
    public int session_id;
    public Photo photo;
    public String question;
    public List<String> missing_slots;

    public static class Photo {
        public int id;
        public String file_path;
        public String exif_datetime;
        public String latitude;
        public String longitude;
        public String created_at;
    }
}

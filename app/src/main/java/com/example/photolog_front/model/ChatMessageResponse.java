package com.example.photolog_front.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ChatMessageResponse {

    @SerializedName("session_id")
    public int session_id;

    @SerializedName("completed")
    public boolean completed;

    @SerializedName("missing_slots")
    public List<String> missing_slots;

    @SerializedName("next_question")
    public String next_question;

    @SerializedName("next_questions")
    public List<String> next_questions;

    @SerializedName("diary")
    public Diary diary;

    @SerializedName("user_transcript")
    public String user_transcript;

    @SerializedName("tts_audio_url")
    public String tts_audio_url;

    public static class Diary {
        @SerializedName("id")
        public int id;

        @SerializedName("title")
        public String title;

        @SerializedName("content")
        public String content;

        @SerializedName("date")
        public String date;

        @SerializedName("place")
        public String place;

        @SerializedName("people")
        public String people;

        @SerializedName("emotion")
        public String emotion;

        @SerializedName("created_at")
        public String created_at;
    }
}

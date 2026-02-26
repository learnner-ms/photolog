package com.example.photolog_front.model;

import com.google.gson.annotations.SerializedName;

public class ChatMessageRequest {
    @SerializedName("answer_text")
    public String answer_text;

    public ChatMessageRequest(String answer) {
        this.answer_text = answer;
    }
}


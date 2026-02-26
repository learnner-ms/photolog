package com.example.photolog_front;

import android.net.Uri;
public class ChatMessage {
    public static final int VIEW_TYPE_IMAGE = 0;
    public static final int VIEW_TYPE_AI_QUESTION = 1;
    public static final int VIEW_TYPE_USER_ANSWER = 2;

    private int viewType;
    private String text;
    private Uri imageUri;

    public ChatMessage(int viewType, String text, Uri imageUri) {
        this.viewType = viewType;
        this.text = text;
        this.imageUri = imageUri;
    }

    public int getViewType() { return viewType; }
    public String getText() { return text; }
    public Uri getImageUri() { return imageUri; }
}

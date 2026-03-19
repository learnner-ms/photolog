package com.example.photolog_front.mock;

import com.example.photolog_front.db.entity.ChatMessageEntity;

import java.util.List;

public class MockDiaryChatManager {

    private static final String[] QUESTIONS = {
            "오늘 이 사진은 어디에서 찍은 건가요?",
            "누구와 함께 있었나요?",
            "그날 날씨나 분위기는 어땠나요?",
            "그때 어떤 감정을 느꼈나요?",
            "가장 기억에 남는 활동이나 순간은 무엇이었나요?"
    };

    private MockDiaryChatManager() {}

    public static String getFirstQuestion() {
        return QUESTIONS[0];
    }

    public static String getQuestionByStep(int stepIndex) {
        if (stepIndex < 0 || stepIndex >= QUESTIONS.length) return null;
        return QUESTIONS[stepIndex];
    }

    public static boolean hasNextQuestion(int currentStepIndex) {
        return currentStepIndex + 1 < QUESTIONS.length;
    }

    public static String getNextQuestion(int currentStepIndex) {
        int nextIndex = currentStepIndex + 1;
        if (nextIndex >= QUESTIONS.length) return null;
        return QUESTIONS[nextIndex];
    }

    public static int getRequiredQuestionCount() {
        return QUESTIONS.length;
    }

    public static String buildDiary(List<ChatMessageEntity> messages) {
        String place = "";
        String companion = "";
        String weather = "";
        String emotion = "";
        String activity = "";

        int answerIndex = 0;
        for (ChatMessageEntity message : messages) {
            if ("USER".equals(message.sender)) {
                String text = safe(message.message);
                switch (answerIndex) {
                    case 0:
                        place = text;
                        break;
                    case 1:
                        companion = text;
                        break;
                    case 2:
                        weather = text;
                        break;
                    case 3:
                        emotion = text;
                        break;
                    case 4:
                        activity = text;
                        break;
                }
                answerIndex++;
            }
        }

        return "오늘은 " + safe(place) + "에서의 순간을 사진으로 남겼다.\n"
                + safe(companion) + "와(과) 함께 시간을 보냈고, "
                + "그날의 분위기는 " + safe(weather) + "였다.\n"
                + "그 순간 나는 " + safe(emotion) + " 감정을 느꼈다.\n"
                + "특히 " + safe(activity) + " 일이 가장 기억에 남는다.\n"
                + "이렇게 오늘의 소중한 하루를 일기로 남긴다.";
    }

    public static String buildTitle(List<ChatMessageEntity> messages) {
        String place = "";
        String activity = "";

        int answerIndex = 0;
        for (ChatMessageEntity message : messages) {
            if ("USER".equals(message.sender)) {
                if (answerIndex == 0) place = safe(message.message);
                if (answerIndex == 4) activity = safe(message.message);
                answerIndex++;
            }
        }

        if (!activity.isEmpty()) {
            return activity + "의 하루";
        }
        if (!place.isEmpty()) {
            return place + "에서의 하루";
        }
        return "오늘의 일기";
    }

    private static String safe(String value) {
        if (value == null) return "기억에 남는";
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "기억에 남는" : trimmed;
    }
}
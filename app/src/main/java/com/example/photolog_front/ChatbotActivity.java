package com.example.photolog_front;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.entity.ChatMessageEntity;
import com.example.photolog_front.db.entity.ChatSessionEntity;
import com.example.photolog_front.db.entity.DiaryEntity;
import com.example.photolog_front.mock.MockDiaryChatManager;
import com.example.photolog_front.util.PrefsKeys;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatbotActivity extends AppCompatActivity {

    private static final String PLACEHOLDER_TEXT = "답변을 입력하려면 여기를 눌러주세요.";
    private static final int MIN_ANSWERS = 3;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private ImageButton btnMic;
    private AppCompatButton btnFinishChat;

    private long chatSessionId = -1L;
    private long currentUserId = -1L;
    private String imageUriString;
    private String firstQuestionFromIntent;

    private int answerCount = 0;
    private boolean isInitializing = false;
    private boolean isProcessingAnswer = false;
    private boolean isNavigatingToResult = false;

    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            ArrayList<String> results =
                                    result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                            if (results != null && !results.isEmpty()) {
                                addUserAnswer(results.get(0), "voice");
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        bindViews();
        setupRecyclerView();
        setupClickListeners();
        readIntentData();
        resolveCurrentUser();

        if (!validateRequiredData()) {
            return;
        }

        prepareChatSessionAndInit();
    }

    private void bindViews() {
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        btnMic = findViewById(R.id.btn_mic);
        btnFinishChat = findViewById(R.id.btn_finish_chat);

        btnFinishChat.setText("일기 생성하기");
        btnFinishChat.setVisibility(View.INVISIBLE);
        btnFinishChat.setEnabled(false);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(this, messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        btnMic.setOnClickListener(v -> {
            if (isInitializing || isProcessingAnswer || isNavigatingToResult) return;
            if (checkAudioPermission()) startSpeechRecognition();
        });

        btnFinishChat.setOnClickListener(v -> {
            if (isInitializing || isProcessingAnswer || isNavigatingToResult) return;
            finishManually();
        });

        findViewById(R.id.layout_logo).setOnClickListener(v -> showExitConfirmDialog());
    }

    private void readIntentData() {
        Intent intent = getIntent();
        chatSessionId = intent.getLongExtra("chat_session_id", -1L);
        currentUserId = intent.getLongExtra("current_user_id", -1L);
        imageUriString = intent.getStringExtra("selected_photo_uri");
        firstQuestionFromIntent = intent.getStringExtra("question");
    }

    private void resolveCurrentUser() {
        if (currentUserId != -1L) return;

        SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
        currentUserId = prefs.getLong(PrefsKeys.KEY_CURRENT_USER_ID, -1L);
    }

    private boolean validateRequiredData() {
        if (currentUserId == -1L) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        if (imageUriString == null || imageUriString.trim().isEmpty()) {
            Toast.makeText(this, "사진 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        return true;
    }

    private void prepareChatSessionAndInit() {
        isInitializing = true;
        setInputEnabled(false);

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                if (chatSessionId != -1L) {
                    ChatSessionEntity existingSession = db.chatSessionDao().findById(chatSessionId);

                    if (existingSession == null) {
                        runOnUiThread(() -> {
                            isInitializing = false;
                            Toast.makeText(this, "채팅 세션을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                        return;
                    }

                    List<ChatMessageEntity> savedMessages = db.chatMessageDao().findBySessionId(chatSessionId);

                    runOnUiThread(() -> {
                        initChatUiFromSavedSession(savedMessages);
                        isInitializing = false;
                        setInputEnabled(true);
                        updateFinishButtonState();
                    });
                    return;
                }

                long now = System.currentTimeMillis();

                ChatSessionEntity newSession = new ChatSessionEntity();
                newSession.userId = currentUserId;
                newSession.photoUri = imageUriString;
                newSession.stepIndex = 0;
                newSession.isCompleted = false;
                newSession.createdAt = now;

                long newSessionId = db.chatSessionDao().insert(newSession);

                final String firstQuestion =
                        (firstQuestionFromIntent == null || firstQuestionFromIntent.trim().isEmpty())
                                ? MockDiaryChatManager.getFirstQuestion()
                                : firstQuestionFromIntent;

                ChatMessageEntity firstBotMessage = new ChatMessageEntity();
                firstBotMessage.sessionId = newSessionId;
                firstBotMessage.sender = "BOT";
                firstBotMessage.message = firstQuestion;
                firstBotMessage.createdAt = now;
                db.chatMessageDao().insert(firstBotMessage);

                chatSessionId = newSessionId;

                runOnUiThread(() -> {
                    initChatUiFresh(firstQuestion);
                    isInitializing = false;
                    setInputEnabled(true);
                    updateFinishButtonState();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    isInitializing = false;
                    Toast.makeText(this, "채팅 준비 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private void initChatUiFresh(String firstQuestion) {
        messageList.clear();

        Uri uri = Uri.parse(imageUriString);
        messageList.add(new ChatMessage(ChatMessage.VIEW_TYPE_IMAGE, null, uri));

        if (firstQuestion != null && !firstQuestion.trim().isEmpty()) {
            messageList.add(new ChatMessage(ChatMessage.VIEW_TYPE_AI_QUESTION, firstQuestion, null));
        }

        messageList.add(new ChatMessage(
                ChatMessage.VIEW_TYPE_USER_ANSWER,
                PLACEHOLDER_TEXT,
                null
        ));

        answerCount = 0;
        chatAdapter.notifyDataSetChanged();
        scrollToBottom();
    }

    private void initChatUiFromSavedSession(List<ChatMessageEntity> savedMessages) {
        messageList.clear();

        Uri uri = Uri.parse(imageUriString);
        messageList.add(new ChatMessage(ChatMessage.VIEW_TYPE_IMAGE, null, uri));

        answerCount = 0;

        if (savedMessages != null) {
            for (ChatMessageEntity entity : savedMessages) {
                if ("BOT".equals(entity.sender)) {
                    messageList.add(new ChatMessage(
                            ChatMessage.VIEW_TYPE_AI_QUESTION,
                            entity.message,
                            null
                    ));
                } else if ("USER".equals(entity.sender)) {
                    messageList.add(new ChatMessage(
                            ChatMessage.VIEW_TYPE_USER_ANSWER,
                            entity.message,
                            null
                    ));
                    answerCount++;
                }
            }
        }

        if (shouldShowInputPlaceholder(savedMessages)) {
            messageList.add(new ChatMessage(
                    ChatMessage.VIEW_TYPE_USER_ANSWER,
                    PLACEHOLDER_TEXT,
                    null
            ));
        }

        chatAdapter.notifyDataSetChanged();
        scrollToBottom();
    }

    private boolean shouldShowInputPlaceholder(List<ChatMessageEntity> savedMessages) {
        if (savedMessages == null || savedMessages.isEmpty()) {
            return true;
        }

        ChatMessageEntity last = savedMessages.get(savedMessages.size() - 1);
        return "BOT".equals(last.sender);
    }

    private void setInputEnabled(boolean enabled) {
        btnMic.setEnabled(enabled);
        if (answerCount >= MIN_ANSWERS) {
            btnFinishChat.setEnabled(enabled);
        } else {
            btnFinishChat.setEnabled(false);
        }
    }

    private void scrollToBottom() {
        if (!messageList.isEmpty()) {
            chatRecyclerView.scrollToPosition(messageList.size() - 1);
        }
    }

    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    100
            );
            return false;
        }
        return true;
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        speechLauncher.launch(intent);
    }

    public void addUserAnswer(String text, String inputType) {
        if (isInitializing || isProcessingAnswer || isNavigatingToResult) return;

        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            Toast.makeText(this, "답변을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        isProcessingAnswer = true;
        setInputEnabled(false);

        int lastIdx = findLastPlaceholderIndex();

        if (lastIdx != -1) {
            messageList.set(lastIdx, new ChatMessage(ChatMessage.VIEW_TYPE_USER_ANSWER, trimmed, null));
            chatAdapter.notifyItemChanged(lastIdx);
        } else {
            messageList.add(new ChatMessage(ChatMessage.VIEW_TYPE_USER_ANSWER, trimmed, null));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
        }

        answerCount++;
        if (answerCount >= MIN_ANSWERS) {
            if (btnFinishChat.getVisibility() != View.VISIBLE) {
                btnFinishChat.setVisibility(View.VISIBLE);
                Toast.makeText(this, "이제 원하면 현재 내용으로 일기를 만들 수 있어요.", Toast.LENGTH_SHORT).show();
            }
        }

        scrollToBottom();
        processUserAnswerLocally(trimmed);
    }

    private int findLastPlaceholderIndex() {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            ChatMessage item = messageList.get(i);
            if (item.getViewType() == ChatMessage.VIEW_TYPE_USER_ANSWER &&
                    PLACEHOLDER_TEXT.equals(item.getText())) {
                return i;
            }
        }
        return -1;
    }

    private void processUserAnswerLocally(String answerText) {
        if (chatSessionId <= 0) {
            isProcessingAnswer = false;
            Toast.makeText(this, "채팅 세션 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                ChatSessionEntity session = db.chatSessionDao().findById(chatSessionId);
                if (session == null) {
                    runOnUiThread(() -> {
                        isProcessingAnswer = false;
                        setInputEnabled(true);
                        updateFinishButtonState();
                        Toast.makeText(this, "세션을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                long now = System.currentTimeMillis();

                ChatMessageEntity userMessage = new ChatMessageEntity();
                userMessage.sessionId = chatSessionId;
                userMessage.sender = "USER";
                userMessage.message = answerText;
                userMessage.createdAt = now;
                db.chatMessageDao().insert(userMessage);

                int currentStep = session.stepIndex;

                if (MockDiaryChatManager.hasNextQuestion(currentStep)) {
                    String nextQuestion = MockDiaryChatManager.getNextQuestion(currentStep);

                    session.stepIndex = currentStep + 1;
                    db.chatSessionDao().update(session);

                    ChatMessageEntity botMessage = new ChatMessageEntity();
                    botMessage.sessionId = chatSessionId;
                    botMessage.sender = "BOT";
                    botMessage.message = nextQuestion;
                    botMessage.createdAt = System.currentTimeMillis();
                    db.chatMessageDao().insert(botMessage);

                    runOnUiThread(() -> {
                        addNextQuestion(nextQuestion);
                        isProcessingAnswer = false;
                        setInputEnabled(true);
                        updateFinishButtonState();
                    });

                } else {
                    session.isCompleted = true;
                    db.chatSessionDao().update(session);

                    List<ChatMessageEntity> allMessages = db.chatMessageDao().findBySessionId(chatSessionId);
                    long diaryId = saveDiaryToRoom(db, allMessages);

                    runOnUiThread(() -> {
                        isProcessingAnswer = false;
                        setInputEnabled(true);
                        updateFinishButtonState();
                        goToDiaryResult(diaryId);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    isProcessingAnswer = false;
                    setInputEnabled(true);
                    updateFinishButtonState();
                    Toast.makeText(
                            ChatbotActivity.this,
                            "답변 처리 중 오류가 발생했습니다.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        }).start();
    }

    private void addNextQuestion(String question) {
        messageList.add(new ChatMessage(ChatMessage.VIEW_TYPE_AI_QUESTION, question, null));
        messageList.add(new ChatMessage(
                ChatMessage.VIEW_TYPE_USER_ANSWER,
                PLACEHOLDER_TEXT,
                null
        ));

        chatAdapter.notifyDataSetChanged();
        scrollToBottom();
    }

    private long saveDiaryToRoom(AppDatabase db, List<ChatMessageEntity> messages) {
        long now = System.currentTimeMillis();

        DiaryEntity diary = new DiaryEntity();
        diary.userId = currentUserId;
        diary.title = MockDiaryChatManager.buildTitle(messages);
        diary.content = MockDiaryChatManager.buildDiary(messages);
        diary.dateText = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(new Date(now));
        diary.photoUri = imageUriString;
        diary.createdAt = now;

        return db.diaryDao().insert(diary);
    }

    private void goToDiaryResult(long diaryId) {
        if (isNavigatingToResult) return;
        isNavigatingToResult = true;

        Intent intent = new Intent(this, DiaryResultActivity.class);
        intent.putExtra("diary_id", diaryId);
        intent.putExtra("photo_uri", imageUriString);
        startActivity(intent);
        finish();
    }

    private void finishManually() {
        if (chatSessionId <= 0) {
            Toast.makeText(this, "세션 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        isProcessingAnswer = true;
        setInputEnabled(false);

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                ChatSessionEntity session = db.chatSessionDao().findById(chatSessionId);
                if (session == null) {
                    runOnUiThread(() -> {
                        isProcessingAnswer = false;
                        setInputEnabled(true);
                        updateFinishButtonState();
                        Toast.makeText(this, "세션을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                List<ChatMessageEntity> allMessages = db.chatMessageDao().findBySessionId(chatSessionId);

                int userAnswerCount = 0;
                for (ChatMessageEntity message : allMessages) {
                    if ("USER".equals(message.sender)) {
                        userAnswerCount++;
                    }
                }

                if (userAnswerCount < MIN_ANSWERS) {
                    runOnUiThread(() -> {
                        isProcessingAnswer = false;
                        setInputEnabled(true);
                        updateFinishButtonState();
                        Toast.makeText(
                                this,
                                "아직 일기를 만들 수 있는 정보가 부족해요.",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                    return;
                }

                session.isCompleted = true;
                db.chatSessionDao().update(session);

                long diaryId = saveDiaryToRoom(db, allMessages);

                runOnUiThread(() -> {
                    isProcessingAnswer = false;
                    goToDiaryResult(diaryId);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    isProcessingAnswer = false;
                    setInputEnabled(true);
                    updateFinishButtonState();
                    Toast.makeText(
                            ChatbotActivity.this,
                            "일기 생성 중 오류가 발생했습니다.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        }).start();
    }

    private void updateFinishButtonState() {
        if (answerCount >= MIN_ANSWERS) {
            btnFinishChat.setVisibility(View.VISIBLE);
            btnFinishChat.setEnabled(!isInitializing && !isProcessingAnswer && !isNavigatingToResult);
        } else {
            btnFinishChat.setVisibility(View.INVISIBLE);
            btnFinishChat.setEnabled(false);
        }
    }

    private void showExitConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_exit_chatbot, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        AppCompatButton btnYes = dialogView.findViewById(R.id.btn_yes);
        AppCompatButton btnNo = dialogView.findViewById(R.id.btn_no);

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public void showCustomInputDialog(String title, String defaultText, OnSaveListener listener) {
        if (isInitializing || isProcessingAnswer || isNavigatingToResult) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_custom, null);
        TextView tv = dialogView.findViewById(R.id.tv_dialog_title);
        EditText et = dialogView.findViewById(R.id.et_dialog_input);
        AppCompatButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        AppCompatButton btnSave = dialogView.findViewById(R.id.btn_save);

        tv.setText(title);

        if (defaultText == null || PLACEHOLDER_TEXT.equals(defaultText)) {
            et.setHint("답변을 입력하세요");
        } else {
            et.setText(defaultText);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String text = et.getText().toString().trim();
            if (!text.isEmpty()) {
                listener.onSave(text);
            } else {
                Toast.makeText(this, "답변을 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    public interface OnSaveListener {
        void onSave(String text);
    }
}
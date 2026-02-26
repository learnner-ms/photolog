package com.example.photolog_front;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
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

import com.example.photolog_front.model.ChatMessageRequest;
import com.example.photolog_front.model.ChatMessageResponse;
import com.example.photolog_front.network.ApiService;
import com.example.photolog_front.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private ImageButton btnMic;
    private AppCompatButton btnFinishChat;

    private int sessionId;          // Integer 세션 ID
    private String imageUriString;  // 선택한 사진 원본 URI

    private static final int MIN_ANSWERS = 3;
    private int answerCount = 0;

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

        // ====== Intent 데이터 받아오기 ======
        sessionId = getIntent().getIntExtra("session_id", -1);
        String firstQuestion = getIntent().getStringExtra("question");
        imageUriString = getIntent().getStringExtra("selected_photo_uri");

        // ====== UI 초기화 ======
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        btnMic = findViewById(R.id.btn_mic);
        btnFinishChat = findViewById(R.id.btn_finish_chat);

        btnFinishChat.setVisibility(View.INVISIBLE);
        btnFinishChat.setEnabled(false);

        chatAdapter = new ChatAdapter(this, messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        // ====== 초기 메시지 표시 (사진 + 첫 질문) ======
        Uri uri = imageUriString != null ? Uri.parse(imageUriString) : null;

        if (uri != null) {
            messageList.add(new ChatMessage(ChatMessage.VIEW_TYPE_IMAGE, null, uri));
        }
        if (firstQuestion != null) {
            messageList.add(new ChatMessage(ChatMessage.VIEW_TYPE_AI_QUESTION, firstQuestion, null));
        }

        messageList.add(new ChatMessage(
                ChatMessage.VIEW_TYPE_USER_ANSWER,
                "답변을 입력하려면 여기를 눌러주세요.",
                null
        ));

        chatAdapter.notifyDataSetChanged();

        // 마이크 버튼
        btnMic.setOnClickListener(v -> {
            if (checkAudioPermission()) startSpeechRecognition();
        });

        // 종료 버튼 (수동 종료 → 세션 강제 완료 API 연결 가능)
        btnFinishChat.setOnClickListener(v -> finishManually());

        findViewById(R.id.layout_logo).setOnClickListener(v -> showExitConfirmDialog());
    }


    // ===== 권한 =====
    private boolean checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 100);
            return false;
        }
        return true;
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        speechLauncher.launch(intent);
    }


    // ===== 답변 추가 =====
    public void addUserAnswer(String text, String inputType) {

        int lastIdx = -1;
        for (int i = messageList.size() - 1; i >= 0; i--) {
            if (messageList.get(i).getViewType() == ChatMessage.VIEW_TYPE_USER_ANSWER) {
                lastIdx = i;
                break;
            }
        }

        if (lastIdx != -1) {
            messageList.set(lastIdx, new ChatMessage(ChatMessage.VIEW_TYPE_USER_ANSWER, text, null));
            chatAdapter.notifyItemChanged(lastIdx);
        }

        answerCount++;
        if (answerCount >= MIN_ANSWERS) {
            btnFinishChat.setVisibility(View.VISIBLE);
            btnFinishChat.setEnabled(true);
        }

        sendUserMessageToServer(text);
    }


    // ===== 서버 전송 =====
    private void sendUserMessageToServer(String content) {

        ChatMessageRequest body = new ChatMessageRequest(content);
        ApiService api = RetrofitClient.getApiService(this);

        api.sendChatAnswer(sessionId, body).enqueue(new Callback<ChatMessageResponse>() {
            @Override
            public void onResponse(Call<ChatMessageResponse> call, Response<ChatMessageResponse> response) {

                if (!response.isSuccessful()) {
                    Log.e("Chatbot", "서버 오류: " + response.code());
                    return;
                }

                ChatMessageResponse res = response.body();
                if (res == null) return;

                if (!res.completed) {
                    addNextQuestion(res.next_question);

                } else {
                    goToDiaryResult(res);   // 🔥 자동 완성 시 DiaryResultActivity로 이동
                }
            }

            @Override
            public void onFailure(Call<ChatMessageResponse> call, Throwable t) {
                Toast.makeText(ChatbotActivity.this,
                        "서버 통신 오류: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }


    // ===== 다음 질문 =====
    private void addNextQuestion(String question) {
        messageList.add(new ChatMessage(ChatMessage.VIEW_TYPE_AI_QUESTION, question, null));
        messageList.add(new ChatMessage(ChatMessage.VIEW_TYPE_USER_ANSWER,
                "답변을 입력하려면 여기를 눌러주세요.", null));

        chatAdapter.notifyDataSetChanged();
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }


    // ===== AI가 완성한 일기 결과 화면 이동 =====
    private void goToDiaryResult(ChatMessageResponse res) {
        Intent intent = new Intent(this, DiaryResultActivity.class);
        intent.putExtra("diary_title", res.diary.title);
        intent.putExtra("diary_content", res.diary.content);
        intent.putExtra("photo_uri", imageUriString); // 🔥 선택한 사진 함께 전달
        startActivity(intent);
    }

    // ===== 유저가 수동 종료 (세션 stop 호출) =====
    private void finishManually() {
        if (sessionId <= 0) {
            Toast.makeText(this, "세션 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 버튼 연타 방지 (선택)
        btnFinishChat.setEnabled(false);

        ApiService api = RetrofitClient.getApiService(this);
        api.stopSession(sessionId).enqueue(new Callback<ChatMessageResponse>() {
            @Override
            public void onResponse(Call<ChatMessageResponse> call,
                                   Response<ChatMessageResponse> response) {

                btnFinishChat.setEnabled(true);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ChatbotActivity.this,
                            "일기 생성에 실패했습니다. (서버 응답: " + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                ChatMessageResponse res = response.body();

                // completed + diary 가 있어야 일기 화면으로 이동
                if (!res.completed || res.diary == null) {
                    Toast.makeText(ChatbotActivity.this,
                            "아직 일기를 만들 수 있는 정보가 부족해요.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // 자동완성 때 쓰던 로직 재사용
                goToDiaryResult(res);
                finish();
            }

            @Override
            public void onFailure(Call<ChatMessageResponse> call, Throwable t) {
                btnFinishChat.setEnabled(true);
                Toast.makeText(ChatbotActivity.this,
                        "서버 통신 오류: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }



    // ===== 종료 다이얼로그 =====
    private void showExitConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_exit_chatbot, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        AppCompatButton btnYes = dialogView.findViewById(R.id.btn_yes);
        AppCompatButton btnNo = dialogView.findViewById(R.id.btn_no);

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    // ===== 사용자 입력 팝업 =====
    public void showCustomInputDialog(String title, String defaultText, OnSaveListener listener) {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_custom, null);
        TextView tv = dialogView.findViewById(R.id.tv_dialog_title);
        EditText et = dialogView.findViewById(R.id.et_dialog_input);
        AppCompatButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        AppCompatButton btnSave = dialogView.findViewById(R.id.btn_save);

        tv.setText(title);

        if (defaultText == null || defaultText.equals("답변을 입력하려면 여기를 눌러주세요.")) {
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
            if (!text.isEmpty()) listener.onSave(text);
            dialog.dismiss();
        });

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    public interface OnSaveListener {
        void onSave(String text);
    }
}

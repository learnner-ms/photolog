package com.example.photolog_front;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.entity.ChatMessageEntity;
import com.example.photolog_front.db.entity.ChatSessionEntity;
import com.example.photolog_front.mock.MockDiaryChatManager;
import com.example.photolog_front.util.PrefsKeys;

public class DiaryGenerationActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView guideTextView;
    private Button selectPhotoButton;

    private boolean isPhotoSelected = false;
    private Uri selectedImageUri = null;

    private static final int REQUEST_MEDIA_PERMISSION = 101;

    // 고지 동의 저장용
    private static final String PREFS_NOTICE = "notice_prefs";
    private static final String KEY_AGREED_PHOTO_NOTICE = "agreed_photo_notice";
    private static final String KEY_AGREED_AI_NOTICE = "agreed_ai_notice";

    private final ActivityResultLauncher<String[]> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    this::onImageSelected
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_generation);

        LinearLayout logoLayout = findViewById(R.id.layout_logo);
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        imageView = findViewById(R.id.img_placeholder);
        selectPhotoButton = findViewById(R.id.btn_select_photo);
        guideTextView = findViewById(R.id.tv_guide);

        selectPhotoButton.setOnClickListener(v -> {
            if (isPhotoSelected) {
                if (selectedImageUri == null) {
                    Toast.makeText(this, "선택된 사진이 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (hasAgreedAiNotice()) {
                    startDiaryChatFlow(selectedImageUri);
                } else {
                    showDiaryGenerationNoticeDialog();
                }
            } else {
                if (hasAgreedPhotoNotice()) {
                    openGalleryWithPermission();
                } else {
                    showPhotoSelectNoticeDialog();
                }
            }
        });

        imageView.setOnClickListener(v -> {
            if (!isPhotoSelected) {
                if (hasAgreedPhotoNotice()) {
                    openGalleryWithPermission();
                } else {
                    showPhotoSelectNoticeDialog();
                }
            }
        });
    }

    private void onImageSelected(Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "사진을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException e) {
            e.printStackTrace();
            // 일부 기기/문서 제공자에서는 예외가 날 수 있으니 앱이 죽지 않게만 처리
        } catch (Exception e) {
            e.printStackTrace();
        }

        isPhotoSelected = true;
        selectedImageUri = uri;

        imageView.setImageURI(uri);
        guideTextView.setText("사진 업로드 준비 완료!\n일기 생성을 시작하세요.");
        guideTextView.setGravity(Gravity.CENTER);
        selectPhotoButton.setText("일기 생성 시작");
    }

    private boolean checkMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void openGalleryWithPermission() {
        if (checkMediaPermission()) {
            galleryLauncher.launch(new String[]{"image/*"});
        } else {
            requestMediaPermission();
        }
    }

    private void requestMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_MEDIA_PERMISSION
            );
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_MEDIA_PERMISSION
            );
        }
    }

    private void startDiaryChatFlow(Uri uri) {
        SharedPreferences authPrefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
        long currentUserId = authPrefs.getLong(PrefsKeys.KEY_CURRENT_USER_ID, -1L);

        if (currentUserId == -1L) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uri == null) {
            Toast.makeText(this, "선택된 사진이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        selectPhotoButton.setEnabled(false);
        selectPhotoButton.setText("준비 중...");

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                long sessionId = createMockChatSession(db, currentUserId, uri.toString());
                String firstQuestion = MockDiaryChatManager.getFirstQuestion();

                runOnUiThread(() -> {
                    restoreGenerateButton();

                    Intent intent = new Intent(DiaryGenerationActivity.this, ChatbotActivity.class);
                    intent.putExtra("chat_session_id", sessionId);
                    intent.putExtra("question", firstQuestion);
                    intent.putExtra("selected_photo_uri", uri.toString());
                    intent.putExtra("current_user_id", currentUserId);
                    startActivity(intent);
                });

            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(() -> {
                    restoreGenerateButton();
                    Toast.makeText(
                            DiaryGenerationActivity.this,
                            "일기 생성 준비 중 오류가 발생했습니다.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        }).start();
    }

    private long createMockChatSession(AppDatabase db, long userId, String photoUri) {
        long now = System.currentTimeMillis();

        ChatSessionEntity session = new ChatSessionEntity();
        session.userId = userId;
        session.photoUri = photoUri;
        session.stepIndex = 0;
        session.isCompleted = false;
        session.createdAt = now;

        long sessionId = db.chatSessionDao().insert(session);

        ChatMessageEntity firstBotMessage = new ChatMessageEntity();
        firstBotMessage.sessionId = sessionId;
        firstBotMessage.sender = "BOT";
        firstBotMessage.message = MockDiaryChatManager.getFirstQuestion();
        firstBotMessage.createdAt = now;

        db.chatMessageDao().insert(firstBotMessage);

        return sessionId;
    }

    private void restoreGenerateButton() {
        selectPhotoButton.setEnabled(true);
        selectPhotoButton.setText("일기 생성 시작");
    }

    private void showPhotoSelectNoticeDialog() {
        String title = "사진 선택 전 안내";
        String body =
                "📷 선택한 사진은 AI 일기 생성을 위한 분석에 사용될 수 있습니다.\n\n" +
                        "🔒 사진 속 개인정보나 민감한 정보가 포함되지 않도록 주의해주세요.\n\n" +
                        "⚠️ 원치 않는 사진은 업로드하지 않는 것을 권장합니다.";
        String agreeText =
                "위 내용을 이해했고 동의합니다.\n" +
                        "(이후 동의 철회 또는 회원 탈퇴 전까지 동의 상태가 유지됩니다.)";

        showCommonNoticeDialog(
                title,
                body,
                agreeText,
                "동의하고 사진 선택",
                "취소",
                () -> {
                    savePhotoNoticeAgreed(true);
                    openGalleryWithPermission();
                }
        );
    }

    private void showDiaryGenerationNoticeDialog() {
        String title = "AI 일기 생성 안내";
        String body =
                "🖼️ 선택한 사진과 대화 내용을 분석하여 오늘의 일기를 생성합니다.\n\n" +
                        "💭 감정 정보가 추론될 수 있습니다.\n\n" +
                        "🤖 충분한 정보가 모이면 일기가 자동으로 완성될 수 있습니다.";
        String agreeText =
                "위 내용을 이해했고 동의합니다.\n" +
                        "(이후 동의 철회 또는 회원 탈퇴 전까지 동의 상태가 유지됩니다.)";

        showCommonNoticeDialog(
                title,
                body,
                agreeText,
                "동의하고 시작하기",
                "취소",
                () -> {
                    saveAiNoticeAgreed(true);
                    startDiaryChatFlow(selectedImageUri);
                }
        );
    }

    private void showCommonNoticeDialog(
            String title,
            String body,
            String agreeText,
            String primaryLabel,
            String secondaryLabel,
            Runnable onConfirmed
    ) {
        View view = getLayoutInflater().inflate(R.layout.dialog_notice_common, null);

        TextView tvTitle = view.findViewById(R.id.tv_notice_title);
        TextView tvBody = view.findViewById(R.id.tv_notice_body);

        CheckBox cbAgree = view.findViewById(R.id.cb_notice_agree);
        TextView tvAgree = view.findViewById(R.id.tv_notice_agree);

        AppCompatButton btnPrimary = view.findViewById(R.id.btn_notice_primary);
        AppCompatButton btnSecondary = view.findViewById(R.id.btn_notice_secondary);

        tvTitle.setText(title);
        tvBody.setText(body);
        tvAgree.setText(agreeText);

        btnPrimary.setText(primaryLabel);
        btnSecondary.setText(secondaryLabel);

        btnPrimary.setEnabled(false);
        btnPrimary.setAlpha(0.45f);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        cbAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnPrimary.setEnabled(isChecked);
            btnPrimary.setAlpha(isChecked ? 1.0f : 0.45f);
        });

        btnSecondary.setOnClickListener(v -> dialog.dismiss());

        btnPrimary.setOnClickListener(v -> {
            if (!cbAgree.isChecked()) {
                Toast.makeText(this, "동의 체크가 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();

            if (onConfirmed != null) {
                onConfirmed.run();
            }
        });

        dialog.show();
    }

    private boolean hasAgreedPhotoNotice() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NOTICE, MODE_PRIVATE);
        return prefs.getBoolean(KEY_AGREED_PHOTO_NOTICE, false);
    }

    private boolean hasAgreedAiNotice() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NOTICE, MODE_PRIVATE);
        return prefs.getBoolean(KEY_AGREED_AI_NOTICE, false);
    }

    private void savePhotoNoticeAgreed(boolean agreed) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NOTICE, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AGREED_PHOTO_NOTICE, agreed).apply();
    }

    private void saveAiNoticeAgreed(boolean agreed) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NOTICE, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AGREED_AI_NOTICE, agreed).apply();
    }
}
package com.example.photolog_front;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.DiaryDao;
import com.example.photolog_front.db.DiaryEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiaryResultActivity extends AppCompatActivity {

    private ImageView ivPhoto;
    private TextView tvTitle, tvDate;
    private EditText etContent;
    private AppCompatButton btnEdit, btnDone, btnRetry;

    private Uri photoUri;
    private String diaryTitle, diaryContent;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_result);

        ivPhoto = findViewById(R.id.iv_photo);
        tvTitle = findViewById(R.id.tv_title);
        tvDate = findViewById(R.id.tv_date);
        etContent = findViewById(R.id.et_content);
        btnEdit = findViewById(R.id.btn_edit);
        btnDone = findViewById(R.id.btn_done);
        btnRetry = findViewById(R.id.btn_retry);

        // 텍스트뷰 모드 (수정 불가)
        etContent.setFocusable(false);
        etContent.setFocusableInTouchMode(false);
        etContent.setCursorVisible(false);

        // 데이터 수신
        Intent intent = getIntent();
        String photoUriString = intent.getStringExtra("photo_uri");
        diaryTitle = intent.getStringExtra("diary_title");
        diaryContent = intent.getStringExtra("diary_content");

        if (photoUriString != null) {
            photoUri = Uri.parse(photoUriString);
            ivPhoto.setImageURI(photoUri);
        }

        if (diaryTitle != null) tvTitle.setText(diaryTitle);
        if (diaryContent != null) etContent.setText(diaryContent);

        String currentDate = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
                .format(new Date());
        tvDate.setText(currentDate);

        // 수정하기 → 팝업 열기
        btnEdit.setOnClickListener(v -> showCustomDiaryEditDialog(etContent.getText().toString()));

        // 로고 클릭 → 나가기 확인 다이얼로그
        findViewById(R.id.layout_logo).setOnClickListener(v -> showExitConfirmDialog());

        // ✅ 완료 버튼 → Room 저장 후 메인 이동
        btnDone.setOnClickListener(v -> saveDiaryToRoomAndGoMain());

        // 다시 작성 버튼
        btnRetry.setOnClickListener(v -> showRetryDialog());
    }

    private void saveDiaryToRoomAndGoMain() {

        // 연타 방지
        btnDone.setEnabled(false);

        String finalTitle = tvTitle.getText().toString();
        String finalContent = etContent.getText().toString();
        String finalDate = tvDate.getText().toString();
        String finalPhotoUri = (photoUri != null ? photoUri.toString() : null);

        // ✅ 로그인 사용자 정보 가져오기
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        long userId = prefs.getLong("currentUserId", -1);
        String username = prefs.getString("currentUsername", null);

        if (userId == -1) {
            btnDone.setEnabled(true);
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        dbExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                DiaryDao diaryDao = db.diaryDao();

                DiaryEntity entity = new DiaryEntity();
                entity.userId = userId;
                entity.title = finalTitle;
                entity.content = finalContent;
                entity.dateText = finalDate;
                entity.photoUri = finalPhotoUri;
                entity.createdAt = System.currentTimeMillis();

                diaryDao.insert(entity);

                runOnUiThread(() -> {
                    Toast.makeText(this, "일기가 저장되었습니다!", Toast.LENGTH_SHORT).show();

                    Intent mainIntent = new Intent(this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(mainIntent);
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnDone.setEnabled(true);
                    Toast.makeText(this, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // CUSTOM DIARY EDIT POPUP (핵심 추가 부분)
    private void showCustomDiaryEditDialog(String defaultText) {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_custom, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etInput = dialogView.findViewById(R.id.et_dialog_input);
        AppCompatButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        AppCompatButton btnSave = dialogView.findViewById(R.id.btn_save);

        // 팝업 제목
        tvTitle.setText("일기 수정");

        // 기존 글 전달
        etInput.setText(defaultText);

        // 긴 글 스크롤 가능하게
        etInput.setSingleLine(false);
        etInput.setMaxLines(Integer.MAX_VALUE);
        etInput.setVerticalScrollBarEnabled(true);
        etInput.setMovementMethod(new ScrollingMovementMethod());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String edited = etInput.getText().toString().trim();
            if (!edited.isEmpty()) {
                etContent.setText(edited); // 실제 일기 내용 업데이트
            }
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    // 기존 다이얼로그들 (나가기 / 다시작성)
    private void showExitConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_exit_confirm, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        AppCompatButton btnNo = dialogView.findViewById(R.id.btn_no);
        AppCompatButton btnYes = dialogView.findViewById(R.id.btn_yes);

        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish();
        });

        dialog.show();
    }

    private void showRetryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_retry, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        AppCompatButton btnNo = dialogView.findViewById(R.id.btn_no);
        AppCompatButton btnYes = dialogView.findViewById(R.id.btn_yes);

        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            showRetryOptions();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void showRetryOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_retry_options, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        AppCompatButton btnSelectPhoto = dialogView.findViewById(R.id.btn_select_photo);
        AppCompatButton btnRewriteDiary = dialogView.findViewById(R.id.btn_rewrite_diary);
        AppCompatButton btnCancel = dialogView.findViewById(R.id.btn_cancel);

        btnSelectPhoto.setOnClickListener(v -> {
            dialog.dismiss();
            Intent photoIntent = new Intent(this, DiaryGenerationActivity.class);
            photoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            photoIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(photoIntent);
            finish();
        });

        btnRewriteDiary.setOnClickListener(v -> {
            dialog.dismiss();
            Intent chatIntent = new Intent(this, ChatbotActivity.class);
            chatIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (photoUri != null) {
                chatIntent.putExtra("selected_photo_uri", photoUri.toString());
            }
            chatIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(chatIntent);
            finish();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
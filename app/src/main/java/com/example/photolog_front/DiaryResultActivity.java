package com.example.photolog_front;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.dao.DiaryDao;
import com.example.photolog_front.db.entity.DiaryEntity;
import com.example.photolog_front.util.PrefsKeys;

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
    private CheckBox cbGroupSee;

    private Uri photoUri;
    private long diaryId = -1L;
    private DiaryEntity currentDiaryEntity;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_result);

        bindViews();
        setupContentReadonly();
        readIntentData();

        if (diaryId != -1L) {
            loadDiaryFromRoom(diaryId);
        } else {
            loadFallbackIntentData();
        }

        setupClickListeners();
    }

    private void bindViews() {
        ivPhoto = findViewById(R.id.iv_photo);
        tvTitle = findViewById(R.id.tv_title);
        tvDate = findViewById(R.id.tv_date);
        etContent = findViewById(R.id.et_content);
        btnEdit = findViewById(R.id.btn_edit);
        btnDone = findViewById(R.id.btn_done);
        btnRetry = findViewById(R.id.btn_retry);
        cbGroupSee = findViewById(R.id.groupsee);
    }

    private void setupContentReadonly() {
        etContent.setFocusable(false);
        etContent.setFocusableInTouchMode(false);
        etContent.setCursorVisible(false);
    }

    private void readIntentData() {
        Intent intent = getIntent();
        diaryId = intent.getLongExtra("diary_id", -1L);

        String photoUriString = intent.getStringExtra("photo_uri");
        if (photoUriString != null && !photoUriString.trim().isEmpty()) {
            photoUri = Uri.parse(photoUriString);
            ivPhoto.setImageURI(photoUri);
        }
    }

    private void loadFallbackIntentData() {
        Intent intent = getIntent();
        String diaryTitle = intent.getStringExtra("diary_title");
        String diaryContent = intent.getStringExtra("diary_content");

        if (diaryTitle != null) tvTitle.setText(diaryTitle);
        if (diaryContent != null) etContent.setText(diaryContent);

        String currentDate = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
                .format(new Date());
        tvDate.setText(currentDate);
    }

    private void setupClickListeners() {
        btnEdit.setOnClickListener(v -> showCustomDiaryEditDialog(etContent.getText().toString()));
        findViewById(R.id.layout_logo).setOnClickListener(v -> showExitConfirmDialog());
        btnDone.setOnClickListener(v -> updateDiaryAndGoMain());
        btnRetry.setOnClickListener(v -> showRetryDialog());
    }

    private void loadDiaryFromRoom(long diaryId) {
        dbExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                DiaryDao diaryDao = db.diaryDao();
                DiaryEntity diary = diaryDao.getDiaryById(diaryId);

                runOnUiThread(() -> {
                    if (diary == null) {
                        Toast.makeText(this, "일기 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    currentDiaryEntity = diary;

                    tvTitle.setText(diary.title != null ? diary.title : "오늘의 일기");
                    etContent.setText(diary.content != null ? diary.content : "");

                    if (diary.dateText != null && !diary.dateText.trim().isEmpty()) {
                        tvDate.setText(diary.dateText);
                    } else {
                        String currentDate = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
                                .format(new Date(diary.createdAt));
                        tvDate.setText(currentDate);
                    }

                    if (photoUri == null && diary.photoUri != null) {
                        photoUri = Uri.parse(diary.photoUri);
                        ivPhoto.setImageURI(photoUri);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "일기 로드 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void updateDiaryAndGoMain() {
        btnDone.setEnabled(false);

        String finalTitle = tvTitle.getText().toString().trim();
        String finalContent = etContent.getText().toString().trim();
        String finalDate = tvDate.getText().toString().trim();
        String finalPhotoUri = (photoUri != null ? photoUri.toString() : null);

        SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
        long userId = prefs.getLong(PrefsKeys.KEY_CURRENT_USER_ID, -1L);

        if (userId == -1L) {
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

                if (currentDiaryEntity != null) {
                    currentDiaryEntity.title = finalTitle;
                    currentDiaryEntity.content = finalContent;
                    currentDiaryEntity.dateText = finalDate;
                    currentDiaryEntity.photoUri = finalPhotoUri;
                    currentDiaryEntity.userId = userId;

                    diaryDao.update(currentDiaryEntity);

                } else {
                    DiaryEntity entity = new DiaryEntity();
                    entity.userId = userId;
                    entity.title = finalTitle;
                    entity.content = finalContent;
                    entity.dateText = finalDate;
                    entity.photoUri = finalPhotoUri;
                    entity.createdAt = System.currentTimeMillis();

                    long insertedId = diaryDao.insert(entity);
                    diaryId = insertedId;
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "일기가 저장되었습니다!", Toast.LENGTH_SHORT).show();

                    Intent mainIntent = new Intent(this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mainIntent);
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    btnDone.setEnabled(true);
                    Toast.makeText(this, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showCustomDiaryEditDialog(String defaultText) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_custom, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etInput = dialogView.findViewById(R.id.et_dialog_input);
        AppCompatButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        AppCompatButton btnSave = dialogView.findViewById(R.id.btn_save);

        tvDialogTitle.setText("일기 수정");
        etInput.setText(defaultText);

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
                etContent.setText(edited);
            }
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

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
            photoIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(photoIntent);
            finish();
        });

        btnRewriteDiary.setOnClickListener(v -> {
            dialog.dismiss();

            SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
            long currentUserId = prefs.getLong(PrefsKeys.KEY_CURRENT_USER_ID, -1L);

            Intent chatIntent = new Intent(this, ChatbotActivity.class);
            if (photoUri != null) {
                chatIntent.putExtra("selected_photo_uri", photoUri.toString());
            }
            chatIntent.putExtra("current_user_id", currentUserId);
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
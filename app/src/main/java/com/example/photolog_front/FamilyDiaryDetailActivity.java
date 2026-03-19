package com.example.photolog_front;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.bumptech.glide.Glide;
import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.dao.DiaryDao;
import com.example.photolog_front.db.entity.DiaryEntity;
import com.example.photolog_front.model.FamilyCommentRequest;
import com.example.photolog_front.model.FamilyCommentResponse;
import com.example.photolog_front.network.ApiService;
import com.example.photolog_front.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FamilyDiaryDetailActivity extends AppCompatActivity {

    private ImageView imgDiary;
    private TextView tvTitle, tvInfo, tvContent;

    private LinearLayout commentContainer;
    private TextView tvNoComment;
    private EditText etComment;
    private View btnSend;

    private View tvCommentHeader;
    private View viewCommentDivider;
    private View commentArea;
    private View commentInputLayout;

    private MaterialButton btnDeleteDiary;
    private long currentLocalDiaryId = -1L;

    private ApiService api;
    private int postId;

    private AppDatabase db;
    private DiaryDao diaryDao;

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private static final String DEFAULT_HINT = "댓글을 입력하세요";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_diary_detail);

        imgDiary = findViewById(R.id.img_diary);
        tvTitle = findViewById(R.id.tv_title);
        tvInfo = findViewById(R.id.tv_info);
        tvContent = findViewById(R.id.tv_content);

        commentContainer = findViewById(R.id.comment_container);
        tvNoComment = findViewById(R.id.tv_no_comment);
        etComment = findViewById(R.id.et_comment);
        btnSend = findViewById(R.id.btn_send);

        tvCommentHeader = findViewById(R.id.tv_comment_header);
        viewCommentDivider = findViewById(R.id.view_comment_divider);
        commentArea = findViewById(R.id.comment_area);
        commentInputLayout = findViewById(R.id.layout_comment_input);

        btnDeleteDiary = findViewById(R.id.btn_delete_diary);

        findViewById(R.id.btn_back_to_list).setOnClickListener(v -> finish());

        api = RetrofitClient.getApiService(this);

        db = AppDatabase.getInstance(getApplicationContext());
        diaryDao = db.diaryDao();

        postId = getIntent().getIntExtra("post_id", -1);

        if (postId <= 0) {
            hideCommentUi();

            long diaryId = getIntent().getLongExtra("diary_id", -1L);
            currentLocalDiaryId = diaryId;

            btnDeleteDiary.setVisibility(View.VISIBLE);
            btnDeleteDiary.setOnClickListener(v -> showDeleteConfirmDialog());

            if (diaryId <= 0) {
                Toast.makeText(this, "로컬 일기 ID가 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            loadLocalDiaryFromRoom(diaryId);
            return;
        }

        btnDeleteDiary.setVisibility(View.GONE);
        showCommentUi();

        Diary diary = (Diary) getIntent().getSerializableExtra("diary");
        if (diary == null) {
            Toast.makeText(this, "일기 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindDiaryToUi(
                diary.getAuthor(),
                diary.getTitle(),
                diary.getContent(),
                diary.getDate(),
                diary.getImageUri()
        );

        loadComments();

        btnSend.setOnClickListener(v -> submitComment());
        etComment.setOnClickListener(v -> showCommentDialog("댓글 달기", etComment.getText().toString()));
    }

    private void loadLocalDiaryFromRoom(long diaryId) {
        io.execute(() -> {
            DiaryEntity entity = diaryDao.getDiaryById(diaryId);

            runOnUiThread(() -> {
                if (entity == null) {
                    Toast.makeText(this, "일기를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                bindDiaryToUi(
                        "나",
                        entity.title,
                        entity.content,
                        entity.dateText,
                        entity.photoUri
                );
            });
        });
    }

    private void bindDiaryToUi(String author, String title, String content, String dateText, String photoUri) {
        tvTitle.setText(title != null ? title : "");
        tvInfo.setText((author != null ? author : "") + " | " + (dateText != null ? dateText : ""));
        tvContent.setText(content != null ? content : "");

        if (photoUri != null && !photoUri.trim().isEmpty()) {
            try {
                Uri uri = Uri.parse(photoUri);
                Glide.with(this)
                        .load(uri)
                        .placeholder(R.drawable.sample)
                        .error(R.drawable.sample)
                        .into(imgDiary);
            } catch (Exception e) {
                e.printStackTrace();
                imgDiary.setImageResource(R.drawable.sample);
            }
        } else {
            imgDiary.setImageResource(R.drawable.sample);
        }
    }

    private void hideCommentUi() {
        if (tvCommentHeader != null) tvCommentHeader.setVisibility(View.GONE);
        if (viewCommentDivider != null) viewCommentDivider.setVisibility(View.GONE);
        if (commentArea != null) commentArea.setVisibility(View.GONE);
        if (commentInputLayout != null) commentInputLayout.setVisibility(View.GONE);
    }

    private void showCommentUi() {
        if (tvCommentHeader != null) tvCommentHeader.setVisibility(View.VISIBLE);
        if (viewCommentDivider != null) viewCommentDivider.setVisibility(View.VISIBLE);
        if (commentArea != null) commentArea.setVisibility(View.VISIBLE);
        if (commentInputLayout != null) commentInputLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 기존 dialog_exit_confirm 디자인 재사용
     */
    private void showDeleteConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_exit_confirm, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        AppCompatButton btnNo = dialogView.findViewById(R.id.btn_no);
        AppCompatButton btnYes = dialogView.findViewById(R.id.btn_yes);

        // 다이얼로그 안 메시지 TextView가 있으면 텍스트 교체
        tryUpdateDialogMessage(dialogView, "이 일기를 삭제하시겠습니까?");
        btnYes.setText("삭제");

        btnNo.setOnClickListener(v -> dialog.dismiss());
        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            deleteLocalDiary();
        });

        dialog.show();
    }

    /**
     * dialog_exit_confirm 내부의 메시지 TextView id가 무엇이든
     * 있으면 찾아서 텍스트를 바꾸도록 시도
     */
    private void tryUpdateDialogMessage(View dialogView, String message) {
        int[] candidateIds = new int[] {
                getResources().getIdentifier("tv_message", "id", getPackageName()),
                getResources().getIdentifier("tv_dialog_message", "id", getPackageName()),
                getResources().getIdentifier("tv_title", "id", getPackageName()),
                getResources().getIdentifier("tv_dialog_title", "id", getPackageName())
        };

        for (int id : candidateIds) {
            if (id != 0) {
                View v = dialogView.findViewById(id);
                if (v instanceof TextView) {
                    ((TextView) v).setText(message);
                    return;
                }
            }
        }
    }

    private void deleteLocalDiary() {
        if (currentLocalDiaryId <= 0) {
            Toast.makeText(this, "삭제할 일기 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        io.execute(() -> {
            try {
                diaryDao.deleteById(currentLocalDiaryId);

                runOnUiThread(() -> {
                    Toast.makeText(this, "일기가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void showCommentDialog(String title, String defaultText) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_custom, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etInput = dialogView.findViewById(R.id.et_dialog_input);
        AppCompatButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        AppCompatButton btnSave = dialogView.findViewById(R.id.btn_save);

        tvDialogTitle.setText(title);
        etInput.setText(defaultText);
        etInput.setSingleLine(false);
        etInput.setMaxLines(Integer.MAX_VALUE);
        etInput.setMovementMethod(new ScrollingMovementMethod());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                etComment.setText(text);
                etComment.setSelection(text.length());
            }
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void loadComments() {
        api.getComments(postId).enqueue(new Callback<List<FamilyCommentResponse>>() {
            @Override
            public void onResponse(Call<List<FamilyCommentResponse>> call, Response<List<FamilyCommentResponse>> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(FamilyDiaryDetailActivity.this, "댓글 불러오기 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<FamilyCommentResponse> comments = response.body();
                commentContainer.removeAllViews();

                if (comments == null || comments.isEmpty()) {
                    tvNoComment.setVisibility(View.VISIBLE);
                    return;
                }

                tvNoComment.setVisibility(View.GONE);

                for (FamilyCommentResponse c : comments) {
                    addCommentView(c.user_name, c.content, c.created_at);
                }
            }

            @Override
            public void onFailure(Call<List<FamilyCommentResponse>> call, Throwable t) {
                Toast.makeText(FamilyDiaryDetailActivity.this, "서버 연결 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitComment() {
        String text = etComment.getText().toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(this, DEFAULT_HINT, Toast.LENGTH_SHORT).show();
            return;
        }

        FamilyCommentRequest request = new FamilyCommentRequest(text);

        api.addComment(postId, request).enqueue(new Callback<FamilyCommentResponse>() {
            @Override
            public void onResponse(Call<FamilyCommentResponse> call, Response<FamilyCommentResponse> response) {
                if (response.isSuccessful()) {
                    etComment.setText("");
                    loadComments();
                } else {
                    Toast.makeText(FamilyDiaryDetailActivity.this, "댓글 등록 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FamilyCommentResponse> call, Throwable t) {
                Toast.makeText(FamilyDiaryDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCommentView(String user, String text, String time) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, dpToPx(8), dpToPx(10), dpToPx(8));

        TextView tvUser = new TextView(this);
        tvUser.setText(user + " • " + formatTime(time));
        tvUser.setTextColor(Color.parseColor("#5D3316"));
        tvUser.setTypeface(Typeface.DEFAULT_BOLD);
        tvUser.setTextSize(13);

        TextView tvText = new TextView(this);
        tvText.setText("- " + text);
        tvText.setTextColor(Color.parseColor("#5D3316"));
        tvText.setTextSize(15);

        layout.addView(tvUser);
        layout.addView(tvText);

        commentContainer.addView(layout);
    }

    private String formatTime(String datetime) {
        try {
            return datetime.substring(11, 16);
        } catch (Exception e) {
            return "--:--";
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
package com.example.photolog_front;

import android.app.AlertDialog;
import android.content.Intent;
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

import com.example.photolog_front.model.FamilyCommentRequest;
import com.example.photolog_front.model.FamilyCommentResponse;
import com.example.photolog_front.network.ApiService;
import com.example.photolog_front.network.RetrofitClient;

import java.util.List;

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

    private ApiService api;
    private int postId;

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

        api = RetrofitClient.getApiService(this);
        postId = getIntent().getIntExtra("post_id", -1);

        if (postId == -1) {
            Toast.makeText(this, "게시물을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Diary diary = (Diary) getIntent().getSerializableExtra("diary");

        tvTitle.setText(diary.getTitle());
        tvInfo.setText(diary.getAuthor() + " | " + diary.getDate());
        tvContent.setText(diary.getContent());

        if (diary.getImageUri() != null)
            imgDiary.setImageURI(Uri.parse(diary.getImageUri()));
        else if (diary.getImageRes() != 0)
            imgDiary.setImageResource(diary.getImageRes());

        findViewById(R.id.btn_back_to_list).setOnClickListener(v -> finish());

        // 댓글 불러오기
        loadComments();

        // 댓글 등록
        btnSend.setOnClickListener(v -> submitComment());

        // 팝업 입력창 열기
        etComment.setOnClickListener(v -> {
            showCommentDialog("댓글 달기", etComment.getText().toString());
        });
    }

    // -------------------------
    // 댓글 입력 팝업
    // -------------------------
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

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    // -------------------------
    // 댓글 서버에서 불러오기
    // -------------------------
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

    // -------------------------
    // 서버로 댓글 등록
    // -------------------------
    private void submitComment() {
        String text = etComment.getText().toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(this, "댓글을 입력하세요.", Toast.LENGTH_SHORT).show();
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

    // -------------------------
    // UI에 댓글 표시
    // -------------------------
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
}

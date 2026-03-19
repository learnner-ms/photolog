package com.example.photolog_front;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.bumptech.glide.Glide;
import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.dao.DiaryDao;
import com.example.photolog_front.db.entity.DiaryEntity;
import com.example.photolog_front.model.FamilyItem;
import com.example.photolog_front.model.FamilyPostItem;
import com.example.photolog_front.network.ApiService;
import com.example.photolog_front.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ImageView imgDiary;
    private TextView tvDiaryTitle, tvDiaryDate, tvDiaryContent;

    private ApiService api;

    // Room
    private AppDatabase db;
    private DiaryDao diaryDao;

    // Thread 통일
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = RetrofitClient.getApiService(this);

        // Room init
        db = AppDatabase.getInstance(getApplicationContext());
        diaryDao = db.diaryDao();

        // UI 요소 연결
        imgDiary = findViewById(R.id.img_diary);
        tvDiaryTitle = findViewById(R.id.tv_diary_title);
        tvDiaryDate = findViewById(R.id.tv_diary_date);
        tvDiaryContent = findViewById(R.id.tv_diary_content);

        // 상단 "내 일기 카드" 클릭 → 최신 내 일기 상세
        ConstraintLayout myDiaryLayout = findViewById(R.id.my_diary);
        myDiaryLayout.setOnClickListener(v -> openLatestDiaryDetailFromRoom());

        // 카드 전체(card_random_diary)도 동일하게 최신 내 일기 상세로
        findViewById(R.id.card_random_diary).setOnClickListener(v -> openLatestDiaryDetailFromRoom());

        // 새 일기 작성하기 버튼
        FrameLayout addDiaryButton = findViewById(R.id.layout_add_diary);
        addDiaryButton.setOnClickListener(v -> {
            Intent newDiaryIntent = new Intent(MainActivity.this, DiaryGenerationActivity.class);
            startActivity(newDiaryIntent);
        });

        // 우리 가족 일기 목록 열기
        LinearLayout layoutFamilyHeader = findViewById(R.id.layout_family_header);
        layoutFamilyHeader.setOnClickListener(v -> {
            Intent familyIntent = new Intent(MainActivity.this, FamilyDiaryActivity.class);
            startActivity(familyIntent);
        });

        // 마이페이지 이동
        ImageView myPage = findViewById(R.id.my_page);
        myPage.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MyPageActivity.class);
            startActivity(intent);
        });

        // 가족 추가 (+ 아이콘)
        ImageView imgPlusIcon = findViewById(R.id.img_plus_icon);
        imgPlusIcon.setOnClickListener(v -> {
            Intent familyIntent = new Intent(MainActivity.this, FamilyDiaryActivity.class);
            startActivity(familyIntent);
        });

        // 상단 카드뷰 최신 내 일기 로드 (Room)
        loadLatestMyDiaryFromRoom();

        // 하단 가족 일기 미리보기 (기존 유지: 서버)
        populateFamilyDiaryPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLatestMyDiaryFromRoom();
        populateFamilyDiaryPreview();
    }

    private long getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        return prefs.getLong("userId", -1L);
    }

    /**
     * ✅ Room에서 "내 최신 일기 1개" 조회해서 상단 카드에 표시
     */
    private void loadLatestMyDiaryFromRoom() {
        final long userId = getCurrentUserId();

        if (userId <= 0) {
            showEmptyMyDiary();
            return;
        }

        io.execute(() -> {
            DiaryEntity latest = diaryDao.getLatestDiary(userId);

            runOnUiThread(() -> {
                if (latest == null) {
                    showEmptyMyDiary();
                    return;
                }

                tvDiaryTitle.setText(latest.title);
                tvDiaryDate.setText(latest.dateText);
                tvDiaryContent.setText(latest.content);

                if (latest.photoUri != null && !latest.photoUri.trim().isEmpty()) {
                    try {
                        Uri uri = Uri.parse(latest.photoUri);
                        Glide.with(MainActivity.this)
                                .load(uri)
                                .placeholder(R.drawable.sample)
                                .error(R.drawable.sample)
                                .into(imgDiary);
                    } catch (Exception e) {
                        imgDiary.setImageResource(R.drawable.sample);
                    }
                } else {
                    imgDiary.setImageResource(R.drawable.sample);
                }
            });
        });
    }

    private void showEmptyMyDiary() {
        tvDiaryTitle.setText("아직 작성된 일기가 없어요!");
        tvDiaryDate.setText("");
        tvDiaryContent.setText("새로 일기를 작성하러 가볼까요?");
        imgDiary.setImageResource(R.drawable.sample);
    }

    /**
     * ✅ 최신 내 일기 → 상세 화면으로 이동 (Room PK만 전달)
     */
    private void openLatestDiaryDetailFromRoom() {
        final long userId = getCurrentUserId();

        if (userId <= 0) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        io.execute(() -> {
            DiaryEntity latest = diaryDao.getLatestDiary(userId);

            runOnUiThread(() -> {
                if (latest == null) {
                    Toast.makeText(this, "아직 작성된 일기가 없어요!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, FamilyDiaryDetailActivity.class);

                // 로컬 모드 표시
                intent.putExtra("post_id", -1);

                // 로컬 일기 PK만 넘김 (상세에서 Room 조회)
                intent.putExtra("diary_id", latest.id);

                startActivity(intent);
            });
        });
    }

    // ---------------------- 아래는 기존 코드 그대로 (가족/서버/미리보기 유지) ----------------------

    private String getPostTitle(FamilyPostItem post) {
        if (post == null) return "";

        if (post.title != null && !post.title.trim().isEmpty()) {
            return post.title.trim();
        }

        if (post.content == null || post.content.isEmpty()) return "";

        String[] lines = post.content.split("\\r?\\n");
        String firstLine = lines.length > 0 ? lines[0].trim() : "";

        if (firstLine.length() > 20) {
            return firstLine.substring(0, 20) + "...";
        }
        return firstLine;
    }

    private void populateFamilyDiaryPreview() {
        ConstraintLayout layout = findViewById(R.id.family_Diary_Layout);
        TextView emptyView = findViewById(R.id.tv_family_empty);

        if (layout.getChildCount() > 11) {
            layout.removeViews(11, layout.getChildCount() - 11);
        }

        hideFamilyPreviewStaticViews();
        emptyView.setVisibility(View.GONE);

        api.getMyFamily().enqueue(new Callback<List<FamilyItem>>() {
            @Override
            public void onResponse(Call<List<FamilyItem>> call, Response<List<FamilyItem>> response) {

                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    emptyView.setText("아직 가입한 가족이 없어요!");
                    emptyView.setVisibility(View.VISIBLE);
                    return;
                }

                FamilyItem family = response.body().get(0);
                int familyId = family.id;

                api.getFamilyPosts(familyId).enqueue(new Callback<List<FamilyPostItem>>() {
                    @Override
                    public void onResponse(Call<List<FamilyPostItem>> call2, Response<List<FamilyPostItem>> response2) {

                        if (!response2.isSuccessful() || response2.body() == null || response2.body().isEmpty()) {
                            emptyView.setText("아직 작성된 가족 일기가 없어요!");
                            emptyView.setVisibility(View.VISIBLE);
                            return;
                        }

                        List<FamilyPostItem> posts = response2.body();

                        emptyView.setVisibility(View.GONE);
                        showFamilyPreviewStaticViews();

                        int limit = Math.min(posts.size(), 5);
                        List<FamilyPostItem> previewList = new ArrayList<>();
                        for (int i = 0; i < limit; i++) previewList.add(posts.get(i));

                        int[] anchorIds = {
                                R.id.row_anchor_1, R.id.row_anchor_2,
                                R.id.row_anchor_3, R.id.row_anchor_4, R.id.row_anchor_5
                        };

                        ConstraintSet cs = new ConstraintSet();
                        cs.clone(layout);

                        for (int i = 0; i < previewList.size(); i++) {
                            FamilyPostItem post = previewList.get(i);

                            TextView authorView = new TextView(MainActivity.this);
                            authorView.setId(View.generateViewId());
                            authorView.setText(post.user_name != null ? post.user_name : "");
                            authorView.setTextColor(Color.parseColor("#665F5A"));
                            authorView.setGravity(Gravity.CENTER);
                            layout.addView(authorView);

                            TextView titleView = new TextView(MainActivity.this);
                            titleView.setId(View.generateViewId());
                            titleView.setText(getPostTitle(post));
                            titleView.setTextColor(Color.parseColor("#665F5A"));
                            titleView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                            titleView.setPadding(24, 0, 0, 0);
                            layout.addView(titleView);

                            int anchor = anchorIds[i];

                            cs.connect(authorView.getId(), ConstraintSet.TOP, anchor, ConstraintSet.TOP);
                            cs.connect(authorView.getId(), ConstraintSet.BOTTOM, anchor, ConstraintSet.BOTTOM);
                            cs.connect(authorView.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                            cs.connect(authorView.getId(), ConstraintSet.END, R.id.guideline_vertical, ConstraintSet.START);
                            cs.constrainWidth(authorView.getId(), ConstraintSet.MATCH_CONSTRAINT);
                            cs.constrainHeight(authorView.getId(), ConstraintSet.WRAP_CONTENT);

                            cs.connect(titleView.getId(), ConstraintSet.TOP, anchor, ConstraintSet.TOP);
                            cs.connect(titleView.getId(), ConstraintSet.BOTTOM, anchor, ConstraintSet.BOTTOM);
                            cs.connect(titleView.getId(), ConstraintSet.START, R.id.guideline_vertical, ConstraintSet.END);
                            cs.connect(titleView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                            cs.constrainWidth(titleView.getId(), ConstraintSet.MATCH_CONSTRAINT);
                            cs.constrainHeight(titleView.getId(), ConstraintSet.WRAP_CONTENT);
                        }

                        cs.applyTo(layout);
                    }

                    @Override
                    public void onFailure(Call<List<FamilyPostItem>> call2, Throwable t2) {
                        emptyView.setText("가족 일기를 불러오지 못했어요.");
                        emptyView.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onFailure(Call<List<FamilyItem>> call, Throwable t) {
                emptyView.setText("가족 정보를 불러오지 못했어요.");
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideFamilyPreviewStaticViews() {
        findViewById(R.id.header_author).setVisibility(View.GONE);
        findViewById(R.id.header_title).setVisibility(View.GONE);
        findViewById(R.id.line_horizontal).setVisibility(View.GONE);
        findViewById(R.id.line_vertical).setVisibility(View.GONE);

        findViewById(R.id.row_anchor_1).setVisibility(View.GONE);
        findViewById(R.id.row_anchor_2).setVisibility(View.GONE);
        findViewById(R.id.row_anchor_3).setVisibility(View.GONE);
        findViewById(R.id.row_anchor_4).setVisibility(View.GONE);
        findViewById(R.id.row_anchor_5).setVisibility(View.GONE);
    }

    private void showFamilyPreviewStaticViews() {
        findViewById(R.id.header_author).setVisibility(View.VISIBLE);
        findViewById(R.id.header_title).setVisibility(View.VISIBLE);
        findViewById(R.id.line_horizontal).setVisibility(View.VISIBLE);
        findViewById(R.id.line_vertical).setVisibility(View.VISIBLE);

        findViewById(R.id.row_anchor_1).setVisibility(View.VISIBLE);
        findViewById(R.id.row_anchor_2).setVisibility(View.VISIBLE);
        findViewById(R.id.row_anchor_3).setVisibility(View.VISIBLE);
        findViewById(R.id.row_anchor_4).setVisibility(View.VISIBLE);
        findViewById(R.id.row_anchor_5).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
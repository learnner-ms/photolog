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
import com.example.photolog_front.util.PrefsKeys;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ImageView imgDiary;
    private TextView tvDiaryTitle, tvDiaryDate, tvDiaryContent;

    private AppDatabase db;
    private DiaryDao diaryDao;

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(getApplicationContext());
        diaryDao = db.diaryDao();

        imgDiary = findViewById(R.id.img_diary);
        tvDiaryTitle = findViewById(R.id.tv_diary_title);
        tvDiaryDate = findViewById(R.id.tv_diary_date);
        tvDiaryContent = findViewById(R.id.tv_diary_content);

        // 상단 내 일기 카드 클릭 -> 최신 일기 상세
        ConstraintLayout myDiaryLayout = findViewById(R.id.my_diary);
        myDiaryLayout.setOnClickListener(v -> openLatestDiaryDetailFromRoom());

        findViewById(R.id.card_random_diary).setOnClickListener(v -> openLatestDiaryDetailFromRoom());

        // 새 일기 작성
        FrameLayout addDiaryButton = findViewById(R.id.layout_add_diary);
        addDiaryButton.setOnClickListener(v -> {
            Intent newDiaryIntent = new Intent(MainActivity.this, DiaryGenerationActivity.class);
            startActivity(newDiaryIntent);
        });

        // "우리 가족 일기" 헤더 클릭 -> 전체 로컬 일기 목록
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

        // 우측 하단 + 버튼도 일기 목록으로
        ImageView imgPlusIcon = findViewById(R.id.img_plus_icon);
        imgPlusIcon.setOnClickListener(v -> {
            Intent familyIntent = new Intent(MainActivity.this, FamilyDiaryActivity.class);
            startActivity(familyIntent);
        });

        loadLatestMyDiaryFromRoom();
        populateFamilyDiaryPreviewFromRoom();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLatestMyDiaryFromRoom();
        populateFamilyDiaryPreviewFromRoom();
    }

    private long getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
        return prefs.getLong(PrefsKeys.KEY_CURRENT_USER_ID, -1L);
    }

    /**
     * 상단 대표 카드: 최신 내 일기 1개
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

                tvDiaryTitle.setText(getDiaryTitle(latest));
                tvDiaryDate.setText(latest.dateText != null ? latest.dateText : "");
                tvDiaryContent.setText(latest.content != null ? latest.content : "");

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
        tvDiaryContent.setText("새로 일기를 작성하고 나만의 일상을 기록해보세요!");
        imgDiary.setImageResource(R.drawable.sample);
    }

    /**
     * 상단 카드 클릭 -> 최신 일기 상세
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
                intent.putExtra("post_id", -1);   // 로컬 모드
                intent.putExtra("diary_id", latest.id);
                startActivity(intent);
            });
        });
    }

    /**
     * "우리 가족 일기" 미리보기 영역:
     * 서버 대신 Room의 내 일기 최신순 5개 표시
     */
    private void populateFamilyDiaryPreviewFromRoom() {
        ConstraintLayout layout = findViewById(R.id.family_Diary_Layout);
        TextView emptyView = findViewById(R.id.tv_family_empty);

        if (layout.getChildCount() > 11) {
            layout.removeViews(11, layout.getChildCount() - 11);
        }

        hideFamilyPreviewStaticViews();
        emptyView.setVisibility(View.GONE);

        long userId = getCurrentUserId();

        if (userId <= 0) {
            emptyView.setText("로그인이 필요합니다.");
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        io.execute(() -> {
            List<DiaryEntity> diaries = diaryDao.getAllDiaries(userId);

            runOnUiThread(() -> {
                if (diaries == null || diaries.isEmpty()) {
                    emptyView.setText("아직 작성된 일기가 없어요!");
                    emptyView.setVisibility(View.VISIBLE);
                    return;
                }

                showFamilyPreviewStaticViews();
                emptyView.setVisibility(View.GONE);

                int limit = Math.min(diaries.size(), 5);
                int[] anchorIds = {
                        R.id.row_anchor_1, R.id.row_anchor_2,
                        R.id.row_anchor_3, R.id.row_anchor_4, R.id.row_anchor_5
                };

                ConstraintSet cs = new ConstraintSet();
                cs.clone(layout);

                for (int i = 0; i < limit; i++) {
                    DiaryEntity diary = diaries.get(i);

                    TextView authorView = new TextView(MainActivity.this);
                    authorView.setId(View.generateViewId());
                    authorView.setText("나");
                    authorView.setTextColor(Color.parseColor("#665F5A"));
                    authorView.setGravity(Gravity.CENTER);
                    layout.addView(authorView);

                    TextView titleView = new TextView(MainActivity.this);
                    titleView.setId(View.generateViewId());
                    titleView.setText(getDiaryTitle(diary));
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

                    View.OnClickListener clickListener = v -> {
                        Intent intent = new Intent(MainActivity.this, FamilyDiaryDetailActivity.class);
                        intent.putExtra("post_id", -1);
                        intent.putExtra("diary_id", diary.id);
                        startActivity(intent);
                    };

                    authorView.setOnClickListener(clickListener);
                    titleView.setOnClickListener(clickListener);
                }

                cs.applyTo(layout);
            });
        });
    }

    private String getDiaryTitle(DiaryEntity diary) {
        if (diary == null) return "오늘의 하루";

        if (diary.title != null && !diary.title.trim().isEmpty()) {
            return diary.title.trim();
        }

        return "오늘의 하루";
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
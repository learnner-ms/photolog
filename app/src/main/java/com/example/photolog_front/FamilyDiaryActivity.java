package com.example.photolog_front;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.dao.DiaryDao;
import com.example.photolog_front.db.entity.DiaryEntity;
import com.example.photolog_front.util.PrefsKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FamilyDiaryActivity extends AppCompatActivity {

    private TableLayout familyTable;
    private TextView tvPageNumber;
    private ImageView btnPrev, btnNext;

    private int currentPage = 1;
    private final int ITEMS_PER_PAGE = 6;

    private List<DiaryEntity> diaryList;

    private Typeface fontPaper6;
    private Typeface fontPaper5;

    private AppDatabase db;
    private DiaryDao diaryDao;

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_diary);

        familyTable = findViewById(R.id.recycler_family_diary);
        tvPageNumber = findViewById(R.id.tv_page_number);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);

        findViewById(R.id.layout_logo).setOnClickListener(v -> finish());

        fontPaper6 = ResourcesCompat.getFont(this, R.font.paperlogy_6);
        fontPaper5 = ResourcesCompat.getFont(this, R.font.paperlogy_5);

        db = AppDatabase.getInstance(getApplicationContext());
        diaryDao = db.diaryDao();

        diaryList = new ArrayList<>();

        loadDiariesFromRoom();

        btnPrev.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                displayPage(currentPage);
            }
        });

        btnNext.setOnClickListener(v -> {
            int totalPages = (int) Math.ceil((double) diaryList.size() / ITEMS_PER_PAGE);
            if (currentPage < totalPages) {
                currentPage++;
                displayPage(currentPage);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDiariesFromRoom();
    }

    private long getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
        return prefs.getLong(PrefsKeys.KEY_CURRENT_USER_ID, -1L);
    }

    private void loadDiariesFromRoom() {
        TextView empty = findViewById(R.id.tv_empty);

        empty.setVisibility(TextView.GONE);
        familyTable.setVisibility(TableLayout.VISIBLE);
        tvPageNumber.setVisibility(TextView.VISIBLE);
        btnPrev.setVisibility(ImageView.VISIBLE);
        btnNext.setVisibility(ImageView.VISIBLE);

        long userId = getCurrentUserId();

        if (userId <= 0) {
            empty.setText("로그인이 필요합니다.");
            showEmptyView();
            return;
        }

        io.execute(() -> {
            List<DiaryEntity> result = diaryDao.getAllDiaries(userId);

            runOnUiThread(() -> {
                diaryList.clear();

                if (result != null) {
                    diaryList.addAll(result);
                }

                if (diaryList.isEmpty()) {
                    empty.setText("아직 작성된 일기가 없어요!");
                    showEmptyView();
                    return;
                }

                currentPage = 1;
                displayPage(currentPage);
            });
        });
    }

    private void showEmptyView() {
        TextView empty = findViewById(R.id.tv_empty);

        empty.setVisibility(TextView.VISIBLE);
        familyTable.setVisibility(TableLayout.GONE);
        tvPageNumber.setVisibility(TextView.GONE);
        btnPrev.setVisibility(ImageView.GONE);
        btnNext.setVisibility(ImageView.GONE);
    }

    private void displayPage(int page) {
        if (familyTable.getChildCount() > 1) {
            familyTable.removeViews(1, familyTable.getChildCount() - 1);
        }

        int totalPages = (int) Math.ceil((double) diaryList.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        int start = (page - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, diaryList.size());

        for (int i = start; i < end; i++) {
            DiaryEntity diary = diaryList.get(i);
            TableRow row = createTableRow(diary);

            if (i < end - 1) {
                row.setBackgroundResource(R.drawable.row_border_bottom);
            }

            familyTable.addView(row);
        }

        tvPageNumber.setText(page + " / " + totalPages);

        btnPrev.setAlpha(page == 1 ? 0.3f : 1f);
        btnPrev.setEnabled(page != 1);

        btnNext.setAlpha(page == totalPages ? 0.3f : 1f);
        btnNext.setEnabled(page != totalPages);
    }

    private TableRow createTableRow(DiaryEntity diary) {
        TableRow row = new TableRow(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        int rowMinHeight = (int) (getResources().getDisplayMetrics().density * 120);
        row.setMinimumHeight(rowMinHeight);

        // 왼쪽 작성자
        TextView author = new TextView(this);
        author.setText("나");
        author.setTextColor(Color.parseColor("#5D3316"));
        author.setTextSize(18);
        author.setGravity(Gravity.CENTER);

        if (fontPaper6 != null) {
            author.setTypeface(fontPaper6, Typeface.BOLD);
        }

        TableRow.LayoutParams authorParams = new TableRow.LayoutParams(
                0, TableRow.LayoutParams.WRAP_CONTENT, 1f
        );
        author.setLayoutParams(authorParams);
        row.addView(author);

        // 오른쪽 사진 + 텍스트
        android.widget.LinearLayout diaryLayout = new android.widget.LinearLayout(this);
        diaryLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        diaryLayout.setPadding(12, 12, 12, 12);
        diaryLayout.setLayoutParams(new TableRow.LayoutParams(
                0, TableRow.LayoutParams.WRAP_CONTENT, 3f
        ));

        ImageView img = new ImageView(this);
        int size = (int) (getResources().getDisplayMetrics().density * 90);
        android.widget.LinearLayout.LayoutParams imgParams =
                new android.widget.LinearLayout.LayoutParams(size, size);
        img.setLayoutParams(imgParams);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);

        img.setBackgroundResource(R.drawable.rounded_image_bg);
        img.setClipToOutline(true);

        if (diary.photoUri != null && !diary.photoUri.trim().isEmpty()) {
            try {
                img.setImageURI(Uri.parse(diary.photoUri));
            } catch (Exception e) {
                img.setImageResource(R.drawable.sample);
            }
        } else {
            img.setImageResource(R.drawable.sample);
        }

        diaryLayout.addView(img);

        android.widget.LinearLayout rightLayout = new android.widget.LinearLayout(this);
        rightLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        rightLayout.setPadding(12, 0, 0, 0);

        TextView title = new TextView(this);
        title.setText(getDiaryTitle(diary));
        title.setTextSize(18);
        title.setTextColor(Color.parseColor("#5D3316"));

        if (fontPaper6 != null) {
            title.setTypeface(fontPaper6, Typeface.BOLD);
        } else {
            title.setTypeface(title.getTypeface(), Typeface.BOLD);
        }

        TextView content = new TextView(this);
        content.setText(diary.content != null ? diary.content : "");
        content.setTextSize(16);
        content.setTextColor(Color.parseColor("#5D3316"));
        content.setMaxLines(2);
        content.setEllipsize(TextUtils.TruncateAt.END);

        if (fontPaper5 != null) {
            content.setTypeface(fontPaper5);
        }

        rightLayout.addView(title);
        rightLayout.addView(content);

        diaryLayout.addView(rightLayout);
        row.addView(diaryLayout);

        row.setOnClickListener(v -> {
            Intent intent = new Intent(FamilyDiaryActivity.this, FamilyDiaryDetailActivity.class);
            intent.putExtra("post_id", -1);
            intent.putExtra("diary_id", diary.id);
            startActivity(intent);
        });

        return row;
    }

    private String getDiaryTitle(DiaryEntity diary) {
        if (diary == null) return "오늘의 하루";

        if (diary.title != null && !diary.title.trim().isEmpty()) {
            return diary.title.trim();
        }

        return "오늘의 하루";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
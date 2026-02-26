package com.example.photolog_front;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.example.photolog_front.model.FamilyItem;
import com.example.photolog_front.model.FamilyPostItem;
import com.example.photolog_front.network.ApiService;
import com.example.photolog_front.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FamilyDiaryActivity extends AppCompatActivity {

    private TableLayout familyTable;
    private TextView tvPageNumber;
    private ImageView btnPrev, btnNext;

    private int currentPage = 1;
    private final int ITEMS_PER_PAGE = 6;

    // 서버에서 받은 FamilyPostItem 목록
    private List<FamilyPostItem> diaryList;

    // 폰트
    private Typeface fontPaper6;
    private Typeface fontPaper5;

    // Retrofit
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_diary);

        api = RetrofitClient.getApiService(this);

        familyTable = findViewById(R.id.recycler_family_diary);
        tvPageNumber = findViewById(R.id.tv_page_number);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);

        // 상단 로고 클릭 시 뒤로가기
        findViewById(R.id.layout_logo).setOnClickListener(v -> finish());

        // 폰트 로드
        fontPaper6 = ResourcesCompat.getFont(this, R.font.paperlogy_6);
        fontPaper5 = ResourcesCompat.getFont(this, R.font.paperlogy_5);

        diaryList = new ArrayList<>();

        // 최초 진입 시 가족 일기 목록 가져오기
        loadFamilyPostsFromServer();

        // 페이지 이동 버튼
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

    /**
     * /myfamily → 첫 번째 가족 id → /families/{family_id}/posts
     * 순서로 가족 일기 전체를 불러와서 테이블에 뿌려주는 함수
     */
    private void loadFamilyPostsFromServer() {
        TextView empty = findViewById(R.id.tv_empty);

        // 일단 빈 문구는 숨겨두고 시작
        empty.setVisibility(View.GONE);
        familyTable.setVisibility(View.VISIBLE);
        tvPageNumber.setVisibility(View.VISIBLE);
        btnPrev.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.VISIBLE);

        // 1) 내가 속한 가족 목록 조회
        api.getMyFamily().enqueue(new Callback<List<FamilyItem>>() {
            @Override
            public void onResponse(Call<List<FamilyItem>> call, Response<List<FamilyItem>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    empty.setText("아직 가입한 가족이 없어요!");
                    showEmptyView();
                    return;
                }

                // 현재는 첫 번째 가족만 대상으로 사용
                FamilyItem family = response.body().get(0);
                int familyId = family.id;

                // 2) 해당 가족의 게시글 목록 조회
                api.getFamilyPosts(familyId).enqueue(new Callback<List<FamilyPostItem>>() {
                    @Override
                    public void onResponse(Call<List<FamilyPostItem>> call2, Response<List<FamilyPostItem>> response2) {
                        if (!response2.isSuccessful() || response2.body() == null || response2.body().isEmpty()) {
                            empty.setText("아직 작성된 가족 일기가 없어요!");
                            showEmptyView();
                            return;
                        }

                        diaryList.clear();
                        diaryList.addAll(response2.body());

                        if (diaryList.isEmpty()) {
                            empty.setText("아직 작성된 가족 일기가 없어요!");
                            showEmptyView();
                            return;
                        }

                        currentPage = 1;
                        displayPage(currentPage);
                    }

                    @Override
                    public void onFailure(Call<List<FamilyPostItem>> call2, Throwable t2) {
                        empty.setText("가족 일기를 불러오지 못했어요.");
                        showEmptyView();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<FamilyItem>> call, Throwable t) {
                empty.setText("가족 정보를 불러오지 못했어요.");
                showEmptyView();
            }
        });
    }

    // 데이터 없거나 에러일 때
    private void showEmptyView() {
        TextView empty = findViewById(R.id.tv_empty);

        empty.setVisibility(View.VISIBLE);       // 안내문 표시
        familyTable.setVisibility(View.GONE);    // 테이블 숨김
        tvPageNumber.setVisibility(View.GONE);   // 페이지 번호 숨김
        btnPrev.setVisibility(View.GONE);        // 이전 버튼 숨김
        btnNext.setVisibility(View.GONE);        // 다음 버튼 숨김
    }

    // 페이지 출력
    private void displayPage(int page) {
        // 헤더(제목 행)만 남기고 아래 행 제거
        if (familyTable.getChildCount() > 1) {
            familyTable.removeViews(1, familyTable.getChildCount() - 1);
        }

        int totalPages = (int) Math.ceil((double) diaryList.size() / ITEMS_PER_PAGE);

        int start = (page - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, diaryList.size());

        for (int i = start; i < end; i++) {
            FamilyPostItem post = diaryList.get(i);
            TableRow row = createTableRow(post);

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

    // 표 한 행 만들기 (FamilyPostItem 기반)
    private TableRow createTableRow(FamilyPostItem post) {

        TableRow row = new TableRow(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        int rowMinHeight = (int) (getResources().getDisplayMetrics().density * 120);
        row.setMinimumHeight(rowMinHeight);

        // ───────── 왼쪽: 작성자 ─────────
        TextView author = new TextView(this);
        author.setText(post.user_name != null ? post.user_name : "");
        author.setTextColor(Color.parseColor("#5D3316"));
        author.setTextSize(18); // 18sp
        author.setGravity(Gravity.CENTER);
        if (fontPaper6 != null) {
            author.setTypeface(fontPaper6, Typeface.BOLD);
        }

        TableRow.LayoutParams authorParams = new TableRow.LayoutParams(
                0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        author.setLayoutParams(authorParams);
        row.addView(author);

        // ───────── 오른쪽: 사진 + 텍스트 영역 ─────────
        LinearLayout diaryLayout = new LinearLayout(this);
        diaryLayout.setOrientation(LinearLayout.HORIZONTAL);
        diaryLayout.setPadding(12, 12, 12, 12);
        diaryLayout.setLayoutParams(new TableRow.LayoutParams(
                0, TableRow.LayoutParams.WRAP_CONTENT, 3f));

        // 이미지 (지금은 샘플 이미지)
        ImageView img = new ImageView(this);
        int size = (int) (getResources().getDisplayMetrics().density * 90);
        img.setImageResource(R.drawable.sample);  // 필요하면 서버 이미지 URL 연동 가능
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        diaryLayout.addView(img);

        // 제목 + 내용
        LinearLayout rightLayout = new LinearLayout(this);
        rightLayout.setOrientation(LinearLayout.VERTICAL);
        rightLayout.setPadding(12, 0, 0, 0);

        // ─ 제목 ─
        TextView title = new TextView(this);
        title.setText(getPostTitle(post));
        title.setTextSize(18); // 18sp
        title.setTextColor(Color.parseColor("#5D3316"));
        if (fontPaper6 != null) {
            title.setTypeface(fontPaper6, Typeface.BOLD);
        } else {
            title.setTypeface(title.getTypeface(), Typeface.BOLD);
        }

        // ─ 내용 ─
        TextView content = new TextView(this);
        content.setText(post.content != null ? post.content : "");
        content.setTextSize(16); // 16sp
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

        // 🔥 클릭 시 상세 페이지 이동
        row.setOnClickListener(v -> {
            Intent intent = new Intent(FamilyDiaryActivity.this, FamilyDiaryDetailActivity.class);
            intent.putExtra("post_id", post.id);                 // Detail에서 getIntExtra("post_id", -1)로 받는 값
            intent.putExtra("author", post.user_name);
            intent.putExtra("title", getPostTitle(post));
            intent.putExtra("content", post.content);
            startActivity(intent);
        });

        return row;
    }

    /**
     * 가족 게시글에서 제목으로 쓸 문자열 추출
     * - title이 있으면 title 사용
     * - title이 비어 있으면 content의 첫 줄 사용 (너무 길면 잘라서 ...)
     */
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
}

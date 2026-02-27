package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.photolog_front.model.MyDiaryItem;
import com.example.photolog_front.model.MyDiaryListResponse;
import com.example.photolog_front.network.ApiService;
import com.example.photolog_front.network.RetrofitClient;

import java.time.Instant;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyDiaryActivity extends AppCompatActivity {

    private TableLayout tableMyDiary;
    private TextView tvEmpty;
    private ImageView btnPrev, btnNext;
    private TextView tvPageNumber;

    private int currentPage = 1;

    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_diary);

        api = RetrofitClient.getApiService(this);

        initViews();
        // 로고 클릭하면 MainActivity로 이동
        findViewById(R.id.layout_logo).setOnClickListener(v -> {
            Intent intent = new Intent(MyDiaryActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        initListeners();

        loadMyDiaries(currentPage);


    }

    private void initViews() {
        tableMyDiary = findViewById(R.id.recycler_my_diary);
        tvEmpty = findViewById(R.id.tv_my_empty);

        btnPrev = findViewById(R.id.btn_my_prev);
        btnNext = findViewById(R.id.btn_my_next);
        tvPageNumber = findViewById(R.id.tv_my_page_number);
    }

    private void initListeners() {

        btnPrev.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                loadMyDiaries(currentPage);
            }
        });

        btnNext.setOnClickListener(v -> {
            currentPage++;
            loadMyDiaries(currentPage);
        });
    }

    /**
     * 나의 일기 목록 불러오기
     */
    private void loadMyDiaries(int page) {

        api.getMyDiaries().enqueue(new Callback<MyDiaryListResponse>() {
            @Override
            public void onResponse(Call<MyDiaryListResponse> call, Response<MyDiaryListResponse> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                MyDiaryListResponse result = response.body();
                List<MyDiaryItem> diaries = result.diaries;

                tvPageNumber.setText("-" + currentPage + "-");

                if (diaries == null || diaries.size() == 0) {

                    tvEmpty.setVisibility(View.VISIBLE);
                    clearDiaryRows();
                    return;
                }

                tvEmpty.setVisibility(View.GONE);
                showDiaryCards(diaries);
            }

            @Override
            public void onFailure(Call<MyDiaryListResponse> call, Throwable t) { }
        });
    }

    /**
     * 기존 테이블 내용 제거
     */
    private void clearDiaryRows() {
        int childCount = tableMyDiary.getChildCount();

        if (childCount > 1) {
            tableMyDiary.removeViews(1, childCount - 1);
        }
    }

    /**
     * item_family_diary.xml 형태로 카드 리스트를 출력
     */
    private void showDiaryCards(List<MyDiaryItem> diaries) {

        clearDiaryRows();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (MyDiaryItem diary : diaries) {

            View itemView = inflater.inflate(R.layout.item_family_diary, null);

            ImageView imgThumb = itemView.findViewById(R.id.img_thumbnail);
            TextView tvAuthor = itemView.findViewById(R.id.tv_author);
            TextView tvTitle = itemView.findViewById(R.id.tv_title);
            TextView tvContent = itemView.findViewById(R.id.tv_content);
            TextView tvDate = itemView.findViewById(R.id.tv_date);

            tvAuthor.setText("나");
            tvTitle.setText(diary.title);
            tvContent.setText(diary.content);
            tvDate.setText(diary.date);

            if (diary.photo != null && diary.photo.file_path != null) {
                Glide.with(this)
                        .load(RetrofitClient.BASE_URL + diary.photo.file_path)
                        .into(imgThumb);
            }

            // ⭐ 클릭 시 상세 페이지 이동 (Diary로 변환)
            itemView.setOnClickListener(v -> {
                Diary converted = convertToDiary(diary);

                Intent intent = new Intent(MyDiaryActivity.this, FamilyDiaryDetailActivity.class);
                intent.putExtra("diary", converted);
                startActivity(intent);
            });

            TableRow row = new TableRow(this);
            row.addView(itemView);

            tableMyDiary.addView(row);
        }
    }



    private Diary convertToDiary(MyDiaryItem item) {
        Diary diary = new Diary();

        diary.setTitle(item.title);
        diary.setAuthor("나");                         // 내 일기이므로 작성자 = 나
        diary.setDate(item.date);
        diary.setContent(item.content);

        if (item.photo != null && item.photo.file_path != null) {
            diary.setImageUri(RetrofitClient.BASE_URL + item.photo.file_path);
        } else {
            diary.setImageRes(R.drawable.sample);
        }

        return diary;
    }


}

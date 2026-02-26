package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.photolog_front.model.UserResponse;
import com.example.photolog_front.model.FamilyMemberResponse;
import com.example.photolog_front.network.ApiService;
import com.example.photolog_front.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyPageActivity extends AppCompatActivity {

    // 메인 프로필 & 유저 정보
    private ImageView profile;
    private TextView tvNickname, tvDiaryCount, tvFamilyCount;

    // 가족 구성원 박스 1
    private LinearLayout familyBox1Container;
    private ImageView profileFamily1;
    private TextView tvNicknameFamily1, tvDiaryCountFamily1;

    // 가족 구성원 박스 2
    private LinearLayout familyBox2Container;
    private ImageView profileFamily2;
    private TextView tvNicknameFamily2, tvDiaryCountFamily2;

    // 가족 추가 버튼
    private LinearLayout layoutAddFamily;

    // 로고 → 홈 이동
    private LinearLayout layoutLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        initViews();
        setListeners();
        loadUserData();
    }

    // XML 요소 연결
    private void initViews() {

        layoutLogo = findViewById(R.id.layout_logo);

        // 메인 프로필
        profile = findViewById(R.id.profile);
        tvNickname = findViewById(R.id.tvNickname);
        tvDiaryCount = findViewById(R.id.tvDiaryCount);
        tvFamilyCount = findViewById(R.id.tvFamilyCount);

        // 가족 1
        familyBox1Container = findViewById(R.id.family_box_1);
        profileFamily1 = findViewById(R.id.profile_family1);
        tvNicknameFamily1 = findViewById(R.id.tvNickname_family1);
        tvDiaryCountFamily1 = findViewById(R.id.tvDiaryCount_family1);

        // 가족 2
        familyBox2Container = findViewById(R.id.family_box_2);
        profileFamily2 = findViewById(R.id.profile_family2);
        tvNicknameFamily2 = findViewById(R.id.tvNickname_family2);
        tvDiaryCountFamily2 = findViewById(R.id.tvDiaryCount_family2);

        // 가족 추가 레이아웃
        layoutAddFamily = findViewById(R.id.layout_add_family);
    }

    // 클릭 리스너 설정
    private void setListeners() {

        // 로고 클릭 → 메인 페이지 이동
        layoutLogo.setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // "가족 추가하기" 클릭
        layoutAddFamily.setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, MakeGroupActivity.class);
            startActivity(intent);
        });
    }

    // 사용자 정보 로딩
    private void loadUserData() {

        ApiService api = RetrofitClient.getApiService(this);

        api.getUserInfo().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {

                if (!response.isSuccessful() || response.body() == null) return;

                UserResponse user = response.body();

                // 🔵 사용자 기본 UI 반영
                tvNickname.setText(user.name);
                tvDiaryCount.setText("작성한 일기 수 : " + user.diaries_count);

                // 가족 그룹이 하나도 없다 → 구성원 조회도 불가
                if (user.families == null || user.families.size() == 0) {
                    tvFamilyCount.setText("추가한 가족 수 : 0");

                    familyBox1Container.setVisibility(View.GONE);
                    familyBox2Container.setVisibility(View.GONE);
                    layoutAddFamily.setVisibility(View.VISIBLE);
                    return;
                }

                // 첫 가족 그룹 ID 가져오기 → 구성원 조회
                int familyId = user.families.get(0).id;
                loadFamilyMembers(familyId);
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {}
        });
    }

    // 가족 구성원 조회
    private void loadFamilyMembers(int familyId) {

        ApiService api = RetrofitClient.getApiService(this);

        api.getFamilyMembers(familyId).enqueue(new Callback<List<FamilyMemberResponse>>() {
            @Override
            public void onResponse(Call<List<FamilyMemberResponse>> call, Response<List<FamilyMemberResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) return;

                List<FamilyMemberResponse> members = response.body();
                int count = members.size();

                tvFamilyCount.setText("추가한 가족 수 : " + count);

                // 🔸 구성원 0명 → 추가만 가능
                if (count == 0) {
                    familyBox1Container.setVisibility(View.GONE);
                    familyBox2Container.setVisibility(View.GONE);
                    layoutAddFamily.setVisibility(View.VISIBLE);
                    return;
                }

                // 🔸 구성원 1명
                if (count >= 1) {
                    FamilyMemberResponse m1 = members.get(0);

                    familyBox1Container.setVisibility(View.VISIBLE);
                    tvNicknameFamily1.setText(m1.name);
                    tvDiaryCountFamily1.setText("작성한 일기 수 : " + m1.diaries_count);
                } else {
                    familyBox1Container.setVisibility(View.GONE);
                }

                // 🔸 구성원 2명
                if (count >= 2) {
                    FamilyMemberResponse m2 = members.get(1);

                    familyBox2Container.setVisibility(View.VISIBLE);
                    tvNicknameFamily2.setText(m2.name);
                    tvDiaryCountFamily2.setText("작성한 일기 수 : " + m2.diaries_count);
                } else {
                    familyBox2Container.setVisibility(View.GONE);
                }

                // 구성원이 1명 이상이면 "가족 추가" 버튼 숨김
                layoutAddFamily.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<List<FamilyMemberResponse>> call, Throwable t) {}
        });
    }
}

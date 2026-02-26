package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.photolog_front.model.FamilyJoinRequest;
import com.example.photolog_front.network.ApiService;
import com.example.photolog_front.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MakeGroupResponse extends AppCompatActivity {

    private EditText inviteCodeEditText;
    private TextView errorTextView;
    private Button confirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_group_response);

        inviteCodeEditText = findViewById(R.id.max_member);
        errorTextView = findViewById(R.id.tvGroupError);
        confirmButton = findViewById(R.id.btnLogin);

        // 🔥 추가: 로고 클릭 시 이전 화면으로 이동
        View logoLayout = findViewById(R.id.layout_logo);
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(MakeGroupResponse.this, MakeGroupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // 화면 스택 정리
        });

        confirmButton.setOnClickListener(v -> validateCode());
    }

    private void validateCode() {

        String inviteCode = inviteCodeEditText.getText().toString().trim();

        if (inviteCode.isEmpty()) {
            errorTextView.setVisibility(View.VISIBLE);
            Toast.makeText(this, "초대코드를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        errorTextView.setVisibility(View.GONE);

        ApiService api = RetrofitClient.getApiService(this);

        Toast.makeText(this, "초대코드 확인 중...", Toast.LENGTH_SHORT).show();

        FamilyJoinRequest request = new FamilyJoinRequest(inviteCode);

        api.joinFamily(request).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {

                if (response.isSuccessful()) {
                    Toast.makeText(MakeGroupResponse.this, "가족 참여 성공!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MakeGroupResponse.this, FamilyDiaryActivity.class);
                    intent.putExtra("invite_code", inviteCode);
                    startActivity(intent);
                    finish();
                }
                else if (response.code() == 404) {
                    Toast.makeText(MakeGroupResponse.this, "잘못된 초대코드입니다.", Toast.LENGTH_SHORT).show();
                    errorTextView.setVisibility(View.VISIBLE);
                }
                else if (response.code() == 400) {
                    Toast.makeText(MakeGroupResponse.this, "이미 가입된 가족입니다.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(MakeGroupResponse.this, "서버 오류 (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Toast.makeText(MakeGroupResponse.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
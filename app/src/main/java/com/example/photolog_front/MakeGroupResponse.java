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
    private View logoLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_group_response);

        initViews();
        setListeners();
    }

    private void initViews() {
        inviteCodeEditText = findViewById(R.id.max_member);
        errorTextView = findViewById(R.id.tvGroupError);
        confirmButton = findViewById(R.id.btnLogin);
        logoLayout = findViewById(R.id.layout_logo);

        hideError();
    }

    private void setListeners() {
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(MakeGroupResponse.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        confirmButton.setOnClickListener(v -> validateCode());
    }

    private void validateCode() {
        String inviteCode = inviteCodeEditText.getText().toString().trim();

        if (inviteCode.isEmpty()) {
            showError("초대코드를 입력해주세요.");
            return;
        }

        hideError();

        ApiService api = RetrofitClient.getApiService(this);
        FamilyJoinRequest request = new FamilyJoinRequest(inviteCode);

        Toast.makeText(this, "초대코드 확인 중...", Toast.LENGTH_SHORT).show();

        api.joinFamily(request).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (response.isSuccessful()) {
                    hideError();
                    Toast.makeText(MakeGroupResponse.this, "가족 참여 성공!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MakeGroupResponse.this, FamilyDiaryActivity.class);
                    intent.putExtra("invite_code", inviteCode);
                    startActivity(intent);
                    finish();

                } else if (response.code() == 404) {
                    showError("잘못된 초대코드입니다.");

                } else if (response.code() == 400) {
                    showError("이미 가입된 가족입니다.");

                } else {
                    showError("서버 오류가 발생했습니다. (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                showError("네트워크 오류가 발생했습니다.");
            }
        });
    }

    private void showError(String message) {
        errorTextView.setText(message);
        errorTextView.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorTextView.setText("");
        errorTextView.setVisibility(View.GONE);
    }
}
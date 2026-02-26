package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.photolog_front.model.SignupRequest;
import com.example.photolog_front.model.SignupResponse;
import com.example.photolog_front.network.ApiService;
import com.example.photolog_front.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    CheckBox chkAll, chkUse, chkPrivacy, chkAd;
    EditText signName, signId, signPwd, signPwdCheck;
    TextView tvError;
    Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // 체크박스
        chkAll = findViewById(R.id.chkAll);
        chkUse = findViewById(R.id.chkUse);
        chkPrivacy = findViewById(R.id.chkPrivacy);
        chkAd = findViewById(R.id.chkAd);

        // 입력칸
        signName = findViewById(R.id.signName);
        signId = findViewById(R.id.signId);
        signPwd = findViewById(R.id.signPwd);
        signPwdCheck = findViewById(R.id.signPwdCheck);

        // 에러 문구
        tvError = findViewById(R.id.tvError);

        // 회원가입 버튼
        btnSignup = findViewById(R.id.btnSignup);

        // 로고 클릭 → 로그인 이동
        LinearLayout logoLayout = findViewById(R.id.layout_logo);
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 전체 선택
        chkAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chkUse.setChecked(isChecked);
            chkPrivacy.setChecked(isChecked);
            chkAd.setChecked(isChecked);
        });

        chkUse.setOnCheckedChangeListener((b, c) -> updateAllChecked());
        chkPrivacy.setOnCheckedChangeListener((b, c) -> updateAllChecked());
        chkAd.setOnCheckedChangeListener((b, c) -> updateAllChecked());

        // 회원가입 버튼 클릭
        btnSignup.setOnClickListener(v -> checkSignup());
    }

    // 전체 체크 업데이트
    private void updateAllChecked() {
        boolean allChecked = chkUse.isChecked() && chkPrivacy.isChecked() && chkAd.isChecked();
        chkAll.setChecked(allChecked);
    }

    // 1단계: 입력 검증
    private void checkSignup() {

        String name = signName.getText().toString().trim();
        String id = signId.getText().toString().trim();
        String pw = signPwd.getText().toString().trim();
        String pwCheck = signPwdCheck.getText().toString().trim();

        // 빈칸 체크
        if (name.isEmpty() || id.isEmpty() || pw.isEmpty() || pwCheck.isEmpty()) {
            showError("모든 칸을 채워주세요!");
            return;
        }

        // 비밀번호 일치 확인
        if (!pw.equals(pwCheck)) {
            showError("비밀번호가 일치하지 않습니다.");
            return;
        }

        // 필수 약관 체크
        if (!chkUse.isChecked() || !chkPrivacy.isChecked()) {
            showError("필수 약관에 동의해야 합니다.");
            return;
        }

        // 여기까지 통과하면 서버에 요청
        tvError.setVisibility(View.GONE);
        requestSignup(name, id, pw);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // 2단계: 서버 회원가입 요청
    private void requestSignup(String name, String id, String pw) {
        // API 스펙: name, username, password
        SignupRequest request = new SignupRequest(name, id, pw);

        ApiService api = RetrofitClient.getApiService(this);
        api.signup(request).enqueue(new Callback<SignupResponse>() {
            @Override
            public void onResponse(Call<SignupResponse> call, Response<SignupResponse> response) {

                if (response.isSuccessful()) {
                    // 200 OK
                    Toast.makeText(SignupActivity.this,
                            "회원가입 완료!", Toast.LENGTH_SHORT).show();

                    // 로그인 페이지로 이동
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    // 필요하면 회원정보 넘겨도 됨
                    startActivity(intent);
                    finish();

                } else if (response.code() == 422) {
                    // Validation Error
                    showError("회원가입 실패: 입력값을 다시 확인해주세요.");
                } else {
                    showError("회원가입 실패: 서버 오류(" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<SignupResponse> call, Throwable t) {
                showError("네트워크 오류가 발생했습니다.\n다시 시도해주세요.");
            }
        });
    }
}

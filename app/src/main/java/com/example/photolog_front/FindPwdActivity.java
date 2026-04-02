package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FindPwdActivity extends AppCompatActivity {

    private LinearLayout logoLayout;
    private TextView tvLogin, tvFindId, tvPwdError;
    private Button goFindPwdButton, btnSendCode;
    private EditText etEmail, etUserId, etCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_pwd);

        initViews();
        setListeners();
    }

    private void initViews() {
        logoLayout = findViewById(R.id.layout_logo);
        tvLogin = findViewById(R.id.tvLogin);
        tvFindId = findViewById(R.id.tvFindId);
        tvPwdError = findViewById(R.id.tvPwdError);

        goFindPwdButton = findViewById(R.id.goFindPwd);
        btnSendCode = findViewById(R.id.btnSendCode);

        etEmail = findViewById(R.id.etEmail);
        etUserId = findViewById(R.id.etUserId);
        etCode = findViewById(R.id.etCode);
    }

    private void setListeners() {
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(FindPwdActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(FindPwdActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        tvFindId.setOnClickListener(v -> {
            Intent intent = new Intent(FindPwdActivity.this, FindIdActivity.class);
            startActivity(intent);
        });

        btnSendCode.setOnClickListener(v -> handleSendCode());

        goFindPwdButton.setOnClickListener(v -> handleFindPwd());
    }

    private void handleSendCode() {
        String email = etEmail.getText().toString().trim();
        String userId = etUserId.getText().toString().trim();

        if (email.isEmpty() || userId.isEmpty()) {
            showError("이메일과 아이디를 먼저 입력해주세요.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("올바른 이메일 주소를 입력해주세요.");
            return;
        }

        hideError();

        // 실제 인증번호 발송 로직이 있다면 여기 추가
        // 지금은 화면 흐름만 막아두기 위한 처리
    }

    private void handleFindPwd() {
        String email = etEmail.getText().toString().trim();
        String userId = etUserId.getText().toString().trim();
        String code = etCode.getText().toString().trim();

        if (email.isEmpty() || userId.isEmpty() || code.isEmpty()) {
            showError("모든 칸을 채워주세요.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("올바른 이메일 주소를 입력해주세요.");
            return;
        }

        if (code.length() != 6) {
            showError("인증번호 6자리를 입력해주세요.");
            return;
        }

        hideError();

        Intent intent = new Intent(FindPwdActivity.this, RePwdActivity.class);
        startActivity(intent);
    }

    private void showError(String message) {
        tvPwdError.setText(message);
        tvPwdError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvPwdError.setText("");
        tvPwdError.setVisibility(View.GONE);
    }
}
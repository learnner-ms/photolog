package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class FindPwdActivity extends AppCompatActivity {

    private LinearLayout logoLayout;
    private TextView tvLogin, tvFindId;
    private Button goFindPwdButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_pwd);

        // XML 요소 연결
        logoLayout = findViewById(R.id.layout_logo);
        tvLogin = findViewById(R.id.tvLogin);
        tvFindId = findViewById(R.id.tvFindId);
        goFindPwdButton = findViewById(R.id.goFindPwd);

        // 🔹 로고 클릭 → 로그인 화면으로 이동
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(FindPwdActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 🔹 로그인 텍스트 클릭 → LoginActivity 이동
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(FindPwdActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // 🔹 아이디 찾기 클릭 → FindIdActivity 이동
        tvFindId.setOnClickListener(v -> {
            Intent intent = new Intent(FindPwdActivity.this, FindIdActivity.class);
            startActivity(intent);
        });

        // 🔹 확인 버튼 클릭 → 비밀번호 재설정 화면으로 이동 (activity_re_pwd)
        goFindPwdButton.setOnClickListener(v -> {
            Intent intent = new Intent(FindPwdActivity.this, RePwdActivity.class);
            startActivity(intent);
        });
    }
}

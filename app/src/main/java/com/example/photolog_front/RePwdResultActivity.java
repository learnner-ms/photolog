package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RePwdResultActivity extends AppCompatActivity {

    private LinearLayout logoLayout;
    private TextView tvLogin, tvFindId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_re_pwd_result);

        // XML 요소 연결
        logoLayout = findViewById(R.id.layout_logo);
        tvLogin = findViewById(R.id.tvLogin);
        tvFindId = findViewById(R.id.tvFindId);

        // 로고 클릭 → 로그인 화면으로 이동
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(RePwdResultActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 로그인 클릭 → LoginActivity 이동
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RePwdResultActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // 아이디 찾기 클릭 → FindIdActivity 이동
        tvFindId.setOnClickListener(v -> {
            Intent intent = new Intent(RePwdResultActivity.this, FindIdActivity.class);
            startActivity(intent);
        });
    }
}

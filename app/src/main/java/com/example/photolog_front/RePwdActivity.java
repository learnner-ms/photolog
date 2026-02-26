package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class RePwdActivity extends AppCompatActivity {

    private LinearLayout logoLayout;
    private Button goRePwdButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_re_pwd);

        // XML 요소 연결
        logoLayout = findViewById(R.id.layout_logo);
        goRePwdButton = findViewById(R.id.goRePwd);

        // 로고 클릭 시 → LoginActivity 이동
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(RePwdActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 버튼 클릭 시 → RePwdResultActivity 이동
        goRePwdButton.setOnClickListener(v -> {
            Intent intent = new Intent(RePwdActivity.this, RePwdResultActivity.class);
            startActivity(intent);
        });
    }
}

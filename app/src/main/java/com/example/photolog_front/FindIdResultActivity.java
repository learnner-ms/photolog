package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FindIdResultActivity extends AppCompatActivity {

    private LinearLayout logoLayout;
    private TextView tvLogin, tvFindPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id_result);

        // XML 연결
        logoLayout = findViewById(R.id.layout_logo);
        tvLogin = findViewById(R.id.tvLogin);
        tvFindPassword = findViewById(R.id.tvFindPassword);

        //로그인 화면
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(FindIdResultActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // LoginActivity 이동
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(FindIdResultActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // FindPwdActivity 이동
        tvFindPassword.setOnClickListener(v -> {
            Intent intent = new Intent(FindIdResultActivity.this, FindPwdActivity.class);
            startActivity(intent);
        });
    }
}

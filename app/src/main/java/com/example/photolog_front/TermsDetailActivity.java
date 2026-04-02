package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class TermsDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_TYPE = "type";

    private TextView tvTermsTitle, tvTermsContent;
    private AppCompatButton btnBack, btnAgree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_detail);

        tvTermsTitle = findViewById(R.id.tvTermsTitle);
        tvTermsContent = findViewById(R.id.tvTermsContent);
        btnBack = findViewById(R.id.btnBack);
        btnAgree = findViewById(R.id.btnAgree);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String content = getIntent().getStringExtra(EXTRA_CONTENT);
        String type = getIntent().getStringExtra(EXTRA_TYPE);

        tvTermsTitle.setText(title != null ? title : "약관");
        tvTermsContent.setText(content != null ? content : "약관 내용이 없습니다.");

        btnBack.setOnClickListener(v -> finish());

        btnAgree.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("agreed", true);
            result.putExtra("type", type);
            setResult(RESULT_OK, result);
            finish();
        });
    }
}
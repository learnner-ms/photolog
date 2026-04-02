package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

public class FindIdActivity extends AppCompatActivity {

    private EditText findNameEditText;
    private EditText findEmailEditText;
    private TextView errorTextView;
    private Button sendIdButton;
    private LinearLayout logoLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_id);

        initViews();
        setupErrorTextView();
        setListeners();
    }

    private void initViews() {
        findNameEditText = findViewById(R.id.find_name);
        findEmailEditText = findViewById(R.id.find_email);
        errorTextView = findViewById(R.id.tvGroupError);
        sendIdButton = findViewById(R.id.sendIdToEmail);
        logoLayout = findViewById(R.id.layout_logo);
    }

    private void setupErrorTextView() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(dpToPx(280), LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dpToPx(6);
        params.gravity = Gravity.CENTER_HORIZONTAL;

        errorTextView.setLayoutParams(params);
        errorTextView.setGravity(Gravity.START);
        errorTextView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
    }

    private void setListeners() {
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(FindIdActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        sendIdButton.setOnClickListener(v -> handleSendId());
    }

    private void handleSendId() {
        String name = findNameEditText.getText().toString().trim();
        String email = findEmailEditText.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            showError("모든 칸을 채워주세요.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("올바른 이메일 주소를 입력해주세요.");
            return;
        }

        hideError();

        Intent intent = new Intent(FindIdActivity.this, FindIdResultActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
    }

    private void showError(String message) {
        errorTextView.setText(message);
        errorTextView.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorTextView.setText("");
        errorTextView.setVisibility(View.GONE);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}
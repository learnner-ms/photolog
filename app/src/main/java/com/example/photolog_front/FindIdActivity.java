package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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

        // XML 연결
        findNameEditText = findViewById(R.id.find_name);
        findEmailEditText = findViewById(R.id.find_email);
        errorTextView = findViewById(R.id.tvGroupError);
        sendIdButton = findViewById(R.id.sendIdToEmail);
        logoLayout = findViewById(R.id.layout_logo);

        // 로그인 화면으로 이동
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(FindIdActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });


        sendIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = findNameEditText.getText().toString().trim();
                String email = findEmailEditText.getText().toString().trim();

                // 하나라도 비어있으면 오류 표시
                if (name.isEmpty() || email.isEmpty()) {
                    errorTextView.setVisibility(View.VISIBLE);
                    return;
                }

                // 오류 숨기기
                errorTextView.setVisibility(View.GONE);

                // 아이디 발송 완료 화면으로 이동
                Intent intent = new Intent(FindIdActivity.this, FindIdResultActivity.class);
                intent.putExtra("email", email);  // 필요시 결과 액티비티에서 표시 가능
                startActivity(intent);
            }
        });
    }
}

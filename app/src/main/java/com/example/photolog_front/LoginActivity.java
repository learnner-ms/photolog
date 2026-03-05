package com.example.photolog_front;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.UserDao;
import com.example.photolog_front.db.UserEntity;
import com.example.photolog_front.util.PasswordUtil;
import com.example.photolog_front.util.PrefsKeys;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etId, etPw;
    private TextView tvError, tvJoin;
    private Button btnLogin;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etId = findViewById(R.id.etId);
        etPw = findViewById(R.id.etPw);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);
        tvJoin = findViewById(R.id.tvJoin);

        tvJoin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String id = etId.getText().toString().trim();
        String pw = etPw.getText().toString().trim();

        if (id.isEmpty() || pw.isEmpty()) {
            showError("아이디와 비밀번호를 입력해주세요.");
            return;
        }

        loginRequest(id, pw);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(TextView.VISIBLE);
    }

    private void hideError() {
        tvError.setText("");
        tvError.setVisibility(TextView.GONE);
    }

    // Room 기반 로그인
    private void loginRequest(String id, String pw) {
        btnLogin.setEnabled(false);

        dbExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                UserDao userDao = db.userDao();

                UserEntity user = userDao.findByUsername(id);

                if (user == null) {
                    runOnUiThread(() -> {
                        btnLogin.setEnabled(true);
                        showError("아이디 또는 비밀번호가 틀렸습니다.");
                    });
                    return;
                }

                String hashedInput = PasswordUtil.sha256(pw);

                if (!hashedInput.equals(user.passwordHash)) {
                    runOnUiThread(() -> {
                        btnLogin.setEnabled(true);
                        showError("아이디 또는 비밀번호가 틀렸습니다.");
                    });
                    return;
                }

                // 로그인 성공 처리
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    hideError();

                    Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                    SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
                    prefs.edit()
                            .putLong(PrefsKeys.KEY_CURRENT_USER_ID, user.id)
                            .putString(PrefsKeys.KEY_CURRENT_USERNAME, user.username)
                            .apply();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    showError("로그인 처리 중 오류가 발생했습니다.");
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
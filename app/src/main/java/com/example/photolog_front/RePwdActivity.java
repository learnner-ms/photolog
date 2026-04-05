package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.dao.UserDao;
import com.example.photolog_front.util.PasswordUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RePwdActivity extends AppCompatActivity {

    private LinearLayout logoLayout;
    private Button goRePwdButton;
    private EditText etNewPwd;
    private EditText etNewPwdCheck;
    private TextView tvPwdError;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_re_pwd);

        initViews();
        getIntentData();
        setListeners();
    }

    private void initViews() {
        logoLayout = findViewById(R.id.layout_logo);
        goRePwdButton = findViewById(R.id.goRePwd);
        etNewPwd = findViewById(R.id.etNewPwd);
        etNewPwdCheck = findViewById(R.id.etNewPwdCheck);
        tvPwdError = findViewById(R.id.tvPwdError);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
    }

    private void setListeners() {
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(RePwdActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        goRePwdButton.setOnClickListener(v -> handleResetPassword());
    }

    private void handleResetPassword() {
        String newPwd = etNewPwd.getText().toString();
        String newPwdCheck = etNewPwdCheck.getText().toString();

        if (newPwd.isEmpty() || newPwdCheck.isEmpty()) {
            showError("모든 칸을 채워주세요.");
            return;
        }

        String passwordError = validatePassword(newPwd);
        if (passwordError != null) {
            showError(passwordError);
            return;
        }

        if (!newPwd.equals(newPwdCheck)) {
            showError("비밀번호가 일치하지 않습니다.");
            return;
        }

        if (userId == null || userId.trim().isEmpty()) {
            showError("사용자 정보가 올바르지 않습니다.");
            return;
        }

        hideError();
        updatePassword(userId, newPwd);
    }

    private String validatePassword(String password) {
        if (password.length() < 8 || password.length() > 20) {
            return "비밀번호는 8자 이상 20자 이하로 입력해주세요.";
        }

        if (password.contains(" ")) {
            return "비밀번호에는 공백을 사용할 수 없습니다.";
        }

        if (!password.matches(".*[A-Za-z].*")) {
            return "비밀번호에는 영문이 포함되어야 합니다.";
        }

        if (!password.matches(".*\\d.*")) {
            return "비밀번호에는 숫자가 포함되어야 합니다.";
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/\\\\|`~].*")) {
            return "비밀번호에는 특수문자가 포함되어야 합니다.";
        }

        return null;
    }

    private void updatePassword(String username, String newPassword) {
        goRePwdButton.setEnabled(false);

        dbExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                UserDao userDao = db.userDao();

                if (userDao.findByUsername(username) == null) {
                    runOnUiThread(() -> {
                        goRePwdButton.setEnabled(true);
                        showError("존재하지 않는 사용자입니다.");
                    });
                    return;
                }

                String hashedPassword = PasswordUtil.hashPassword(newPassword);
                int updatedRows = userDao.updatePasswordByUsername(username, hashedPassword);

                runOnUiThread(() -> {
                    goRePwdButton.setEnabled(true);

                    if (updatedRows > 0) {
                        hideError();
                        Toast.makeText(RePwdActivity.this, "비밀번호가 재설정되었습니다.", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(RePwdActivity.this, RePwdResultActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        showError("비밀번호 변경에 실패했습니다.");
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    goRePwdButton.setEnabled(true);
                    showError("비밀번호 변경 중 오류가 발생했습니다.");
                });
            }
        });
    }

    private void showError(String message) {
        tvPwdError.setText(message);
        tvPwdError.setVisibility(TextView.VISIBLE);
    }

    private void hideError() {
        tvPwdError.setText("");
        tvPwdError.setVisibility(TextView.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
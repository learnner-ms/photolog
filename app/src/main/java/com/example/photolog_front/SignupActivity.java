package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.UserDao;
import com.example.photolog_front.db.UserEntity;
import com.example.photolog_front.util.PasswordUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignupActivity extends AppCompatActivity {

    private CheckBox chkAll, chkUse, chkPrivacy, chkAd;
    private EditText signName, signId, signPwd, signPwdCheck;
    private TextView tvError;
    private Button btnSignup;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        chkAll = findViewById(R.id.chkAll);
        chkUse = findViewById(R.id.chkUse);
        chkPrivacy = findViewById(R.id.chkPrivacy);
        chkAd = findViewById(R.id.chkAd);

        signName = findViewById(R.id.signName);
        signId = findViewById(R.id.signId);
        signPwd = findViewById(R.id.signPwd);
        signPwdCheck = findViewById(R.id.signPwdCheck);

        tvError = findViewById(R.id.tvError);
        btnSignup = findViewById(R.id.btnSignup);

        // 로고 클릭 → 로그인 이동
        LinearLayout logoLayout = findViewById(R.id.layout_logo);
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // 전체 선택
        chkAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 개별 체크가 바뀔 때 chkAll도 바뀌므로, 불필요한 연쇄를 막고 싶으면
            // 여기서만 setChecked하고, updateAllChecked에서는 chkAll을 직접 건드리지 않아도 됨.
            chkUse.setChecked(isChecked);
            chkPrivacy.setChecked(isChecked);
            chkAd.setChecked(isChecked);
        });

        chkUse.setOnCheckedChangeListener((b, c) -> updateAllChecked());
        chkPrivacy.setOnCheckedChangeListener((b, c) -> updateAllChecked());
        chkAd.setOnCheckedChangeListener((b, c) -> updateAllChecked());

        btnSignup.setOnClickListener(v -> checkSignup());
    }

    private void updateAllChecked() {
        boolean allChecked = chkUse.isChecked() && chkPrivacy.isChecked() && chkAd.isChecked();
        // 무한 루프는 발생하지 않지만, 깔끔하게 유지
        if (chkAll.isChecked() != allChecked) chkAll.setChecked(allChecked);
    }

    private void checkSignup() {
        String name = signName.getText().toString().trim();
        String id = signId.getText().toString().trim();
        String pw = signPwd.getText().toString();
        String pwCheck = signPwdCheck.getText().toString();

        if (name.isEmpty() || id.isEmpty() || pw.isEmpty() || pwCheck.isEmpty()) {
            showError("모든 칸을 채워주세요!");
            return;
        }

        if (!pw.equals(pwCheck)) {
            showError("비밀번호가 일치하지 않습니다.");
            return;
        }

        // 필수 약관 체크 (use / privacy)
        if (!chkUse.isChecked() || !chkPrivacy.isChecked()) {
            showError("필수 약관에 동의해야 합니다.");
            return;
        }

        hideError();
        requestSignup(name, id, pw);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void hideError() {
        tvError.setText("");
        tvError.setVisibility(View.GONE);
    }

    // Room 기반 회원가입
    private void requestSignup(String name, String id, String pw) {
        btnSignup.setEnabled(false);

        dbExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                UserDao userDao = db.userDao();

                // 1) 아이디 중복 체크
                UserEntity existing = userDao.findByUsername(id);
                if (existing != null) {
                    runOnUiThread(() -> {
                        btnSignup.setEnabled(true);
                        showError("이미 존재하는 아이디입니다.");
                    });
                    return;
                }

                // 2) 저장할 사용자 생성
                UserEntity user = new UserEntity();
                user.name = name;
                user.username = id;
                user.passwordHash = PasswordUtil.sha256(pw);
                user.createdAt = System.currentTimeMillis();

                // 3) insert
                // - UserDao.insert가 long 리턴이면 id를 받아올 수 있음
                // - (ABORT 정책이면 중복/제약 위반 시 예외)
                long newId = userDao.insert(user);

                runOnUiThread(() -> {
                    btnSignup.setEnabled(true);
                    hideError();
                    Toast.makeText(SignupActivity.this, "회원가입 완료!", Toast.LENGTH_SHORT).show();

                    // 회원가입 직후 자동 로그인까지는 안 하고, 로그인 화면으로 이동
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSignup.setEnabled(true);
                    showError("회원가입 실패");
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
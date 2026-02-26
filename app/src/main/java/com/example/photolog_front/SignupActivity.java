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

    CheckBox chkAll, chkUse, chkPrivacy, chkAd;
    EditText signName, signId, signPwd, signPwdCheck;
    TextView tvError;
    Button btnSignup;

    // Room 작업은 메인스레드에서 하면 크래시 날 수 있어서 Executor 사용
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // 체크박스
        chkAll = findViewById(R.id.chkAll);
        chkUse = findViewById(R.id.chkUse);
        chkPrivacy = findViewById(R.id.chkPrivacy);
        chkAd = findViewById(R.id.chkAd);

        // 입력칸
        signName = findViewById(R.id.signName);
        signId = findViewById(R.id.signId);
        signPwd = findViewById(R.id.signPwd);
        signPwdCheck = findViewById(R.id.signPwdCheck);

        // 에러 문구
        tvError = findViewById(R.id.tvError);

        // 회원가입 버튼
        btnSignup = findViewById(R.id.btnSignup);

        // 로고 클릭 → 로그인 이동
        LinearLayout logoLayout = findViewById(R.id.layout_logo);
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 전체 선택
        chkAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chkUse.setChecked(isChecked);
            chkPrivacy.setChecked(isChecked);
            chkAd.setChecked(isChecked);
        });

        chkUse.setOnCheckedChangeListener((b, c) -> updateAllChecked());
        chkPrivacy.setOnCheckedChangeListener((b, c) -> updateAllChecked());
        chkAd.setOnCheckedChangeListener((b, c) -> updateAllChecked());

        // 회원가입 버튼 클릭
        btnSignup.setOnClickListener(v -> checkSignup());
    }

    // 전체 체크 업데이트
    private void updateAllChecked() {
        boolean allChecked = chkUse.isChecked() && chkPrivacy.isChecked() && chkAd.isChecked();
        chkAll.setChecked(allChecked);
    }

    // 입력 검증
    private void checkSignup() {

        String name = signName.getText().toString().trim();
        String id = signId.getText().toString().trim();
        String pw = signPwd.getText().toString().trim();
        String pwCheck = signPwdCheck.getText().toString().trim();

        // 빈칸 체크
        if (name.isEmpty() || id.isEmpty() || pw.isEmpty() || pwCheck.isEmpty()) {
            showError("모든 칸을 채워주세요!");
            return;
        }

        // 비밀번호 일치 확인
        if (!pw.equals(pwCheck)) {
            showError("비밀번호가 일치하지 않습니다.");
            return;
        }

        // 필수 약관 체크
        if (!chkUse.isChecked() || !chkPrivacy.isChecked()) {
            showError("필수 약관에 동의해야 합니다.");
            return;
        }

        // 여기까지 통과하면 Room에 저장
        tvError.setVisibility(View.GONE);
        requestSignup(name, id, pw);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    //Room 기반 회원가입(로컬 DB 저장)
    private void requestSignup(String name, String id, String pw) {

        // 버튼 중복 클릭 방지(선택)
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
                user.passwordHash = PasswordUtil.sha256(pw); // 해시 저장
                user.createdAt = System.currentTimeMillis();

                // 3) DB insert
                userDao.insert(user);

                // 4) 성공 처리 (UI 스레드)
                runOnUiThread(() -> {
                    btnSignup.setEnabled(true);
                    Toast.makeText(SignupActivity.this, "회원가입 완료!", Toast.LENGTH_SHORT).show();

                    // 로그인 페이지로 이동
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSignup.setEnabled(true);
                    showError("회원가입 실패: " + e.getMessage());
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
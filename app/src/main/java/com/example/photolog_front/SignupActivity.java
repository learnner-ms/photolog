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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.dao.UserDao;
import com.example.photolog_front.db.entity.UserEntity;
import com.example.photolog_front.util.PasswordUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignupActivity extends AppCompatActivity {

    private CheckBox chkAll;
    private CheckBox chkUse;
    private CheckBox chkPrivacy;
    private CheckBox chkAd;

    private TextView tvTermsUse;
    private TextView tvTermsPrivacy;
    private TextView tvTermsAd;
    private TextView tvError;

    private EditText signName;
    private EditText signId;
    private EditText signPwd;
    private EditText signPwdCheck;

    private Button btnSignup;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    // 전체동의 체크 상태 갱신 중 중복 반응 방지
    private boolean isUpdatingAllCheckState = false;

    // 약관 상세 화면 결과 받기
    private final ActivityResultLauncher<Intent> termsLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                            return;
                        }

                        boolean agreed = result.getData().getBooleanExtra("agreed", false);
                        String type = result.getData().getStringExtra("type");

                        if (!agreed || type == null) {
                            return;
                        }

                        switch (type) {
                            case "use":
                                chkUse.setChecked(true);
                                break;
                            case "privacy":
                                chkPrivacy.setChecked(true);
                                break;
                            case "ad":
                                chkAd.setChecked(true);
                                break;
                        }

                        updateAllChecked();
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        initViews();
        setListeners();
    }

    private void initViews() {
        chkAll = findViewById(R.id.chkAll);
        chkUse = findViewById(R.id.chkUse);
        chkPrivacy = findViewById(R.id.chkPrivacy);
        chkAd = findViewById(R.id.chkAd);

        tvTermsUse = findViewById(R.id.tvTermsUse);
        tvTermsPrivacy = findViewById(R.id.tvTermsPrivacy);
        tvTermsAd = findViewById(R.id.tvTermsAd);

        signName = findViewById(R.id.signName);
        signId = findViewById(R.id.signId);
        signPwd = findViewById(R.id.signPwd);
        signPwdCheck = findViewById(R.id.signPwdCheck);

        tvError = findViewById(R.id.tvError);
        btnSignup = findViewById(R.id.btnSignup);
        tvTermsUse.setPaintFlags(tvTermsUse.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        tvTermsPrivacy.setPaintFlags(tvTermsPrivacy.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        tvTermsAd.setPaintFlags(tvTermsAd.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
    }

    private void setListeners() {
        LinearLayout logoLayout = findViewById(R.id.layout_logo);
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        chkAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingAllCheckState) {
                return;
            }

            chkUse.setChecked(isChecked);
            chkPrivacy.setChecked(isChecked);
            chkAd.setChecked(isChecked);
        });

        chkUse.setOnCheckedChangeListener((buttonView, isChecked) -> updateAllChecked());
        chkPrivacy.setOnCheckedChangeListener((buttonView, isChecked) -> updateAllChecked());
        chkAd.setOnCheckedChangeListener((buttonView, isChecked) -> updateAllChecked());

        tvTermsUse.setOnClickListener(v ->
                openTermsDetail(
                        "이용 약관",
                        getUseTermsContent(),
                        "use"
                )
        );

        tvTermsPrivacy.setOnClickListener(v ->
                openTermsDetail(
                        "개인정보 수집 및 이용 동의",
                        getPrivacyTermsContent(),
                        "privacy"
                )
        );

        tvTermsAd.setOnClickListener(v ->
                openTermsDetail(
                        "맞춤형 광고 수신 동의",
                        getAdTermsContent(),
                        "ad"
                )
        );

        btnSignup.setOnClickListener(v -> checkSignup());
    }

    private void updateAllChecked() {
        boolean allChecked = chkUse.isChecked() && chkPrivacy.isChecked() && chkAd.isChecked();

        isUpdatingAllCheckState = true;
        chkAll.setChecked(allChecked);
        isUpdatingAllCheckState = false;
    }

    private void openTermsDetail(String title, String content, String type) {
        Intent intent = new Intent(SignupActivity.this, TermsDetailActivity.class);
        intent.putExtra(TermsDetailActivity.EXTRA_TITLE, title);
        intent.putExtra(TermsDetailActivity.EXTRA_CONTENT, content);
        intent.putExtra(TermsDetailActivity.EXTRA_TYPE, type);
        termsLauncher.launch(intent);
    }

    private void checkSignup() {
        String name = signName.getText().toString().trim();
        String id = signId.getText().toString().trim();
        String pw = signPwd.getText().toString();
        String pwCheck = signPwdCheck.getText().toString();

        if (name.isEmpty() || id.isEmpty() || pw.isEmpty() || pwCheck.isEmpty()) {
            showError("모든 칸을 채워주세요.");
            return;
        }

        if (!pw.equals(pwCheck)) {
            showError("비밀번호가 일치하지 않습니다.");
            return;
        }

        if (!chkUse.isChecked() || !chkPrivacy.isChecked()) {
            showError("필수 약관에 동의해야 합니다.");
            return;
        }

        hideError();
        requestSignup(name, id, pw);
    }

    private void requestSignup(String name, String id, String pw) {
        btnSignup.setEnabled(false);

        dbExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                UserDao userDao = db.userDao();

                UserEntity existingUser = userDao.findByUsername(id);
                if (existingUser != null) {
                    runOnUiThread(() -> {
                        btnSignup.setEnabled(true);
                        showError("이미 존재하는 아이디입니다.");
                    });
                    return;
                }

                UserEntity user = new UserEntity();
                user.name = name;
                user.username = id;
                user.passwordHash = PasswordUtil.sha256(pw);
                user.createdAt = System.currentTimeMillis();

                userDao.insert(user);

                runOnUiThread(() -> {
                    btnSignup.setEnabled(true);
                    hideError();
                    Toast.makeText(SignupActivity.this, "회원가입 완료!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSignup.setEnabled(true);
                    showError("회원가입 처리 중 오류가 발생했습니다.");
                });
            }
        });
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setText("");
        tvError.setVisibility(View.GONE);
    }

    private String getUseTermsContent() {
        return "제1조(목적)\n\n"
                + "본 약관은 포토로그 서비스의 이용과 관련하여 필요한 사항을 규정합니다.\n\n"
                + "제2조(서비스 내용)\n\n"
                + "포토로그는 사진, 음성, 텍스트 입력을 바탕으로 AI 일기 작성 기능을 제공합니다.\n\n"
                + "제3조(회원의 의무)\n\n"
                + "회원은 타인의 정보를 도용하거나 서비스 운영을 방해해서는 안 됩니다.\n\n"
                + "제4조(서비스 이용 제한)\n\n"
                + "운영 정책 위반 시 서비스 이용이 제한될 수 있습니다.";
    }

    private String getPrivacyTermsContent() {
        return "1. 수집 항목\n\n"
                + "- 이름\n"
                + "- 아이디\n"
                + "- 비밀번호\n"
                + "- 프로필 이미지(선택)\n"
                + "- 사진, 음성, 텍스트 입력 데이터\n\n"
                + "2. 이용 목적\n\n"
                + "- 회원 식별 및 계정 관리\n"
                + "- AI 일기 생성 서비스 제공\n"
                + "- 가족 그룹 공유 기능 제공\n"
                + "- 서비스 품질 개선 및 오류 대응\n\n"
                + "3. 보관 기간\n\n"
                + "- 회원 탈퇴 시 또는 수집 목적 달성 후 관련 법령에 따라 보관 후 파기합니다.";
    }

    private String getAdTermsContent() {
        return "맞춤형 광고 수신 동의는 선택 사항입니다.\n\n"
                + "동의 시 서비스 이용 기록 등을 바탕으로 맞춤형 정보 또는 광고성 안내를 제공할 수 있습니다.\n\n"
                + "동의하지 않아도 서비스 이용에는 제한이 없습니다.";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
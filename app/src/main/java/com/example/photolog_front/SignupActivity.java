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

    private LinearLayout layoutAllTerms;
    private LinearLayout layoutUseTerms;
    private LinearLayout layoutPrivacyTerms;
    private LinearLayout layoutAdTerms;

    private TextView tvTermsUse;
    private TextView tvTermsPrivacy;
    private TextView tvTermsAd;

    private TextView tvViewUse;
    private TextView tvViewPrivacy;
    private TextView tvViewAd;

    private TextView tvError;

    private EditText signName;
    private EditText signId;
    private EditText signPwd;
    private EditText signPwdCheck;

    private Button btnSignup;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private boolean isUpdatingAllCheckState = false;

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
        prepareTermsUiBehavior();
        setListeners();
    }

    private void initViews() {
        chkAll = findViewById(R.id.chkAll);
        chkUse = findViewById(R.id.chkUse);
        chkPrivacy = findViewById(R.id.chkPrivacy);
        chkAd = findViewById(R.id.chkAd);

        layoutAllTerms = findViewById(R.id.layoutAllTerms);
        layoutUseTerms = findViewById(R.id.layoutUseTerms);
        layoutPrivacyTerms = findViewById(R.id.layoutPrivacyTerms);
        layoutAdTerms = findViewById(R.id.layoutAdTerms);

        tvTermsUse = findViewById(R.id.tvTermsUse);
        tvTermsPrivacy = findViewById(R.id.tvTermsPrivacy);
        tvTermsAd = findViewById(R.id.tvTermsAd);

        tvViewUse = findViewById(R.id.tvViewUse);
        tvViewPrivacy = findViewById(R.id.tvViewPrivacy);
        tvViewAd = findViewById(R.id.tvViewAd);

        signName = findViewById(R.id.signName);
        signId = findViewById(R.id.signId);
        signPwd = findViewById(R.id.signPwd);
        signPwdCheck = findViewById(R.id.signPwdCheck);

        tvError = findViewById(R.id.tvError);
        btnSignup = findViewById(R.id.btnSignup);

        tvTermsUse.setPaintFlags(tvTermsUse.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        tvTermsPrivacy.setPaintFlags(tvTermsPrivacy.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        tvTermsAd.setPaintFlags(tvTermsAd.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);

        tvViewUse.setPaintFlags(tvViewUse.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        tvViewPrivacy.setPaintFlags(tvViewPrivacy.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        tvViewAd.setPaintFlags(tvViewAd.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
    }

    private void prepareTermsUiBehavior() {
        disableLayoutClick(layoutAllTerms);
        disableLayoutClick(layoutUseTerms);
        disableLayoutClick(layoutPrivacyTerms);
        disableLayoutClick(layoutAdTerms);

        enableCheckboxInteraction(chkAll);
        enableCheckboxInteraction(chkUse);
        enableCheckboxInteraction(chkPrivacy);
        enableCheckboxInteraction(chkAd);

        enableTextInteraction(tvTermsUse);
        enableTextInteraction(tvTermsPrivacy);
        enableTextInteraction(tvTermsAd);

        enableTextInteraction(tvViewUse);
        enableTextInteraction(tvViewPrivacy);
        enableTextInteraction(tvViewAd);
    }

    private void disableLayoutClick(LinearLayout layout) {
        if (layout == null) return;
        layout.setClickable(false);
        layout.setFocusable(false);
    }

    private void enableCheckboxInteraction(CheckBox checkBox) {
        if (checkBox == null) return;
        checkBox.setClickable(true);
        checkBox.setFocusable(true);
        checkBox.setEnabled(true);
    }

    private void enableTextInteraction(TextView textView) {
        if (textView == null) return;
        textView.setClickable(true);
        textView.setFocusable(true);
        textView.setEnabled(true);
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
            if (isUpdatingAllCheckState) return;

            isUpdatingAllCheckState = true;
            chkUse.setChecked(isChecked);
            chkPrivacy.setChecked(isChecked);
            chkAd.setChecked(isChecked);
            isUpdatingAllCheckState = false;
        });

        chkUse.setOnCheckedChangeListener((buttonView, isChecked) -> updateAllChecked());
        chkPrivacy.setOnCheckedChangeListener((buttonView, isChecked) -> updateAllChecked());
        chkAd.setOnCheckedChangeListener((buttonView, isChecked) -> updateAllChecked());

        tvTermsUse.setOnClickListener(v ->
                openTermsDetail("이용 약관", "terms/use_terms.md", "use"));

        tvTermsPrivacy.setOnClickListener(v ->
                openTermsDetail("개인정보 처리방침", "terms/privacy_terms.md", "privacy"));

        tvTermsAd.setOnClickListener(v ->
                openTermsDetail("맞춤형 광고 수신 동의", "terms/ad_terms.md", "ad"));

        tvViewUse.setOnClickListener(v ->
                openTermsDetail("이용 약관", "terms/use_terms.md", "use"));

        tvViewPrivacy.setOnClickListener(v ->
                openTermsDetail("개인정보 처리방침", "terms/privacy_terms.md", "privacy"));

        tvViewAd.setOnClickListener(v ->
                openTermsDetail("맞춤형 광고 수신 동의", "terms/ad_terms.md", "ad"));

        btnSignup.setOnClickListener(v -> checkSignup());
    }

    private void updateAllChecked() {
        if (isUpdatingAllCheckState) return;

        boolean allChecked = chkUse.isChecked() && chkPrivacy.isChecked() && chkAd.isChecked();

        isUpdatingAllCheckState = true;
        chkAll.setChecked(allChecked);
        isUpdatingAllCheckState = false;
    }

    private void openTermsDetail(String title, String assetPath, String type) {
        Intent intent = new Intent(SignupActivity.this, TermsDetailActivity.class);
        intent.putExtra(TermsDetailActivity.EXTRA_TITLE, title);
        intent.putExtra(TermsDetailActivity.EXTRA_ASSET_PATH, assetPath);
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

        String passwordError = validatePassword(pw);
        if (passwordError != null) {
            showError(passwordError);
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
                user.passwordHash = PasswordUtil.hashPassword(pw);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
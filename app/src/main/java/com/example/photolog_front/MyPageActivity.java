package com.example.photolog_front;

import com.example.photolog_front.util.PrefsKeys;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.bumptech.glide.Glide;
import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.dao.DiaryDao;
import com.example.photolog_front.db.dao.UserDao;
import com.example.photolog_front.db.entity.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyPageActivity extends AppCompatActivity {

    private TextView tvNickname, tvDiaryCount, tvFamilyCount;
    private LinearLayout layoutLogo;

    private AppCompatButton btnTopAction;

    private View sectionNoGroup;
    private View sectionMyGroup;

    private View btnJoinGroup;
    private View btnCreateGroup;

    private View btnDeleteAiData;
    private View btnWithdraw;

    // 프로필 이미지
    private ImageView imgProfile;

    // 프로필 사진 선택 런처
    private ActivityResultLauncher<String> pickImageLauncher;

    private AppDatabase db;
    private UserDao userDao;
    private DiaryDao diaryDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private enum GroupState { NONE, OWNER, MEMBER }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        db = AppDatabase.getInstance(getApplicationContext());
        userDao = db.userDao();
        diaryDao = db.diaryDao();

        initViews();
        setupProfilePicker();
        setListeners();

        loadUserDataFromRoom();

        // ✅ 그룹 연동 전 임시
        bindGroupUi(GroupState.NONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }

    private void initViews() {
        layoutLogo = findViewById(R.id.layout_logo);

        tvNickname = findViewById(R.id.tvNickname);
        tvDiaryCount = findViewById(R.id.tvDiaryCount);
        tvFamilyCount = findViewById(R.id.tvFamilyCount);

        btnTopAction = findViewById(R.id.btn_top_action);

        sectionNoGroup = findViewById(R.id.section_no_group);
        sectionMyGroup = findViewById(R.id.section_my_group);

        btnJoinGroup = findViewById(R.id.btn_join_group);
        btnCreateGroup = findViewById(R.id.btn_create_group);

        btnDeleteAiData = findViewById(R.id.btn_delete_ai_data);
        btnWithdraw = findViewById(R.id.btn_withdraw);

        imgProfile = findViewById(R.id.profile);
    }

    /**
     * 프로필 사진 선택 기능
     * - 저장된 profileUri가 있으면 로드
     * - 클릭하면 갤러리 열고 선택한 이미지 circleCrop 적용 후 저장
     */
    private void setupProfilePicker() {
        // 1) 저장된 프로필 URI 로드
        SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
        String saved = prefs.getString(PrefsKeys.KEY_PROFILE_URI, null);

        if (saved != null && !saved.trim().isEmpty()) {
            try {
                Uri uri = Uri.parse(saved);
                Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .placeholder(R.drawable.profile)
                        .error(R.drawable.profile)
                        .into(imgProfile);
            } catch (Exception e) {
                Glide.with(this).load(R.drawable.profile).circleCrop().into(imgProfile);
            }
        } else {
            Glide.with(this).load(R.drawable.profile).circleCrop().into(imgProfile);
        }

        // 2) 런처 등록
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;

                    Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .placeholder(R.drawable.profile)
                            .error(R.drawable.profile)
                            .into(imgProfile);

                    // 저장 (PrefsKeys로 통일)
                    getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE)
                            .edit()
                            .putString(PrefsKeys.KEY_PROFILE_URI, uri.toString())
                            .apply();

                    Toast.makeText(this, "프로필 사진이 변경되었습니다.", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void setListeners() {
        layoutLogo.setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 그룹 참여하기
        btnJoinGroup.setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, MakeGroupResponse.class);
            startActivity(intent);
        });

        // 그룹 생성하기
        btnCreateGroup.setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, MakeGroupActivity.class);
            startActivity(intent);
        });

        // 우상단 액션 버튼(OWNER/MEMBER에서만 보임)
        btnTopAction.setOnClickListener(v -> {
            CharSequence label = btnTopAction.getText();
            if ("요청 관리".contentEquals(label)) {
                Toast.makeText(this, "요청 관리 기능은 추후 연동 예정입니다.", Toast.LENGTH_SHORT).show();
            } else if ("그룹 탈퇴".contentEquals(label)) {
                Toast.makeText(this, "그룹 탈퇴 기능은 추후 연동 예정입니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 개인정보 보호
        btnDeleteAiData.setOnClickListener(v -> showDeleteAllDiaryDialogCustom());
        btnWithdraw.setOnClickListener(v -> showWithdrawDialogCustom());

        // 프로필 클릭 → 갤러리 열기
        imgProfile.setOnClickListener(v -> {
            if (pickImageLauncher != null) pickImageLauncher.launch("image/*");
        });
    }

    private long getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
        return prefs.getLong(PrefsKeys.KEY_CURRENT_USER_ID, -1L);
    }

    private void loadUserDataFromRoom() {
        final long userId = getCurrentUserId();

        if (userId <= 0) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        io.execute(() -> {
            UserEntity user = userDao.findById(userId);
            int diaryCount = diaryDao.countByUserId(userId);

            runOnUiThread(() -> {
                if (user == null) {
                    Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                tvNickname.setText(
                        user.name != null && !user.name.trim().isEmpty() ? user.name : "닉네임"
                );
                tvDiaryCount.setText("작성한 일기 수 : " + diaryCount);

                // ✅ 그룹 연동 전 임시
                tvFamilyCount.setText("추가한 가족 수 : 0");
            });
        });
    }

    private void bindGroupUi(GroupState state) {
        if (state == GroupState.NONE) {
            sectionNoGroup.setVisibility(View.VISIBLE);
            sectionMyGroup.setVisibility(View.GONE);
            btnTopAction.setVisibility(View.GONE);

        } else if (state == GroupState.OWNER) {
            sectionNoGroup.setVisibility(View.GONE);
            sectionMyGroup.setVisibility(View.VISIBLE);
            btnTopAction.setVisibility(View.VISIBLE);
            btnTopAction.setText("요청 관리");

        } else { // MEMBER
            sectionNoGroup.setVisibility(View.GONE);
            sectionMyGroup.setVisibility(View.VISIBLE);
            btnTopAction.setVisibility(View.VISIBLE);
            btnTopAction.setText("그룹 탈퇴");
        }
    }

    // =========================================================
    // ✅ 커스텀 공지 다이얼로그 (dialog_notice_common 사용)
    // =========================================================

    private void showDeleteAllDiaryDialogCustom() {
        String title = "AI 및 개인정보 전체 삭제";
        String body =
                "📓 저장된 AI 일기 데이터가 모두 삭제됩니다.\n\n" +
                        "💬 사진 분석 및 대화 기록이 함께 제거됩니다.\n\n" +
                        "⚠️ 삭제된 데이터는 복구할 수 없습니다.";
        String agreeText = "위 내용을 확인했으며 삭제에 동의합니다.";

        showCommonNoticeDialog(
                title,
                body,
                agreeText,
                "삭제하기",
                "취소",
                this::deleteAllMyDiaries
        );
    }

    private void showWithdrawDialogCustom() {
        String title = "회원 탈퇴";
        String body =
                "👤 계정 정보가 모두 삭제됩니다.\n\n" +
                        "📓 작성한 AI 일기 데이터도 함께 삭제됩니다.\n\n" +
                        "⚠️ 탈퇴 후에는 복구할 수 없습니다.";
        String agreeText = "위 내용을 확인했으며 탈퇴에 동의합니다.";

        showCommonNoticeDialog(
                title,
                body,
                agreeText,
                "탈퇴하기",
                "취소",
                this::withdrawAccount
        );
    }

    private void showCommonNoticeDialog(
            String title,
            String body,
            String agreeText,
            String primaryLabel,
            String secondaryLabel,
            Runnable onConfirmed
    ) {
        View view = getLayoutInflater().inflate(R.layout.dialog_notice_common, null);

        TextView tvTitle = view.findViewById(R.id.tv_notice_title);
        TextView tvBody = view.findViewById(R.id.tv_notice_body);

        CheckBox cbAgree = view.findViewById(R.id.cb_notice_agree);
        TextView tvAgree = view.findViewById(R.id.tv_notice_agree);

        AppCompatButton btnPrimary = view.findViewById(R.id.btn_notice_primary);
        AppCompatButton btnSecondary = view.findViewById(R.id.btn_notice_secondary);

        tvTitle.setText(title);
        tvBody.setText(body);
        tvAgree.setText(agreeText);

        btnPrimary.setText(primaryLabel);
        btnSecondary.setText(secondaryLabel);

        // 체크 전에는 비활성화
        btnPrimary.setEnabled(false);
        btnPrimary.setAlpha(0.45f);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        cbAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnPrimary.setEnabled(isChecked);
            btnPrimary.setAlpha(isChecked ? 1.0f : 0.45f);
        });

        btnSecondary.setOnClickListener(v -> dialog.dismiss());

        btnPrimary.setOnClickListener(v -> {
            if (!cbAgree.isChecked()) {
                Toast.makeText(this, "동의 체크가 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            if (onConfirmed != null) onConfirmed.run();
        });

        dialog.show();
    }

    // ------------------------
    // 개인정보 보호: 전체 삭제 로직
    // ------------------------
    private void deleteAllMyDiaries() {
        final long userId = getCurrentUserId();
        if (userId <= 0) return;

        io.execute(() -> {
            diaryDao.deleteAllByUser(userId);

            runOnUiThread(() -> {
                Toast.makeText(this, "내 일기 데이터가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                loadUserDataFromRoom();
            });
        });
    }

    // ------------------------
    // 개인정보 보호: 회원 탈퇴 로직
    // ------------------------
    private void withdrawAccount() {
        final long userId = getCurrentUserId();
        if (userId <= 0) return;

        io.execute(() -> {
            diaryDao.deleteAllByUser(userId);
            userDao.deleteById(userId);

            runOnUiThread(() -> {
                // SharedPreferences 초기화 (PrefsKeys로 통일)
                SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
                prefs.edit().clear().apply();

                Toast.makeText(this, "탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }
}
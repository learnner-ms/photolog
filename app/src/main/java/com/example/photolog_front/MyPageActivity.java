package com.example.photolog_front;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.DiaryDao;
import com.example.photolog_front.db.UserDao;
import com.example.photolog_front.db.UserEntity;

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

    // ✅ 디버그 버튼
    private View btnDebugNone;
    private View btnDebugOwner;
    private View btnDebugMember;

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
        setListeners();

        loadUserDataFromRoom();

        // 기본 화면(테스트용)
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

        // ✅ 디버그 버튼 연결
        btnDebugNone = findViewById(R.id.btn_debug_none);
        btnDebugOwner = findViewById(R.id.btn_debug_owner);
        btnDebugMember = findViewById(R.id.btn_debug_member);
    }

    private void setListeners() {
        layoutLogo.setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        btnJoinGroup.setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, MakeGroupResponse.class);
            startActivity(intent);
        });

        btnCreateGroup.setOnClickListener(v -> {
            Intent intent = new Intent(MyPageActivity.this, MakeGroupActivity.class);
            startActivity(intent);
        });

        btnTopAction.setOnClickListener(v -> {
            CharSequence label = btnTopAction.getText();
            if ("요청 관리".contentEquals(label)) {
                Toast.makeText(this, "요청 관리 기능은 추후 연동 예정입니다.", Toast.LENGTH_SHORT).show();
            } else if ("그룹 탈퇴".contentEquals(label)) {
                Toast.makeText(this, "그룹 탈퇴 기능은 추후 연동 예정입니다.", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeleteAiData.setOnClickListener(v -> showDeleteAllDiaryDialog());
        btnWithdraw.setOnClickListener(v -> showWithdrawDialog());

        // ✅ 디버그: 상태 토글
        btnDebugNone.setOnClickListener(v -> bindGroupUi(GroupState.NONE));
        btnDebugOwner.setOnClickListener(v -> bindGroupUi(GroupState.OWNER));
        btnDebugMember.setOnClickListener(v -> bindGroupUi(GroupState.MEMBER));
    }

    private long getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        return prefs.getLong("userId", -1L);
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

                tvNickname.setText(user.name != null ? user.name : "닉네임");
                tvDiaryCount.setText("작성한 일기 수 : " + diaryCount);

                // 그룹 연동 전 임시
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

    // ------------------------
    // 개인정보 보호: 전체 삭제
    // ------------------------
    private void showDeleteAllDiaryDialog() {
        String msg =
                "📓 저장된 AI 일기 데이터가 모두 삭제됩니다.\n" +
                        "💬 사진 분석 및 대화 기록이 함께 제거됩니다.\n" +
                        "⚠️ 삭제된 데이터는 복구할 수 없습니다.";

        new AlertDialog.Builder(this)
                .setTitle("AI 일기 데이터 삭제 안내")
                .setMessage(msg)
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제하기", (d, w) -> deleteAllMyDiaries())
                .show();
    }

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
    // 개인정보 보호: 회원 탈퇴
    // ------------------------
    private void showWithdrawDialog() {
        String msg =
                "👤 계정 정보가 모두 삭제됩니다.\n" +
                        "📓 작성한 AI 일기 데이터도 함께 삭제됩니다.\n" +
                        "⚠️ 탈퇴 후에는 복구할 수 없습니다.";

        new AlertDialog.Builder(this)
                .setTitle("회원 탈퇴 안내")
                .setMessage(msg)
                .setNegativeButton("취소", null)
                .setPositiveButton("탈퇴하기", (d, w) -> withdrawAccount())
                .show();
    }

    private void withdrawAccount() {
        final long userId = getCurrentUserId();
        if (userId <= 0) return;

        io.execute(() -> {
            diaryDao.deleteAllByUser(userId);
            userDao.deleteById(userId);

            runOnUiThread(() -> {
                SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
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
package com.example.photolog_front;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.dao.GroupDao;
import com.example.photolog_front.db.entity.GroupEntity;
import com.example.photolog_front.db.entity.GroupMemberEntity;
import com.example.photolog_front.util.PrefsKeys;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MakeGroupResponse extends AppCompatActivity {

    private EditText inviteCodeEditText;
    private TextView errorTextView;
    private Button confirmButton;
    private View logoLayout;

    private AppDatabase db;
    private GroupDao groupDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_group_response);

        db = AppDatabase.getInstance(getApplicationContext());
        groupDao = db.groupDao();

        initViews();
        setListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }

    private void initViews() {
        inviteCodeEditText = findViewById(R.id.max_member);
        errorTextView = findViewById(R.id.tvGroupError);
        confirmButton = findViewById(R.id.btnLogin);
        logoLayout = findViewById(R.id.layout_logo);

        hideError();
    }

    private void setListeners() {
        logoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(MakeGroupResponse.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        confirmButton.setOnClickListener(v -> validateCode());
    }

    private long getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
        return prefs.getLong(PrefsKeys.KEY_CURRENT_USER_ID, -1L);
    }

    private void validateCode() {
        String inviteCode = inviteCodeEditText.getText().toString().trim().toUpperCase();

        if (inviteCode.isEmpty()) {
            showError("초대코드를 입력해주세요.");
            return;
        }

        long currentUserId = getCurrentUserId();
        if (currentUserId <= 0) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        hideError();
        confirmButton.setEnabled(false);

        io.execute(() -> {
            try {
                GroupMemberEntity myMembership = groupDao.findMembershipByUserId(currentUserId);
                GroupEntity targetGroup = groupDao.findByInviteCode(inviteCode);

                if (targetGroup == null) {
                    runOnUiThread(() -> {
                        confirmButton.setEnabled(true);
                        showError("잘못된 초대코드입니다.");
                    });
                    return;
                }

                if (myMembership != null) {
                    GroupEntity myGroup = groupDao.findGroupByUserId(currentUserId);

                    runOnUiThread(() -> {
                        confirmButton.setEnabled(true);

                        if (myGroup != null && myGroup.id == targetGroup.id) {
                            showError("이미 가입된 그룹입니다.");
                        } else {
                            showError("이미 다른 그룹에 가입되어 있습니다.");
                        }
                    });
                    return;
                }

                int memberCount = groupDao.countMembers(targetGroup.id);
                if (memberCount >= targetGroup.maxMember) {
                    runOnUiThread(() -> {
                        confirmButton.setEnabled(true);
                        showError("그룹 정원이 가득 찼습니다.");
                    });
                    return;
                }

                int exists = groupDao.existsMember(targetGroup.id, currentUserId);
                if (exists > 0) {
                    runOnUiThread(() -> {
                        confirmButton.setEnabled(true);
                        showError("이미 가입된 그룹입니다.");
                    });
                    return;
                }

                groupDao.insertGroupMember(new GroupMemberEntity(targetGroup.id, currentUserId, "MEMBER"));

                runOnUiThread(() -> {
                    confirmButton.setEnabled(true);
                    hideError();
                    Toast.makeText(MakeGroupResponse.this, "그룹 참여 성공!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MakeGroupResponse.this, FamilyDiaryActivity.class);
                    intent.putExtra("invite_code", inviteCode);
                    intent.putExtra("group_name", targetGroup.groupName);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    confirmButton.setEnabled(true);
                    showError("그룹 참여 중 오류가 발생했습니다.");
                });
            }
        });
    }

    private void showError(String message) {
        errorTextView.setText(message);
        errorTextView.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorTextView.setText("");
        errorTextView.setVisibility(View.GONE);
    }
}
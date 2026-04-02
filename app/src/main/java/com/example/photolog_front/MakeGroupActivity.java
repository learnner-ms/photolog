package com.example.photolog_front;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.photolog_front.db.AppDatabase;
import com.example.photolog_front.db.dao.GroupDao;
import com.example.photolog_front.db.entity.GroupEntity;
import com.example.photolog_front.db.entity.GroupMemberEntity;
import com.example.photolog_front.util.PrefsKeys;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MakeGroupActivity extends AppCompatActivity {

    private EditText groupNameEditText;
    private EditText maxMemberEditText;
    private Button createGroupBtn;

    private TextView errorTextView;
    private TextView groupCodeTextView;
    private LinearLayout groupCodeLayout;
    private LinearLayout layoutLogo;
    private TextView goJoinText;

    private AppDatabase db;
    private GroupDao groupDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_group);

        db = AppDatabase.getInstance(getApplicationContext());
        groupDao = db.groupDao();

        layoutLogo = findViewById(R.id.layout_logo);
        groupNameEditText = findViewById(R.id.group_name);
        maxMemberEditText = findViewById(R.id.max_member);
        createGroupBtn = findViewById(R.id.btnLogin);

        errorTextView = findViewById(R.id.tvGroupError);
        groupCodeTextView = findViewById(R.id.groupCode);
        groupCodeLayout = findViewById(R.id.groupCodeLayout);
        goJoinText = findViewById(R.id.tvGoJoinDirect);

        groupCodeLayout.setVisibility(View.GONE);
        hideError();

        layoutLogo.setOnClickListener(v -> {
            Intent intent = new Intent(MakeGroupActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        createGroupBtn.setOnClickListener(v -> createGroup());

        goJoinText.setOnClickListener(v -> {
            Intent intent = new Intent(MakeGroupActivity.this, MakeGroupResponse.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }

    private long getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
        return prefs.getLong(PrefsKeys.KEY_CURRENT_USER_ID, -1L);
    }

    private String generateInviteCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void createGroup() {
        String groupName = groupNameEditText.getText().toString().trim();
        String maxMemberStr = maxMemberEditText.getText().toString().trim();

        if (groupName.isEmpty() || maxMemberStr.isEmpty()) {
            showError("모든 칸을 채워주세요.");
            groupCodeLayout.setVisibility(View.GONE);
            return;
        }

        int maxMember;
        try {
            maxMember = Integer.parseInt(maxMemberStr);
        } catch (Exception e) {
            showError("최대 인원 수는 숫자로 입력해주세요.");
            return;
        }

        if (maxMember < 2) {
            showError("최대 인원 수는 2명 이상이어야 합니다.");
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
        createGroupBtn.setEnabled(false);

        io.execute(() -> {
            try {
                GroupMemberEntity membership = groupDao.findMembershipByUserId(currentUserId);

                if (membership != null) {
                    GroupEntity joinedGroup = groupDao.findGroupByUserId(currentUserId);

                    runOnUiThread(() -> {
                        createGroupBtn.setEnabled(true);

                        if (joinedGroup != null && "OWNER".equals(membership.role)) {
                            groupCodeTextView.setText(joinedGroup.inviteCode);
                            groupCodeLayout.setVisibility(View.VISIBLE);
                            Toast.makeText(this, "이미 생성한 그룹이 있습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            showError("이미 다른 그룹에 가입되어 있습니다.");
                        }
                    });
                    return;
                }

                String inviteCode;
                do {
                    inviteCode = generateInviteCode();
                } while (groupDao.findByInviteCode(inviteCode) != null);

                GroupEntity group = new GroupEntity(groupName, inviteCode, maxMember, currentUserId);
                long groupId = groupDao.insertGroup(group);

                groupDao.insertGroupMember(new GroupMemberEntity(groupId, currentUserId, "OWNER"));

                String finalInviteCode = inviteCode;
                runOnUiThread(() -> {
                    createGroupBtn.setEnabled(true);
                    groupCodeTextView.setText(finalInviteCode);
                    groupCodeLayout.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "그룹 생성 완료!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    createGroupBtn.setEnabled(true);
                    showError("그룹 생성 중 오류가 발생했습니다.");
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
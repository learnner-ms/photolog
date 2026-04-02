package com.example.photolog_front;

import com.example.photolog_front.util.PrefsKeys;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.example.photolog_front.db.dao.GroupDao;
import com.example.photolog_front.db.dao.UserDao;
import com.example.photolog_front.db.entity.GroupEntity;
import com.example.photolog_front.db.entity.GroupMemberEntity;
import com.example.photolog_front.db.entity.UserEntity;

import java.util.ArrayList;
import java.util.List;
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

    private ImageView imgProfile;

    private LinearLayout groupMemberContainer;
    private TextView tvGroupEmptyMessage;

    private ActivityResultLauncher<String> pickImageLauncher;

    private AppDatabase db;
    private UserDao userDao;
    private DiaryDao diaryDao;
    private GroupDao groupDao;

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private enum GroupState { NONE, OWNER, MEMBER }

    private static class MemberUiModel {
        String name;
        int diaryCount;
        String role;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        db = AppDatabase.getInstance(getApplicationContext());
        userDao = db.userDao();
        diaryDao = db.diaryDao();
        groupDao = db.groupDao();

        initViews();
        setupProfilePicker();
        setListeners();

        loadUserDataFromRoom();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserDataFromRoom();
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

        groupMemberContainer = findViewById(R.id.group_member_container);
        tvGroupEmptyMessage = findViewById(R.id.tvGroupEmptyMessage);
    }

    private void setupProfilePicker() {
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
                Glide.with(this)
                        .load(R.drawable.profile)
                        .circleCrop()
                        .into(imgProfile);
            }
        } else {
            Glide.with(this)
                    .load(R.drawable.profile)
                    .circleCrop()
                    .into(imgProfile);
        }

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

        btnDeleteAiData.setOnClickListener(v -> showDeleteAllDiaryDialogCustom());
        btnWithdraw.setOnClickListener(v -> showWithdrawDialogCustom());

        imgProfile.setOnClickListener(v -> {
            if (pickImageLauncher != null) {
                pickImageLauncher.launch("image/*");
            }
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
            try {
                UserEntity user = userDao.findById(userId);
                int myDiaryCount = diaryDao.countByUserId(userId);

                GroupMemberEntity membership = groupDao.findMembershipByUserId(userId);
                GroupEntity myGroup = groupDao.findGroupByUserId(userId);

                int familyCount = 0;
                GroupState groupState = GroupState.NONE;
                List<MemberUiModel> memberItems = new ArrayList<>();

                if (membership != null && myGroup != null) {
                    if ("OWNER".equals(membership.role)) {
                        groupState = GroupState.OWNER;
                    } else {
                        groupState = GroupState.MEMBER;
                    }

                    List<Long> memberUserIds = groupDao.getMemberUserIds(myGroup.id);

                    for (Long memberUserId : memberUserIds) {
                        if (memberUserId == null) continue;
                        if (memberUserId == userId) continue;

                        UserEntity memberUser = userDao.findById(memberUserId);
                        if (memberUser == null) continue;

                        MemberUiModel item = new MemberUiModel();
                        item.name = (memberUser.name != null && !memberUser.name.trim().isEmpty())
                                ? memberUser.name
                                : memberUser.username;
                        item.diaryCount = diaryDao.countByUserId(memberUserId);

                        GroupMemberEntity memberRole = groupDao.findMembershipByUserId(memberUserId);
                        item.role = (memberRole != null) ? memberRole.role : "MEMBER";

                        memberItems.add(item);
                    }

                    familyCount = memberItems.size();
                }

                final UserEntity finalUser = user;
                final int finalMyDiaryCount = myDiaryCount;
                final int finalFamilyCount = familyCount;
                final GroupState finalGroupState = groupState;
                final List<MemberUiModel> finalMemberItems = memberItems;

                runOnUiThread(() -> {
                    if (finalUser == null) {
                        Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    tvNickname.setText(
                            finalUser.name != null && !finalUser.name.trim().isEmpty()
                                    ? finalUser.name
                                    : "닉네임"
                    );

                    tvDiaryCount.setText("작성한 일기 수 : " + finalMyDiaryCount);
                    tvFamilyCount.setText("추가한 가족 수 : " + finalFamilyCount);

                    bindGroupUi(finalGroupState);
                    renderGroupMembers(finalMemberItems);
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "마이페이지 정보를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                );
            }
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

        } else {
            sectionNoGroup.setVisibility(View.GONE);
            sectionMyGroup.setVisibility(View.VISIBLE);
            btnTopAction.setVisibility(View.VISIBLE);
            btnTopAction.setText("그룹 탈퇴");
        }
    }

    private void renderGroupMembers(List<MemberUiModel> members) {
        if (groupMemberContainer == null || tvGroupEmptyMessage == null) return;

        groupMemberContainer.removeAllViews();

        if (members == null || members.isEmpty()) {
            tvGroupEmptyMessage.setVisibility(View.VISIBLE);
            return;
        }

        tvGroupEmptyMessage.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (MemberUiModel member : members) {
            View itemView = inflater.inflate(R.layout.item_group_member, groupMemberContainer, false);

            ImageView imgMemberProfile = itemView.findViewById(R.id.imgMemberProfile);
            TextView tvMemberName = itemView.findViewById(R.id.tvMemberName);
            TextView tvMemberDiaryCount = itemView.findViewById(R.id.tvMemberDiaryCount);
            TextView tvMemberRole = itemView.findViewById(R.id.tvMemberRole);

            Glide.with(this)
                    .load(R.drawable.profile)
                    .circleCrop()
                    .into(imgMemberProfile);

            tvMemberName.setText(member.name);
            tvMemberDiaryCount.setText("작성한 일기 수 : " + member.diaryCount);
            tvMemberRole.setText(member.role);

            groupMemberContainer.addView(itemView);
        }
    }

    private void showDeleteAllDiaryDialogCustom() {
        String title = "AI 및 개인정보 전체 삭제";
        String body =
                "📓 저장된 AI 일기 데이터가 모두 삭제됩니다.\n\n" +
                        "💬 AI 대화 기록과 세션 정보가 함께 삭제됩니다.\n\n" +
                        "🧹 계정은 유지되지만, 서비스 이용 데이터는 초기화됩니다.\n\n" +
                        "⚠️ 삭제된 데이터는 복구할 수 없습니다.";
        String agreeText = "위 내용을 확인했으며 삭제에 동의합니다.";

        showCommonNoticeDialog(
                title,
                body,
                agreeText,
                "삭제하기",
                "취소",
                this::deleteAllMyPersonalData
        );
    }

    private void showWithdrawDialogCustom() {
        String title = "회원 탈퇴";
        String body =
                "👤 계정 정보가 모두 삭제됩니다.\n\n" +
                        "📓 작성한 AI 일기 데이터와 대화 기록도 함께 삭제됩니다.\n\n" +
                        "🚪 탈퇴 후에는 로그인할 수 없으며 복구할 수 없습니다.\n\n" +
                        "⚠️ 삭제된 데이터는 복구할 수 없습니다.";
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

            if (onConfirmed != null) {
                onConfirmed.run();
            }
        });

        dialog.show();
    }

    private void deleteAllMyPersonalData() {
        final long userId = getCurrentUserId();

        if (userId <= 0) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        io.execute(() -> {
            try {
                db.clearUserPersonalData(userId);

                runOnUiThread(() -> {
                    getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE)
                            .edit()
                            .remove(PrefsKeys.KEY_PROFILE_URI)
                            .apply();

                    Glide.with(this)
                            .load(R.drawable.profile)
                            .circleCrop()
                            .into(imgProfile);

                    Toast.makeText(this, "AI 및 개인정보 데이터가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    loadUserDataFromRoom();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "데이터 삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void withdrawAccount() {
        final long userId = getCurrentUserId();

        if (userId <= 0) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        io.execute(() -> {
            try {
                db.deleteUserAccountCompletely(userId);

                runOnUiThread(() -> {
                    SharedPreferences prefs = getSharedPreferences(PrefsKeys.PREFS_AUTH, MODE_PRIVATE);
                    prefs.edit().clear().apply();

                    Toast.makeText(this, "탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MyPageActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "회원 탈퇴 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
}
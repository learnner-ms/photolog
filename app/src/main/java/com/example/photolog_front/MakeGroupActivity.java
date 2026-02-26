package com.example.photolog_front;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.photolog_front.model.FamilyCreateRequest;
import com.example.photolog_front.network.ApiService;
import com.example.photolog_front.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MakeGroupActivity extends AppCompatActivity {

    private EditText groupNameEditText;
    private EditText maxMemberEditText;
    private Button createGroupBtn;

    private TextView errorTextView;
    private TextView groupCodeTextView;
    private LinearLayout groupCodeLayout;
    private LinearLayout layoutLogo;

    // ⬇ ⬇ ⬇ 추가: "이미 그룹이 있어요" 텍스트
    private TextView goJoinText;
    // ⬆ ⬆ ⬆

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_group);

        layoutLogo = findViewById(R.id.layout_logo);

        groupNameEditText = findViewById(R.id.group_name);
        maxMemberEditText = findViewById(R.id.max_member);
        createGroupBtn = findViewById(R.id.btnLogin);

        errorTextView = findViewById(R.id.tvGroupError);
        groupCodeTextView = findViewById(R.id.groupCode);
        groupCodeLayout = findViewById(R.id.groupCodeLayout);

        // ⬇ ⬇ ⬇ 추가: TextView 연결
        goJoinText = findViewById(R.id.tvGoJoinDirect);
        // ⬆ ⬆ ⬆

        groupCodeLayout.setVisibility(View.GONE);

        layoutLogo.setOnClickListener(v -> {
            Intent intent = new Intent(MakeGroupActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        createGroupBtn.setOnClickListener(v -> createGroup());

        // ⬇ ⬇ ⬇ "이미 그룹이 있어요" 클릭 → 초대코드 입력 화면 이동
        goJoinText.setOnClickListener(v -> {
            Intent intent = new Intent(MakeGroupActivity.this, MakeGroupResponse.class);
            startActivity(intent);
        });
        // ⬆ ⬆ ⬆
    }

    private void createGroup() {

        String groupName = groupNameEditText.getText().toString().trim();
        String maxMember = maxMemberEditText.getText().toString().trim();

        if (groupName.isEmpty() || maxMember.isEmpty()) {
            errorTextView.setVisibility(View.VISIBLE);
            groupCodeLayout.setVisibility(View.GONE);
            return;
        }

        errorTextView.setVisibility(View.GONE);

        ApiService api = RetrofitClient.getApiService(this);

        Toast.makeText(this, "가족 생성 중...", Toast.LENGTH_SHORT).show();

        api.createFamily(new FamilyCreateRequest(groupName))
                .enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {

                        if (response.isSuccessful()) {

                            try {
                                com.google.gson.Gson gson = new com.google.gson.Gson();
                                com.google.gson.JsonObject json = gson.toJsonTree(response.body()).getAsJsonObject();

                                String inviteCode = json.get("invite_code").getAsString();

                                groupCodeTextView.setText(inviteCode);
                                groupCodeLayout.setVisibility(View.VISIBLE);

                                Toast.makeText(MakeGroupActivity.this, "가족 생성 완료!", Toast.LENGTH_SHORT).show();

                            } catch (Exception e) {
                                Toast.makeText(MakeGroupActivity.this, "응답 처리 오류", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else if (response.code() == 400) {
                            Toast.makeText(MakeGroupActivity.this, "중복된 그룹 이름입니다.", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(MakeGroupActivity.this, "서버 오류 (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        Toast.makeText(MakeGroupActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
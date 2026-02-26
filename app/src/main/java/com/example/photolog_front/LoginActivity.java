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

import com.example.photolog_front.model.LoginRequest;
import com.example.photolog_front.model.LoginResponse;
import com.example.photolog_front.network.ApiService;
import com.example.photolog_front.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    EditText etId, etPw;
    TextView tvError, tvJoin;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etId = findViewById(R.id.etId);
        etPw = findViewById(R.id.etPw);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);
        tvJoin = findViewById(R.id.tvJoin);

        // 회원가입 이동
        tvJoin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // 로그인 버튼 클릭
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String id = etId.getText().toString().trim();
        String pw = etPw.getText().toString().trim();

        if (id.isEmpty() || pw.isEmpty()) {
            showError("아이디와 비밀번호를 입력해주세요.");
            return;
        }

        loginRequest(id, pw);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void loginRequest(String id, String pw) {
        LoginRequest request = new LoginRequest(id, pw);

        ApiService api = RetrofitClient.getApiService(this);
        api.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                if (response.isSuccessful()) {
                    LoginResponse data = response.body();

                    Toast.makeText(LoginActivity.this,
                            "로그인 성공!", Toast.LENGTH_SHORT).show();

                    // JWT 저장 (필요하면 SharedPreferences로 저장 가능)
                    SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
                    prefs.edit().putString("token", response.body().getAccessToken()).apply();


                    // 메인 화면으로 이동
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                else if (response.code() == 401) {
                    showError("아이디 또는 비밀번호가 틀렸습니다.");
                }
                else {
                    showError("로그인 실패: 서버 오류("+response.code()+")");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showError("네트워크 오류 발생. 다시 시도해주세요.");
            }
        });
    }
}

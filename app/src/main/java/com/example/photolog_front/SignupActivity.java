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
                        "개인정보 처리방침",
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
        return "# 포토로그 이용약관\n\n"
                + "---\n"
                + "# 제 1장 총칙\n\n"
                + "## 제 1조 (목적)\n"
                + "이 약관은 귀하가 포토로그(이하 “회사”)가 제공하는 온라인상의 인터넷 서비스(이하 “서비스”)를 이용함에 있어 회사와 사용자 사이의 권리, 의무, 책임 및 서비스 이용 등 기본적인 사항을 규정함을 목적으로 합니다.\n\n"
                + "사용자는 본 이용약관을 자세히 읽은 후 이용약관에 동의하지 않을 경우, 본 이용약관에 동의하거나 서비스에 등록 또는 액세스하거나 이를 이용하지 말아야 합니다.\n\n"
                + "## 제 2조 (정의)\n"
                + "“서비스”란 사용자의 사진, 음성 및 텍스트 입력을 기반으로 AI를 활용하여 일기를 생성하고 저장 및 공유할 수 있는 모바일 어플리케이션을 의미합니다.\n\n"
                + "“사용자”란 본 약관에 따라 “회사”가 제공하는 “서비스”를 받는 회원을 말합니다.\n\n"
                + "## 제 3조 (약관 등의 명시와 설명 및 개정)\n"
                + "“회사”는 사업자의 상호, 대표자 성명, 주소, 전화번호, 모사전송번호, 전자우편주소, 사업자등록번호, 통신판매업 신고번호 및 개인정보관리책임자 등을 사용자가 쉽게 알 수 있도록 온라인 서비스 초기화면에 게시합니다.\n\n"
                + "“회사”는 전자상거래 등에서의 소비자보호에 관한 법률, 소비자 보호법 등 관련법을 위배하지 않는 범위에서 이 약관을 개정할 수 있습니다.\n\n"
                + "본 약관은 수시로 변경될 수 있으므로 회원은 포토로그 이용약관의 최선 버전을 주기적으로 확인하여야 합니다. 본 약관이 변경되는 경우, 회원이 회원 탈퇴를 진행하지 않는다면 새로운 본 약관에 동의하며, 동 약관이 사용자의 향후의 모든 서비스 제공에 적용됨에 동의함을 나타냅니다.\n\n"
                + "이 약관에서 정하지 아니한 사항과 이 약관의 해석에 관하여는 전자상거래 등에서의 소비자보호에 관한 법률, 약관의 규제 등에 관한 법률, 공정거래위원회가 정하는 전자상거래 등에서의 소비자 보호지침 및 관계법령 또는 상관례에 따릅니다.\n\n"
                + "## 제 4조 (서비스의 제공 및 변경)\n"
                + "“회사”는 다음과 같은 서비스를 제공합니다.\n\n"
                + "1. 사진 기반 일기 작성 기능\n"
                + "2. 음성 및 텍스트 입력 기반 AI 대화 기능\n"
                + "3. AI를 활용한 일기 자동 생성 기능\n"
                + "4. 가족 그룹을 통한 일기 공유 기능\n\n"
                + "## 제 5조 (서비스의 변경 및 중단)\n"
                + "“회사”는 컴퓨터 등 정보통신설비의 보수점검, 교체 및 고장, 통신의 두절 등의 사유가 발생한 경우에는 서비스의 제공을 일시적으로 중단할 수 있습니다.\n\n"
                + "“회사”는 제 1항의 사유로 서비스의 제공이 일시적으로 중단됨으로 인하여 사용자 또는 제 3자가 입은 손해에 대하여 배상합니다. 단, “회사”가 고의 또는 과실이 없음을 입증하는 경우에는 그러하지 아니합니다.\n\n"
                + "사업자의 전환, 사업의 포기, 업체간의 통합 등의 이유로 서비스를 제공할 수 없게 되는 경우에는 “회사”는 제 8조에 정한 방법으로 사용자에게 통지하고, 당초 “회사”가 제시한 조건에 따라 소비자에게 보상합니다.\n\n"
                + "“회사”는 기술적 필요 또는 서비스 개선을 위해 서비스의 일부 또는 전부를 변경할 수 있습니다. 서비스는 개발 단계에 따라 일부 기능이 제한될 수 있습니다.\n\n"
                + "## 제 6조 (AI 생성 콘텐츠 관련)\n"
                + "“서비스”는 인공지능(AI)을 활용하여 콘텐츠를 생성합니다.\n\n"
                + "AI가 생성한 결과는 참고용으로 제공되며, “회사”는 그 정확성 완전성 및 신뢰성을 보장하지 않습니다.\n\n"
                + "AI가 생성한 콘텐츠로 인해 발생하는 문제에 대해 “회사”는 책임을 지지 않습니다.\n\n"
                + "## 제 7조 (사용자 데이터 및 콘텐츠)\n"
                + "“사용자”가 업로드한 사진, 음성, 텍스트 등 모든 데이터에 대한 책임은 “사용자”에게 있습니다.\n\n"
                + "“사용자”는 타인의 개인정보 또는 초상권을 침해하는 콘텐츠를 업로드해서는 안 됩니다.\n\n"
                + "## 제 8조 (가족 공유 기능)\n"
                + "“사용자”는 자신의 선택에 따라 일기 콘텐츠를 가족 그룹에 공유할 수 있습니다.\n\n"
                + "공유된 콘텐츠에 대한 책임은 “사용자” 본인에게 있으며, “회사”는 이에 대해 책임을 지지 않습니다.\n\n"
                + "## 제 9조 (회원가입)\n"
                + "사용자는 “사이트”가 정한 가입 양식에 따라 회원정보를 기입한 후 이 약관에 동희한다는 의사표시를 함으로써 회원가입을 신청합니다.\n\n"
                + "“회사”는 제 1항과 같이 회원으로 가입할 것을 신청한 이용자 중 다음 각 호에 해당하지 않는 한 회원으로 등록합니다.\n"
                + "- 가입신청자가 이 약관 제 10조 제 3항에 의하여 이전에 회원자격을 상실한 적이 있는 경우, 다만 제 7조 제3항에 의한 회원자격 상실 후 경과한 자로서, “회사”의 회원재가입 승낙을 얻은 경우에는 예외로 한다.\n"
                + "- 기타 회원으로 등록하는 것이 “회사”의 기술상 현저히 지장이 있다고 판단되는 경우\n\n"
                + "## 제 10조 (회원 탈퇴 및 자격 상실 등)\n"
                + "회원이 다음 각호의 사유에 해당하는 경우, ”사업자”는 회원자격을 제한 및 정지시킬 수 있습니다.\n\n"
                + "1. 가입 신청시에 허위 내용을 등록한 경우\n"
                + "2. 다른 사람의 “서비스” 이용을 방해하거나 그 정보를 도용하는 등 전자상거래 질서를 위협하는 경우\n"
                + "3. “서비스”를 이용하여 법령 또는 이 약관이 금지하거나 공서양속에 반하는 행위를 하는 경우\n\n"
                + "“회사”가 회원 자격을 제한.정지 시킨후, 동일한 행위가 2회이상 반복되거나 30일 이내에 그 사유가 시정되지 아니하는 경우 “회사”는 회원자격을 상실시킬 수 있습니다.\n\n"
                + "## 제 11조 (회원에 대한 통지)\n"
                + "“회사”가 회원에 대한 통지를 하는 경우, 회원이 “사업자”와 미리 약정하여 지정한 전자우편 주소로 할 수 있습니다.\n\n"
                + "“회사”는 불특정다수 회원에 대한 통지의 경우 1주일이상 “회사” 게시판에 게시함으로서 개별통지에 갈음할 수 있습니다. 다만, 회원 본인의 거래와 관련하여 중대한 영향을 미치는 사항에 대하여는 개별통지를 합니다.\n\n"
                + "## 제 12조 (회사의 의무)\n"
                + "“회사”는 법령과 이 약관이 금지하거나 공서양속에 반하는 행위를 하지 않으며 이 약관이 정하는 바에 따라 지속적이고, 안정적으로 재화ㆍ용역을 제공하는데 최선을 다하여야 합니다.\n\n"
                + "“회사”는 사용자가 안전하게 인터넷 서비스를 이용할 수 있도록 사용자의 개인정보(신용정보 포함)보호를 위한 보안 시스템을 갖추어야 합니다.\n\n"
                + "“회사”가 상품이나 용역에 대하여 「표시ㆍ광고의공정화에관한법률」 제 3조 소정의 부당한 표시ㆍ광고행위를 함으로써 사용자가 손해를 입은 때에는 이를 배상할 책임을 집니다.\n\n"
                + "“회사”는 사용자가 원하지 않는 영리목적의 광고성 전자우편을 발송하지 않습니다.\n\n"
                + "## 제 13조 (사용자의 의무)\n"
                + "사용자는 다음 행위를 하여서는 안됩니다.\n"
                + "- 신청 또는 변경시 허위 내용의 등록\n"
                + "- 타인의 정보 도용\n"
                + "- “회사”가 게시한 정보의 변경\n"
                + "- “회사”가 정한 정보 이외의 정보(컴퓨터 프로그램 등) 등의 송신 또는 게시\n"
                + "- “회사” 기타 제3자의 저작권 등 지적재산권에 대한 침해\n"
                + "- “회사” 기타 제3자의 명예를 손상시키거나 업무를 방해하는 행위\n\n"
                + "## 제 14조 (저작권의 귀속 및 이용제한)\n"
                + "“회사”가 작성한 저작물에 대한 저작권, 기타 지적재산권은 “회사”에게 귀속됩니다.\n\n"
                + "사용자는 “서비스”를 이용함으로써 얻은 정보 중 “회사”에게 지적재산권이 귀속된 정보를 이용하거나 제3자에게 이용하게 하여서는 안됩니다.\n\n"
                + "“회사”는 약정에 따라 사용자에게 귀속된 저작권을 사용하는 경우 당해 사용자에게 통보하여야 합니다.\n\n"
                + "## 제 15조 (면책조항)\n"
                + "“회사”는 천재지변, 시스템 오류, 네트워크 장애 등으로 인한 서비스 장애에 대해 서비스 제공에 관한 책임이 면제됩니다.\n\n"
                + "“회사”는 AI기반 서비스의 특성상 발생할 수 있는 결과의 오류 또는 부정확성에 대해 책임을 지지 않습니다.\n\n"
                + "“회사”는 회원의 귀책사유로 인하여 발생한 서비스 이용의 장애에 대하여는 책임을 지지 않습니다.\n\n"
                + "“회사”는 회원이 서비스와 관련하여 게재한 정보, 자료, 사실의 신뢰도, 정확성 등의 내용에 관하여는 책임을 지지 않습니다.\n\n"
                + "“회사”는 사용자 상호간 또는 사용자와 제3자 간에 콘텐츠를 매개로 하여 발생한 분쟁 등에 대하여 책임을 지지 않습니다.\n\n"
                + "본 “서비스”는 의료 서비스가 아니며, 전문적인 의학적 판단을 대체할 수 없습니다.\n\n"
                + "## 제 16조 (분쟁해결)\n"
                + "“회사”는 사용자가 제기하는 정당한 의견이나 불만을 반영하고 그 피해를 보상처리하기 위하여 피해보상처리기구를 설치ㆍ운영합니다.\n\n"
                + "“회사”는 사용자로부터 제출되는 불만사항 및 의견은 우선적으로 그 사항을 처리합니다. 다만, 신속한 처리가 곤란한 경우에는 사용자에게 그 사유와 처리일정을 즉시 통보해 드립니다.\n\n"
                + "“회사”와 사용자간에 발생한 전자상거래 분쟁과 관련하여 사용자의 피해구제신청이 있는 경우에는 공정거래위원회 또는 시·도지사가 의뢰하는 분쟁조정기관의 조정에 따를 수 있습니다.\n\n"
                + "## 제 17조 (재판권 및 준거법)\n"
                + "“사업자”와 사용자간에 발생한 전자상거래 분쟁에 관한 소송은 제소 당시의 사용자의 주소에 의하고, 주소가 없는 경우에는 거소를 관할하는 지방법원의 전속관할로 합니다. 다만, 제소 당시 사용자의 주소 또는 거소가 분명하지 않거나 외국 거주자의 경우에는 민사소송법상의 관할법원에 제기합니다.\n\n"
                + "“사업자”와 사용자간에 제기된 전자상거래 소송에는 국내법을 적용합니다.\n\n"
                + "## 제 18조 (개인 정보 수집 및 보호)\n"
                + "“회사”는 관련 법령이 정하는 바에 따라 사용자의 개인정보를 보호하기 위해 노력합니다. 개인정보의 수집, 이용 및 보호에 관한 사항은 별도의 “개인정보 처리방침”에 따릅니다.\n\n"
                + "### 부칙\n"
                + "이 약관은 2026년 02월 05일부터 시행합니다.";
    }

    private String getPrivacyTermsContent() {
        return "# 개인정보 처리방침\n\n"
                + "---\n"
                + "## 제 1조 (개인정보의 수집 항목 및 수집 방법)\n"
                + "### [수집 항목]\n"
                + "1. 계정 정보\n"
                + "- 이름 또는 닉네임\n"
                + "- 사용자 ID\n"
                + "- 비밀번호 (암호화 저장)\n"
                + "- 이메일 주소\n"
                + "2. 콘텐츠 데이터\n"
                + "- 이미지 데이터 : 사용자가 업로드한 사진\n"
                + "- 음성 데이터 : 음성 입력 및 음성 인식 처리 과정에서 생성된 데이터\n"
                + "- 텍스트 데이터 : 사용자 입력 및 AI 대화 내용\n"
                + "- 생성 콘텐츠 : AI를 통해 생성된 일기 및 기록물\n"
                + "3. 관계 정보\n"
                + "- 가족 그룹 ID\n"
                + "- 그룹 구성원 정보 및 참여 상태\n"
                + "4. 서비스 이용 정보\n"
                + "- 접속 일시 및 이용 기록\n"
                + "- 서비스 이용 로그\n"
                + "- 오류 및 장애 기록\n\n"
                + "### [개인정보 수집 방법]\n"
                + "- 서비스 회원가입 및 이용 과정에서 수집\n"
                + "- 사용자 입력(사진, 음성, 텍스트)을 통해 수집\n"
                + "- 서비스 이용 과정에서 자동 생성 정보 수집\n\n"
                + "## 제 2조 (개인정보 수집 및 이용 목적)\n"
                + "회사는 다음 목적을 위해 개인정보를 이용합니다.\n"
                + "### 1. 서비스 제공\n"
                + "- AI 기반 일기 생성 및 저장\n"
                + "- 음성 인식 및 텍스트 처리\n"
                + "- 가족 그룹 공유 기능 제공\n"
                + "### 2. 회원 관리\n"
                + "- 사용자 식별 및 계정 관리\n"
                + "- 서비스 이용 관련 안내 및 공지\n"
                + "### 3. 서비스 개선\n"
                + "- 서비스 이용 분석 및 기능 개선\n"
                + "- 오류 분석 및 안정성 확보\n"
                + "### 4. 맞춤형 서비스 제공\n"
                + "- AI 기반 개인화된 콘텐츠 생성\n\n"
                + "## 제 3조 (AI 처리 및 외부 전송)\n"
                + "서비스 제공 과정에서 AI 기반 기능 수행을 위해 사용자의 일부 데이터(텍스트 및 대화 내용)가 외부 AI 서비스(Chat-GPT 5.0)로 전송될 수 있습니다.\n\n"
                + "입력된 데이터는 AI 모델의 성능 개선(학습)을 위해 사용되지 않으며, 오직 결과값 생성을 위한 추론 목적으로만 사용됩니다.\n\n"
                + "해당 데이터는 서비스 제공 목적 범위 내에서만 사용됩니다.\n\n"
                + "## 제 4조 (개인정보의 공유 및 제공)\n"
                + "회사는 원칙적으로 이용자의 개인정보를 외부에 제공하지 않습니다.\n"
                + "다만, 다음의 경우에는 예외로 합니다.\n"
                + "- 이용자의 사전 동의를 받은 경우\n"
                + "- 법령에 의해 요구되는 경우\n\n"
                + "## 제 5조 (개인정보의 처리 위탁)\n"
                + "회사는 서비스 제공을 위해 필요한 경우 일부 업무를 외부에 위탁할 수 있으며, 위탁 시 개인정보 보호 관련 법령을 준수합니다.\n\n"
                + "## 제 6조 (개인정보 보유 및 이용 기간)\n"
                + "회사는 개인정보를 다음과 같이 보관 및 처리합니다.\n"
                + "- 회원 정보 : 회원 탈퇴 시 즉시 삭제\n"
                + "- 일기 및 콘텐츠 데이터 : 사용자 삭제 시 즉시 삭제\n"
                + "- 가족 그룹 정보 : 그룹 탈퇴 또는 회원 탈퇴 시 삭제\n"
                + "- 서비스 이용 기록 및 로그 : 최대 3~6개월 보관 후 삭제\n\n"
                + "단, 관련 법령에 따라 보관이 필요한 경우 해당 기간 동안 보관합니다.\n\n"
                + "## 제 7조 (개인정보 파기 절차 및 방법)\n"
                + "회사는 개인정보 처리 목적이 달성된 경우 지체없이 해당 정보를 파기합니다.\n"
                + "- 전자적 파일 : 복구 불가능한 방법으로 삭제\n"
                + "- 출력물 : 분쇄 또는 소각 처리\n\n"
                + "## 제 8조 (이용자의 권리)\n"
                + "이용자는 언제든지 자신의 개인정보에 대해 다음과 같은 권리를 행사할 수 있습니다.\n\n"
                + "1. 개인정보 열람 요청\n"
                + "이용자는 회사가 처리하고 있는 자신의 개인정보에 대해 열람을 요청할 수 있습니다.\n\n"
                + "2. 개인정보 정정 및 삭제 요청\n"
                + "이용자는 자신의 개인정보가 정확하지 않거나 불필요한 경우 정정 또는 삭제를 요청할 수 있습니다.\n\n"
                + "3. 개인정보 처리 정지 요청\n"
                + "이용자는 자신의 개인정보 처리에 대해 중지를 요청할 수 있습니다.\n"
                + "이용자는 AI 서비스 제공을 위한 데이터 처리에 대해서도 처리 중단을 요청할 수 있습니다.\n"
                + "이용자는 언제든지 서비스 내 기능 또는 고객 문의를 통해 자신의 개인정보를 열람, 정정, 삭제하거나 처리정지를 요청할 수 있습니다.\n\n"
                + "회사는 서비스 내 ‘마이페이지’를 통해 다음 기능을 제공합니다.\n"
                + "- AI 및 개인정보 전체 삭제 : 이용자가 생성한 일기, AI 대화 내용 및 관련 데이터를 삭제할 수 있습니다.\n"
                + "- 회원 탈퇴 : 계정 삭제와 함께 모든 개인정보를 삭제합니다.\n\n"
                + "회사는 이용자의 개인정보 열람, 정정, 삭제 및 처리정지 요청에 대해 지체 없이 처리하며, 특별한 사유가 없는 한 10일 이내에 처리합니다.\n"
                + "처리가 지연되는 경우 그 사유와 처리 일정을 이용자에게 안내합니다.\n\n"
                + "## 제 9조 (개인정보 보호를 위한 안정성 확보 조치)\n"
                + "회사는 개인정보 보호를 위해 다음과 같은 조치를 취합니다.\n"
                + "1. 비밀번호 암호화 저장\n"
                + "2. 접근 권한 최소화\n"
                + "3. 보안 시스템 운영\n"
                + "- 기술적 대책 : 해킹 대비 보안 프로그램 설치, 데이터 암호화 전송 (SSL/TLS)\n"
                + "- 관리적 대책 : 내부 관리계획 수립, 개인정보 취급자 최소화 및 교육\n\n"
                + "## 제 10조 (민감정보 관련)\n"
                + "회사는 이용자의 민감정보를 별도로 수집하지 않습니다.\n\n"
                + "다만, 이용자가 서비스 이용 과정에서 입력하거나 업로드하는 텍스트, 음성, 이미지 등 콘텐츠에 민감정보가 포함될 수 있습니다.\n\n"
                + "또한, AI 서비스 특성상 입력된 데이터로부터 감정, 상황 등의 정보가 추론될 수 있으며, 이 과정에서 민감정보에 해당하는 정보가 간접적으로 생성될 수 있습니다.\n\n"
                + "이용자가 직접 입력한 정보 중 민감정보가 포함되는 경우, 회사는 이를 서비스 제공 목적 범위 내에서만 처리하며 별도의 데이터베이스로 관리하지 않습니다. 이용자는 민감정보가 노출되지 않도록 유의해야합니다.\n\n"
                + "## 제 11조 (개인정보 보호 책임자)\n"
                + "- 성명 : 한민선\n"
                + "- 직책 : 개인정보보호책임자\n"
                + "- 이메일 : 20221669@sungshin.ac.kr\n"
                + "- 전화번호 : 010-9502-1402\n\n"
                + "## 제 12조 (고지의 의무)\n"
                + "회사는 개인정보 처리방침 변경 시 사전 공지합니다.";
    }

    private String getAdTermsContent() {
        return "# 맞춤형 광고 수신 동의\n\n"
                + "회사는 이용자에게 보다 유용한 서비스와 혜택을 제공하기 위해 이용자의 서비스 이용 기록 및 관심사 등을 기반으로 맞춤형 광고 및 마케팅 정보를 제공할 수 있습니다.\n\n"
                + "### 1. 수집 및 활용 항목\n"
                + "- 서비스 이용 기록\n"
                + "- 접속 정보\n"
                + "- 콘텐츠 이용 패턴\n"
                + "- AI 서비스 이용 패턴\n"
                + "### 2. 이용 목적\n"
                + "- 맞춤형 광고 제공\n"
                + "- 이벤트 및 프로모션 안내\n"
                + "### 3. 보유 및 이용 기간\n"
                + "- 동의 철회 시까지\n\n"
                + "이용자는 본 동의에 대해 거부할 권리가 있으며, 동의하지 않아도 서비스 이용에는 제한이 없습니다. 또한 이용자는 언제든지 설정 또는 마이페이지를 통해 동의를 철회할 수 있습니다.";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
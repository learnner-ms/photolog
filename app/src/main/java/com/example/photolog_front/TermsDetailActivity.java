package com.example.photolog_front;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.res.ResourcesCompat;

public class TermsDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_TYPE = "type";

    private TextView tvTermsTitle;
    private TextView tvTermsContent;
    private AppCompatButton btnBack;
    private AppCompatButton btnAgree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_detail);

        tvTermsTitle = findViewById(R.id.tvTermsTitle);
        tvTermsContent = findViewById(R.id.tvTermsContent);
        btnBack = findViewById(R.id.btnBack);
        btnAgree = findViewById(R.id.btnAgree);

        LinearLayout logoLayout = findViewById(R.id.layout_logo);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String content = getIntent().getStringExtra(EXTRA_CONTENT);
        String type = getIntent().getStringExtra(EXTRA_TYPE);

        tvTermsTitle.setText(title != null ? title : "약관");
        tvTermsContent.setText(renderMarkdownLike(content != null ? content : "약관 내용이 없습니다."));

        logoLayout.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());

        btnAgree.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("agreed", true);
            result.putExtra("type", type);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    private CharSequence renderMarkdownLike(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        if (text == null || text.isEmpty()) {
            return builder;
        }

        String[] lines = text.split("\\n", -1);

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            String trimmed = rawLine == null ? "" : rawLine.trim();

            // 완전 빈 줄은 원문 그대로 1줄만 유지
            if (trimmed.isEmpty()) {
                builder.append("\n");
                continue;
            }

            // 구분선 문자열은 화면에서 생략
            if (trimmed.equals("---")) {
                continue;
            }

            if (trimmed.startsWith("# ")) {
                appendHeading1(builder, trimmed.substring(2).trim());
            } else if (trimmed.startsWith("## ")) {
                String heading = trimmed.substring(3).trim();

                if (heading.matches("^제\\s*\\d+\\s*장.*")) {
                    appendChapterTitle(builder, heading);
                } else if (heading.matches("^제\\s*\\d+\\s*조.*")) {
                    appendSectionTitle(builder, heading);
                } else {
                    appendHeading2(builder, heading);
                }
            } else if (trimmed.startsWith("### ")) {
                appendHeading3(builder, trimmed.substring(4).trim());
            } else if (trimmed.matches("^제\\s*\\d+\\s*장.*")) {
                appendChapterTitle(builder, trimmed);
            } else if (trimmed.matches("^제\\s*\\d+\\s*조.*")) {
                appendSectionTitle(builder, trimmed);
            } else if (trimmed.startsWith("- ")) {
                appendBulletText(builder, trimmed.substring(2).trim());
            } else if (trimmed.matches("^\\d+\\.\\s+.*")) {
                appendNumberedText(builder, trimmed);
            } else {
                appendBodyText(builder, trimmed);
            }

            // 줄 끝은 한 번만
            if (i < lines.length - 1) {
                builder.append("\n");
            }
        }

        return builder;
    }

    private void appendHeading1(SpannableStringBuilder builder, String text) {
        int start = builder.length();
        builder.append(text);
        int end = builder.length();

        builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new AbsoluteSizeSpan(24, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(
                new CustomTypefaceSpan(ResourcesCompat.getFont(this, R.font.paperlogy_7)),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    private void appendHeading2(SpannableStringBuilder builder, String text) {
        int start = builder.length();
        builder.append(text);
        int end = builder.length();

        builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new AbsoluteSizeSpan(22, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(
                new CustomTypefaceSpan(ResourcesCompat.getFont(this, R.font.paperlogy_7)),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    private void appendHeading3(SpannableStringBuilder builder, String text) {
        int start = builder.length();
        builder.append(text);
        int end = builder.length();

        builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new AbsoluteSizeSpan(19, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(
                new CustomTypefaceSpan(ResourcesCompat.getFont(this, R.font.paperlogy_7)),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    private void appendChapterTitle(SpannableStringBuilder builder, String text) {
        int start = builder.length();
        builder.append(text);
        int end = builder.length();

        builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new AbsoluteSizeSpan(23, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(
                new CustomTypefaceSpan(ResourcesCompat.getFont(this, R.font.paperlogy_7)),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    private void appendSectionTitle(SpannableStringBuilder builder, String text) {
        int start = builder.length();
        builder.append(text);
        int end = builder.length();

        builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new AbsoluteSizeSpan(21, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(
                new CustomTypefaceSpan(ResourcesCompat.getFont(this, R.font.paperlogy_7)),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    private void appendBodyText(SpannableStringBuilder builder, String text) {
        int start = builder.length();
        builder.append(text);
        int end = builder.length();

        builder.setSpan(new AbsoluteSizeSpan(18, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(
                new CustomTypefaceSpan(ResourcesCompat.getFont(this, R.font.paperlogy_5)),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    private void appendBulletText(SpannableStringBuilder builder, String text) {
        int start = builder.length();
        builder.append(text);
        int end = builder.length();

        builder.setSpan(new BulletSpan(16), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new LeadingMarginSpan.Standard(28, 28), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new AbsoluteSizeSpan(18, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(
                new CustomTypefaceSpan(ResourcesCompat.getFont(this, R.font.paperlogy_5)),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }

    private void appendNumberedText(SpannableStringBuilder builder, String text) {
        int start = builder.length();
        builder.append(text);
        int end = builder.length();

        // 1. 계정 정보 같은 건 너무 세지 않게
        builder.setSpan(new AbsoluteSizeSpan(18, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(
                new CustomTypefaceSpan(ResourcesCompat.getFont(this, R.font.paperlogy_6)),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }
}
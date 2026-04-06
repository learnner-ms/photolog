package com.example.photolog_front;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TermsDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ASSET_PATH = "asset_path";
    public static final String EXTRA_TYPE = "type";

    private TextView tvTermsTitle;
    private LinearLayout layoutTermsContent;
    private AppCompatButton btnBack;
    private AppCompatButton btnAgree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_detail);

        tvTermsTitle = findViewById(R.id.tvTermsTitle);
        layoutTermsContent = findViewById(R.id.layoutTermsContent);
        btnBack = findViewById(R.id.btnBack);
        btnAgree = findViewById(R.id.btnAgree);

        LinearLayout logoLayout = findViewById(R.id.layout_logo);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String assetPath = getIntent().getStringExtra(EXTRA_ASSET_PATH);
        String type = getIntent().getStringExtra(EXTRA_TYPE);

        tvTermsTitle.setText(title != null ? title : "약관");

        String content = readAssetFile(assetPath);
        renderMarkdownContent(content);

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

    private String readAssetFile(String assetPath) {
        if (assetPath == null || assetPath.trim().isEmpty()) {
            return "약관 파일 경로가 없습니다.";
        }

        StringBuilder builder = new StringBuilder();

        try (InputStream is = getAssets().open(assetPath);
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }

        } catch (IOException e) {
            return "약관 파일을 불러오지 못했습니다.\n(" + assetPath + ")";
        }

        return builder.toString().trim();
    }

    private void renderMarkdownContent(String text) {
        layoutTermsContent.removeAllViews();

        if (text == null || text.isEmpty()) {
            addBodyTextView("약관 내용이 없습니다.");
            return;
        }

        String[] lines = text.split("\\n", -1);

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            String trimmed = rawLine == null ? "" : rawLine.trim();

            if (trimmed.isEmpty()) {
                addSpacer(10);
                continue;
            }

            if (trimmed.equals("---")) {
                addSpacer(14);
                continue;
            }

            if (trimmed.startsWith("# ")) {
                addStyledTextView(trimmed.substring(2).trim(), 24, true, R.font.paperlogy_7, false);
            } else if (trimmed.startsWith("## ")) {
                String heading = trimmed.substring(3).trim();

                if (heading.matches("^제\\s*\\d+\\s*장.*")) {
                    addStyledTextView(heading, 23, true, R.font.paperlogy_7, false);
                } else if (heading.matches("^제\\s*\\d+\\s*조.*")) {
                    addStyledTextView(heading, 21, true, R.font.paperlogy_7, false);
                } else {
                    addStyledTextView(heading, 22, true, R.font.paperlogy_7, false);
                }
            } else if (trimmed.startsWith("### ")) {
                addStyledTextView(trimmed.substring(4).trim(), 19, true, R.font.paperlogy_7, false);
            } else if (trimmed.matches("^제\\s*\\d+\\s*장.*")) {
                addStyledTextView(trimmed, 23, true, R.font.paperlogy_7, false);
            } else if (trimmed.matches("^제\\s*\\d+\\s*조.*")) {
                addStyledTextView(trimmed, 21, true, R.font.paperlogy_7, false);
            } else if (isMarkdownTableRow(trimmed)) {
                List<List<String>> rows = new ArrayList<>();

                while (i < lines.length) {
                    String line = lines[i] == null ? "" : lines[i].trim();

                    if (line.isEmpty()) {
                        break;
                    }

                    if (isMarkdownTableSeparator(line)) {
                        i++;
                        continue;
                    }

                    if (!isMarkdownTableRow(line)) {
                        break;
                    }

                    rows.add(parseTableCells(line));
                    i++;
                }

                i--;
                addTableView(rows);
            } else if (trimmed.startsWith(">")) {
                addQuoteTextView(trimmed.substring(1).trim());
            } else if (trimmed.startsWith("- ")) {
                addBulletTextView(trimmed.substring(2).trim());
            } else if (trimmed.matches("^\\d+\\.\\s+.*")) {
                addStyledTextView(trimmed, 18, false, R.font.paperlogy_6, false);
            } else {
                addBodyTextView(trimmed);
            }
        }
    }

    private boolean isMarkdownTableRow(String line) {
        return line.startsWith("|") && line.endsWith("|") && line.indexOf('|', 1) > 0;
    }

    private boolean isMarkdownTableSeparator(String line) {
        return line.matches("^\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?$");
    }

    private List<String> parseTableCells(String line) {
        String row = line.trim();

        if (row.startsWith("|")) {
            row = row.substring(1);
        }
        if (row.endsWith("|")) {
            row = row.substring(0, row.length() - 1);
        }

        String[] parts = row.split("\\|");
        List<String> cells = new ArrayList<>();
        for (String part : parts) {
            cells.add(part.trim());
        }
        return cells;
    }

    private void addStyledTextView(String text, int textSizeSp, boolean bold, int fontRes, boolean italic) {
        TextView textView = new TextView(this);
        textView.setText(applyInlineMarkdown(text));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        textView.setLineSpacing(0f, 1.4f);
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.black));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(4);
        params.bottomMargin = dpToPx(4);
        textView.setLayoutParams(params);

        Typeface typeface = ResourcesCompat.getFont(this, fontRes);
        if (typeface != null) {
            if (bold && italic) {
                textView.setTypeface(typeface, Typeface.BOLD_ITALIC);
            } else if (bold) {
                textView.setTypeface(typeface, Typeface.BOLD);
            } else if (italic) {
                textView.setTypeface(typeface, Typeface.ITALIC);
            } else {
                textView.setTypeface(typeface);
            }
        }

        layoutTermsContent.addView(textView);
    }

    private void addBodyTextView(String text) {
        addStyledTextView(text, 18, false, R.font.paperlogy_5, false);
    }

    private void addQuoteTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(applyInlineMarkdown("※ " + text));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        textView.setLineSpacing(0f, 1.4f);
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        textView.setTypeface(ResourcesCompat.getFont(this, R.font.paperlogy_5), Typeface.ITALIC);
        textView.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(4);
        params.bottomMargin = dpToPx(4);
        textView.setLayoutParams(params);

        layoutTermsContent.addView(textView);
    }

    private void addBulletTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(applyInlineMarkdown("• " + text));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        textView.setLineSpacing(0f, 1.4f);
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        textView.setTypeface(ResourcesCompat.getFont(this, R.font.paperlogy_5));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dpToPx(4);
        params.bottomMargin = dpToPx(4);
        textView.setLayoutParams(params);

        layoutTermsContent.addView(textView);
    }

    private void addTableView(List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) return;

        TableLayout tableLayout = new TableLayout(this);
        LinearLayout.LayoutParams tableParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tableParams.topMargin = dpToPx(12);
        tableParams.bottomMargin = dpToPx(12);
        tableLayout.setLayoutParams(tableParams);
        tableLayout.setStretchAllColumns(true);
        tableLayout.setShrinkAllColumns(true);
        tableLayout.setBackgroundResource(R.drawable.bg_terms_table_outer);

        for (int r = 0; r < rows.size(); r++) {
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
            ));
            tableRow.setBaselineAligned(false);

            List<String> row = rows.get(r);
            List<TextView> cellViews = new ArrayList<>();

            for (int c = 0; c < row.size(); c++) {
                TextView cell = new TextView(this);
                cell.setText(applyInlineMarkdown(row.get(c)));
                cell.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
                cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                cell.setLineSpacing(0f, 1.3f);
                cell.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                cell.setBackgroundResource(R.drawable.bg_terms_table_cell);
                cell.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

                Typeface typeface = ResourcesCompat.getFont(
                        this,
                        r == 0 ? R.font.paperlogy_7 : R.font.paperlogy_5
                );
                if (typeface != null) {
                    cell.setTypeface(typeface, r == 0 ? Typeface.BOLD : Typeface.NORMAL);
                }

                TableRow.LayoutParams cellParams = new TableRow.LayoutParams(
                        0,
                        TableRow.LayoutParams.WRAP_CONTENT,
                        c == 0 ? 1.2f : 2.8f
                );
                cell.setLayoutParams(cellParams);

                cellViews.add(cell);
                tableRow.addView(cell);
            }

            tableLayout.addView(tableRow);

            tableRow.post(() -> equalizeRowHeight(cellViews));
        }

        layoutTermsContent.addView(tableLayout);
    }

    private void equalizeRowHeight(List<TextView> cellViews) {
        int maxHeight = 0;

        for (TextView cell : cellViews) {
            if (cell.getHeight() > maxHeight) {
                maxHeight = cell.getHeight();
            }
        }

        if (maxHeight == 0) return;

        for (TextView cell : cellViews) {
            TableRow.LayoutParams params = (TableRow.LayoutParams) cell.getLayoutParams();
            params.height = maxHeight;
            cell.setLayoutParams(params);
        }
    }

    private void addSpacer(int dp) {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(dp)
        ));
        layoutTermsContent.addView(spacer);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private CharSequence applyInlineMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        List<InlineStyleRange> ranges = new ArrayList<>();
        String parsed = parseInlineMarkdown(text, ranges);

        SpannableString spannable = new SpannableString(parsed);

        for (InlineStyleRange range : ranges) {
            if (range.start < 0 || range.end > parsed.length() || range.start >= range.end) {
                continue;
            }

            if (range.bold && range.italic) {
                spannable.setSpan(
                        new StyleSpan(Typeface.BOLD_ITALIC),
                        range.start,
                        range.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else if (range.bold) {
                spannable.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        range.start,
                        range.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else if (range.italic) {
                spannable.setSpan(
                        new StyleSpan(Typeface.ITALIC),
                        range.start,
                        range.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }

        return spannable;
    }

    private String parseInlineMarkdown(String source, List<InlineStyleRange> ranges) {
        StringBuilder out = new StringBuilder();
        int i = 0;

        while (i < source.length()) {
            if (i + 2 < source.length() && source.startsWith("***", i)) {
                int end = source.indexOf("***", i + 3);
                if (end != -1) {
                    int startOut = out.length();
                    String inner = source.substring(i + 3, end);
                    out.append(inner);
                    ranges.add(new InlineStyleRange(startOut, startOut + inner.length(), true, true));
                    i = end + 3;
                    continue;
                }
            }

            if (i + 1 < source.length() && source.startsWith("**", i)) {
                int end = source.indexOf("**", i + 2);
                if (end != -1) {
                    int startOut = out.length();
                    String inner = source.substring(i + 2, end);
                    out.append(inner);
                    ranges.add(new InlineStyleRange(startOut, startOut + inner.length(), true, false));
                    i = end + 2;
                    continue;
                }
            }

            if (source.charAt(i) == '*') {
                int end = source.indexOf("*", i + 1);
                if (end != -1) {
                    int startOut = out.length();
                    String inner = source.substring(i + 1, end);
                    out.append(inner);
                    ranges.add(new InlineStyleRange(startOut, startOut + inner.length(), false, true));
                    i = end + 1;
                    continue;
                }
            }

            out.append(source.charAt(i));
            i++;
        }

        return out.toString();
    }

    private static class InlineStyleRange {
        int start;
        int end;
        boolean bold;
        boolean italic;

        InlineStyleRange(int start, int end, boolean bold, boolean italic) {
            this.start = start;
            this.end = end;
            this.bold = bold;
            this.italic = italic;
        }
    }
}
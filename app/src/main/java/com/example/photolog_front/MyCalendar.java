package com.example.photolog_front;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@SuppressLint("NewApi")
public class MyCalendar extends FrameLayout {
    private CalendarView calendarView;
    private  TextView tvYearMonth;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private LocalDate selectedDate = null;
    private YearMonth currentMonth = YearMonth.now();
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault());

    public MyCalendar(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(getContext()).inflate(R.layout.view_custom_calendar, this, true);
        calendarView = findViewById(R.id.calendar_view);
        tvYearMonth = findViewById(R.id.tv_year_month);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);
        setupCalendar();
    }

    private void setupCalendar() {
        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }
            @Override
            public void bind(@NonNull DayViewContainer container, CalendarDay day) {
                container.dayText.setText(String.valueOf(day.getDate().getDayOfMonth()));
                container.dayBackground.setVisibility(View.INVISIBLE);

                if (day.getPosition() == DayPosition.MonthDate) {
                    //이번 달 날짜 처리
                    container.dayText.setTextColor(Color.BLACK);
                    container.getView().setClickable(true);
                    //클릭 이벤트
                    container.getView().setOnClickListener(v -> {
                        if (selectedDate != null && selectedDate.equals(day.getDate())) {
                            selectedDate = null;
                        }
                        else {
                            selectedDate = day.getDate();
                        }
                        calendarView.notifyCalendarChanged();
                    });
                    //선택된 날짜 배경 표시
                    if (day.getDate().equals(selectedDate)) {
                        container.dayBackground.setVisibility(View.VISIBLE);
                        container.dayBackground.setBackgroundResource(R.drawable.calendar_day_selected_bg);
                    }
                }
                else {
                    container.dayText.setTextColor(Color.GRAY);
                    container.getView().setClickable(false);
                }
            }
        });

        calendarView.setMonthScrollListener(calendarMonth -> {
            currentMonth = calendarMonth.getYearMonth();
            updateYearMonthText();
            return null;
        });

        btnPrevMonth.setOnClickListener(v -> {
            currentMonth = currentMonth.minusMonths(1);
            calendarView.smoothScrollToMonth(currentMonth);
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth = currentMonth.plusMonths(1);
            calendarView.smoothScrollToMonth(currentMonth);
        });

        YearMonth startMonth = currentMonth.minusMonths(100);
        YearMonth endMonth = currentMonth.plusMonths(100);
        calendarView.setup(startMonth, endMonth, DayOfWeek.SUNDAY);
        calendarView.scrollToMonth(currentMonth);
        updateYearMonthText();
    }

    private void updateYearMonthText() {
        String year = String.valueOf(currentMonth.getYear());
        String month = monthFormatter.format(currentMonth);
        tvYearMonth.setText(year +  " " + month);
    }
}

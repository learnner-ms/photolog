package com.example.photolog_front;

import android.view.View;
import android.widget.TextView;
import com.kizitonwose.calendar.view.ViewContainer;

public class DayViewContainer extends ViewContainer {
    public TextView dayText;
    public View dayBackground;

    public DayViewContainer(View view) {
        super(view);
        dayText = view.findViewById(R.id.calendar_day_text);
        dayBackground = view.findViewById(R.id.day_background_view);
    }
}

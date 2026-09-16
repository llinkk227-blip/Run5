package com.example.runintervals;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.graphics.Typeface;

public class MainActivity extends Activity {

    private long seconds = 0;
    private boolean running = false;
    private TextView timer;
    private final Handler handler = new Handler();

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (running) {
                seconds++;
                timer.setText(formatTime(seconds));
                handler.postDelayed(this, 1000);
            }
        }
    };

    private String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;

        return String.format(
                "%02d:%02d:%02d",
                hours,
                minutes,
                secs
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(40, 80, 40, 40);

        TextView title = new TextView(this);
        title.setText("Бег");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        layout.addView(title);

        timer = new TextView(this);
        timer.setText("00:00:00");
        timer.setTextSize(48);
        timer.setGravity(Gravity.CENTER);
        layout.addView(timer);

        Button start = new Button(this);
        start.setText("СТАРТ / ПАУЗА");

        start.setOnClickListener(v -> {
            running = !running;

            if (running) {
                handler.post(tick);
            }
        });

        layout.addView(start);

        Button reset = new Button(this);
        reset.setText("СБРОС");

        reset.setOnClickListener(v -> {
            running = false;
            seconds = 0;
            timer.setText("00:00:00");
        });

        layout.addView(reset);

        setContentView(layout);
    }
    }

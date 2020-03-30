package com.showreal.app.features.real.myreal;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.CountDownTimer;
import android.support.percent.PercentRelativeLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.showreal.app.R;

import java.util.concurrent.TimeUnit;

public class CountdownView extends PercentRelativeLayout {
    private TextView text;
    private Paint paint = new Paint();
    private Paint fillPaint = new Paint();
    private int textSpace;
    private float lineThickness;

    private CountDownTimer timer;
    private CountdownListener listener;
    private View line;
    private ObjectAnimator lineAnimator;

    interface CountdownListener {

        void onCountdownFinished();
    }

    public CountdownView(Context context) {
        super(context);
    }

    public CountdownView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public CountdownView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    public void setListener(CountdownListener listener) {
        this.listener = listener;
    }

    private void init() {
        lineThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, getResources().getDisplayMetrics());
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(lineThickness);
        paint.setStyle(Paint.Style.STROKE);

        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAlpha((int) (0.4f * 255));
    }

    private CountDownTimer getTimer() {
        return timer = new CountDownTimer(3100, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i("countdown", "" + millisUntilFinished + ", " + String.valueOf(TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)));
                text.setText(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)));
            }

            @Override
            public void onFinish() {
                finishCountdown();
            }
        };
    }

    private void finishCountdown() {
        stopCountdown();
        if (listener != null) {
            listener.onCountdownFinished();
        }
    }

    public void startCountdown() {
        invalidate();
        stopCountdown();
        if (text == null) {
            text = (TextView) getChildAt(0);
            text.setText("3");
        }
        if (line == null) {
            line = findViewById(R.id.line);
            line.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    line.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    line.setPivotY(line.getHeight());
                }
            });
        }

        line.setRotation(0);
        lineAnimator = ObjectAnimator.ofFloat(line, "rotation", 360);
        lineAnimator.setDuration(1000);
        lineAnimator.setRepeatCount(2);
        lineAnimator.start();

        getTimer().start();
    }

    public void stopCountdown() {
        if (lineAnimator != null) {
            lineAnimator.cancel();
        }
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (text == null) {
            text = (TextView) getChildAt(0);
            text.setText("3");
        }

        int width = getWidth();
        int height = getHeight();
        float cX = width / 2f;
        float cY = height / 2f;
        float radius = (width / 2f) - lineThickness;

        canvas.drawCircle(cX, cY, radius, fillPaint);
        canvas.drawCircle(cX, cY, radius, paint);
        canvas.drawCircle(cX, cY, radius * 0.8f, paint);

        if (textSpace == 0) {
            textSpace = Math.max(text.getWidth(), text.getHeight());
        }

        if (textSpace > 0) {

            float lineLength = (width / 2f) - (textSpace / 2f);

            canvas.drawLine(0 + lineThickness, cY, lineLength, cY, paint);
            canvas.drawLine(width - lineThickness, cY, width - lineLength, cY, paint);

            lineLength = (height / 2f) - (textSpace / 2f);

            canvas.drawLine(cX, 0 + lineThickness, cX, lineLength, paint);
            canvas.drawLine(cX, height - lineThickness, cX, height - lineLength, paint);

        }
    }
}

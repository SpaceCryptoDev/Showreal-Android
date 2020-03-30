package com.showreal.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.annotation.ColorInt;
import android.support.percent.PercentFrameLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;


public class CircleOverlayView extends PercentFrameLayout {

    protected Paint clearPaint;
    protected float radius;
    protected int color = Color.WHITE;
    protected float centerY;
    protected float centerX;
    protected boolean highlight;
    protected Paint highlightPaint;

    public CircleOverlayView(Context context) {
        super(context);
        init(context, null);
    }

    public CircleOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircleOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {
        setWillNotDraw(false);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleOverlayView);
            color = a.getColor(R.styleable.CircleOverlayView_overlay_backgroundColor, color);
            a.recycle();
        }
    }

    public void setOverlayColor(@ColorInt int color) {
        this.color = color;
        invalidate();
    }

    private void initHighlight() {
        if (highlightPaint == null) {
            highlightPaint = new Paint();
            highlightPaint.setColor(ContextCompat.getColor(getContext(), R.color.red_translucent));
        }
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(color);

        radius = Math.min(getWidth(), getHeight()) / 2f;
        centerY = getHeight() / 2f;
        centerX = getWidth() / 2f;

        if (radius >= 0) {
            radius -= getPaddingLeft() / 2f;
            radius -= getPaddingRight() / 2f;

            if (highlight) {
                initHighlight();
                float highlightRadius = radius * 1.2f;
                highlightRadius = Math.min(getWidth() / 2f, highlightRadius);
                canvas.drawCircle(centerX, centerY, highlightRadius, highlightPaint);
            }

            canvas.drawCircle(centerX, centerY, radius, clearPaint);
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean drawn = super.drawChild(canvas, child, drawingTime);
        if (radius >= 0) {
            canvas.drawCircle(centerX, centerY, radius, clearPaint);
        }
        return drawn;
    }


}
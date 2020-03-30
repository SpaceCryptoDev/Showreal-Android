package com.showreal.app.features.real.myreal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import com.showreal.app.CircleOverlayView;
import com.showreal.app.R;


public class RecordOverlayView extends CircleOverlayView {

    Paint paint;
    RectF arcRect;
    boolean drawArc = false;

    public void setDrawArc(boolean drawArc) {
        this.drawArc = drawArc;
        invalidate();

    }

    public RecordOverlayView(Context context) {
        super(context);
    }

    public RecordOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecordOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context, AttributeSet attrs) {
        super.init(context, attrs);

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        if (isInEditMode()) {
            paint.setColor(Color.RED);
        } else {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.black54));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (drawArc && radius >= 0) {
            if (arcRect == null) {
                arcRect = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
            }
            canvas.drawArc(arcRect, 60, 60, false, paint);
        }
    }
}
package com.showreal.app;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class SRRelativeLayout extends RelativeLayout {

    static Paint linePaint = new Paint();
    static {
        linePaint.setStyle(Paint.Style.STROKE);
    }
    boolean lineDrawn;
    public boolean drawLines = true;

    public SRRelativeLayout(Context context) {
        super(context);
        init();
    }

    public SRRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SRRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SRRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        linePaint.setPathEffect(new DashPathEffect(new float[] {width, width}, 0));
        linePaint.setStrokeWidth(width / 2);
        linePaint.setColor(getContext().getResources().getColor(R.color.grey_divider));
    }


    private float x = -1;
    private float heightTop;
    private float heightBottom;

    @Override
    protected void dispatchDraw(Canvas canvas) {
        lineDrawn = false;
        super.dispatchDraw(canvas);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean drawn =  super.drawChild(canvas, child, drawingTime);

        if (drawLines && !lineDrawn && child instanceof ImageView) {
            if (x == -1) {
                x = child.getX() + (child.getWidth() / 2f);
                heightTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
                heightBottom = heightTop + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
            }
            float gap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());

            Path path = new Path();
            path.moveTo(x, gap);
            path.quadTo(x, heightTop, x, heightTop);

            canvas.drawPath(path, linePaint);

            path.moveTo(x, heightBottom);
            path.quadTo(x, getHeight() - gap, x, getHeight() - gap);

            canvas.drawPath(path, linePaint);

            lineDrawn = true;
        }

        return drawn;
    }
}

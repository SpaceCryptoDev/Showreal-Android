package com.showreal.app.features.onboarding.signup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SignUpStepsLayout extends LinearLayout {
    private Paint paint;

    public SignUpStepsLayout(Context context) {
        super(context);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, getResources().getDisplayMetrics()));
    }

    public SignUpStepsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SignUpStepsLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SignUpStepsLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getChildCount() >= 3) {
            View one = getChildAt(0);
            View two = getChildAt(1);
            View three = getChildAt(2);

            if (one != null && two != null && three != null) {
                Drawable drawable1 = ((TextView) one).getCompoundDrawables()[1];
                Drawable drawable2 = ((TextView) two).getCompoundDrawables()[1];
                Drawable drawable3 = ((TextView) three).getCompoundDrawables()[1];

                Rect rect1 = drawable1.getBounds();
                Rect rect2 = drawable2.getBounds();
                Rect rect3 = drawable3.getBounds();

                int y = rect1.centerY() + one.getTop() + one.getPaddingTop();
                int x1 = (int) ((rect1.width() / 2f) + one.getLeft() + (one.getWidth() / 2f));
                int x2 = (int)  (two.getLeft() + (two.getWidth() / 2f) - (rect2.width() / 2f));

                canvas.drawLine(x1, y, x2, y, paint);

                x1 = (int) ((rect2.width() / 2f) + two.getLeft() + (two.getWidth() / 2f));
                x2 = (int) (three.getLeft() + (three.getWidth() / 2f) - (rect3.width() / 2f));

                canvas.drawLine(x1, y, x2, y, paint);
            }
        }
    }
}

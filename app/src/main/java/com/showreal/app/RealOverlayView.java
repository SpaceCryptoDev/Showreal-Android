package com.showreal.app;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.percent.PercentFrameLayout;
import android.support.percent.PercentRelativeLayout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;

import com.showreal.app.databinding.FragmentShowrealBinding;
import com.showreal.app.databinding.ItemRealBinding;
import com.showreal.app.databinding.ItemRealMyProfileBinding;
import com.showreal.app.databinding.ItemRealProfileBinding;

import io.netopen.hotbitmapgg.library.view.RingProgressBar;


public class RealOverlayView extends PercentFrameLayout {

    private Paint mBackgroundPaint;
    private float radius;
    private float centerX;
    private float centerY;
    private float topSpace = 0;
    private boolean topSpaceDisabled;
    private float ringDistance;
    private boolean highlight = false;
    private Paint mHighlightPaint;
    private boolean done;

    public RealOverlayView(Context context) {
        super(context);
        init();
    }

    public RealOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RealOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        mHighlightPaint = new Paint();
        mHighlightPaint.setStyle(Paint.Style.STROKE);
        mHighlightPaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, getResources().getDisplayMetrics()));
        mHighlightPaint.setColor(getResources().getColor(R.color.red));

        ringDistance = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, getResources().getDisplayMetrics());
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);

        ViewDataBinding binding = DataBindingUtil.findBinding(this);

        if (binding instanceof ItemRealBinding) {
            topSpace = ((ItemRealBinding) binding).frame.getY();
            sizeAndPositionRing((int) topSpace, ((ItemRealBinding) binding).frame.getWidth(), ((ItemRealBinding) binding).progress);

        } else if (binding instanceof ItemRealProfileBinding) {
            topSpace = ((ItemRealProfileBinding) binding).frame.getY() - ((ItemRealProfileBinding) binding).details.getHeight();
            sizeAndPositionRing((int) ((ItemRealProfileBinding) binding).frame.getY(), ((ItemRealProfileBinding) binding).frame.getWidth(), ((ItemRealProfileBinding) binding).progress);
        } else if (binding instanceof ItemRealMyProfileBinding) {
            topSpace = ((ItemRealMyProfileBinding) binding).frame.getY();
            sizeAndPositionRing((int) topSpace, ((ItemRealMyProfileBinding) binding).frame.getWidth(), ((ItemRealMyProfileBinding) binding).progress);
        } else if (binding instanceof FragmentShowrealBinding) {
            topSpaceDisabled = true;
            if (!done) {
                done = true;
                int videoTop = (int) ((FragmentShowrealBinding) binding).frame.getY();
                int videoSize = ((FragmentShowrealBinding) binding).frame.getWidth();

                int ringSize = (int) (videoSize + (2 * ringDistance));
                int ringTop = (int) (videoTop - ringDistance);
                PercentRelativeLayout.LayoutParams params = new PercentRelativeLayout.LayoutParams(ringSize, ringSize);
                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                params.addRule(RelativeLayout.BELOW, R.id.top_bar);
                params.topMargin = ringTop;
                ((FragmentShowrealBinding) binding).progress.setLayoutParams(params);
                ((FragmentShowrealBinding) binding).progress.requestLayout();
            }
        } else {
            topSpaceDisabled = true;
        }

        radius = ((getWidth() * 0.792f) / 2f);
        centerX = getWidth() / 2f;
        centerY = radius + (!topSpaceDisabled && topSpace == 0 ? TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics()) : topSpace);

        if (centerX >= 0 && radius >= 0) {
            canvas.drawCircle(centerX, centerY, radius, mBackgroundPaint);

            if (highlight) {
                canvas.drawCircle(centerX, centerY, radius - (mHighlightPaint.getStrokeWidth()), mHighlightPaint);
            }
        }
    }

    private void sizeAndPositionRing(int videoTop, int videoSize, RingProgressBar ring) {
        int ringSize = (int) (videoSize + (2 * ringDistance));
        int ringTop = (int) (videoTop - ringDistance);
        PercentRelativeLayout.LayoutParams params = new PercentRelativeLayout.LayoutParams(ringSize, ringSize);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.topMargin = ringTop;
        ring.setLayoutParams(params);
        ring.requestLayout();
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean drawn = super.drawChild(canvas, child, drawingTime);
        if (centerX >= 0 && radius >= 0) {
            canvas.drawCircle(centerX, centerY, radius, mBackgroundPaint);
        }
        return drawn;
    }


}
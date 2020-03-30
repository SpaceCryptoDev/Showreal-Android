package com.showreal.app.features.real.myreal;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;

public class GradientImageView extends ImageView {

    private int[] gradient = new int[2];
    private Paint tintPaint;
    private boolean useGradient;

    public GradientImageView(Context context) {
        super(context);
    }

    public GradientImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GradientImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GradientImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setGradient(@ColorInt int color1, @ColorInt int color2) {
        useGradient = true;
        gradient[0] = color1;
        gradient[1] = color2;
        if (tintPaint == null) {
            tintPaint = new Paint();
            tintPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        }
    }

    public void setGradientResources(@ColorRes int colorRes1, @ColorRes int colorRes2) {
        setGradient(ContextCompat.getColor(getContext(), colorRes1), ContextCompat.getColor(getContext(), colorRes2));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (isInEditMode()) {
            setGradient(Color.RED, Color.WHITE);
        }

        if (useGradient) {
            LinearGradient shader = new LinearGradient(0, 0, 0, height, gradient[0], gradient[1], Shader.TileMode.CLAMP);
            tintPaint.setShader(shader);
            canvas.drawRect(0, 0, width, height, tintPaint);
        }
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(Math.min(alpha, 0.99f));
    }
}

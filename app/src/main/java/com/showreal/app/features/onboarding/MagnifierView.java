package com.showreal.app.features.onboarding;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.showreal.app.features.onboarding.tutorial.CustomRecyclerView;


public class MagnifierView extends View implements CustomRecyclerView.DrawingListener {
    private Bitmap bitmap;
    private Rect srcRect;
    private RectF dstRect;

    public MagnifierView(Context context) {
        super(context);
    }

    public MagnifierView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MagnifierView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onDrawn(Bitmap bitmap) {
        this.bitmap = bitmap;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bitmap != null) {
            canvas.drawBitmap(bitmap, getSrcRect(bitmap), getDstRect(), null);
        }
    }

    public Rect getSrcRect(Bitmap bitmap) {
        if (srcRect == null) {
            float centerX = bitmap.getWidth() / 2f;
            float centerY = bitmap.getHeight() / 2f;

            int left = (int) (centerX - centerY);
            int right = left + bitmap.getHeight();
            srcRect = new Rect(left, 0, right, bitmap.getHeight());
        }
        return srcRect;
    }

    public RectF getDstRect() {
        if (dstRect == null) {
            dstRect = new RectF(0, 0, getWidth(), getHeight());
        }
        return dstRect;
    }
}

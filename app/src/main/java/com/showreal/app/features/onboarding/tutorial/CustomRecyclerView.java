package com.showreal.app.features.onboarding.tutorial;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;


public class CustomRecyclerView extends RecyclerView {

    private Canvas canvas;

    public interface DrawingListener {
        void onDrawn(Bitmap bitmap);
    }

    private DrawingListener drawingListener;

    public void setDrawingListener(DrawingListener drawingListener) {
//        setDrawingCacheEnabled(true);
        this.drawingListener = drawingListener;
    }

    public CustomRecyclerView(Context context) {
        super(context);
    }

    public CustomRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    Bitmap bitmap;

    @Override
    public void draw(Canvas c) {
        super.draw(c);

        if (drawingListener != null) {
            drawToBitmap();
            drawingListener.onDrawn(bitmap);
        }
    }

    private void drawToBitmap() {
        int width = getRight() - getLeft();
        int height = getBottom() - getTop();

        if (bitmap == null) {
            try {
                bitmap = Bitmap.createBitmap(getResources().getDisplayMetrics(),
                        width, height, Bitmap.Config.ARGB_8888);
                bitmap.setDensity(getResources().getDisplayMetrics().densityDpi);

            } catch (OutOfMemoryError e) {
                return;
            }
        }

        if (canvas == null) {
            canvas = new Canvas(bitmap);
        }

        bitmap.eraseColor(Color.TRANSPARENT);

        computeScroll();
        final int restoreCount = canvas.save();

        canvas.setMatrix(null);
        canvas.translate(-getScrollX(), -getScrollY());

        super.draw(canvas);

        canvas.restoreToCount(restoreCount);
    }
}

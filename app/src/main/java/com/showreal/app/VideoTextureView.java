package com.showreal.app;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.showreal.app.data.model.Video;

public class VideoTextureView extends TextureView implements SimpleExoPlayer.VideoListener, TextureView.SurfaceTextureListener {

    boolean adjust;
    private double scaleAdjust = 0;
    private float xAdjust = 0;
    private float yAdjust = 0;
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onSurfaceReady(Surface surface);
    }

    public VideoTextureView(Context context) {
        super(context);

        setSurfaceTextureListener(this);
    }

    public void setAdjust(boolean adjust) {
        this.adjust = adjust;
    }

    public void updateTextureViewSize(float width, float height, int rotation) {
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float scaleX = 1.0f;
        float scaleY = 1.0f;

        if (height > width) {
            scaleY = (viewWidth / width) / (viewHeight / height);
        } else {
            scaleX = (viewHeight / height) / (viewWidth / width);
        }

        // Calculate pivot points, in our case crop from center
        int pivotPointX;
        int pivotPointY;

        pivotPointX = (int) (viewWidth / 2);
        pivotPointY = (int) (viewHeight / (adjust ? 4 : 2));

        if (scaleAdjust != 0) {
            scaleX *= scaleAdjust;
            scaleY *= scaleAdjust;
        }

        Matrix matrix = new Matrix();

        matrix.setScale(scaleX, scaleY, pivotPointX, pivotPointY);
        if (rotation != 0) {
            matrix.postRotate(rotation, pivotPointX, pivotPointY);
        }
        matrix.postTranslate(xAdjust * getWidth(), yAdjust * getWidth());

        setTransform(matrix);
    }


    public void setVideo(Video video) {
        scaleAdjust = video.scale;
        xAdjust = (float) video.offsetX;
        yAdjust = (float) video.offsetY;

        if (Math.abs(xAdjust) > 1) {
            xAdjust = 0;
        }
        if (Math.abs(yAdjust) > 1) {
            yAdjust = 0;
        }
    }

    public void ready() {
        setSurfaceTextureListener(this);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        updateTextureViewSize(width, height, unappliedRotationDegrees);
    }

    @Override
    public void onRenderedFirstFrame() {

    }

    @Override
    public void onVideoTracksDisabled() {

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
        updateTextureViewSize(720, 1280, 0);
        if (listener != null) {
            Surface surface = new Surface(texture);
            listener.onSurfaceReady(surface);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
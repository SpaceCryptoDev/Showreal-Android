package com.showreal.app.features.real.myreal;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import com.showreal.app.R;

public class MyRealScrollView extends ScrollView {
    private View recycler;

    public MyRealScrollView(Context context) {
        super(context);
    }

    public MyRealScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyRealScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MyRealScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (recycler == null) {
            recycler = getParent(findViewById(R.id.segment_recycler));
        }

        if (recycler == null) {
            return super.onInterceptTouchEvent(ev);
        }

        float top = recycler.getY() - getScrollY();
        if (top > 0) {
            int height = recycler.getHeight();
            float bottom = top + height;

            if (ev.getY() >= top && ev.getY() <= bottom) {
                return false;
            }
        }

        return super.onInterceptTouchEvent(ev);
    }

    private View getParent(View view) {
        if (view == null) {
            return null;
        }
        View parent = (View) view.getParent();
        if (parent.equals(getChildAt(0))) {
            return view;
        }
        return getParent(parent);
    }
}

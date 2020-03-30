package com.showreal.app;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class BugFixViewPager extends ViewPager {
    public BugFixViewPager(Context context) {
        super(context);
    }

    public BugFixViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (java.lang.IllegalArgumentException e){
            return false;
        }
    }
}

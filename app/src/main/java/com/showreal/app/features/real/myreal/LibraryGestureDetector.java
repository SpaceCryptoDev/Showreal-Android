package com.showreal.app.features.real.myreal;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

class LibraryGestureDetector extends GestureDetector {

    public View view;

    public LibraryGestureDetector(Context context, OnGestureListener listener) {
        super(context, listener);
    }

    public boolean onTouchEvent(View view, MotionEvent ev) {
        this.view = view;
        return super.onTouchEvent(ev);
    }
}

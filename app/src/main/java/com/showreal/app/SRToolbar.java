package com.showreal.app;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class SRToolbar extends Toolbar {
    public SRToolbar(Context context) {
        super(context);
    }

    public SRToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SRToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTitle(CharSequence title) {
        View text = ((View) getParent().getParent()).findViewById(R.id.title);
        if (text != null) {
            ((TextView) text).setText(title);
        } else {
            super.setTitle(title);
        }
    }
}

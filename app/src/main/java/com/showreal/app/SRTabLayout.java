package com.showreal.app;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import uk.co.thedistance.thedistancetheming.fonts.Font;

public class SRTabLayout extends TabLayout {
    public SRTabLayout(Context context) {
        super(context);
    }

    public SRTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SRTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addTab(@NonNull Tab tab) {
        super.addTab(tab);

        customise(tab);
    }

    @Override
    public void addTab(@NonNull Tab tab, int position) {
        super.addTab(tab, position);

        customise(tab);
    }

    @Override
    public void addTab(@NonNull Tab tab, boolean setSelected) {
        super.addTab(tab, setSelected);

        customise(tab);
    }

    @Override
    public void addTab(@NonNull Tab tab, int position, boolean setSelected) {
        super.addTab(tab, position, setSelected);

        customise(tab);
    }

    private void customise(Tab tab) {
        ViewGroup view = (ViewGroup) getChildAt(0);
        ViewGroup tabView = (ViewGroup) view.getChildAt(tab.getPosition());
        View tabViewChild = tabView.getChildAt(1);
        Font.setFont(tabViewChild, getResources().getString(R.string.FontSubHeadline));

        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) tabView.getLayoutParams();
        p.setMargins(margin, 0, margin, 0);
        tabView.requestLayout();
    }
}

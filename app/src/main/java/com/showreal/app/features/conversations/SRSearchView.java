package com.showreal.app.features.conversations;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.showreal.app.R;


public class SRSearchView extends MaterialSearchView {
    public SRSearchView(Context context) {
        super(context);
    }

    public SRSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SRSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void showSuggestions() {
        showTintView(true);
        super.showSuggestions();
    }

    @Override
    public void dismissSuggestions() {
        showTintView(false);
        super.dismissSuggestions();
    }

    public void showTintView(boolean show) {
        View tintView = findViewById(R.id.transparent_view);
        if (tintView != null) {
            tintView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}

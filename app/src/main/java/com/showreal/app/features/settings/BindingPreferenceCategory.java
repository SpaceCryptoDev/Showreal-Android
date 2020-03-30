package com.showreal.app.features.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.showreal.app.R;
import com.showreal.app.databinding.PreferenceCategoryBinding;

import uk.co.thedistance.thedistancetheming.fonts.Font;

public class BindingPreferenceCategory extends PreferenceCategory {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BindingPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BindingPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BindingPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BindingPreferenceCategory(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        final LayoutInflater layoutInflater =
                (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final PreferenceCategoryBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.preference_category, parent, false);

        Font.setFont(binding.getRoot().findViewById(android.R.id.title), getContext().getString(R.string.FontBody2));

        return binding.getRoot();
    }
}


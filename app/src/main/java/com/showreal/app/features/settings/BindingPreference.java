package com.showreal.app.features.settings;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.showreal.app.R;
import com.showreal.app.databinding.PreferenceBinding;

import uk.co.thedistance.thedistancetheming.fonts.Font;

public class BindingPreference extends Preference {
    public BindingPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BindingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BindingPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {


        final LayoutInflater layoutInflater =
                (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final PreferenceBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.preference, parent, false);

        final ViewGroup widgetFrame = binding.widgetFrame;

        Font.setFont(binding.getRoot().findViewById(android.R.id.title), getContext().getString(R.string.FontTitle));
        Font.setFont(binding.getRoot().findViewById(android.R.id.summary), getContext().getString(R.string.FontBody1));

        if (widgetFrame != null) {
            if (getWidgetLayoutResource() != 0) {
                layoutInflater.inflate(getWidgetLayoutResource(), widgetFrame);
            } else {
                widgetFrame.setVisibility(View.GONE);
            }
        }

        return binding.getRoot();
    }
}

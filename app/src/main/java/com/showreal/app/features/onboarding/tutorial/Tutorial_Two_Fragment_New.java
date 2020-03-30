package com.showreal.app.features.onboarding.tutorial;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.showreal.app.BaseFragment;
import com.showreal.app.R;
import com.showreal.app.databinding.PageOnboardingOneBinding;

public class Tutorial_Two_Fragment_New extends BaseFragment {

    private PageOnboardingOneBinding binding;

    @Override
    protected String getScreenName() {
        return null;
    }

    public static Tutorial_Two_Fragment_New newInstance() {
        return new Tutorial_Two_Fragment_New();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tutorial_one_new, container, false);

        binding.image.setImageResource(R.drawable.tutorial_2);
        binding.title.setText(getResources().getString(R.string.onboarding_title_2));
        binding.text.setText(getResources().getString(R.string.onboarding_text_2_new));

        return binding.getRoot();
    }
}
package com.showreal.app.features.onboarding.tutorial;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.showreal.app.BaseFragment;
import com.showreal.app.R;
import com.showreal.app.databinding.PageOnboardingBinding;

public class TutorialPageFragment extends BaseFragment {

    @Override
    protected String getScreenName() {
        return null;
    }

    public static TutorialPageFragment newInstance(int page) {

        Bundle args = new Bundle();
        args.putInt("page", page);
        TutorialPageFragment fragment = new TutorialPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        PageOnboardingBinding binding = DataBindingUtil.inflate(inflater, R.layout.page_onboarding, container, false);

        int pageInt = getArguments().getInt("page");
        Page page = Page.values()[pageInt];
        binding.image.setImageResource(page.imageRes);
        binding.title.setText(getString(page.titleRes));
        binding.text.setText(getString(page.textRes));

        return binding.getRoot();
    }
}

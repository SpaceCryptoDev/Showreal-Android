package com.showreal.app.features.onboarding.tutorial;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.showreal.app.R;
import com.showreal.app.databinding.ActivityIntroBinding;

import uk.co.thedistance.thedistancecore.Version;

public class IntroActivity extends OnboardingActivity implements IntroViewModel.IntroView {

    private ActivityIntroBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_intro);

        if (Version.isMarshmallow()) {
            binding.getRoot().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        binding.pager.setAdapter(new OnboardingAdapter(getSupportFragmentManager(), -1));
        binding.circles.setViewPager(binding.pager);

        binding.setViewModel(new IntroViewModel(this));
    }

    @Override
    public void close() {
        finish();
    }
}

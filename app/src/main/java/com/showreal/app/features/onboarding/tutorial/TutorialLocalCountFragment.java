package com.showreal.app.features.onboarding.tutorial;

import android.animation.ValueAnimator;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.showreal.app.BaseFragment;
import com.showreal.app.R;
import com.showreal.app.databinding.FragmentTutorialCountBinding;

import uk.co.thedistance.thedistancecore.animation.AnimationHelper;

public class TutorialLocalCountFragment extends BaseFragment {

    private FragmentTutorialCountBinding binding;
    private int count;

    @Override
    protected String getScreenName() {
        return null;
    }

    public static TutorialLocalCountFragment newInstance(int count) {

        Bundle args = new Bundle();
        args.putInt("count", count);
        TutorialLocalCountFragment fragment = new TutorialLocalCountFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tutorial_count, container, false);

        count = getArguments().getInt("count");
        binding.countFrame.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.countFrame.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                showCount();
            }
        });

        return binding.getRoot();
    }

    private void showCount() {
        AnimationHelper.circularReveal(binding.countFrame, 600, 200);
        AnimationHelper.fadeIn(binding.users, 800, 800);
        AnimationHelper.fadeIn(binding.text, 800, 400);
        AnimationHelper.fadeIn(binding.showreal, 800, 800);

        binding.progress.setProgress(0);
        binding.progress.setMax(count);
        final AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        ValueAnimator animator = ValueAnimator.ofInt(1, count);
        animator.setInterpolator(interpolator);
        animator.setDuration(2000);
        animator.setStartDelay(600);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                binding.progress.setProgress(value);
                binding.count.setText(String.valueOf(value));
            }
        });
        animator.start();
    }


}

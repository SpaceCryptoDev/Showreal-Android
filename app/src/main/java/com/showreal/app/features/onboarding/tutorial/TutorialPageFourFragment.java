package com.showreal.app.features.onboarding.tutorial;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.showreal.app.BaseFragment;
import com.showreal.app.R;
import com.showreal.app.databinding.PageOnboardingFourBinding;

import java.util.ArrayList;
import java.util.List;

public class TutorialPageFourFragment extends BaseFragment {

    private PageOnboardingFourBinding binding;
    private AnimatorSet animatorSet;

    @Override
    protected String getScreenName() {
        return null;
    }

    public static TutorialPageFourFragment newInstance() {
        return new TutorialPageFourFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.page_onboarding_four, container, false);

        return binding.getRoot();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (!isVisibleToUser) {
            stopAnimations();
        } else if (animatorSet == null) {
            startAnimations();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        stopAnimations();
    }

    private void stopAnimations() {
        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }
        if (binding != null) {
            binding.chat.setSelected(false);
            binding.messageOne.setVisibility(View.INVISIBLE);
            binding.messageTwo.setVisibility(View.INVISIBLE);
            binding.messageThree.setVisibility(View.INVISIBLE);
            binding.messageFour.setVisibility(View.INVISIBLE);
            binding.messageFive.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getUserVisibleHint()) {
            startAnimations();
        }
    }

    final Handler handler = new Handler(Looper.getMainLooper());
    private void startAnimations() {
        stopAnimations();

        List<Animator> animators = new ArrayList<>();

        handler.postDelayed(new FlashButtonRunnable(binding.chat), 400);

        animators.add(translation(binding.chat, 0, (int) (-1.05f * binding.chat.getWidth()), 300, 1000));

        int startRight = binding.chat.getWidth();
        int margin = (int) (0.05f * binding.image.getWidth());

        animators.add(translation(binding.messageOne, startRight, 0, 300, 200));
        animators.add(translation(binding.messageTwo, -binding.messageTwo.getWidth() - margin, 0, 300, 200));
        animators.add(translation(binding.messageThree, -binding.messageThree.getWidth() - margin, 0, 300, 0));
        animators.add(translation(binding.messageFour, startRight, 0, 300, 0));
        animators.add(translation(binding.messageFive, -binding.messageFive.getWidth() - margin, 0, 300, 0));

        animatorSet = new AnimatorSet();
        animatorSet.playSequentially(animators);
        animatorSet.start();

    }

    private class FlashButtonRunnable implements Runnable {
        private final View view;

        private FlashButtonRunnable(View view) {
            this.view = view;
        }

        @Override
        public void run() {
            view.setSelected(true);
        }
    }

    private ObjectAnimator translation(View view, int from, int distance, int duration, int delay) {
        if (from != view.getTranslationX()) {
            view.setTranslationX(from);
        }
        view.setVisibility(View.VISIBLE);
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", distance);
        animator.setDuration(duration);
        animator.setStartDelay(delay);

        return animator;
    }
}

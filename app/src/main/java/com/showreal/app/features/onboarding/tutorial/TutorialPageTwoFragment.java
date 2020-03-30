package com.showreal.app.features.onboarding.tutorial;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.percent.PercentRelativeLayout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.showreal.app.BaseFragment;
import com.showreal.app.R;
import com.showreal.app.databinding.PageOnboardingTwoBinding;

public class TutorialPageTwoFragment extends BaseFragment {

    private PageOnboardingTwoBinding binding;
    private ObjectAnimator animator;
    private boolean laidOut;

    @Override
    protected String getScreenName() {
        return null;
    }

    public static TutorialPageTwoFragment newInstance() {
        return new TutorialPageTwoFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.page_onboarding_two, container, false);

        SpannableString spannableString = new SpannableString("2nd\nChance");
        spannableString.setSpan(new SuperscriptSpan(), 1, 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableString.setSpan(new RelativeSizeSpan(0.8f), 1, 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        binding.buttons.textView.setText(spannableString);
        binding.buttonsTwo.textView.setText(spannableString);

        binding.reviews.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.reviews.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                laidOut = true;

                PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) binding.buttons.getRoot().getLayoutParams();
                params.topMargin = (int) (0.36f * binding.reviewsImage.getHeight());
                binding.buttons.getRoot().requestLayout();

                params = (PercentRelativeLayout.LayoutParams) binding.buttonsTwo.getRoot().getLayoutParams();
                params.topMargin = (int) (0.87f * binding.reviewsImage.getHeight());
                binding.buttonsTwo.getRoot().requestLayout();

                if (getUserVisibleHint()) {
                    startAnimations();
                }
            }
        });

        return binding.getRoot();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (!isVisibleToUser) {
            stopAnimations();
        } else {
            if (laidOut && animator == null) {
                startAnimations();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        stopAnimations();
    }

    private void stopAnimations() {
        if (binding == null) {
            return;
        }
        binding.buttons.buttonKeep.setSelected(false);
        binding.buttons.buttonChance.setSelected(false);
        binding.buttons.buttonCut.setSelected(false);

        handler.removeCallbacksAndMessages(null);
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (laidOut && getUserVisibleHint()) {
            startAnimations();
        }
    }

    final Handler handler = new Handler(Looper.getMainLooper());

    private void startAnimations() {
        stopAnimations();

        float current = binding.reviews.getTranslationY();
        float translationY = binding.reviewsImage.getHeight() / 1.99f;

        if (current == -translationY) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(binding.reviews, "translationY", 0)
                    .setDuration(800);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    startAnimations();
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.start();
            return;
        }

        animator = ObjectAnimator.ofFloat(binding.reviews, "translationY", -translationY);
        long duration = current == 0 ? 1700 : (long) (Math.abs(current / translationY) * 1000);
        long delay = current == 0 ? 3200 : 0;
        animator.setDuration(duration);
        animator.setStartDelay(delay);
        animator.start();

        if (current == 0) {
            handler.postDelayed(new FlashButtonRunnable(binding.buttons.buttonChance), 200);
            handler.postDelayed(new FlashButtonRunnable(binding.buttons.buttonKeep), 1200);
            handler.postDelayed(new FlashButtonRunnable(binding.buttons.buttonCut), 2200);
        }
    }

    private class FlashButtonRunnable implements Runnable {
        private final View view;

        private FlashButtonRunnable(View view) {
            this.view = view;
        }

        @Override
        public void run() {
            view.setSelected(true);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.setSelected(false);
                }
            }, 600);
        }
    }
}

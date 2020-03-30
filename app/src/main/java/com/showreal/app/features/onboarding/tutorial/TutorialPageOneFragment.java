package com.showreal.app.features.onboarding.tutorial;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.showreal.app.BaseFragment;
import com.showreal.app.R;
import com.showreal.app.databinding.PageOnboardingOneBinding;
import com.showreal.app.databinding.PageOnboardingReelBinding;

import java.util.Timer;
import java.util.TimerTask;

import uk.co.thedistance.components.lists.BindingViewHolder;

public class TutorialPageOneFragment extends BaseFragment {

    private PageOnboardingOneBinding binding;
    private int scrollOffset = 0;

    @Override
    protected String getScreenName() {
        return null;
    }

    public static TutorialPageOneFragment newInstance() {
        return new TutorialPageOneFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.page_onboarding_one, container, false);

        binding.recycler.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.recycler.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                float reelRatio = 0.676f;
                float reelWidth = binding.recycler.getHeight() * reelRatio;

                scrollOffset = (int) (reelWidth * 1.77f);
                startAnimating();
            }
        });
        binding.recycler.setDrawingListener(binding.magnifier);
        binding.recycler.setAdapter(new ReelAdapter(getActivity()));
        binding.recycler.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return true;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        return binding.getRoot();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            if (timer == null) {
                startAnimating();
            }
        } else {
            stopTimer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        stopTimer();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (scrollOffset != 0) {
            startAnimating();
        }
    }

    private Timer timer;

    private Interpolator scrollInterpolator = new DecelerateInterpolator();

    private void startAnimating() {
        stopTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.recycler.smoothScrollBy(scrollOffset, 0, scrollInterpolator);
                    }
                });
            }
        }, 750, 750);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private static class ReelAdapter extends RecyclerView.Adapter<ReelViewHolder> {
        private static final int[] images = new int[]{
                R.drawable.onboarding_reel_1, R.drawable.onboarding_reel_2, R.drawable.onboarding_reel_3,
                R.drawable.onboarding_reel_4, R.drawable.onboarding_reel_5};

        private static final int[] repeatedImages = new int[]{
                R.drawable.onboarding_reel_6, R.drawable.onboarding_reel_7, R.drawable.onboarding_reel_8,
                R.drawable.onboarding_reel_9, R.drawable.onboarding_reel_10, R.drawable.onboarding_reel_11};

        private final LayoutInflater layoutInflater;

        public ReelAdapter(Context context) {
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public ReelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            PageOnboardingReelBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.page_onboarding_reel, parent, false);
            return new ReelViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(ReelViewHolder holder, int position) {
            holder.binding.image.setImageResource(getImage(position));
        }

        private int getImage(int position) {
            if (position < images.length) {
                return images[position];
            }

            position -= (images.length - 1);
            return repeatedImages[position % repeatedImages.length];
        }

        @Override
        public int getItemCount() {
            return Integer.MAX_VALUE;
        }
    }

    private static class ReelViewHolder extends BindingViewHolder<PageOnboardingReelBinding> {

        public ReelViewHolder(PageOnboardingReelBinding binding) {
            super(binding);
        }
    }
}

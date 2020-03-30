package com.showreal.app.features.onboarding.tutorial;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import com.showreal.app.BaseFragment;
import com.showreal.app.R;
import com.showreal.app.databinding.ItemOnboardingPotentialBinding;
import com.showreal.app.databinding.PageOnboardingThreeBinding;

import java.util.ArrayList;
import java.util.List;

import uk.co.thedistance.components.lists.BindingViewHolder;

public class TutorialPageThreeFragment extends BaseFragment {

    private PageOnboardingThreeBinding binding;
    private float swipeThreshhold;
    private AnimatorSet animatorSet;

    @Override
    protected String getScreenName() {
        return null;
    }

    public static TutorialPageThreeFragment newInstance() {
        return new TutorialPageThreeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.page_onboarding_three, container, false);

        SpannableString spannableString = new SpannableString("2nd\nChance");
        spannableString.setSpan(new SuperscriptSpan(), 1, 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableString.setSpan(new RelativeSizeSpan(0.8f), 1, 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        binding.buttons.textView.setText(spannableString);

        binding.recycler.setLayoutManager(new CustomScrollLinearLayoutManager(getActivity()));
        binding.recycler.setAdapter(new PotentialAdapter(getActivity()));

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
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getUserVisibleHint()) {
            startAnimations();
        }
    }

    private void startAnimations() {
        stopAnimations();

        if (swipeThreshhold == 0) {
            swipeThreshhold = (binding.buttons.getRoot().getWidth() - binding.buttons.buttonCut.getLeft()) + (0.03f * binding.buttons.getRoot().getWidth());
        }

        if (((LinearLayoutManager) binding.recycler.getLayoutManager()).findLastVisibleItemPosition() == binding.recycler.getAdapter().getItemCount() - 1) {
            ValueAnimator returnAnimator = ValueAnimator.ofFloat(0, 1.0f);
            returnAnimator.setDuration(600);
            returnAnimator.setInterpolator(new LinearInterpolator());

            float scrollDistance = getScrollDistance(null, 0);
            returnAnimator.addUpdateListener(new ScrollAnimationLister(scrollDistance));
            returnAnimator.addListener(new Animator.AnimatorListener() {
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
            returnAnimator.start();

            return;
        }

        List<Animator> animatorList = new ArrayList<>();
        RecyclerView.ViewHolder holder = binding.recycler.findViewHolderForAdapterPosition(0);
        if (holder != null) {

            ObjectAnimator outAnimator = ObjectAnimator.ofFloat(holder.itemView, "translationX", -swipeThreshhold);
            outAnimator.setDuration(300);
            outAnimator.setStartDelay(200);
            animatorList.add(outAnimator);

            ObjectAnimator inAnimator = ObjectAnimator.ofFloat(holder.itemView, "translationX", 0);
            inAnimator.setDuration(300);
            inAnimator.setStartDelay(1000);
            animatorList.add(inAnimator);

            ValueAnimator scrollAnimator = ValueAnimator.ofFloat(0, 1.0f);
            scrollAnimator.setDuration(1300);
            scrollAnimator.setInterpolator(new LinearInterpolator());

            float scrollDistance = getScrollDistance(holder, binding.recycler.getAdapter().getItemCount() - 1);
            scrollAnimator.addUpdateListener(new ScrollAnimationLister(scrollDistance));
            scrollAnimator.setStartDelay(400);
            animatorList.add(scrollAnimator);

            animatorSet = new AnimatorSet();
            animatorSet.playSequentially(animatorList);
            animatorSet.start();
        }
    }

    float itemSize = 0;

    private float getScrollDistance(RecyclerView.ViewHolder holder, int position) {
        if (itemSize == 0 && holder != null) {
            itemSize = holder.itemView.getHeight();
        }
        float viewSize = binding.recycler.getHeight();
        float itemBottom = (itemSize * (position + 1)) + binding.recycler.getPaddingBottom();

        return itemBottom - viewSize;
    }

    private class ScrollAnimationLister implements ValueAnimator.AnimatorUpdateListener {

        final float distance;
        int scrolled = 0;

        private ScrollAnimationLister(float distance) {
            this.distance = distance;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float f = animation.getAnimatedFraction();
            float target = f * distance;

            int scrollY = (int) (target - scrolled);
            binding.recycler.scrollBy(0, scrollY);
            scrolled += scrollY;
        }
    }

    private static final class PotentialAdapter extends RecyclerView.Adapter<PotentialViewHolder> {

        private static final String[] people = new String[]{"Paul, 27", "Josh, 29", "Rob, 24", "Scott, 30", "Henry, 31", "Sam, 32", "Ben, 27", "Stephen, 23"};
        private final LayoutInflater layoutInflater;

        public PotentialAdapter(Context context) {
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public PotentialViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ItemOnboardingPotentialBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.item_onboarding_potential, parent, false);
            return new PotentialViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(PotentialViewHolder holder, int position) {
            holder.binding.name.setText(people[position]);
        }

        @Override
        public int getItemCount() {
            return people.length;
        }
    }

    private static final class PotentialViewHolder extends BindingViewHolder<ItemOnboardingPotentialBinding> {

        public PotentialViewHolder(ItemOnboardingPotentialBinding binding) {
            super(binding);
        }
    }
}

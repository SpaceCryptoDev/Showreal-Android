package com.showreal.app.features.onboarding.tutorial;

import android.accounts.AccountManager;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.View;

import com.showreal.app.BaseActivity;
import com.showreal.app.R;
import com.showreal.app.databinding.ActivityOnboardingBinding;
import com.showreal.app.features.onboarding.explore.ExploreActivity;
import com.showreal.app.features.onboarding.signup.AccountActivity;
import com.showreal.app.features.onboarding.signup.SignUpProfileActivity;

import uk.co.thedistance.thedistancecore.Version;

public class OnboardingActivity extends BaseActivity implements OnboardingViewModel.OnboardingView, LocationCountFragment.LocationCountCallback {

    private ActivityOnboardingBinding binding;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (getClass() == OnboardingActivity.class) {
            setTheme(R.style.Theme_ShowReal_LightStatus);
        }
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_onboarding);

        if (Version.isMarshmallow()) {
            binding.getRoot().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        if (getClass() == OnboardingActivity.class) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.count_fragment);
            if (fragment == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.count_fragment, new LocationCountFragment())
                        .commit();
            }
        }
        binding.setViewModel(new OnboardingViewModel(this));
    }

    @Override
    public void openExplore() {
        Intent intent = new Intent(this, ExploreActivity.class);
        startActivity(intent);
    }

    @Override
    public void openSignUp() {
        if (getAppComponent().accountHelper().isLoggedIn()) {
            Intent intent = new Intent(this, SignUpProfileActivity.class);
            startActivity(intent);
            return;
        }

        AccountManager.get(this).addAccount(getString(R.string.account_type), "session",
                null, null, this, null, new Handler());
    }

    @Override
    public void openLogin() {
        if (getAppComponent().accountHelper().isLoggedIn()) {
            Intent intent = new Intent(this, SignUpProfileActivity.class);
            startActivity(intent);
            return;
        }

        Bundle bundle = new Bundle(1);
        bundle.putInt(AccountActivity.EXTRA_SELECTED_PAGE, 1);
        AccountManager.get(this).addAccount(getString(R.string.account_type), "session",
                null, bundle, this, null, new Handler());
    }

    @Override
    public void skipCount() {
        if (isActivityUnavailable()) {
            return;
        }
        binding.pager.setAdapter(new OnboardingAdapter(getSupportFragmentManager(), -1));
        binding.circles.setViewPager(binding.pager);
        removeLogo();
    }

    private void removeLogo() {
        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.count_fragment);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.slide_down)
                    .remove(fragment)
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void showCount(int count) {
        if (isActivityUnavailable()) {
            return;
        }
        binding.pager.setAdapter(new OnboardingAdapter(getSupportFragmentManager(), count));
        binding.circles.setViewPager(binding.pager);
        removeLogo();
    }

    static class OnboardingAdapter extends FragmentPagerAdapter {

        private final int count;
        private final boolean showCount;

        public OnboardingAdapter(FragmentManager fm, int count) {
            super(fm);
            this.count = count;
            showCount = (count >= 200);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0 && showCount) {
                return TutorialLocalCountFragment.newInstance(count);
            }
            if (showCount) {
                position--;
            }
            switch (position) {
//                case 0:
//                    return Tutorial_One_Fragment_New.newInstance();
//                case 1:
//                    return Tutorial_Two_Fragment_New.newInstance();
//                case 2:
//                    return TutorialPageThreeFragment.newInstance();
//                case 3:
//                    return TutorialPageFourFragment.newInstance();
                default:
                    return TutorialPageFragment.newInstance(position);
            }
        }

        @Override
        public int getCount() {
            return Page.values().length + (showCount ? 1 : 0);
        }
    }
}

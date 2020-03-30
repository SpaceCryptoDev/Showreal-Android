package com.showreal.app.features.onboarding.tutorial;

import android.databinding.BaseObservable;
import android.view.View;


public class OnboardingViewModel extends BaseObservable {

    public OnboardingViewModel(OnboardingView view) {
        this.onboardingView = view;
    }

    interface OnboardingView {

        void openExplore();
        void openSignUp();
        void openLogin();
    }

    final OnboardingView onboardingView;

    public void onExplore(View view) {
        onboardingView.openExplore();
    }

    public void onSignUp(View view) {
        onboardingView.openSignUp();
    }

    public void onLogin(View view) {
        onboardingView.openLogin();
    }
}

package com.showreal.app.features.onboarding.tutorial;

import android.view.View;


public class IntroViewModel extends OnboardingViewModel {

    public IntroViewModel(IntroView view) {
        super(view);
    }

    interface IntroView extends OnboardingViewModel.OnboardingView {

        void close();
    }

    public void onClose(View view) {
        ((IntroView) onboardingView).close();
    }
}

package com.showreal.app.features.onboarding.tutorial;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.showreal.app.R;


enum Page {
    One(R.drawable.tutorial_1, R.string.onboarding_title_1, R.string.onboarding_text_1_new),
    Two(R.drawable.tutorial_2, R.string.onboarding_title_2, R.string.onboarding_text_2_new);
//    Three(R.drawable.intro_3, R.string.onboarding_title_3, R.string.onboarding_text_3),
//    Four(R.drawable.intro_4, R.string.onboarding_title_4, R.string.onboarding_text_4);

    @DrawableRes
    final public int imageRes;
    final
    @StringRes
    public int titleRes;
    final
    @StringRes
    public int textRes;

    Page(int imageRes, int titleRes, int textRes) {
        this.imageRes = imageRes;
        this.titleRes = titleRes;
        this.textRes = textRes;
    }
}



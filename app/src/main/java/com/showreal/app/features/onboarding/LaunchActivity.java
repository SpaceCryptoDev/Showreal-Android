package com.showreal.app.features.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.showreal.app.BaseActivity;
import com.showreal.app.MainActivity;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.features.onboarding.signup.SignUpProfileActivity;
import com.showreal.app.features.onboarding.tutorial.OnboardingActivity;
import com.showreal.app.features.real.myreal.MyRealActivity;


public class LaunchActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getAppComponent().settings().update();

        AccountHelper accountHelper = getAppComponent().accountHelper();

        if (accountHelper.isLoggedIn()) {
            if (accountHelper.profileUpdateNeeded()) {
                startActivity(new Intent(this, SignUpProfileActivity.class));
            } else if (accountHelper.showRealNeeded()) {
                Intent intent = new Intent(this, MyRealActivity.class);
                intent.putExtra(MyRealActivity.EXTRA_PROFILE, getAppComponent().staleProfileProvider().readNullable().toBlocking().first());
                intent.putExtra(MyRealActivity.EXTRA_SIGN_UP, true);
                startActivity(intent);

            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
        } else {
            startActivity(new Intent(this, OnboardingActivity.class));
        }

        finish();
    }

    @Override
    protected String getScreenName() {
        return null;
    }
}

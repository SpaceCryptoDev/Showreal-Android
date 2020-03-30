package com.showreal.app.features.onboarding.signup;

import android.databinding.BaseObservable;
import android.view.View;

public class AccountViewModel extends BaseObservable {

    public AccountViewModel(AccountView accountView) {
        this.accountView = accountView;
    }

    interface AccountView {

        void openTerms();
        void openPrivacy();
        void openTips();
    }

    private final AccountView accountView;

    public void onTerms(View view) {
        accountView.openTerms();
    }

    public void onPrivacy(View view) {
        accountView.openPrivacy();
    }

    public void onTips(View view) {
        accountView.openTips();
    }
}

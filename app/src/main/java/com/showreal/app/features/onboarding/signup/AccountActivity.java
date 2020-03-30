package com.showreal.app.features.onboarding.signup;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;

import com.facebook.CallbackManager;
import com.novoda.simplechromecustomtabs.SimpleChromeCustomTabs;
import com.novoda.simplechromecustomtabs.navigation.IntentCustomizer;
import com.novoda.simplechromecustomtabs.navigation.NavigationFallback;
import com.novoda.simplechromecustomtabs.navigation.SimpleChromeCustomTabsIntentBuilder;
import com.showreal.app.BaseActivity;
import com.showreal.app.MainActivity;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.databinding.ActivityAccountBinding;

public class AccountActivity extends BaseActivity implements AccountViewModel.AccountView {

    public static final String EXTRA_SELECTED_PAGE = "selected_page";
    public static final int SignUp = 0;
    public static final int Login = 1;
    public static final String ARG_ACCOUNT_TYPE = "account_type";
    public static final String ARG_AUTH_TYPE = "auth_type";
    public static final String ARG_IS_ADDING_NEW_ACCOUNT = "new_account";
    public static final String EXTRA_EXPIRED = "expired";
    private Uri terms;
    private Uri privacy;
    private CallbackManager facebookCallbackManager;
    private Uri tips;

    @Override
    public void openTerms() {
        navigateTo(terms);
    }

    @Override
    public void openPrivacy() {
        navigateTo(privacy);
    }

    @Override
    public void openTips() {
        navigateTo(tips);
    }

    @IntDef(flag = true, value = {SignUp, Login})
    public @interface Page {
    }

    private ActivityAccountBinding binding;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_account);
        binding.setViewModel(new AccountViewModel(this));

        int page = getIntent().getIntExtra(EXTRA_SELECTED_PAGE, SignUp);

        binding.pager.setAdapter(new SignUpPagerAdapter(this, getSupportFragmentManager()));
        binding.tabs.setupWithViewPager(binding.pager);
        binding.pager.setCurrentItem(page);

        terms = Uri.parse(getString(R.string.url_terms));
        privacy = Uri.parse(getString(R.string.url_privacy));
        tips = Uri.parse(getString(R.string.url_safety));

        SimpleChromeCustomTabs.initialize(this);

        if (getIntent().getBooleanExtra(EXTRA_EXPIRED, false)) {
            RxAlertDialog.with(this)
                    .title(R.string.alert_title_logged_out)
                    .message(R.string.alert_msg_expired)
                    .positiveButton(R.string.button_ok)
                    .subscribe();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SimpleChromeCustomTabs.getInstance().mayLaunch(terms);
        SimpleChromeCustomTabs.getInstance().mayLaunch(privacy);
        SimpleChromeCustomTabs.getInstance().mayLaunch(tips);
        SimpleChromeCustomTabs.getInstance().connectTo(this);
    }

    private void navigateTo(Uri uri) {
        SimpleChromeCustomTabs.getInstance().withFallback(navigationFallback)
                .withIntentCustomizer(intentCustomizer)
                .navigateTo(uri, this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        SimpleChromeCustomTabs.getInstance().disconnectFrom(this);
    }

    public void showLoading(boolean show) {
        if (show) {
            binding.loading.loadingLayout.show();
        } else {
            binding.loading.loadingLayout.hide();
        }
    }

    public void finishLogin(boolean newUser) {
        if (newUser) {
            getAppComponent().accountHelper().setProfileUpdateNeeded(true);
            getAppComponent().accountHelper().setRealNeeded(true);
            Intent intent = new Intent(this, SignUpProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    public CallbackManager getFacebookCallbackManager() {
        if (facebookCallbackManager == null) {
            facebookCallbackManager = CallbackManager.Factory.create();
        }
        return facebookCallbackManager;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        getFacebookCallbackManager().onActivityResult(requestCode, resultCode, data);
    }

    private static class SignUpPagerAdapter extends FragmentPagerAdapter {

        private final Context context;

        public SignUpPagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            this.context = context;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case SignUp:
                    return new SignUpFragment();
                case Login:
                    return new LoginFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case SignUp:
                    return context.getResources().getString(R.string.tab_sign_up);
                case Login:
                    return context.getResources().getString(R.string.tab_login);
            }

            return super.getPageTitle(position);
        }
    }

    private final IntentCustomizer intentCustomizer = new IntentCustomizer() {
        @Override
        public SimpleChromeCustomTabsIntentBuilder onCustomiseIntent(SimpleChromeCustomTabsIntentBuilder intentBuilder) {
            return intentBuilder.withToolbarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                    .showingTitle();
        }
    };

    private final NavigationFallback navigationFallback = new NavigationFallback() {
        @Override
        public void onFallbackNavigateTo(Uri url) {

            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setData(url)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    };
}

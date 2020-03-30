package com.showreal.app.features.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.facebook.CallbackManager;
import com.facebook.login.LoginResult;
import com.novoda.simplechromecustomtabs.SimpleChromeCustomTabs;
import com.novoda.simplechromecustomtabs.navigation.IntentCustomizer;
import com.novoda.simplechromecustomtabs.navigation.NavigationFallback;
import com.novoda.simplechromecustomtabs.navigation.SimpleChromeCustomTabsIntentBuilder;
import com.showreal.app.BaseActivity;
import com.showreal.app.ErrorHandler;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.MultiUploadPresenterView;
import com.showreal.app.data.MultiUploadingPresenter;
import com.showreal.app.data.account.FacebookHelper;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Settings;
import com.showreal.app.databinding.ActivitySettingsBinding;
import com.showreal.app.features.onboarding.signup.SignUpProfileUploader;
import com.showreal.app.features.onboarding.tutorial.IntroActivity;
import com.showreal.app.features.settings.delete.DeleteAccountActivity;
import com.showreal.app.features.settings.password.ChangePasswordActivity;
import com.showreal.app.injection.ApplicationComponent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Subscriber;
import rx.functions.Action1;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenter;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenterView;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;
import uk.co.thedistance.components.views.DelayFrameLayout;

public class SettingsActivity extends BaseActivity {

    private ActivitySettingsBinding binding;
    private PresenterLoaderHelper<MultiUploadingPresenter> uploadingLoaderHelper;
    private PresenterLoaderHelper<SettingsPresenter> loaderHelper;
    private SettingsPresenter presenter;
    private MultiUploadingPresenter uploader;
    private CallbackManager facebookCallbackManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);
        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content, new SettingsFragment())
                    .commit();
        }
        uploadingLoaderHelper = new PresenterLoaderHelper<>(this, new SettingsUploaderFactory());
        loaderHelper = new PresenterLoaderHelper<>(this, new SettingsPresenterFactory());

        getSupportLoaderManager().initLoader(0, null, loaderHelper);
        getSupportLoaderManager().initLoader(1, null, uploadingLoaderHelper);
    }

    @Override
    protected void onResume() {
        super.onResume();

        presenter = loaderHelper.getPresenter();
        uploader = uploadingLoaderHelper.getPresenter();
    }

    public SettingsPresenter getPresenter() {
        return presenter;
    }

    public MultiUploadingPresenter getUploader() {
        return uploader;
    }

    private class SettingsPresenter extends ContentLoadingPresenter<Settings, SettingsDataSource, ContentLoadingPresenterView<Settings>> {

        public SettingsPresenter(SettingsDataSource dataSource) {
            super(dataSource);
        }
    }

    private class SettingsUploaderFactory implements PresenterFactory<MultiUploadingPresenter> {

        @Override
        public MultiUploadingPresenter create() {
            Map<Class, DataUploader> dataUploaders = new HashMap<>();
            dataUploaders.put(Settings.class, new SettingsUploader(SettingsActivity.this));
            dataUploaders.put(FacebookConnectUploader.FacebookConnect.class, new FacebookConnectUploader(SettingsActivity.this, getAppComponent().api()));
            return new MultiUploadingPresenter(dataUploaders);
        }
    }

    private class SettingsPresenterFactory implements PresenterFactory<SettingsPresenter> {

        @Override
        public SettingsPresenter create() {
            return new SettingsPresenter(new SettingsDataSource(SettingsActivity.this));
        }
    }

    @Override
    protected String getScreenName() {
        return null;
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

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, MultiUploadPresenterView, ContentLoadingPresenterView<Settings> {

        private List<String> pref_keys;
        private List<String> switch_keys;
        private Uri terms;
        private Uri privacy;

        private MultiUploadingPresenter uploader;
        private Settings settings;
        private SettingsPresenter presenter;
        private Preference facebookPref;
        private Preference friendsPref;
        private Preference passwordPref;
        private Preference passwordDividerPref;
        private Uri safety;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            Resources resources = getResources();
            String[] keys = new String[]{
                    resources.getString(R.string.pref_key_password),
                    resources.getString(R.string.pref_key_facebook),
                    resources.getString(R.string.pref_key_introduction),
                    resources.getString(R.string.pref_key_report),
                    resources.getString(R.string.pref_key_terms),
                    resources.getString(R.string.pref_key_privacy),
                    resources.getString(R.string.pref_key_safety),
                    resources.getString(R.string.pref_key_delete),
                    resources.getString(R.string.pref_key_logout)
            };
            pref_keys = Arrays.asList(keys);

            keys = new String[]{
                    resources.getString(R.string.pref_key_private),
                    resources.getString(R.string.pref_key_facebook_friends),
                    resources.getString(R.string.pref_key_height),
                    resources.getString(R.string.pref_key_radius),
                    resources.getString(R.string.pref_key_notifications_nudge),
                    resources.getString(R.string.pref_key_notifications_match),
                    resources.getString(R.string.pref_key_notifications_user_update),
                    resources.getString(R.string.pref_key_notifications_message)
            };
            switch_keys = Arrays.asList(keys);

            setupClicks();

            terms = Uri.parse(getString(R.string.url_terms));
            privacy = Uri.parse(getString(R.string.url_privacy));
            safety = Uri.parse(getString(R.string.url_safety));

            SimpleChromeCustomTabs.initialize(getActivity());

            TheDistanceApplication.getApplicationComponent(getActivity()).accountHelper()
                    .getProfile()
                    .subscribe(new Action1<Profile>() {
                        @Override
                        public void call(Profile profile) {
                            facebookPref = findPreference(getString(R.string.pref_key_facebook));
                            friendsPref = findPreference(getString(R.string.pref_key_facebook_friends));
                            passwordPref = findPreference(getString(R.string.pref_key_password));
                            passwordDividerPref = findPreference(getString(R.string.pref_key_password_divider));

                            if (TextUtils.isEmpty(profile.facebookId)) {
                                if (friendsPref != null) {
                                    getPreferenceScreen().removePreference(friendsPref);
                                }
                            } else {
                                if (facebookPref != null) {
                                    getPreferenceScreen().removePreference(facebookPref);
                                }
                                if (passwordPref != null) {
                                    getPreferenceScreen().removePreference(passwordPref);
                                }
                                if (passwordDividerPref != null) {
                                    getPreferenceScreen().removePreference(passwordDividerPref);
                                }
                            }

                            if (settings == null) {
                                TheDistanceApplication.getApplicationComponent(getActivity())
                                        .accountHelper()
                                        .getSettings()
                                        .subscribe(new Action1<Settings>() {
                                            @Override
                                            public void call(Settings settings) {
                                                if (settings == null) {
                                                    return;
                                                }
                                                updateSettings(settings);
                                            }
                                        });
                            }
                        }
                    });


        }

        @Override
        public void onResume() {
            super.onResume();

            uploader = ((SettingsActivity) getActivity()).getUploader();
            presenter = ((SettingsActivity) getActivity()).getPresenter();
            uploader.onViewAttached(this);
            presenter.onViewAttached(this);

            SimpleChromeCustomTabs.getInstance().mayLaunch(terms);
            SimpleChromeCustomTabs.getInstance().mayLaunch(privacy);
            SimpleChromeCustomTabs.getInstance().connectTo(getActivity());
        }

        private void navigateTo(Uri uri) {
            SimpleChromeCustomTabs.getInstance().withFallback(navigationFallback)
                    .withIntentCustomizer(intentCustomizer)
                    .navigateTo(uri, getActivity());
        }

        @Override
        public void onPause() {
            super.onPause();

            SimpleChromeCustomTabs.getInstance().disconnectFrom(getActivity());
        }

        private void setupClicks() {
            for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
                Preference preference = getPreferenceScreen().getPreference(i);
                if (preference.getClass() == BindingPreference.class) {
                    preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            onClick(preference);
                            return true;
                        }
                    });
                }
            }
        }

        private void onClick(Preference preference) {
            int pos = pref_keys.indexOf(preference.getKey());
            switch (pos) {
                case 0:
                    changePassword();
                    break;
                case 1:
                    connectFacebook();
                    break;
                case 2:
                    openIntro();
                    break;
                case 3:
                    report();
                    break;
                case 4:
                    openTerms();
                    break;
                case 5:
                    openPrivacyPolicy();
                    break;
                case 6:
                    openSafetyTips();
                    break;
                case 7:
                    deleteAccount();
                    break;
                case 8:
                    logout();
                    break;
            }
        }

        private void updateSetting(String key, SharedPreferences sharedPreferences) {
            if (settings == null) {
                return;
            }
            int pos = switch_keys.indexOf(key);
            switch (pos) {
                case 0:
                    boolean isPrivate = sharedPreferences.getBoolean(key, settings.isPrivate);
                    if (isPrivate != settings.isPrivate) {
                        settings.isPrivate = isPrivate;
                        uploader.uploadContent(settings);
                    }
                    break;
                case 1:
                    boolean blockFriends = sharedPreferences.getBoolean(key, settings.blockFriends);
                    if (blockFriends != settings.blockFriends) {
                        settings.blockFriends = blockFriends;
                        uploader.uploadContent(settings);
                    }
                    break;
                case 2:
                    int value = sharedPreferences.getInt(key, settings.height_unit);
                    if (settings.height_unit != value) {
                        settings.height_unit = value;
                        uploader.uploadContent(settings);
                    }
                    break;
                case 3:
                    int value1 = sharedPreferences.getInt(key, settings.radius_unit);
                    if (settings.radius_unit != value1) {
                        settings.radius_unit = value1;
                        uploader.uploadContent(settings);
                    }
                    break;
                case 4:
                    boolean nudge = sharedPreferences.getBoolean(key, settings.nudgeNotification);
                    if (nudge != settings.nudgeNotification) {
                        settings.nudgeNotification = nudge;
                        uploader.uploadContent(settings);
                    }
                    break;
                case 5:
                    boolean match = sharedPreferences.getBoolean(key, settings.matchNotification);
                    if (match != settings.matchNotification) {
                        settings.matchNotification = match;
                        uploader.uploadContent(settings);
                    }
                    break;
                case 6:
                    boolean user = sharedPreferences.getBoolean(key, settings.userUpdateMatchNotification);
                    if (user != settings.userUpdateMatchNotification) {
                        settings.userUpdateMatchNotification = user;
                        uploader.uploadContent(settings);
                    }
                    break;
                case 7:
                    boolean message = sharedPreferences.getBoolean(key, settings.newMessageMatchNotification);
                    if (message != settings.newMessageMatchNotification) {
                        settings.newMessageMatchNotification = message;
                        uploader.uploadContent(settings);
                    }
                    break;
            }

        }

        private void changePassword() {
            startActivity(new Intent(getActivity(), ChangePasswordActivity.class));
        }

        private void connectFacebook() {
            FacebookHelper.login(getActivity(), ((SettingsActivity) getActivity()).getFacebookCallbackManager())
                    .subscribe(new Subscriber<LoginResult>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(LoginResult loginResult) {
                            updateProfile(loginResult);
                        }
                    });
        }

        private void updateProfile(final LoginResult loginResult) {
            final ApplicationComponent component = TheDistanceApplication.getApplicationComponent(getActivity());
            component.accountHelper()
                    .getProfile()
                    .subscribe(new Action1<Profile>() {
                        @Override
                        public void call(Profile profile) {
                            FacebookConnectUploader.FacebookConnect connect = new FacebookConnectUploader.FacebookConnect(profile, loginResult);

                            uploader.uploadContent(connect);
                        }
                    });
        }

        private void openIntro() {
            startActivity(new Intent(getActivity(), IntroActivity.class));
        }

        private void report() {
            startActivity(new Intent(getActivity(), ReportActivity.class));
        }

        private void openTerms() {
            navigateTo(terms);
        }

        private void openPrivacyPolicy() {
            navigateTo(privacy);
        }

        private void openSafetyTips() {
            navigateTo(safety);
        }

        private void deleteAccount() {
            startActivity(new Intent(getActivity(), DeleteAccountActivity.class));
        }

        @Override
        public void onStart() {
            super.onStart();

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ListView lv = (ListView) view.findViewById(android.R.id.list);
            lv.setDivider(null);
        }

        @Override
        public void onStop() {
            super.onStop();

            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void addPreferencesFromResource(int preferencesResId) {
            super.addPreferencesFromResource(preferencesResId);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(getResources().getString(R.string.pref_key_analytics_enabled))) {
                ((BaseActivity) getActivity()).getAppComponent().analytics().setEnabled(sharedPreferences.getBoolean(key, true));
            }

            updateSetting(key, sharedPreferences);
        }

        private void logout() {
            RxAlertDialog.with(getActivity())
                    .title(R.string.alert_title_logout)
                    .message(R.string.alert_msg_logout)
                    .positiveButton(R.string.action_logout)
                    .negativeButton(R.string.button_cancel)
                    .subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer integer) {
                            if (integer == RxAlertDialog.ButtonPositive) {
                                showLoading(true, false);
                                ((BaseActivity) getActivity()).getAppComponent().accountHelper().logout(getActivity(), false);
                            }
                        }
                    });
        }

        @Override
        public void showUploading(boolean show) {
            if (show) {
                ((DelayFrameLayout) getActivity().findViewById(R.id.loading_layout)).show();
            } else {
                ((DelayFrameLayout) getActivity().findViewById(R.id.loading_layout)).hide();
            }
        }

        @Override
        public void uploadComplete(Object response) {
            if (response instanceof Settings) {
                updateSettings((Settings) response);
            } else {
                getPreferenceScreen().removePreference(facebookPref);
                getPreferenceScreen().removePreference(passwordDividerPref);
                getPreferenceScreen().removePreference(passwordPref);
                getPreferenceScreen().addPreference(friendsPref);

                RxAlertDialog.with(this)
                        .title(R.string.alert_title_facebook_connected)
                        .message(R.string.alert_msg_facebook_connected)
                        .positiveButton(R.string.button_ok)
                        .subscribe();
            }
        }

        private void updateSettings(final Settings settings) {
            this.settings = settings;
            ((SegmentPreference) findPreference(getString(R.string.pref_key_height))).setSelection(settings.height_unit);
            ((SegmentPreference) findPreference(getString(R.string.pref_key_radius))).setSelection(settings.radius_unit);
            ((SwitchPreference) findPreference(getString(R.string.pref_key_private))).setChecked(settings.isPrivate);
            ((SwitchPreference) findPreference(getString(R.string.pref_key_notifications_nudge))).setChecked(settings.nudgeNotification);
            ((SwitchPreference) findPreference(getString(R.string.pref_key_notifications_match))).setChecked(settings.matchNotification);
            ((SwitchPreference) findPreference(getString(R.string.pref_key_notifications_user_update))).setChecked(settings.userUpdateMatchNotification);
            ((SwitchPreference) findPreference(getString(R.string.pref_key_notifications_message))).setChecked(settings.newMessageMatchNotification);


            Preference friendsPref = findPreference(getString(R.string.pref_key_facebook_friends));

            if (friendsPref != null) {
                ((SwitchPreference) friendsPref).setChecked(settings.blockFriends);
            }

        }

        @Override
        public void showLoading(boolean show, boolean isRefresh) {
            showUploading(show);
        }

        @Override
        public void showContent(Settings content, boolean refresh) {
            updateSettings(content);
        }

        @Override
        public void showError(Throwable throwable, String error) {
            TheDistanceApplication.getApplicationComponent(getActivity())
                    .accountHelper()
                    .getSettings()
                    .subscribe(new Action1<Settings>() {
                        @Override
                        public void call(Settings settings) {
                            if (settings == null) {
                                return;
                            }
                            updateSettings(settings);
                        }
                    });
        }

        @Override
        public void showError(Throwable throwable, String error, Class cls) {
            if (ErrorHandler.handle(throwable, getActivity())) {
                return;
            }

            if (throwable instanceof FacebookConnectUploader.FacebookConnectException) {
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_facebook_connect_error)
                        .message(error)
                        .positiveButton(R.string.button_ok)
                        .subscribe();
                return;
            }

            if (cls == Settings.class) {
                showError(throwable, error);
                return;
            }
        }

        private final IntentCustomizer intentCustomizer = new IntentCustomizer() {
            @Override
            public SimpleChromeCustomTabsIntentBuilder onCustomiseIntent(SimpleChromeCustomTabsIntentBuilder intentBuilder) {
                return intentBuilder.withToolbarColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorPrimary))
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
}

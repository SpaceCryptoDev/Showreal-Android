package com.showreal.app;

import android.graphics.Typeface;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;

import com.showreal.app.data.model.Profile;
import com.showreal.app.injection.ApplicationComponent;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.analytics.model.AnalyticEvent;
import uk.co.thedistance.components.analytics.model.ScreenEvent;
import uk.co.thedistance.thedistancetheming.fonts.FontCache;
import uk.co.thedistance.thedistancetheming.fonts.FontSpan;

/**
 * Extend this class to enable thedistancekit functionality.
 * <p/>
 * <ul>
 * <li>set a screen name to automatically send Google Analytics screen views</li>
 * <li>send an analytic event using {@link #sendEvent(AnalyticEvent)}</li>
 * </ul>
 */
public abstract class BaseFragment extends Fragment {

    protected abstract String getScreenName();

    boolean trackingEnabled = true;

    @Override
    public void onResume() {
        super.onResume();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isVisible()) {
                    sendScreen();
                }
            }
        }, 200);
    }

    protected void sendScreen() {
        if (!trackingEnabled || getScreenName() == null) {
            return;
        }
        // Send screen if fragment still visible (to avoid sending home every time backstack cleared)
        getAppComponent().analytics().send(new ScreenEvent(getScreenName()));
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        Typeface typeface = FontCache.getInstance(getActivity()).get(getString(R.string.FontHeadline));
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.isVisible() && item.getIcon() == null) {
                FontSpan span = new FontSpan(typeface);
                SpannableString title = new SpannableString(item.getTitle());
                title.setSpan(span, 0, title.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                item.setTitle(title);
            }
        }
    }

    protected void sendEvent(AnalyticEvent event) {
        getAppComponent().analytics().send(event);
    }

    protected ApplicationComponent getAppComponent() {
        return TheDistanceApplication.getApplicationComponent(getActivity());
    }

    protected Observable<Boolean> showShowRealMessage(@StringRes final int titleRes) {
        return getAppComponent().accountHelper().getProfile()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<Profile, Boolean>() {
                    @Override
                    public Boolean call(Profile profile) {
                        if (profile == null || !profile.hasShowReal(getActivity())) {

                            RxAlertDialog.with(BaseFragment.this)
                                    .title(titleRes)
                                    .message(R.string.alert_msg_no_showreal)
                                    .positiveButton(R.string.button_ok)
                                    .create()
                                    .subscribe();

                            return true;
                        }

                        return false;
                    }
                });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        getActivity().supportInvalidateOptionsMenu();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isVisible()) {
                    sendScreen();
                }
            }
        }, 200);
    }

    public boolean onBackPressed() {
        return false;
    }
}

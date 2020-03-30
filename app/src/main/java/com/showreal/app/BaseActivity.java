package com.showreal.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;

import com.showreal.app.data.model.Profile;
import com.showreal.app.features.conversations.ConversationActivity;
import com.showreal.app.features.notifications.SRFirebaseMessagingService;
import com.showreal.app.injection.ApplicationComponent;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.analytics.model.AnalyticEvent;
import uk.co.thedistance.components.analytics.model.ScreenEvent;
import uk.co.thedistance.components.base.TheDistanceActivity;
import uk.co.thedistance.thedistancetheming.fonts.FontCache;
import uk.co.thedistance.thedistancetheming.fonts.FontSpan;

/**
 * Extend this class to enable thedistancekit functionality.
 * <p/>
 * <ul>
 * <li>set a screen name to automatically send Google Analytics screen views</li>
 * </ul>
 */
abstract public class BaseActivity extends TheDistanceActivity {

    boolean trackingEnabled = true;

    private static final IntentFilter TOAST_FILTER = new IntentFilter(SRFirebaseMessagingService.ACTION_NOTIFICATION_TOAST);
    private final BroadcastReceiver toastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(SRFirebaseMessagingService.EXTRA_FROM_SENDBIRD, false) && BaseActivity.this instanceof ConversationActivity) {
                return;
            }
            handleToast(intent);
        }
    };

    public void handleToast(Intent intent) {
        String message = intent.getStringExtra(SRFirebaseMessagingService.EXTRA_MESSAGE);
        SRToast.showToast(BaseActivity.this, message);
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                sendScreen();
            }
        }, 200);

        registerReceiver(toastReceiver, TOAST_FILTER);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(toastReceiver);
    }

    protected abstract String getScreenName();

    protected void sendScreen() {
        if (!trackingEnabled || getScreenName() == null) {
            return;
        }
        getAppComponent().analytics().send(new ScreenEvent(getScreenName()));
    }

    protected void sendEvent(AnalyticEvent event) {
        getAppComponent().analytics().send(event);
    }

    public ApplicationComponent getAppComponent() {
        return TheDistanceApplication.getApplicationComponent(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean b = super.onPrepareOptionsMenu(menu);

        Typeface typeface = FontCache.getInstance(this).get(getString(R.string.FontHeadline));
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.isVisible() && item.getIcon() == null) {
                FontSpan span = new FontSpan(typeface);
                SpannableString title = new SpannableString(item.getTitle());
                title.setSpan(span, 0, title.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                item.setTitle(title);
            }
        }

        return b;
    }

    protected void showImplementationMessage(@StringRes int titleRes) {
        RxAlertDialog.with(this)
                .title(titleRes)
                .message(R.string.alert_msg_implementation)
                .positiveButton(R.string.button_ok)
                .create()
                .subscribe();
    }

    protected Observable<Boolean> showShowRealMessage(@StringRes final int titleRes) {
        return getAppComponent().accountHelper().getProfile()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<Profile, Boolean>() {
                    @Override
                    public Boolean call(Profile profile) {
                        if (profile != null && profile.hasShowReal(BaseActivity.this)) {
                            return false;
                        }

                        RxAlertDialog.with(BaseActivity.this)
                                .title(titleRes)
                                .message(R.string.alert_msg_no_showreal)
                                .positiveButton(R.string.button_ok)
                                .create()
                                .subscribe();

                        return true;
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);

                if (upIntent == null) {
                    finish();
                    return true;
                }
                if (NavUtils.shouldUpRecreateTask(this, upIntent) || isTaskRoot()) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                            // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

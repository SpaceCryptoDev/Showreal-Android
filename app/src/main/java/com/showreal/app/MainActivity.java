package com.showreal.app;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarBadge;
import com.roughike.bottombar.OnMenuTabClickListener;
import com.showreal.app.data.SendBirdHelper;
import com.showreal.app.data.model.Notification;
import com.showreal.app.data.model.Profile;
import com.showreal.app.databinding.ActivityMainBinding;
import com.showreal.app.features.conversations.matches.MatchesFragment;
import com.showreal.app.features.notifications.NotificationsFragment;
import com.showreal.app.features.notifications.SRFirebaseMessagingService;
import com.showreal.app.features.onboarding.tutorial.OnboardingActivity;
import com.showreal.app.features.potential.PotentialFragment;
import com.showreal.app.features.profile.ProfileFragment;
import com.showreal.app.features.profile.other.OtherProfileActivity;
import com.showreal.app.features.real.myreal.MyRealActivity;
import com.showreal.app.features.reviews.ReviewsFragment;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends BaseActivity implements OnMenuTabClickListener {

    public static final String EXTRA_TAB = "tab";
    public static final String EXTRA_NOTIFICATION_TYPE = "notification_type";
    public static final String EXTRA_NOTIFICATION_MATCH_ID = "notification_match_id";
    private ActivityMainBinding binding;
    private BottomBar bottomBar;
    private int currentId = R.id.item_review;
    private int tabBackgroundRes;
    private BottomBarBadge chatBadge;
    private List<Integer> tabs = new ArrayList<>();

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_ShowReal);
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        NotificationManagerCompat.from(this).cancelAll();

        bottomBar = BottomBar.attach(binding.fragment, savedInstanceState);
        bottomBar.noTabletGoodness();
        bottomBar.noNavBarGoodness();
        bottomBar.useFixedMode();
        bottomBar.setTextAppearance(R.style.TextAppearance_ShowReal_Body1);
        bottomBar.setTypeFace("fonts/Pacifico.ttf");
        bottomBar.setItems(R.menu.bottombar_menu);
        bottomBar.getBar().setBackgroundResource(R.color.tabs_color);
        int background = ContextCompat.getColor(this, R.color.white87);
        bottomBar.setActiveTabColor(background);
        bottomBar.setOnMenuTabClickListener(this);
        bottomBar.noTopOffset();

        chatBadge = bottomBar.makeBadgeForTabAt(2, ContextCompat.getColor(this, R.color.red), 0);
        chatBadge.hide();

        int[] attrs = new int[]{R.attr.selectableItemBackgroundBorderless};
        TypedArray typedArray = obtainStyledAttributes(attrs);
        tabBackgroundRes = typedArray.getResourceId(0, 0);
        typedArray.recycle();

        if (savedInstanceState != null) {
            currentId = savedInstanceState.getInt("current");
        }

        customiseTabs();

        updateMessageCount();

        registerReceiver(NOTIFICATION_RECEIVER, NOTIFICATION_INTENT_FILTER);
        registerReceiver(MESSAGE_RECEIVER, MESSAGE_INTENT_FILTER);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(EXTRA_TAB)) {
            int tab = intent.getIntExtra(EXTRA_TAB, 0);
            bottomBar.selectTabAtPosition(tab, false);
        }

        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (!intent.hasExtra(EXTRA_NOTIFICATION_TYPE)) {
            return;
        }

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancelAll();

        int type = intent.getIntExtra(EXTRA_NOTIFICATION_TYPE, -1);

        switch (type) {
            case Notification.Matched:
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setSelectedTab(R.id.item_messages);
                    }
                }, 400);
                break;
            case Notification.MatchRealUpdated:
            case Notification.SecondChanceRealUpdated:
                if (intent.hasExtra(EXTRA_NOTIFICATION_MATCH_ID)) {
                    int matchId = intent.getIntExtra(EXTRA_NOTIFICATION_MATCH_ID, -1);
                    openProfile(matchId);
                }
                break;
            case Notification.NewQuestion:
                Intent realIntent = new Intent(this, MyRealActivity.class);
                startActivity(realIntent);
                break;
        }
    }

    private void openProfile(int profileId) {
        if (profileId == -1) {
            return;
        }
        binding.loading.loadingLayout.show();
        getAppComponent().api().getProfile(profileId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Profile>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        binding.loading.loadingLayout.hide();
                    }

                    @Override
                    public void onNext(Profile profile) {
                        binding.loading.loadingLayout.hide();
                        Intent intent = new Intent(MainActivity.this, OtherProfileActivity.class);
                        intent.putExtra(OtherProfileActivity.EXTRA_PROFILE, profile);
                        intent.putExtra(OtherProfileActivity.EXTRA_SOURCE, OtherProfileActivity.SOURCE_NOTIFICATIONS);
                        startActivity(intent);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(NOTIFICATION_RECEIVER);
        unregisterReceiver(MESSAGE_RECEIVER);
    }

    private void updateMessageCount() {
        if (getAppComponent().accountHelper().isLoggedIn()) {
            setChatBadge(getAppComponent().sendbird().getTotalUnreadCount());
        }
    }

    private final IntentFilter NOTIFICATION_INTENT_FILTER = new IntentFilter(SRFirebaseMessagingService.ACTION_SENDBIRD_NOTIFICATION);
    private final BroadcastReceiver NOTIFICATION_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMessageCount();
        }
    };
    private final IntentFilter MESSAGE_INTENT_FILTER = new IntentFilter(SendBirdHelper.ACTION_SENDBIRD_MESSAGE);
    private final BroadcastReceiver MESSAGE_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            incrementMessageCount();
        }
    };

    private void incrementMessageCount() {
        if (currentId == R.id.item_messages) {
            return;
        }
        chatBadge.setCount(chatBadge.getCount() + 1);
        chatBadge.show();
    }

    private void setChatBadge(Integer count) {
        if (count != null && count > 0) {
            chatBadge.setCount(count);
            chatBadge.show();
        }
    }

    @Override
    public void handleToast(Intent intent) {
        super.handleToast(intent);

        if (currentId == R.id.item_messages) {
            return;
        }

        int type = intent.getIntExtra(SRFirebaseMessagingService.EXTRA_TYPE, -1);
        if (type == Notification.Matched) {
            chatBadge.setText("");
            chatBadge.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!getAppComponent().accountHelper().isLoggedIn()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        }
    }

    private void showFragment(int fragmentId) {
        if (isActivityUnavailable() || isPaused()) {
            return;
        }

        Fragment fragment;

        String tag = String.valueOf(fragmentId);
        fragment = getSupportFragmentManager().findFragmentByTag(tag);

        boolean fragmentExists = false;

        if (fragment == null) {
            switch (fragmentId) {
                case R.id.item_review:
                    fragment = new ReviewsFragment();
                    break;
                case R.id.item_profile:
                    fragment = new ProfileFragment();
                    break;
                case R.id.item_potential:
                    fragment = new PotentialFragment();
                    break;
                case R.id.item_notifications:
                    fragment = new NotificationsFragment();
                    break;
                case R.id.item_messages:
                    fragment = new MatchesFragment();
                    break;
            }
        } else {
            fragmentExists = true;
        }

        selectTab(fragmentId);

        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
            if (fragmentList != null) {
                for (Fragment existing : fragmentList) {
                    if (!fragment.equals(existing)) {
                        transaction.hide(existing);
                    }
                }
            }

            if (fragmentExists) {
                transaction.show(fragment);
            } else {
                transaction.add(R.id.fragment, fragment, String.valueOf(fragmentId));
            }

            if (isActivityUnavailable()) {
                return;
            }

            transaction.commit();
        }
    }

    public void setSelectedTab(int tab_id) {
        int tabPosition = 0;
        switch (tab_id) {
            case R.id.item_messages:
                tabPosition = 2;
                break;
            case R.id.item_potential:
                tabPosition = 1;
                break;
            case R.id.item_notifications:
                tabPosition = 3;
                break;
            case R.id.item_profile:
                tabPosition = 4;
                break;
        }
        if (tabPosition != bottomBar.getCurrentTabPosition()) {
            bottomBar.selectTabAtPosition(tabPosition, true);
        }
    }

    private void customiseTabs() {
        int[] ids = new int[]{R.id.item_review, R.id.item_profile, R.id.item_potential, R.id.item_messages, R.id.item_notifications};
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        for (int id : ids) {
            FrameLayout tab = (FrameLayout) bottomBar.getBar().findViewById(id);

            View icon = tab.getChildAt(0);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) icon.getLayoutParams();
            params.topMargin = margin;
            icon.setLayoutParams(params);


            tab.getChildAt(1).setVisibility(View.GONE);
        }
    }

    private void selectTab(int tab_id) {
        if (tab_id == R.id.item_messages) {
            chatBadge.setCount(0);
        }
        View old = bottomBar.getBar().findViewById(currentId);
        if (old != null) {
            old.setBackgroundResource(tabBackgroundRes);
        }

        View view = bottomBar.getBar().findViewById(tab_id);
        if (view != null) {
            currentId = tab_id;
            view.setBackgroundResource(R.drawable.gradient_red_bottom);
        }
    }

    @Override
    public void onMenuTabSelected(@IdRes int menuItemId) {
        showFragment(menuItemId);
    }

    @Override
    public void onMenuTabReSelected(@IdRes int menuItemId) {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        bottomBar.onSaveInstanceState(outState);
        outState.putInt("current", currentId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onBackPressed() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment.isVisible() && fragment instanceof BaseFragment && ((BaseFragment) fragment).onBackPressed()) {
                    return;
                }
            }
        }

        if (bottomBar.getCurrentTabPosition() != 0) {
            bottomBar.selectTabAtPosition(0, true);
            return;
        }
        super.onBackPressed();
    }
}

package com.showreal.app.features.notifications;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.novoda.simplechromecustomtabs.SimpleChromeCustomTabs;
import com.novoda.simplechromecustomtabs.navigation.IntentCustomizer;
import com.novoda.simplechromecustomtabs.navigation.NavigationFallback;
import com.novoda.simplechromecustomtabs.navigation.SimpleChromeCustomTabsIntentBuilder;
import com.showreal.app.BaseFragment;
import com.showreal.app.FabricHelper;
import com.showreal.app.MainActivity;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.SRSnackbar;
import com.showreal.app.analytics.AppboyEvent;
import com.showreal.app.analytics.Screens;
import com.showreal.app.data.model.Notification;
import com.showreal.app.data.model.Profile;
import com.showreal.app.databinding.FragmentNotificationsBinding;
import com.showreal.app.databinding.ItemNotificationBinding;
import com.showreal.app.features.profile.other.OtherProfileActivity;
import com.showreal.app.features.real.myreal.MyRealActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.lists.AbsSortedListItemAdapterDelegate;
import uk.co.thedistance.components.lists.BindingViewHolder;
import uk.co.thedistance.components.lists.SortedListDelegationAdapter;
import uk.co.thedistance.components.lists.Sorter;
import uk.co.thedistance.components.lists.interfaces.ListPresenterView;
import uk.co.thedistance.components.lists.interfaces.Sortable;
import uk.co.thedistance.components.lists.model.ListContent;
import uk.co.thedistance.components.lists.presenter.ListPresenter;


public class NotificationsFragment extends BaseFragment implements NotificationViewModel.NotificationView, ListPresenterView<Notification> {

    private NotificationsPresenter presenter;
    private NotificationAdapter adapter;

    @Override
    protected String getScreenName() {
        return Screens.NOTIFICATIONS;
    }

    private FragmentNotificationsBinding binding;
    private PresenterLoaderHelper<NotificationsPresenter> loaderHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SimpleChromeCustomTabs.initialize(getActivity());
        loaderHelper = new PresenterLoaderHelper<>(getActivity(), new NotificationsPresenterFactory(getActivity()));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_notifications, container, false);
        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbarLayout.toolbar);
        getActivity().setTitle(R.string.title_notifications);

        setHasOptionsMenu(true);

        int colorOne = ContextCompat.getColor(getActivity(), R.color.colorAccent);
        int colorTwo = ContextCompat.getColor(getActivity(), R.color.colorPrimary);
        binding.refresh.setColorSchemeColors(colorOne, colorTwo);

        binding.recycler.setAdapter(adapter = new NotificationAdapter(getActivity(), this));

        getLoaderManager().initLoader(0, null, loaderHelper);

        ItemTouchHelper.SimpleCallback touchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                removeItem((NotificationAdapter.NotificationViewHolder) viewHolder);
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                NotificationAdapter.NotificationViewHolder holder = (NotificationAdapter.NotificationViewHolder) viewHolder;
                if (holder.isSwipeable()) {
                    return super.getMovementFlags(recyclerView, viewHolder);
                }
                return 0;
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                getDefaultUIUtil().onDraw(c, recyclerView, ((NotificationAdapter.NotificationViewHolder) viewHolder).binding.content, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                getDefaultUIUtil().onDrawOver(c, recyclerView, ((NotificationAdapter.NotificationViewHolder) viewHolder).binding.content, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                getDefaultUIUtil().clearView(((NotificationAdapter.NotificationViewHolder) viewHolder).binding.content);
            }
        };

        new ItemTouchHelper(touchCallback).attachToRecyclerView(binding.recycler);

        return binding.getRoot();
    }

    private void removeItem(NotificationAdapter.NotificationViewHolder holder) {
        Notification notification = holder.binding.getViewModel().getNotification();
        adapter.removeItem(notification);
        Snackbar snackbar = SRSnackbar.make(binding.coordinator, getString(R.string.notification_deleted, notification.getTitle()), SRSnackbar.LENGTH_SHORT);
        SRSnackbar.setAction(snackbar, R.string.button_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.undoRemove();
            }
        }).show();
    }

    private void removeAll() {
        RxAlertDialog.with(this)
                .title(R.string.alert_title_remove_notifications)
                .message(R.string.alert_msg_remove_notifications)
                .positiveButton(R.string.button_delete)
                .neutralButton(R.string.button_cancel)
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        if (integer == RxAlertDialog.ButtonPositive) {
                            adapter.removeAll();
                            Snackbar snackbar = SRSnackbar.make(binding.coordinator, R.string.notifications_deleted, SRSnackbar.LENGTH_SHORT);
                            SRSnackbar.setAction(snackbar, R.string.button_undo, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    adapter.undoRemove();
                                }
                            }).show();
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();

        SimpleChromeCustomTabs.getInstance().connectTo(getActivity());
        presenter = loaderHelper.getPresenter();
        presenter.onViewAttached(this, binding.refresh);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden && binding != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbarLayout.toolbar);
            getActivity().setTitle(R.string.title_notifications);
        }
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_notifications, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_all:
                removeAll();
                break;
        }
        return super.onOptionsItemSelected(item);
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

    @Override
    public void openNotification(Notification notification) {
        if (!TextUtils.isEmpty(notification.url)) {
            navigateTo(Uri.parse(notification.url));
            return;
        }

        switch (notification.type) {
            case Notification.Matched:
                ((MainActivity) getActivity()).setSelectedTab(R.id.item_messages);
                break;
            case Notification.MatchRealUpdated:
            case Notification.SecondChanceRealUpdated:
                if (notification.matchId != -1) {
                    openProfile(notification.matchId);
                }
                break;
            case Notification.NewQuestion:
                Intent intent = new Intent(getActivity(), MyRealActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void openProfile(int profileId) {
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
                        Intent intent = new Intent(getActivity(), OtherProfileActivity.class);
                        intent.putExtra(OtherProfileActivity.EXTRA_PROFILE, profile);
                        intent.putExtra(OtherProfileActivity.EXTRA_SOURCE, OtherProfileActivity.SOURCE_NOTIFICATIONS);
                        startActivity(intent);
                    }
                });
    }

    @Override
    public void addToCalendar(Notification notification) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, notification.startDate.getTime())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, notification.endDate.getTime())
                .putExtra(Events.TITLE, notification.getTitle())
                .putExtra(Events.DESCRIPTION, "Group class")
                .putExtra(Events.EVENT_LOCATION, notification.summary)
                .putExtra(Events.AVAILABILITY, Events.AVAILABILITY_BUSY)
                .putExtra(Events.EVENT_TIMEZONE, "Europe/London");
        startActivity(intent);
    }

    @Override
    public Context getResourceContext() {
        return getActivity();
    }

    private final IntentCustomizer intentCustomizer = new IntentCustomizer() {
        @Override
        public SimpleChromeCustomTabsIntentBuilder onCustomiseIntent(SimpleChromeCustomTabsIntentBuilder intentBuilder) {
            return intentBuilder.withToolbarColor(ContextCompat.getColor(getActivity(), R.color.colorPrimary))
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

    @Override
    public void showEmpty(boolean show) {
        binding.empty.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recycler.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public RecyclerView getRecyclerView() {
        return binding.recycler;
    }

    @Override
    public void showLoading(boolean show, boolean isRefresh) {

    }

    @Override
    public void showContent(ListContent<Notification> content, boolean refresh) {
        adapter.addItems(content.items);
    }

    @Override
    public void deleteNotifications(List<Notification> notifications) {
        getAppComponent().analytics().send(new AppboyEvent(com.showreal.app.analytics.Events.NOTIFICATIONS_DELETE));
        presenter.remove(notifications);
    }

    @Override
    public void showError(Throwable throwable, String error) {
        FabricHelper.logException(throwable);
        RxAlertDialog.with(getActivity())
                .title(R.string.alert_title_error)
                .message(error)
                .positiveButton(R.string.button_ok)
                .create()
                .subscribe();
    }

    private static class NotificationAdapter extends SortedListDelegationAdapter<Notification> {

        final static Sorter SORTER = new Sorter() {
            @Override
            public int compare(Sortable o1, Sortable o2) {
                return ((Notification) o2).startDate.compareTo(((Notification) o1).startDate);
            }
        };
        final NotificationViewModel.NotificationView notificationView;
        private List<Notification> removed;
        private final static int REMOVE_DELAY = 1500;
        private final Handler removeHandler = new Handler();
        private Runnable currentRunnable;

        NotificationAdapter(Context context, NotificationViewModel.NotificationView notificationView) {
            super(Notification.class, SORTER);
            this.notificationView = notificationView;
            delegatesManager.addDelegate(new NotificationDelegate(context));
        }

        @Override
        public int getItemCount() {
            if (getItems() == null) {
                return 0;
            }
            return getItems().size();
        }

        @Override
        public void removeItem(Notification item) {
            super.removeItem(item);

            removeHandler.removeCallbacksAndMessages(null);
            if (currentRunnable != null) {
                currentRunnable.run();
            }

            removed = Collections.singletonList(item);
            currentRunnable = new Runnable() {
                @Override
                public void run() {
                    notificationView.deleteNotifications(removed);
                    removed = null;
                    currentRunnable = null;
                }
            };
            removeHandler.postDelayed(currentRunnable, REMOVE_DELAY);
        }

        void undoRemove() {
            if (removed == null) {
                return;
            }
            if (currentRunnable != null) {
                removeHandler.removeCallbacks(currentRunnable);
                currentRunnable = null;
            }
            removeHandler.removeCallbacksAndMessages(null);
            items.addAll(removed);
            removed = null;
        }

        public void removeAll() {
            removeHandler.removeCallbacksAndMessages(null);
            if (currentRunnable != null) {
                currentRunnable.run();
                currentRunnable = null;
            }

            removed = new ArrayList<>(items.size());
            for (int i = 0; i < items.size(); i++) {
                Notification notification = items.get(i);
                if (notification.type != Notification.Event) {
                    removed.add(notification);
                }
            }
            for (Notification notification : removed) {
                items.remove(notification);
            }

            currentRunnable = new Runnable() {
                @Override
                public void run() {
                    notificationView.deleteNotifications(removed);
                    removed = null;
                    currentRunnable = null;
                }
            };
            removeHandler.postDelayed(currentRunnable, REMOVE_DELAY);
        }

        private class NotificationDelegate extends AbsSortedListItemAdapterDelegate<Notification, Notification, NotificationViewHolder> {

            final LayoutInflater inflater;

            NotificationDelegate(Context context) {
                this.inflater = LayoutInflater.from(context);
            }


            @Override
            protected boolean isForViewType(@NonNull Notification item, SortedList<Notification> items, int position) {
                return true;
            }

            @NonNull
            @Override
            public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
                ItemNotificationBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_notification, parent, false);
                return new NotificationViewHolder(binding);
            }

            @Override
            protected void onBindViewHolder(@NonNull Notification item, @NonNull NotificationViewHolder viewHolder) {
                viewHolder.binding.setViewModel(new NotificationViewModel(item, notificationView));
                viewHolder.binding.content.drawLines = item.type != Notification.Event;
            }

        }

        private class NotificationViewHolder extends BindingViewHolder<ItemNotificationBinding> {

            NotificationViewHolder(ItemNotificationBinding binding) {
                super(binding);
            }

            public boolean isSwipeable() {
                return binding.getViewModel().getNotification().type != Notification.Event;
            }
        }
    }

    private static class NotificationsPresenter extends ListPresenter<Notification, NotificationsDataSource> {

        NotificationsPresenter(NotificationsDataSource dataSource) {
            super(dataSource);
        }

        void remove(List<Notification> notifications) {
            dataSource.remove(notifications);
        }
    }

    private static class NotificationsPresenterFactory implements PresenterFactory<NotificationsPresenter> {

        final Context context;

        private NotificationsPresenterFactory(Context context) {
            this.context = context;
        }

        @Override
        public NotificationsPresenter create() {
            return new NotificationsPresenter(new NotificationsDataSource(context));
        }
    }
}

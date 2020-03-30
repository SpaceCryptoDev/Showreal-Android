package com.showreal.app.features.conversations.matches;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.showreal.app.BaseActivity;
import com.showreal.app.BaseFragment;
import com.showreal.app.ErrorHandler;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.analytics.Screens;
import com.showreal.app.data.SendBirdHelper;
import com.showreal.app.data.model.Match;
import com.showreal.app.data.model.MessageNotification;
import com.showreal.app.data.model.Notification;
import com.showreal.app.databinding.FragmentMatchesBinding;
import com.showreal.app.features.conversations.ConversationActivity;
import com.showreal.app.features.conversations.SRDividerDecoration;
import com.showreal.app.features.notifications.SRFirebaseMessagingService;
import com.showreal.app.injection.ApplicationComponent;

import java.util.ArrayList;
import java.util.List;

import nl.qbusict.cupboard.DatabaseCompartment;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.lists.interfaces.ListPresenterView;
import uk.co.thedistance.components.lists.model.ListContent;
import uk.co.thedistance.components.lists.presenter.ListPresenter;
import uk.co.thedistance.thedistancecore.TDObservers;
import uk.co.thedistance.thedistancecore.TDSubscribers;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

public class MatchesFragment extends BaseFragment implements MatchViewModel.MatchView, ListPresenterView<Match> {

    private MatchesPresenter presenter;
    private PresenterLoaderHelper<MatchesPresenter> loaderHelper;
    private MatchAdapter adapter;
    private FragmentMatchesBinding binding;

    @Override
    protected String getScreenName() {
        return Screens.MATCHES;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_matches, container, false);

        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbarLayout.toolbar);
        getActivity().setTitle(R.string.title_messages);
        setHasOptionsMenu(true);

        binding.setViewModel(new MatchesViewModel());

        int colorOne = ContextCompat.getColor(getActivity(), R.color.colorAccent);
        int colorTwo = ContextCompat.getColor(getActivity(), R.color.colorPrimary);
        binding.refresh.setColorSchemeColors(colorOne, colorTwo);

        binding.recycler.setAdapter(adapter = new MatchAdapter(getActivity(), this));
        binding.recycler.addItemDecoration(new SRDividerDecoration(getActivity(), DividerItemDecoration.VERTICAL));

        loaderHelper = new PresenterLoaderHelper<>(getActivity(), new MatchesPresenterFactory(getAppComponent()));
        getLoaderManager().initLoader(0, null, loaderHelper);

        binding.searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                binding.searchView.dismissSuggestions();
                binding.searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                showEmpty(adapter.getItemCount() == 0);
                return false;
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        getActivity().registerReceiver(NOTIFICATION_RECEIVER, NOTIFICATION_INTENT_FILTER);
        getActivity().registerReceiver(MESSAGE_RECEIVER, MESSAGE_INTENT_FILTER);
    }

    @Override
    public void onStop() {
        super.onStop();

        getActivity().unregisterReceiver(NOTIFICATION_RECEIVER);
        getActivity().unregisterReceiver(MESSAGE_RECEIVER);
    }

    private final IntentFilter NOTIFICATION_INTENT_FILTER = new IntentFilter(SRFirebaseMessagingService.ACTION_SENDBIRD_NOTIFICATION);
    private final IntentFilter MESSAGE_INTENT_FILTER = new IntentFilter(SendBirdHelper.ACTION_SENDBIRD_MESSAGE);
    private final BroadcastReceiver NOTIFICATION_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMessageCount(intent.getStringExtra(SRFirebaseMessagingService.EXTRA_CONVERSATION_URL));
        }
    };
    private final BroadcastReceiver MESSAGE_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMessageCount(intent.getStringExtra(SendBirdHelper.EXTRA_CONVERSATION_URL));
        }
    };
    private static final IntentFilter TOAST_FILTER = new IntentFilter(SRFirebaseMessagingService.ACTION_NOTIFICATION_TOAST);
    private final BroadcastReceiver toastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int type = intent.getIntExtra(SRFirebaseMessagingService.EXTRA_TYPE, -1);
            if (type == Notification.Matched) {
                presenter.loadContent(true);
            }
        }
    };


    private void updateMessageCount(String url) {
        adapter.update(url);
    }

    @Override
    public void onResume() {
        super.onResume();

        presenter = loaderHelper.getPresenter();
        presenter.content = null;
        presenter.onViewAttached(this, binding.refresh);

        getContext().registerReceiver(toastReceiver, TOAST_FILTER);
    }

    @Override
    public void onPause() {
        super.onPause();

        getContext().unregisterReceiver(toastReceiver);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden && binding != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbarLayout.toolbar);
            getActivity().setTitle(R.string.title_messages);
        }
        super.onHiddenChanged(hidden);

        if (!hidden) {
            presenter.loadContent(true);
            getContext().registerReceiver(toastReceiver, TOAST_FILTER);
        } else {
            getContext().unregisterReceiver(toastReceiver);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_messages, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        binding.searchView.setMenuItem(item);
    }

    @Override
    public void openMatch(Match match) {
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        intent.putExtra(ConversationActivity.EXTRA_MATCH, match);
        startActivity(intent);
    }

    @Override
    public Context getImageContext() {
        return getActivity();
    }

    @Override
    public void showEmpty(boolean show) {
        binding.empty.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.empty2.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.empty3.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public RecyclerView getRecyclerView() {
        return binding.recycler;
    }

    @Override
    public void showLoading(boolean show, boolean isRefresh) {
        binding.refresh.setRefreshing(show);
    }

    @Override
    public void showContent(ListContent<Match> content, boolean refresh) {
        if (content.shouldClear) {
            adapter.clear();
        }
        adapter.addItems(content.items);

        List<String> suggestions = new ArrayList<>();
        for (Match match : content.items) {
            if (!suggestions.contains(match.profile.firstName)) {
                suggestions.add(match.profile.firstName);
            }
        }
        binding.searchView.setSuggestions(suggestions.toArray(new String[suggestions.size()]));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < adapter.getItemCount(); i++) {
                    RecyclerView.ViewHolder viewHolder = binding.recycler.findViewHolderForLayoutPosition(i);
                    if (viewHolder != null && viewHolder instanceof MatchAdapter.MatchViewHolder) {
                        ((MatchAdapter.MatchViewHolder) viewHolder).binding.getViewModel().updateCount();
                    }
                }
            }
        }, 400);
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, getActivity())) {
            return;
        }

        FabricHelper.logException(throwable);

        RxAlertDialog.with(this)
                .title(R.string.alert_title_matches)
                .message(getString(R.string.alert_msg_error_matches))
                .positiveButton(R.string.button_ok)
                .create()
                .subscribe();
    }

    @Override
    public boolean onBackPressed() {
        if (binding.searchView.isSearchOpen()) {
            binding.searchView.closeSearch();
            return true;
        }
        return super.onBackPressed();
    }

    private static class MatchesPresenter extends ListPresenter<Match, MatchesDataSource> {
        final ApplicationComponent applicationComponent;

        public MatchesPresenter(MatchesDataSource dataSource, ApplicationComponent applicationComponent) {
            super(dataSource);
            this.applicationComponent = applicationComponent;
        }

        @Override
        public void loadContent(boolean refresh) {
            DatabaseCompartment compartment = cupboard().withDatabase(applicationComponent.sendbird().getDatabase());
            compartment.delete(MessageNotification.class, null);

            applicationComponent.cache().<List<Match>>provider().withKey("matches_stale").readNullable()
                    .subscribeOn(Schedulers.io())
                    .subscribe(TDSubscribers.ignorant(new Action1<List<Match>>() {
                        @Override
                        public void call(List<Match> matches) {
                            if (matches == null)
                                return;
                            for (Match match : matches) {
                                if (!TextUtils.isEmpty(match.conversationUrl))
                                applicationComponent.cache().<List<MessageNotification>>provider().withKey(match.conversationUrl).evict().subscribe(TDObservers.<Void>empty());
                            }
                        }
                    }));

            super.loadContent(refresh);
        }
    }

    private static class MatchesPresenterFactory implements PresenterFactory<MatchesPresenter> {

        final ApplicationComponent applicationComponent;

        private MatchesPresenterFactory(ApplicationComponent applicationComponent) {
            this.applicationComponent = applicationComponent;
        }

        @Override
        public MatchesPresenter create() {
            return new MatchesPresenter(new MatchesDataSource(applicationComponent), applicationComponent);
        }
    }
}

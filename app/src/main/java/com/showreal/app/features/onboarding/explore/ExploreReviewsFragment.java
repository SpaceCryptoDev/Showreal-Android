package com.showreal.app.features.onboarding.explore;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.github.florent37.tutoshowcase.TutoShowcase;
import com.showreal.app.BaseFragment;
import com.showreal.app.ErrorHandler;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.analytics.Screens;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Reviewees;
import com.showreal.app.databinding.FragmentRealReviewBinding;
import com.showreal.app.databinding.ItemRealBinding;
import com.showreal.app.features.profile.other.OtherProfileActivity;
import com.showreal.app.features.real.ReelPlayer;
import com.showreal.app.features.reviews.RealLayoutManager;
import com.showreal.app.features.reviews.ReviewAdapter;
import com.showreal.app.features.reviews.ReviewViewModel;
import com.showreal.app.features.reviews.ReviewsDataSource;

import rx.functions.Action1;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenter;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenterView;

public class ExploreReviewsFragment extends BaseFragment implements ContentLoadingPresenterView<Reviewees>, ReviewViewModel.ReviewView {

    private static final String PREFS_FIELD_REVIEWS_HELP = "show_reviews_help";
    protected FragmentRealReviewBinding binding;
    private PresenterLoaderHelper<ReviewsPresenter> loaderHelper;
    public ReviewsPresenter presenter;
    protected ReviewAdapter adapter;
    protected RealLayoutManager layoutManager;
    protected int currentlyPlaying = -1;
    protected ReelPlayer currentPlayer;
    private boolean audioEnabled = false;
    protected boolean clearAdapter;
    protected boolean showingHelp;
    private boolean ifFromPreview = false;

    @Override
    protected String getScreenName() {
        return Screens.REVIEWS;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_real_review, container, false);
        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbarLayout.toolbar);
        getActivity().setTitle(R.string.title_real);

        setHasOptionsMenu(true);

        if (getClass() == ExploreReviewsFragment.class) {
            binding.toolbarLayout.toolbar.setBackgroundResource(R.drawable.gradient_toolbar);
        }

        int colorOne = ContextCompat.getColor(getActivity(), R.color.colorAccent);
        int colorTwo = ContextCompat.getColor(getActivity(), R.color.colorPrimary);
        binding.refresh.setColorSchemeColors(colorOne, colorTwo);

        loaderHelper = new PresenterLoaderHelper<>(getActivity(), new ReviewsPresenterFactory());
        getLoaderManager().initLoader(0, null, loaderHelper);

        binding.recycler.setAdapter(adapter = new ReviewAdapter(getActivity(), this));

        layoutManager = (RealLayoutManager) binding.recycler.getLayoutManager();

        return binding.getRoot();
    }

    protected final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            int lastComplete = layoutManager.findLastCompletelyVisibleItemPosition();

            if (lastComplete != RecyclerView.NO_POSITION && currentlyPlaying != lastComplete) {
                startPlayer(lastComplete);
            }
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (getClass() == ExploreReviewsFragment.class) {
            inflater.inflate(R.menu.menu_explore, menu);
            ifFromPreview = true;
        } else {
            inflater.inflate(R.menu.menu_reviews, menu);
            ifFromPreview = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_preferences:
                openPreferences(false);
                break;
            case R.id.action_signup:
                AccountManager.get(getActivity()).addAccount(getString(R.string.account_type), "session",
                        null, null, getActivity(), null, new Handler());
                getActivity().finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void openPreferences(boolean firstTime) {
        showSignUpPrompt();
    }

    @Override
    public void onPause() {
        super.onPause();

        pausePlayer();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden && binding != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbarLayout.toolbar);
            getActivity().setTitle(R.string.title_real);
        }
        super.onHiddenChanged(hidden);

        if (hidden) {
            pausePlayer();
        } else {
            readyPlayer();
        }
    }

    private void pausePlayer() {
        if (currentPlayer != null) {
            currentPlayer.pause();
            currentPlayer.clear();
        }
    }

    private void readyPlayer() {
        if (currentPlayer != null) {
            currentPlayer.ready();
        }
    }

    private final Handler playHandler = new Handler();

    private class PlayRunnable implements Runnable {

        private final ItemRealBinding binding;
        int position;

        private PlayRunnable(int position, ItemRealBinding binding) {
            this.position = position;
            this.binding = binding;
        }

        @Override
        public void run() {
            currentPlayer = ReelPlayer.with(binding.getRoot().getContext(), binding.getViewModel().profile.getReelId())
                    .binding(binding)
                    .videos(binding.getViewModel().profile.videos)
                    .audio(audioEnabled)
                    .hasImage(binding.getViewModel().profile.image != null)
                    .promo(binding.getViewModel().profile.id == 6)
                    .create();
            currentPlayer.setup();
            currentPlayer.start(false);
        }
    }

    protected void startPlayer(int position) {
        if (position == currentlyPlaying) {
            return;
        }

        stopPlayer();

        View view = layoutManager.findViewByPosition(position);
        ViewDataBinding binding = DataBindingUtil.getBinding(view);
        if (!(binding instanceof ItemRealBinding)) {
            position++;
            view = layoutManager.findViewByPosition(position);
            if (view != null) {
                binding = DataBindingUtil.getBinding(view);
            }
        }
        if (binding instanceof ItemRealBinding) {
            currentlyPlaying = position;
            playHandler.postDelayed(new PlayRunnable(position, (ItemRealBinding) binding), 400);

        }

        //  TODO: Divyesh Add Video link here for every 3rd view.
    }

    protected void stopPlayer() {
        currentlyPlaying = -1;
        playHandler.removeCallbacksAndMessages(null);
        if (currentPlayer != null) {
            audioEnabled = currentPlayer.isAudioEnabled();
            currentPlayer.destroy();
        }
        currentPlayer = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        readyPlayer();
        presenter = loaderHelper.getPresenter();
        presenter.onViewAttached(this, binding.refresh);
    }

    @Override
    public void showLoading(boolean show, boolean isRefresh) {
        binding.refresh.setRefreshing(isRefresh);
    }

    private void showHelp() {
        if (showingHelp) {
            return;
        }
        showingHelp = true;

        if (adapter.getReviewCount() == 1) {
            binding.recycler.setPadding(0, 0, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics()));
        }

        showHelp(0, null);
    }

    protected void displayTuto2(final int page, final ViewDataBinding bnding) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ifFromPreview) {
                    final TutoShowcase showcase = TutoShowcase.from(getActivity())
                            .setListener(new TutoShowcase.Listener() {
                                @Override
                                public void onDismissed() {

                                }
                            })
                            .setContentView(R.layout.tuto_reviews_search)
                            .setBackgroundColor(getResources().getColor(R.color.black87))
                            .setFitsSystemWindows(true)
                            .on(R.id.action_signup)
                            .addCircle()
                            .show();

                    showcase.onClickContentView(R.id.button_next, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showcase.dismiss();
                            showHelp(page + 1, bnding);
                        }
                    });
                } else {
                    final TutoShowcase showcase = TutoShowcase.from(getActivity())
                            .setListener(new TutoShowcase.Listener() {
                                @Override
                                public void onDismissed() {

                                }
                            })
                            .setContentView(R.layout.tuto_reviews_search)
                            .setBackgroundColor(getResources().getColor(R.color.black87))
                            .setFitsSystemWindows(true)
                            .on(R.id.action_preferences)
                            .addCircle()
                            .show();

                    showcase.onClickContentView(R.id.button_next, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showcase.dismiss();
                            showHelp(page + 1, bnding);
                        }
                    });
                }
            }
        }, (false ? 300 : 0));
    }

    private int helpScrollPosition = -1;

    private void showHelp(final int page, ViewDataBinding firstBinding) {
        boolean second_binding = false;
        if (page > 1) {
            showingHelp = false;
            if (helpScrollPosition != -1) {
                binding.recycler.smoothScrollBy(0, -helpScrollPosition);
            }
            binding.recycler.setPadding(0, 0, 0, 0);
            startPlayer(0);
            getAppComponent().preferences().edit().putBoolean(PREFS_FIELD_REVIEWS_HELP, false).apply();
            return;
        } else if(page == 1) {
            displayTuto2(page, null);
            second_binding = true;
        } else if (firstBinding == null) {
            int pos = layoutManager.findLastCompletelyVisibleItemPosition();
            View view = layoutManager.findViewByPosition(pos);
            firstBinding = DataBindingUtil.getBinding(view);
            if (!(firstBinding instanceof ItemRealBinding)) {
                pos++;
                view = layoutManager.findViewByPosition(pos);
                if (view != null) {
                    firstBinding = DataBindingUtil.getBinding(view);
                }
            }
        }

        if (firstBinding instanceof ItemRealBinding && second_binding == false) {
            int layoutId = 0;
            View highlight = null;
            View highlight1 = null;
            View highlight2 = null;
            switch (page) {
                case 0:
                    layoutId = R.layout.tuto_reviews_keep;
                    highlight1 = ((ItemRealBinding) firstBinding).buttonKeep;
                    highlight2 = ((ItemRealBinding) firstBinding).buttonCut;
                    highlight = ((ItemRealBinding) firstBinding).buttonChance;
                    break;
//                case 1:
//                    layoutId = R.layout.tuto_reviews_cut;
//                    highlight = ((ItemRealBinding) firstBinding).buttonCut;
//                    break;
//                case 2:
//                    layoutId = R.layout.tuto_reviews_chance;
//                    highlight = ((ItemRealBinding) firstBinding).buttonChance;
//                    break;

                case 1:


                    break;
            }

            boolean delay = false;
            if (helpScrollPosition == -1) {
                float bottom = binding.getRoot().getBottom() - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, getResources().getDisplayMetrics());
                int[] location = {0, 0};
                highlight.getLocationOnScreen(location);
                int viewBottom = (int) (location[1] + (highlight.getHeight() * 1.2f));
                highlight1.getLocationOnScreen(location);
                int viewBottom1 = (int) (location[1] + (highlight1.getHeight() * 1.2f));
                highlight2.getLocationOnScreen(location);
                int viewBottom2 = (int) (location[1] + (highlight2.getHeight() * 1.2f));

                if (viewBottom > bottom) {
                    delay = true;
                    helpScrollPosition = (int) (viewBottom - bottom);
                    binding.recycler.smoothScrollBy(0, helpScrollPosition);

                } else {
                    helpScrollPosition = 0;
                }

                if (viewBottom1 > bottom) {
                    delay = true;
                    helpScrollPosition = (int) (viewBottom1 - bottom);
                    binding.recycler.smoothScrollBy(0, helpScrollPosition);

                } else {
                    helpScrollPosition = 0;
                }

                if (viewBottom2 > bottom) {
                    delay = true;
                    helpScrollPosition = (int) (viewBottom2 - bottom);
                    binding.recycler.smoothScrollBy(0, helpScrollPosition);

                } else {
                    helpScrollPosition = 0;
                }
            }

            final int finalLayoutId = layoutId;
            final View finalHighlight = highlight;
            final View finalHighlight1 = highlight1;
            final View finalHighlight2 = highlight2;
            final ViewDataBinding finalFirstBinding = firstBinding;
            final int marginBottom = (getClass() == ExploreReviewsFragment.class) ? 0 : (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56f, getResources().getDisplayMetrics());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    final TutoShowcase showcase = TutoShowcase.from(getActivity())
                            .setContentView(finalLayoutId)
                            .setBackgroundColor(getResources().getColor(R.color.black87))
                            .on(finalHighlight)
                            .addCircle(1.0f)
                            .on(finalHighlight1)
                            .addCircle(1.0f)
                            .on(finalHighlight2)
                            .addCircle(1.0f)
                            .show();

                    showcase.onClickContentView(R.id.button_next, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showcase.dismiss();
                            showHelp(page + 1, finalFirstBinding);
                        }
                    });
                }
            }, (delay ? 300 : 0));
        }
    }

    @Override
    public void showContent(final Reviewees content, final boolean refresh) {
        binding.recycler.removeOnScrollListener(scrollListener);

        adapter.getItems().beginBatchedUpdates();

        int index = 0;
        int top = 0;

        if (refresh) {
            index = layoutManager.findFirstVisibleItemPosition();
            View v = binding.recycler.getChildAt(0);
            top = (v == null) ? 0 : (v.getTop() - binding.recycler.getPaddingTop());
            stopPlayer();

            if (clearAdapter) {
                adapter.clear();
                clearAdapter = false;
            }
        }

        adapter.addPrimary(content.primary);
        adapter.addSecondary(content.secondary);

        adapter.getItems().endBatchedUpdates();

        binding.empty.setVisibility(adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);

        if (refresh) {
            layoutManager.scrollToPositionWithOffset(index, top);
        }

        if (getAppComponent().preferences().getBoolean(PREFS_FIELD_REVIEWS_HELP, true) && (!content.primary.isEmpty() || !content.secondary.isEmpty())) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showHelp();
                }
            }, 400);
        }

        if (refresh) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (content.primary.isEmpty() && content.secondary.isEmpty()) {
                        return;
                    }
                    if (refresh && !showingHelp) {
                        int current = layoutManager.findLastCompletelyVisibleItemPosition();
                        startPlayer(current);
                    }

                    binding.recycler.addOnScrollListener(scrollListener);
                }
            }, 400);
        } else if (!content.primary.isEmpty() || !content.secondary.isEmpty()) {
            binding.recycler.addOnScrollListener(scrollListener);
        }
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, getActivity())) {
            return;
        }

        FabricHelper.logException(throwable);

        RxAlertDialog.with(this)
                .title(R.string.alert_title_reviews)
                .message(getString(R.string.alert_msg_error_review))
                .positiveButton(R.string.button_ok)
                .create()
                .subscribe();

    }

    @Override
    public void secondChance(final Profile profile) {
        showSignUpPrompt();
    }

    @Override
    public void keep(final Profile profile) {
        showSignUpPrompt();
    }

    @Override
    public void goforit(final Profile profile) {

    }

    @Override
    public void cut(final Profile profile) {
        showSignUpPrompt();
    }

    private void showSignUpPrompt() {
        RxAlertDialog.with(this)
                .title(R.string.alert_title_sign_up)
                .message(R.string.alert_msg_sign_up)
                .positiveButton(R.string.button_sign_up)
                .negativeButton(R.string.button_cancel)
                .create()
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        if (integer == RxAlertDialog.ButtonPositive) {
                            AccountManager.get(getActivity()).addAccount(getString(R.string.account_type), "session",
                                    null, null, getActivity(), null, new Handler());
                            getActivity().finish();
                        }
                    }
                });
    }

    @Override
    public void open(Profile profile) {
        Intent intent = new Intent(getActivity(), OtherProfileActivity.class);
        intent.putExtra(OtherProfileActivity.EXTRA_PROFILE, profile);
        intent.putExtra(OtherProfileActivity.EXTRA_SOURCE, OtherProfileActivity.SOURCE_REVIEWS);
        startActivity(intent);
    }

    @Override
    public int getViewWidth() {
        return binding.recycler.getWidth();
    }

    @Override
    public Context getImageContext() {
        return getActivity();
    }

    protected class ReviewsPresenter extends ContentLoadingPresenter<Reviewees, ReviewsDataSource, ContentLoadingPresenterView<Reviewees>> {

        public ReviewsPresenter(ReviewsDataSource dataSource) {
            super(dataSource);
        }
    }

    protected class ReviewsPresenterFactory implements PresenterFactory<ReviewsPresenter> {

        @Override
        public ReviewsPresenter create() {
            return new ReviewsPresenter(new ReviewsDataSource(getActivity()));
        }
    }
}

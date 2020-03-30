package com.showreal.app.features.potential;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.showreal.app.BaseFragment;
import com.showreal.app.ErrorHandler;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.analytics.AppboyEvent;
import com.showreal.app.analytics.Events;
import com.showreal.app.analytics.Screens;
import com.showreal.app.data.model.Liked;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.ProfileResponse;
import com.showreal.app.databinding.FragmentPotentialBinding;
import com.showreal.app.databinding.ItemPotentialBinding;
import com.showreal.app.features.profile.other.OtherProfileActivity;

import java.util.List;

import rx.functions.Action1;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.base.UploaderLoaderHelper;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenter;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenterView;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;
import uk.co.thedistance.thedistancecore.TDSubscribers;


public class PotentialFragment extends BaseFragment implements ContentLoadingPresenterView<List<Liked>>, PotentialViewModel.PotentialView, PotentialClickListener.OnItemClickListener, UploadingPresenterView<Integer> {

    private FragmentPotentialBinding binding;
    private PresenterLoaderHelper<PotentialPresenter> loaderHelper;
    private PotentialPresenter presenter;
    private PotentialAdapter adapter;
    private float swipeThreshhold = 0;
    private ItemTouchHelper touchHelper;
    private UploaderLoaderHelper<OtherProfileActivity.ResponseUploader> uploaderHelper;
    private OtherProfileActivity.ResponseUploader uploader;

    private enum Action {
        None,
        Cut,
        Chance
    }

    private Action action = Action.None;

    @Override
    protected String getScreenName() {
        return Screens.POTENTIAL;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_potential, container, false);

        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbarLayout.toolbar);
        getActivity().setTitle(R.string.title_potential);

        int colorOne = ContextCompat.getColor(getActivity(), R.color.colorAccent);
        int colorTwo = ContextCompat.getColor(getActivity(), R.color.colorPrimary);
        binding.refresh.setColorSchemeColors(colorOne, colorTwo);

        binding.recycler.setAdapter(adapter = new PotentialAdapter(getActivity(), this));

        loaderHelper = new PresenterLoaderHelper<>(getActivity(), new PotentialPresenterFactory());
        getLoaderManager().initLoader(0, null, loaderHelper);

        binding.recycler.addOnItemTouchListener(new PotentialClickListener(getActivity(), this));

        final ItemTouchHelper.SimpleCallback touchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                ItemPotentialBinding binding = ((PotentialAdapter.MatchViewHolder) viewHolder).binding;
                if (swipeThreshhold == 0) {
                    swipeThreshhold = binding.buttons.getWidth();
                }

                Log.i("swipe", String.valueOf(dX));
                float translationX = Math.max(0, Math.min(-dX, swipeThreshhold));
                getDefaultUIUtil().onDraw(c, recyclerView, binding.content, -translationX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onChildDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                ItemPotentialBinding binding = ((PotentialAdapter.MatchViewHolder) viewHolder).binding;
                getDefaultUIUtil().clearView(binding.content);
            }
        };

        touchHelper = new ItemTouchHelper(touchCallback);
        touchHelper.attachToRecyclerView(binding.recycler);

        uploaderHelper = new UploaderLoaderHelper<>(getActivity(), new OtherProfileActivity.ResponseUploaderFactory(getActivity(), 0));
        getLoaderManager().initLoader(1, null, uploaderHelper);

        return binding.getRoot();
    }


    @Override
    public boolean onItemClick(PotentialAdapter.MatchViewHolder holder, MotionEvent event) {
        ItemPotentialBinding binding = holder.binding;
        float buttonsX = binding.buttons.getX();
        float cutX = buttonsX + binding.buttonCut.getX();
        float chanceX = buttonsX + binding.buttonChance.getX();

        if (event.getX() >= cutX
                && event.getX() <= cutX + binding.buttonCut.getWidth()) {

            cutPotential(binding.getViewModel().getProfile());

            return true;
        } else if (event.getX() >= chanceX
                && event.getX() <= chanceX + binding.buttonChance.getWidth()) {

            chancePotential(binding.getViewModel().getProfile());

            return true;
        }

        touchHelper.startSwipe(holder);

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        presenter = loaderHelper.getPresenter();
        presenter.onViewAttached(this, binding.refresh);

        uploader = uploaderHelper.getUploader();
        uploader.onViewAttached(this);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden && binding != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbarLayout.toolbar);
            getActivity().setTitle(R.string.title_potential);
        }
        super.onHiddenChanged(hidden);

        if (!hidden) {
            presenter.loadContent(true);
        }
    }

    @Override
    public void showLoading(boolean show, boolean isRefresh) {
        binding.refresh.setRefreshing(show);
    }

    @Override
    public void showContent(List<Liked> content, boolean refresh) {
        adapter.clear();
        adapter.addItems(content);
        binding.empty.setVisibility(adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void showUploading(boolean show) {
        if (show) {
            binding.loading.loadingLayout.show();
        } else {
            binding.loading.loadingLayout.hide();
        }
    }

    @Override
    public void uploadComplete(Integer response) {
        action = Action.None;
        presenter.loadContent(true);
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, getActivity())) {
            return;
        }

        FabricHelper.logException(throwable);

        switch (action) {
            case None:
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_potential)
                        .message(getString(R.string.alert_msg_error_potential))
                        .positiveButton(R.string.button_ok)
                        .create()
                        .subscribe();
                break;
            case Cut:
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_potential)
                        .message(getString(R.string.alert_msg_error_cut))
                        .positiveButton(R.string.button_ok)
                        .create()
                        .subscribe();
                break;
            case Chance:
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_potential)
                        .message(getString(R.string.alert_msg_error_chance))
                        .positiveButton(R.string.button_ok)
                        .create()
                        .subscribe();
        }
        action = Action.None;

    }

    @Override
    public void openPotential(Profile profile) {
        Intent intent = new Intent(getActivity(), OtherProfileActivity.class);
        intent.putExtra(OtherProfileActivity.EXTRA_PROFILE, profile);
        intent.putExtra(OtherProfileActivity.EXTRA_SOURCE, OtherProfileActivity.SOURCE_POTENTIAL);
        startActivity(intent);
    }


    @Override
    public void chancePotential(final Profile profile) {
        showShowRealMessage(R.string.button_second_chance)
                .subscribe(TDSubscribers.ignorant(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean shown) {
                        if (!shown) {
                            chance(profile);
                        }
                    }
                }));

    }

    private void chance(Profile profile) {
        getAppComponent().analytics().send(new AppboyEvent(Events.POTENTIAL_CHANCE));
        RecyclerView.ViewHolder viewHolder = binding.recycler.findViewHolderForItemId(profile.id);
        if (viewHolder != null) {
            touchHelper.startSwipe(viewHolder);
        }
        uploader.setUserId(profile.id);
        action = Action.Chance;
        uploader.uploadContent(ProfileResponse.SecondChance);
    }

    @Override
    public void cutPotential(final Profile profile) {
        showShowRealMessage(R.string.button_cut)
                .subscribe(TDSubscribers.ignorant(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean shown) {
                        if (!shown) {
                            cut(profile);
                        }
                    }
                }));
    }

    private void cut(Profile profile) {
        getAppComponent().analytics().send(new AppboyEvent(Events.POTENTIAL_CUT));
        RecyclerView.ViewHolder viewHolder = binding.recycler.findViewHolderForItemId(profile.id);
        if (viewHolder != null) {
            touchHelper.startSwipe(viewHolder);
        }
        uploader.setUserId(profile.id);
        action = Action.Cut;
        uploader.uploadContent(ProfileResponse.Cut);
    }

    private static class PotentialPresenter extends ContentLoadingPresenter<List<Liked>, PotentialDataSource, ContentLoadingPresenterView<List<Liked>>> {

        public PotentialPresenter(PotentialDataSource dataSource) {
            super(dataSource);
        }
    }

    private class PotentialPresenterFactory implements PresenterFactory<PotentialPresenter> {

        @Override
        public PotentialPresenter create() {
            return new PotentialPresenter(new PotentialDataSource(getAppComponent()));
        }
    }

}

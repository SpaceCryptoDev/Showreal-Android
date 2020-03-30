package com.showreal.app.features.reviews;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.showreal.app.ErrorHandler;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.analytics.AppboyEvent;
import com.showreal.app.analytics.Events;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.ProfileResponse;
import com.showreal.app.features.onboarding.explore.ExploreReviewsFragment;
import com.showreal.app.features.profile.other.OtherProfileActivity;
import com.showreal.app.features.reviews.preferences.PreferencesActivity;

import retrofit2.adapter.rxjava.HttpException;
import rx.functions.Action1;
import uk.co.thedistance.components.base.UploaderLoaderHelper;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;
import uk.co.thedistance.thedistancecore.TDSubscribers;

public class ReviewsFragment extends ExploreReviewsFragment implements UploadingPresenterView<Integer> {

    private static final int RC_PREFERENCES = 0x0;
    private UploaderLoaderHelper<OtherProfileActivity.ResponseUploader> uploaderHelper;
    private OtherProfileActivity.ResponseUploader uploader;

    private enum Action {
        None,
        Cut,
        Chance,
        Keep
    }

    private Action action = Action.None;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        uploaderHelper = new UploaderLoaderHelper<>(getActivity(), new OtherProfileActivity.ResponseUploaderFactory(getActivity(), 0));
        getLoaderManager().initLoader(1, null, uploaderHelper);

        return view;
    }

    @Override
    protected void startPlayer(int position) {
        if (showingHelp) {
            return;
        }
        super.startPlayer(position);
    }

    @Override
    protected void openPreferences(final boolean firstTime) {
        showShowRealMessage(R.string.alert_title_reviews)
                .subscribe(TDSubscribers.ignorant(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean shown) {
                        if (shown) {
                            return;
                        }
                        Intent intent = new Intent(getActivity(), PreferencesActivity.class);
                        intent.putExtra(PreferencesActivity.EXTRA_FIRST_TIME, firstTime);
                        startActivityForResult(intent, RC_PREFERENCES);
                    }
                }));
    }

    @Override
    public void onResume() {
        super.onResume();

        uploader = uploaderHelper.getUploader();
        uploader.onViewAttached(this);
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
    public void uploadComplete(final Integer response) {
        switch (action) {
            case Chance:
                getAppComponent().analytics().send(new AppboyEvent(Events.REVIEWS_CHANCE));
                break;
            case Cut:
                getAppComponent().analytics().send(new AppboyEvent(Events.REVIEWS_CUT));
                break;
            case Keep:
                getAppComponent().analytics().send(new AppboyEvent(Events.REVIEWS_KEEP));
                break;
        }

        action = Action.None;
        currentlyPlaying = -1;

        stopPlayer();
        adapter.remove(response);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int pos = layoutManager.findLastVisibleItemPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    binding.recycler.smoothScrollToPosition(pos);
                }
            }
        }, 200);

        if (adapter.getItems().size() == 0) {
            presenter.loadContent(true);
        }
    }

    @Override
    public void showError(Throwable throwable, String error) {
        binding.loading.loadingLayout.hide();
        if (ErrorHandler.handle(throwable, getActivity())) {
            return;
        }

        FabricHelper.logException(throwable);

        switch (action) {
            case None:
                if (throwable instanceof HttpException && ((HttpException) throwable).code() == 400) {
                    openPreferences(true);
                    return;
                }
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_reviews)
                        .message(getString(R.string.alert_msg_error_review))
                        .positiveButton(R.string.button_ok)
                        .create()
                        .subscribe();
                break;
            case Cut:
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_reviews)
                        .message(getString(R.string.alert_msg_error_review_cut))
                        .positiveButton(R.string.button_ok)
                        .create()
                        .subscribe();
                break;
            case Chance:
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_reviews)
                        .message(getString(R.string.alert_msg_error_review_chance))
                        .positiveButton(R.string.button_ok)
                        .create()
                        .subscribe();
                break;
            case Keep:
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_reviews)
                        .message(getString(R.string.alert_msg_error_review_keep))
                        .positiveButton(R.string.button_ok)
                        .create()
                        .subscribe();
        }
        action = Action.None;
    }

    @Override
    public void secondChance(final Profile profile) {
        showShowRealMessage(R.string.button_second_chance)
                .subscribe(TDSubscribers.ignorant(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean shown) {
                        if (shown) {
                            return;
                        }
                        action = Action.Chance;
                        uploader.setUserId(profile.id);
                        uploader.uploadContent(ProfileResponse.SecondChance);
                    }
                }));
    }

    @Override
    public void goforit(final Profile profile) {

        // TODO: Divyesh click event for go for it.
    }

    @Override
    public void keep(final Profile profile) {
        showShowRealMessage(R.string.button_keep)
                .subscribe(TDSubscribers.ignorant(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean shown) {
                        if (shown) {
                            return;
                        }
                        action = Action.Keep;
                        uploader.setUserId(profile.id);
                        uploader.uploadContent(ProfileResponse.Keep);
                    }
                }));
    }

    @Override
    public void cut(final Profile profile) {
        showShowRealMessage(R.string.button_cut)
                .subscribe(TDSubscribers.ignorant(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean shown) {
                        if (shown) {
                            return;
                        }
                        action = Action.Cut;
                        uploader.setUserId(profile.id);
                        uploader.uploadContent(ProfileResponse.Cut);
                    }
                }));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_PREFERENCES && resultCode == Activity.RESULT_OK) {
            clearAdapter = true;
            presenter.loadContent(true);
        }
    }
}

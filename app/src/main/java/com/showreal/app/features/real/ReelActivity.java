package com.showreal.app.features.real;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.showreal.app.BaseActivity;
import com.showreal.app.BaseFragment;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.data.model.Profile;
import com.showreal.app.databinding.ActivityRealBinding;
import com.showreal.app.databinding.FragmentRealBinding;
import com.showreal.app.features.profile.other.OtherProfileActivity;
import com.showreal.app.features.real.promo.PromoDataSource;
import com.showreal.app.features.reviews.ReviewViewModel;

import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenterView;

public class ReelActivity extends BaseActivity {

    ActivityRealBinding binding;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_real);
    }

    public static class ReelFragment extends BaseFragment implements ContentLoadingPresenterView<Profile>, ReviewViewModel.ReviewView {

        FragmentRealBinding binding;
        private PresenterLoaderHelper<ReelPresenter> loaderHelper;
        private ReelPresenter presenter;
        private ReelPlayer reelPlayer;

        @Override
        protected String getScreenName() {
            return null;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_real, container, false);
            ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbarLayout.toolbar);
            getActivity().setTitle(R.string.title_real);

            loaderHelper = new PresenterLoaderHelper<>(getActivity(), new ReelPresenterFactory());
            getLoaderManager().initLoader(0, null, loaderHelper);

            return binding.getRoot();
        }


        @Override
        public void onResume() {
            super.onResume();

            readyPlayer();
            presenter = loaderHelper.getPresenter();
            presenter.onViewAttached(this);
        }

        @Override
        public void onPause() {
            super.onPause();

            pausePlayer();
        }

        @Override
        public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);

            if (hidden) {
                pausePlayer();
            } else {
                readyPlayer();
            }
        }

        private void pausePlayer() {
            if (reelPlayer != null) {
                reelPlayer.pause();
                reelPlayer.clear();
            }
        }

        private void readyPlayer() {
            if (reelPlayer != null) {
                reelPlayer.ready();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            if (reelPlayer != null) {
                reelPlayer.destroy();
            }
        }

        @Override
        public void showLoading(boolean show, boolean isRefresh) {

        }

        @Override
        public void showContent(Profile profile, boolean refresh) {
            if (reelPlayer == null) {
                reelPlayer = ReelPlayer.with(getActivity(), profile.getReelId())
                        .binding(binding.player)
                        .videos(profile.videos)
                        .promo(true)
                        .audio(true)
                        .create();

                reelPlayer.setup();
                reelPlayer.start(false);
            }
            binding.setViewModel(new ReviewViewModel(profile, this));
        }

        @Override
        public void showError(Throwable throwable, String error) {

        }

        @Override
        public void secondChance(Profile profile) {
            showMessage(R.string.button_second_chance);
        }

        @Override
        public void keep(Profile profile) {
            showMessage(R.string.button_keep);
        }

        @Override
        public void goforit(Profile profile) {
            showMessage(R.string.button_goforit);
        }

        @Override
        public void cut(Profile profile) {
            showMessage(R.string.button_cut);
        }

        protected void showMessage(@StringRes final int titleRes) {
            RxAlertDialog.with(this)
                    .title(titleRes)
                    .message(R.string.alert_msg_no_showreal)
                    .positiveButton(R.string.button_ok)
                    .create()
                    .subscribe();
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
            return binding.getRoot().getWidth();
        }

        @Override
        public Context getImageContext() {
            return getActivity();
        }

        private final class ReelPresenterFactory implements PresenterFactory<ReelPresenter> {

            @Override
            public ReelPresenter create() {
                return new ReelPresenter(new PromoDataSource(getActivity()));
            }
        }
    }


}

package com.showreal.app.features.profile.instagram;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.showreal.app.BaseFragment;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.model.InstagramMedia;
import com.showreal.app.databinding.FragmentInstagramBinding;
import com.showreal.app.features.profile.ProfileFragment;

import retrofit2.adapter.rxjava.HttpException;
import rx.functions.Action1;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.lists.interfaces.ListPresenterView;
import uk.co.thedistance.components.lists.model.ListContent;
import uk.co.thedistance.components.lists.presenter.EndlessListPresenter;


public class InstagramGalleryFragment extends BaseFragment implements InstagramViewModel.InstagramView, ListPresenterView<InstagramMedia.Image> {

    private PresenterLoaderHelper<InstagramPresenter> loaderHelper;
    private FragmentInstagramBinding binding;
    private InstagramPresenter presenter;
    private InstagramAdapter adapter;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loaderHelper = new PresenterLoaderHelper<>(getActivity(), new InstagramPresenterFactory(getActivity()));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_instagram, container, false);

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.getItems().get(position) instanceof InstagramAdapter.LoadingObject ? 2 : 1;
            }
        });

        binding.gallery.setLayoutManager(layoutManager);
        binding.gallery.setAdapter(adapter = new InstagramAdapter(getActivity(), this));

        getLoaderManager().initLoader(0, null, loaderHelper);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        presenter = loaderHelper.getPresenter();
        presenter.onViewAttached(this);
    }

    @Override
    public void showLoading(boolean show, boolean isRefresh) {
        if (adapter.getItemCount() == 0) {
            if (show) {
                binding.loadingLayout.show();
            } else {
                binding.loadingLayout.hide();
            }
        } else {
            adapter.showLoading(show);
        }
    }

    @Override
    public void showContent(ListContent<InstagramMedia.Image> content, boolean refresh) {
        if (content.shouldClear) {
            adapter.clear();
        }
        adapter.addItems(content.items);
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (throwable instanceof HttpException && ((HttpException) throwable).code() == 400) {
            showTokenExpiry();
        }
    }

    private void showTokenExpiry() {
        RxAlertDialog.with(this)
                .title(R.string.alert_title_update_instagram)
                .message(R.string.alert_msg_update_instagram)
                .positiveButton(R.string.button_reconnect)
                .negativeButton(R.string.button_cancel)
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        if (integer == RxAlertDialog.ButtonPositive) {
                            ((ProfileFragment) getTargetFragment()).connectInstagram();
                        }
                    }
                });

    }

    public void reload() {
        presenter.loadContent(true);
    }

    @Override
    public void openImage(View image) {
        PhotoActivity.startWith(getActivity(), adapter.getImages(), image);
    }

    @Override
    public void showEmpty(boolean show) {

    }

    @Override
    public RecyclerView getRecyclerView() {
        return binding.gallery;
    }

    private static class InstagramPresenter extends EndlessListPresenter<InstagramMedia.Image, InstagramMediaDataSource> {

        public InstagramPresenter(InstagramMediaDataSource dataSource) {
            super(dataSource);
        }
    }

    private static class InstagramPresenterFactory implements PresenterFactory<InstagramPresenter> {

        final Context context;

        private InstagramPresenterFactory(Context context) {
            this.context = context;
        }

        @Override
        public InstagramPresenter create() {
            return new InstagramPresenter(new InstagramMediaDataSource(TheDistanceApplication.getApplicationComponent(context)));
        }
    }
}

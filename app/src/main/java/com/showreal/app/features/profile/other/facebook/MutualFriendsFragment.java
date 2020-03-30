package com.showreal.app.features.profile.other.facebook;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.AccessToken;
import com.showreal.app.BaseFragment;
import com.showreal.app.ErrorHandler;
import com.showreal.app.R;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.model.MutualFriend;
import com.showreal.app.databinding.FragmentMutualFriendsBinding;

import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.lists.interfaces.ListPresenterView;
import uk.co.thedistance.components.lists.model.ListContent;
import uk.co.thedistance.components.lists.presenter.ListPresenter;

public class MutualFriendsFragment extends BaseFragment implements ListPresenterView<MutualFriend>, MutualFriendViewModel.MutualFriendView {

    private PresenterLoaderHelper<FriendsPresenter> loaderHelper;
    private FragmentMutualFriendsBinding binding;
    private FriendsPresenter presenter;
    private MutualFriendsAdapter adapter;

    @Override
    protected String getScreenName() {
        return null;
    }

    public static MutualFriendsFragment newInstance(String friendId) {

        Bundle args = new Bundle();
        args.putString("id", friendId);
        MutualFriendsFragment fragment = new MutualFriendsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_mutual_friends, container, false);

        String id = getArguments().getString("id");
        loaderHelper = new PresenterLoaderHelper<>(getActivity(), new FriendsPresenterFactory(getActivity(), id));
        getLoaderManager().initLoader(0, null, loaderHelper);

        binding.recycler.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        binding.recycler.setAdapter(adapter = new MutualFriendsAdapter(this, getActivity()));

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        presenter = loaderHelper.getPresenter();
        presenter.onViewAttached(this);
    }

    @Override
    public void showEmpty(boolean show) {
        binding.empty.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recycler.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public RecyclerView getRecyclerView() {
        return binding.recycler;
    }

    @Override
    public void showLoading(boolean show, boolean isRefresh) {
        if (isRefresh) {
            if (show) {
                binding.loading.show();
            } else {
                binding.loading.hide();
            }
        } else {
            adapter.showLoading(show);
        }
    }

    @Override
    public void showContent(ListContent<MutualFriend> content, boolean refresh) {
        if (content.shouldClear) {
            adapter.clear();
        }
        adapter.addItems(content.items);
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, getActivity())) {
            return;
        }
    }

    @Override
    public void openFriend(MutualFriend friend) {

    }

    private static class FriendsPresenter extends ListPresenter<MutualFriend, MutualFriendsDataSource> {

        public FriendsPresenter(MutualFriendsDataSource dataSource) {
            super(dataSource);
        }
    }

    private static class FriendsPresenterFactory implements PresenterFactory<FriendsPresenter> {

        final Context context;
        final String friendId;

        private FriendsPresenterFactory(Context context, String friendId) {
            this.context = context;
            this.friendId = friendId;
        }

        @Override
        public FriendsPresenter create() {
            String token = AccessToken.getCurrentAccessToken().getToken();
            return new FriendsPresenter(new MutualFriendsDataSource(TheDistanceApplication.getApplicationComponent(context), token, friendId));
        }
    }
}

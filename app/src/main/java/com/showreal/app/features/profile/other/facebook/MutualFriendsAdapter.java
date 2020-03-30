package com.showreal.app.features.profile.other.facebook;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.showreal.app.R;
import com.showreal.app.data.model.MutualFriend;
import com.showreal.app.databinding.ItemFriendLoadingBinding;
import com.showreal.app.databinding.ItemMutualFriendBinding;

import uk.co.thedistance.components.lists.AbsSortedListItemAdapterDelegate;
import uk.co.thedistance.components.lists.BindingViewHolder;
import uk.co.thedistance.components.lists.SortedListDelegationAdapter;
import uk.co.thedistance.components.lists.Sorter;
import uk.co.thedistance.components.lists.interfaces.Sortable;

public class MutualFriendsAdapter extends SortedListDelegationAdapter<Sortable> {

    private static final Sorter SORTER = new Sorter() {
        @Override
        public int compare(Sortable sortable, Sortable t1) {
            if (sortable instanceof LoadingItem) {
                if (t1 instanceof LoadingItem) {
                    return 0;
                }
                return 1;
            }
            if (t1 instanceof LoadingItem) {
                return -1;
            }
            return 0;
        }
    };

    private final MutualFriendViewModel.MutualFriendView friendView;

    public MutualFriendsAdapter(MutualFriendViewModel.MutualFriendView friendView, Context context) {
        super(Sortable.class, SORTER);

        this.friendView = friendView;
        LayoutInflater inflater = LayoutInflater.from(context);
        delegatesManager.addDelegate(new FriendAdapterDelegate(inflater, friendView));
        delegatesManager.addDelegate(new LoadingAdapterDelegate(inflater));
    }


    private final LoadingItem loadingItem = new LoadingItem();

    public void showLoading(boolean show) {
        if (show) {
            addItem(loadingItem);
        } else {
            removeItem(loadingItem);
        }
    }

    private static class FriendAdapterDelegate extends AbsSortedListItemAdapterDelegate<MutualFriend, Sortable, FriendViewHolder> {

        final LayoutInflater inflater;
        final MutualFriendViewModel.MutualFriendView friendView;

        private FriendAdapterDelegate(LayoutInflater inflater, MutualFriendViewModel.MutualFriendView friendView) {
            this.inflater = inflater;
            this.friendView = friendView;
        }

        @Override
        protected boolean isForViewType(@NonNull Sortable item, SortedList<Sortable> items, int position) {
            return item instanceof MutualFriend;
        }

        @NonNull
        @Override
        public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ItemMutualFriendBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_mutual_friend, parent, false);
            return new FriendViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull MutualFriend item, @NonNull FriendViewHolder viewHolder) {
            viewHolder.binding.setViewModel(new MutualFriendViewModel(item, friendView));
        }
    }

    private final class LoadingItem implements Sortable {

        @Override
        public boolean isSameItem(Sortable other) {
            return other instanceof LoadingItem;
        }

        @Override
        public boolean isSameContent(Sortable other) {
            return isSameItem(other);
        }
    }

    private static class LoadingAdapterDelegate extends AbsSortedListItemAdapterDelegate<LoadingItem, Sortable, LoadingViewHolder> {

        final LayoutInflater inflater;

        private LoadingAdapterDelegate(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        @Override
        protected boolean isForViewType(@NonNull Sortable item, SortedList<Sortable> items, int position) {
            return item instanceof LoadingItem;
        }

        @NonNull
        @Override
        public LoadingViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ItemFriendLoadingBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_friend_loading, parent, false);
            return new LoadingViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull LoadingItem item, @NonNull LoadingViewHolder viewHolder) {
        }
    }


    private static class FriendViewHolder extends BindingViewHolder<ItemMutualFriendBinding> {

        public FriendViewHolder(ItemMutualFriendBinding binding) {
            super(binding);
        }
    }

    private static class LoadingViewHolder extends BindingViewHolder<ItemFriendLoadingBinding> {

        public LoadingViewHolder(ItemFriendLoadingBinding binding) {
            super(binding);
        }
    }
}

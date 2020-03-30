package com.showreal.app.features.conversations.matches;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.showreal.app.R;
import com.showreal.app.data.model.Match;
import com.showreal.app.databinding.ItemMatchBinding;

import java.util.ArrayList;
import java.util.List;

import uk.co.thedistance.components.lists.AbsSortedListItemAdapterDelegate;
import uk.co.thedistance.components.lists.BindingViewHolder;
import uk.co.thedistance.components.lists.SortedListDelegationAdapter;
import uk.co.thedistance.components.lists.Sorter;
import uk.co.thedistance.components.lists.interfaces.Sortable;

class MatchAdapter extends SortedListDelegationAdapter<Match> {

    private static final Sorter SORTER = new Sorter() {
        @Override
        public int compare(Sortable sortable, Sortable t1) {
            Match match1 = (Match) sortable;
            Match match2 = (Match) t1;

            if (match1.hasSeenMatch == match2.hasSeenMatch) {
                if (match1.unreadCount == match2.unreadCount) {
                    return match1.createdAt.compareTo(match2.createdAt);
                }
                return Integer.valueOf(match1.unreadCount).compareTo(match2.unreadCount);
            } else {
                return Boolean.valueOf(match1.hasSeenMatch).compareTo(match2.hasSeenMatch);
            }
        }
    };
    private final LayoutInflater inflater;
    private List<Match> filteredOut = new ArrayList<>();

    @Override
    public long getItemId(int position) {
        return getItems().get(position).id;
    }

    MatchAdapter(Context context, MatchViewModel.MatchView matchView) {
        super(Match.class, SORTER);

        setHasStableIds(true);
        this.inflater = LayoutInflater.from(context);
        delegatesManager.addDelegate(new LikedAdapterDelegate(matchView, inflater));
    }

    void filter(String name) {
        name = name.toLowerCase();

        items.beginBatchedUpdates();
        items.addAll(filteredOut);
        filteredOut.clear();
        if (!TextUtils.isEmpty(name)) {
            for (int i = 0; i < items.size(); i++) {
                Match match = items.get(i);
                if (!match.profile.firstName.toLowerCase().startsWith(name)) {
                    filteredOut.add(match);
                }
            }
            for (Match match : filteredOut) {
                items.remove(match);
            }
        }
        items.endBatchedUpdates();
    }

    void update(String conversationUrl) {
        for (int i = 0; i < items.size(); i++) {
            Match match = items.get(i);
            if (match.conversationUrl.equals(conversationUrl)) {
                for (int j = 0; j < getItemCount(); j++) {
                    long itemId = getItemId(i);
                    if (itemId == match.id) {
                        notifyItemChanged(i);
                        return;
                    }
                }
                return;
            }
        }


    }

    private static class LikedAdapterDelegate extends AbsSortedListItemAdapterDelegate<Match, Match, MatchViewHolder> {

        final private MatchViewModel.MatchView matchView;
        final private LayoutInflater inflater;

        private LikedAdapterDelegate(MatchViewModel.MatchView matchView, LayoutInflater inflater) {
            this.matchView = matchView;
            this.inflater = inflater;
        }

        @Override
        protected boolean isForViewType(@NonNull Match item, SortedList<Match> items, int position) {
            return true;
        }

        @NonNull
        @Override
        public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ItemMatchBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_match, parent, false);
            return new MatchViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull Match item, @NonNull final MatchViewHolder viewHolder) {
            viewHolder.binding.setViewModel(new MatchViewModel(item, matchView));
        }
    }

    public static class MatchViewHolder extends BindingViewHolder<ItemMatchBinding> {

        MatchViewHolder(ItemMatchBinding binding) {
            super(binding);
        }
    }
}

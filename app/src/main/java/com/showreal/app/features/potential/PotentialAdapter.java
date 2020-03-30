package com.showreal.app.features.potential;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.showreal.app.R;
import com.showreal.app.data.model.Liked;
import com.showreal.app.data.model.Match;
import com.showreal.app.databinding.ItemPotentialBinding;

import uk.co.thedistance.components.lists.AbsSortedListItemAdapterDelegate;
import uk.co.thedistance.components.lists.BindingViewHolder;
import uk.co.thedistance.components.lists.SortedListDelegationAdapter;
import uk.co.thedistance.components.lists.Sorter;
import uk.co.thedistance.components.lists.interfaces.Sortable;

public class PotentialAdapter extends SortedListDelegationAdapter<Liked> {

    private static final Sorter SORTER = new Sorter() {
        @Override
        public int compare(Sortable sortable, Sortable t1) {
            return ((Liked) sortable).profile.lastOnline.compareTo(((Liked) t1).profile.lastOnline);
        }
    };
    private final LayoutInflater inflater;

    @Override
    public long getItemId(int position) {
        return getItems().get(position).profile.id;
    }

    public PotentialAdapter(Context context, PotentialViewModel.PotentialView potentialView) {
        super(Liked.class, SORTER);

        setHasStableIds(true);
        this.inflater = LayoutInflater.from(context);
        delegatesManager.addDelegate(new LikedAdapterDelegate(potentialView, inflater));
    }

    private static class LikedAdapterDelegate extends AbsSortedListItemAdapterDelegate<Liked, Liked, MatchViewHolder> {

        final private PotentialViewModel.PotentialView potentialView;
        final private LayoutInflater inflater;

        private LikedAdapterDelegate(PotentialViewModel.PotentialView potentialView, LayoutInflater inflater) {
            this.potentialView = potentialView;
            this.inflater = inflater;
        }

        @Override
        protected boolean isForViewType(@NonNull Liked item, SortedList<Liked> items, int position) {
            return true;
        }

        @NonNull
        @Override
        public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ItemPotentialBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_potential, parent, false);
            return new MatchViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull Liked item, @NonNull final MatchViewHolder viewHolder) {
            viewHolder.binding.setViewModel(new PotentialViewModel(item, potentialView));
        }
    }

    static class MatchViewHolder extends BindingViewHolder<ItemPotentialBinding> {

        public MatchViewHolder(ItemPotentialBinding binding) {
            super(binding);
        }
    }
}

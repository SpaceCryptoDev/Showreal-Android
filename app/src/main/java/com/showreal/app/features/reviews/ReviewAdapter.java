package com.showreal.app.features.reviews;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.percent.PercentRelativeLayout;
import android.support.v7.util.SortedList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.showreal.app.R;
import com.showreal.app.data.model.Profile;
import com.showreal.app.databinding.ItemRealBinding;
import com.showreal.app.databinding.ItemReviewHeaderBinding;

import java.util.Collections;
import java.util.List;

import uk.co.thedistance.components.lists.AbsSortedListItemAdapterDelegate;
import uk.co.thedistance.components.lists.BindingViewHolder;
import uk.co.thedistance.components.lists.SortedListDelegationAdapter;
import uk.co.thedistance.components.lists.Sorter;
import uk.co.thedistance.components.lists.interfaces.Sortable;

public class ReviewAdapter extends SortedListDelegationAdapter<Sortable> {

    private static final Sorter SORTER = new Sorter() {
        @Override
        public int compare(Sortable o1, Sortable o2) {
            if (o1 instanceof ReviewHeader && o2 instanceof ReviewHeader) {
                return Integer.valueOf(((ReviewHeader) o1).type).compareTo(((ReviewHeader) o2).type);
            }
            if (o2 instanceof ReviewHeader) {
                if (((ReviewItem) o1).type == ((ReviewHeader) o2).type) {
                    return 1;
                } else {
                    return Integer.valueOf(((ReviewItem) o1).type).compareTo(((ReviewHeader) o2).type);
                }
            } else if (o1 instanceof ReviewHeader) {
                if (((ReviewItem) o2).type == ((ReviewHeader) o1).type) {
                    return -1;
                } else {
                    return Integer.valueOf(((ReviewHeader) o1).type).compareTo(((ReviewItem) o2).type);
                }
            } else {
                return Integer.valueOf(((ReviewItem) o1).type).compareTo(((ReviewItem) o2).type);
            }
        }
    };
    private final ReviewViewModel.ReviewView view;


    public ReviewAdapter(Context context, ReviewViewModel.ReviewView view) {
        super(Sortable.class, SORTER);

        LayoutInflater inflater = LayoutInflater.from(context);
        delegatesManager.addDelegate(new ReviewDelegate(inflater, view));
        delegatesManager.addDelegate(new ReviewHeaderDelegate(inflater, context));
        this.view = view;
    }

    public void addPromo(Profile profile) {
        items.add(ReviewItem.with(profile, ReviewItem.Primary));
        if (pHeader == null) {
            items.add(pHeader = ReviewHeader.with(ReviewItem.Primary));
        }
    }

    private ReviewHeader pHeader;
    private ReviewHeader sHeader;

    @Override
    public long getItemId(int position) {
        if (items.get(position) instanceof ReviewItem) {
            return ((ReviewItem) items.get(position)).profile.id;
        }

        return super.getItemId(position);
    }

    public void addPrimary(List<Profile> primaryProfiles) {
        if (primaryProfiles.isEmpty()) {
            return;
        }

        for (Profile profile : primaryProfiles) {
            items.add(ReviewItem.with(profile, ReviewItem.Primary));
        }
        if (pHeader == null) {
            items.add(pHeader = ReviewHeader.with(ReviewItem.Primary));
        }
    }

    public void addSecondary(List<Profile> secondaryProfiles) {
        if (secondaryProfiles.isEmpty()) {
            return;
        }

        for (Profile profile : secondaryProfiles) {
            items.add(ReviewItem.with(profile, ReviewItem.Secondary));
        }
        if (sHeader == null) {
            items.add(sHeader = ReviewHeader.with(ReviewItem.Secondary));
        }
    }

    @Override
    public void clear() {
        super.clear();
        pHeader = null;
        sHeader = null;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void remove(int id) {
        for (int i = 0; i < items.size(); i++) {
            Sortable item = items.get(i);
            if (item instanceof ReviewItem && ((ReviewItem) item).profile.id == id) {
                removeItem(item);
            }
        }
    }

    @Override
    public void removeItem(Sortable item) {
        super.removeItem(item);

        if (item instanceof ReviewItem) {
            for (int i = 0; i < items.size(); i++) {
                Sortable sortable = items.get(i);
                if (sortable instanceof ReviewItem && ((ReviewItem) sortable).type == ((ReviewItem) item).type) {
                    return;
                }
            }
            if (((ReviewItem) item).type == ReviewItem.Primary) {
                super.removeItem(pHeader);
            } else {
                super.removeItem(sHeader);
            }
        }
    }

    public int getPositionForId(int profileId) {
        for (int i = 0; i < items.size(); i++) {
            Sortable sortable = items.get(i);
            if (sortable instanceof ReviewItem && ((ReviewItem) sortable).profile.id == profileId) {
                return i;
            }
        }
        return -1;
    }

    public int getReviewCount() {
        return getItemCount() - 1;
    }

    private static class ReviewDelegate extends AbsSortedListItemAdapterDelegate<ReviewItem, Sortable, ReviewItemViewHolder> {

        final LayoutInflater inflater;
        final ReviewViewModel.ReviewView view;

        private ReviewDelegate(LayoutInflater inflater, ReviewViewModel.ReviewView view) {
            this.inflater = inflater;
            this.view = view;
        }

        @Override
        protected boolean isForViewType(@NonNull Sortable item, SortedList<Sortable> items, int position) {
            return item instanceof ReviewItem;
        }

        @NonNull
        @Override
        public ReviewItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ItemRealBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_real, parent, false);
            return new ReviewItemViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull ReviewItem item, @NonNull ReviewItemViewHolder viewHolder) {
            viewHolder.binding.setViewModel(new ReviewViewModel(item.profile, view));

            PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) viewHolder.binding.buttonCut.getLayoutParams();
            params.rightMargin = (int) (0.07 * view.getViewWidth());

            params = (PercentRelativeLayout.LayoutParams) viewHolder.binding.buttonChance.getLayoutParams();
            params.leftMargin = (int) (0.07 * view.getViewWidth());

            viewHolder.binding.buttonCut.requestLayout();
            viewHolder.binding.buttonChance.requestLayout();

            if (item.profile.image != null) {
                viewHolder.binding.questionOverlay.setBackgroundColor(Color.TRANSPARENT);
                viewHolder.binding.question.setAlpha(0.0f);
                viewHolder.binding.sponsorImage.setAlpha(0.0f);
                Glide.with(viewHolder.binding.questionOverlay.getContext())
                        .load(item.profile.image)
                        .centerCrop()
                        .crossFade()
                        .into(viewHolder.binding.profileRealImage);
            } else {
                viewHolder.binding.questionOverlay.setBackgroundColor(Color.BLACK);
                Collections.sort(item.profile.videos);
                viewHolder.binding.question.setText(item.profile.videos.get(0).question.text);
                viewHolder.binding.question.setAlpha(1.0f);
                viewHolder.binding.sponsorImage.setAlpha(1.0f);
            }
        }
    }

    private static class ReviewItemViewHolder extends BindingViewHolder<ItemRealBinding> {

        public ReviewItemViewHolder(ItemRealBinding binding) {
            super(binding);
        }
    }

    private static class ReviewHeaderDelegate extends AbsSortedListItemAdapterDelegate<ReviewHeader, Sortable, ReviewHeaderViewHolder> {

        final LayoutInflater inflater;
        private final Context context;

        private ReviewHeaderDelegate(LayoutInflater inflater, Context context) {
            this.inflater = inflater;
            this.context = context;
        }

        @Override
        protected boolean isForViewType(@NonNull Sortable item, SortedList<Sortable> items, int position) {
            return item instanceof ReviewHeader;
        }

        @NonNull
        @Override
        public ReviewHeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ItemReviewHeaderBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_review_header, parent, false);
            return new ReviewHeaderViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull ReviewHeader item, @NonNull ReviewHeaderViewHolder viewHolder) {
            viewHolder.binding.text.setText(item.getHeader(context));
        }
    }

    private static class ReviewHeaderViewHolder extends BindingViewHolder<ItemReviewHeaderBinding> {

        public ReviewHeaderViewHolder(ItemReviewHeaderBinding binding) {
            super(binding);
        }
    }

}

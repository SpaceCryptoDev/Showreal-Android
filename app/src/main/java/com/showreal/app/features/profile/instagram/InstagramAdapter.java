package com.showreal.app.features.profile.instagram;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.util.SortedList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.showreal.app.R;
import com.showreal.app.data.model.InstagramMedia;
import com.showreal.app.databinding.ItemInstagramBinding;
import com.showreal.app.databinding.ItemLoadingBinding;

import java.util.ArrayList;
import java.util.List;

import uk.co.thedistance.components.lists.AbsSortedListItemAdapterDelegate;
import uk.co.thedistance.components.lists.BindingViewHolder;
import uk.co.thedistance.components.lists.SortedListDelegationAdapter;
import uk.co.thedistance.components.lists.Sorter;
import uk.co.thedistance.components.lists.interfaces.Sortable;

public class InstagramAdapter extends SortedListDelegationAdapter<Sortable> {

    private static final Sorter SORTER = new Sorter() {
        @Override
        public int compare(Sortable o1, Sortable o2) {
            if (o1 instanceof InstagramMedia.Image && o2 instanceof InstagramMedia.Image) {
                return Long.valueOf(((InstagramMedia.Image) o2).createdTime).compareTo(((InstagramMedia.Image) o1).createdTime);
            }
            if (o1 instanceof InstagramMedia.Image) {
                return -1;
            }

            return 1;
        }
    };

    public InstagramAdapter(Context context, InstagramViewModel.InstagramView instagramView) {
        super(Sortable.class, SORTER);

        delegatesManager.addDelegate(new ImageDelegate(context, instagramView));
        delegatesManager.addDelegate(new LoadingDelegate(context));
    }

    private final LoadingObject loadingObject = new LoadingObject();

    public void showLoading(boolean show) {
        if (show) {
            items.add(loadingObject);
        } else {
            items.remove(loadingObject);
        }
    }

    public List<InstagramMedia.Image> getImages() {
        List<InstagramMedia.Image> images = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Sortable item = items.get(i);
            if (item instanceof InstagramMedia.Image) {
                images.add((InstagramMedia.Image) item);
            }
        }
        return images;
    }

    private static class ImageDelegate extends AbsSortedListItemAdapterDelegate<InstagramMedia.Image, Sortable, ImageViewHolder> {

        final Context context;
        private final LayoutInflater inflater;
        private final View.OnClickListener clickListener;

        private ImageDelegate(Context context, final InstagramViewModel.InstagramView instagramView) {
            this.context = context;
            inflater = LayoutInflater.from(context);

            clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    instagramView.openImage(v);
                }
            };
        }

        @Override
        protected boolean isForViewType(@NonNull Sortable item, SortedList<Sortable> items, int position) {
            return item instanceof InstagramMedia.Image;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ItemInstagramBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_instagram, parent, false);
            return new ImageViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull InstagramMedia.Image item, @NonNull ImageViewHolder viewHolder) {
            viewHolder.binding.image.setTag(R.id.photo_position, viewHolder.getAdapterPosition());
            Glide.with(context)
                    .load(item.thumbnail.url)
                    .crossFade()
                    .centerCrop()
                    .into(viewHolder.binding.image);
            ViewCompat.setTransitionName(viewHolder.binding.image, context.getResources().getString(R.string.transition_name_photo, viewHolder.getAdapterPosition()));

            viewHolder.binding.image.setOnClickListener(clickListener);

        }
    }

    public class LoadingObject implements Sortable {

        @Override
        public boolean isSameItem(Sortable other) {
            return other instanceof LoadingObject;
        }

        @Override
        public boolean isSameContent(Sortable other) {
            return isSameItem(other);
        }
    }

    private static class LoadingDelegate extends AbsSortedListItemAdapterDelegate<LoadingObject, Sortable, LoadingHolder> {

        final Context context;
        private final LayoutInflater inflater;

        private LoadingDelegate(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
        }

        @Override
        protected boolean isForViewType(@NonNull Sortable item, SortedList<Sortable> items, int position) {
            return item instanceof LoadingObject;
        }

        @NonNull
        @Override
        public LoadingHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ItemLoadingBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_loading, parent, false);
            return new LoadingHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull LoadingObject item, @NonNull LoadingHolder viewHolder) {
        }
    }

    private static class ImageViewHolder extends BindingViewHolder<ItemInstagramBinding> {

        public ImageViewHolder(ItemInstagramBinding binding) {
            super(binding);
        }
    }

    private static class LoadingHolder extends BindingViewHolder<ItemLoadingBinding> {

        public LoadingHolder(ItemLoadingBinding binding) {
            super(binding);
        }
    }
}

package com.showreal.app.features.real.myreal;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hannesdorfmann.adapterdelegates2.AbsListItemAdapterDelegate;
import com.hannesdorfmann.adapterdelegates2.ListDelegationAdapter;
import com.showreal.app.R;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.VideoHelper;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Video;
import com.showreal.app.databinding.ItemClipBinding;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import uk.co.thedistance.components.lists.BindingViewHolder;

public class LibraryAdapter extends ListDelegationAdapter<List<ShowRealVideo>> {

    private final MyRealViewModel.MyRealView myRealView;

    private final VideoHelper videoHelper;
    private final Profile profile;

    public void setItemAdded(int pos, boolean added) {
        if (pos <= items.size() - 1) {
            ShowRealVideo video = items.get(pos);
            video.added = added;
            notifyItemChanged(pos);
        }
    }

    public void setItemAdded(ShowRealVideo video, boolean added) {
        int pos = -1;
        for (int i = 0; i < items.size(); i++) {
            ShowRealVideo realVideo = items.get(i);
            if (realVideo.video._id.equals(video.video._id)) {
                realVideo.added = added;
                pos = i;
                break;
            }
        }
        if (pos > -1 && pos <= items.size() - 1) {
            notifyItemChanged(pos);
        }
    }

    public void addItems(List<Video> newItems) {
        List<ShowRealVideo> videos = new ArrayList<>(items.size());
        for (Video video : newItems) {
            String path = videoHelper.getPlaybackPath(profile, video);
            ShowRealVideo showRealVideo = new ShowRealVideo(video, video.question, path);
            videos.add(showRealVideo);

            boolean added = false;
            for (int i = 0; i < items.size(); i++) {
                ShowRealVideo existing = items.get(i);
                if (existing.question.id == showRealVideo.question.id) {
                    items.set(i, showRealVideo);
                    added = true;
                    notifyItemChanged(i);
                    break;
                }
            }
            if (!added) {
                items.add(showRealVideo);
                notifyItemInserted(items.size() - 1);
            }
        }
    }

    public void addItem(Video video) {
        String path = videoHelper.getPlaybackPath(profile, video);
        ShowRealVideo showRealVideo = new ShowRealVideo(video, video.question, path);

        boolean added = false;
        for (int i = 0; i < items.size(); i++) {
            ShowRealVideo existing = items.get(i);
            if (existing.question.id == showRealVideo.question.id) {
                items.set(i, showRealVideo);
                added = true;
                notifyItemChanged(i);
                break;
            }
        }
        if (!added) {
            items.add(showRealVideo);
            notifyItemInserted(items.size() - 1);
        }
    }

    private LibraryGestureDetector gestureDetector;

    static {
        try {
            Field field = GestureDetector.class.getDeclaredField("LONGPRESS_TIMEOUT");
            setFinalStatic(field, 150);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        field.set(null, newValue);
    }

    public LibraryAdapter(Context context, Profile profile, MyRealViewModel.MyRealView realView) {
        LayoutInflater inflater = LayoutInflater.from(context);
        videoHelper = TheDistanceApplication.getApplicationComponent(context).videoHelper();
        myRealView = realView;
        this.profile = profile;

        gestureDetector = new LibraryGestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                ItemClipBinding binding = DataBindingUtil.findBinding(gestureDetector.view);
                int position = (int) binding.thumbnail.getTag(R.id.position);
                myRealView.selectClip(position, binding);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                ItemClipBinding binding = DataBindingUtil.findBinding(gestureDetector.view);
                int position = (int) binding.thumbnail.getTag(R.id.position);
                if (items.get(position).added) {
                    return;
                }
                myRealView.startLibraryDrag(gestureDetector.view, position);
            }
        });


        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(view, motionEvent);
            }
        };

        delegatesManager.addDelegate(new VideoDelegate(inflater, touchListener, profile));
    }

    public void setItems(List<Video> items) {
        List<ShowRealVideo> videos = new ArrayList<>(items.size());
        for (Video video : items) {
            String path = videoHelper.getPlaybackPath(profile, video);
            ShowRealVideo showRealVideo = new ShowRealVideo(video, video.question, path);
            showRealVideo.added = video.reelId != 0;
            videos.add(showRealVideo);
        }
        setItems(videos);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        ((VideoViewHolder) holder).binding.thumbnail.setTag(R.id.position, position);
        ((VideoViewHolder) holder).binding.thumbnail.setTag(R.id.drag_type, MyRealActivity.MyRealFragment.DragType.Library);
    }

    private static class VideoDelegate extends AbsListItemAdapterDelegate<ShowRealVideo, ShowRealVideo, VideoViewHolder> {

        private final LayoutInflater inflater;
        private final View.OnTouchListener touchListener;
        private final Profile profile;

        private VideoDelegate(LayoutInflater inflater, View.OnTouchListener touchListener, Profile profile) {
            this.inflater = inflater;
            this.touchListener = touchListener;
            this.profile = profile;
        }

        @Override
        protected boolean isForViewType(@NonNull ShowRealVideo item, List<ShowRealVideo> items, int position) {
            return true;
        }

        @NonNull
        @Override
        public VideoViewHolder onCreateViewHolder(ViewGroup parent) {
            ItemClipBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_clip, parent, false);
            return new VideoViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull ShowRealVideo item, @NonNull VideoViewHolder viewHolder) {
            viewHolder.binding.overlay.setVisibility(item.added ? View.VISIBLE : View.GONE);
            viewHolder.binding.thumbnail.setOnTouchListener(touchListener);
            viewHolder.binding.thumbnail.setTag(R.id.item, item);

            if (item.question.colour != null && !item.question.colour.equals("000000")) {
                String colorString = "#" + item.question.colour;
                try {
                    int color = Color.parseColor(colorString);
                    viewHolder.binding.bottom.setBackgroundColor(color);
                } catch (IllegalArgumentException ignored) {
                }
            } else {
                viewHolder.binding.bottom.setBackgroundResource(R.color.black87);
            }

            Glide.with(viewHolder.binding.getRoot().getContext())
                    .load(new VideoThumbailLoader.VideoThumbnail(profile, item))
                    .asBitmap()
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .centerCrop()
                    .into(viewHolder.binding.thumbnail);
        }

    }

    public static class VideoViewHolder extends BindingViewHolder<ItemClipBinding> {

        public VideoViewHolder(ItemClipBinding binding) {
            super(binding);
        }
    }
}

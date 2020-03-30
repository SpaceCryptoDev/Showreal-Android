package com.showreal.app.features.real.myreal;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hannesdorfmann.adapterdelegates2.AbsListItemAdapterDelegate;
import com.hannesdorfmann.adapterdelegates2.ListDelegationAdapter;
import com.showreal.app.R;
import com.showreal.app.data.VideoHelper;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Video;
import com.showreal.app.databinding.ItemSegmentBinding;
import com.showreal.app.databinding.ItemSegmentEmptyBinding;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import uk.co.thedistance.components.lists.BindingViewHolder;

public class SegmentAdapter extends ListDelegationAdapter<List<Object>> {

    private final MyRealActivity.MyRealFragment.SegmentsDragListener dragListener;
    private final SegmentGestureDetector gestureDetector;
    private final MyRealViewModel.MyRealView realView;
    private int selected = -1;
    private RecyclerView recyclerView;

    static {
        try {
            Field field = GestureDetector.class.getDeclaredField("LONGPRESS_TIMEOUT");
            setFinalStatic(field, 150);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean handlingDrag;

    public boolean isHandlingDrag() {
        return handlingDrag;
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        field.set(null, newValue);
    }

    public SegmentAdapter(Context context, MyRealActivity.MyRealFragment.SegmentsDragListener dragListener, Profile profile, MyRealViewModel.MyRealView realView) {
        LayoutInflater inflater = LayoutInflater.from(context);
        this.dragListener = dragListener;
        this.realView = realView;

        gestureDetector = new SegmentGestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                SegmentAdapter.this.realView.selectSegment(gestureDetector.view);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                SegmentAdapter.this.realView.startSegmentDrag(gestureDetector.view);
            }
        });


        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(view, motionEvent);
            }
        };

        delegatesManager.addDelegate(new VideoDelegate(inflater, profile, touchListener));
        delegatesManager.addDelegate(new EmptyDelegate(inflater, dragListener));

        items = new ArrayList<>();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        if (holder instanceof SegmentViewHolder) {
            ((SegmentViewHolder) holder).binding.thumbnail.setTag(R.id.position, position);
            ((SegmentViewHolder) holder).binding.thumbnail.setTag(R.id.drag_type, MyRealActivity.MyRealFragment.DragType.Segment);
            ((SegmentViewHolder) holder).binding.getRoot().setOnDragListener(new DragListener());
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;

        setRecyclerWidth();
    }

    private void setRecyclerWidth() {
        if (recyclerView == null) {
            return;
        }

        if (items.contains(dropBox) && items.size() == 1) {
            items.remove(dropBox);
            notifyItemRemoved(0);
        } else if (!items.contains(dropBox) && items.size() > 0) {
            items.add(dropBox);
            notifyItemInserted(items.size() - 1);
        }

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) recyclerView.getLayoutParams();
        params.width = getItemCount() == 0 ? FrameLayout.LayoutParams.MATCH_PARENT : FrameLayout.LayoutParams.WRAP_CONTENT;
        recyclerView.setLayoutParams(params);
        recyclerView.requestLayout();
        recyclerView.setBackgroundResource(getItemCount() == 0 ? R.drawable.etched_box : android.R.color.transparent);
        recyclerView.setOnDragListener(getItemCount() == 0 ? dragListener : null);
    }

    private final DropBox dropBox = new DropBox();

    @Override
    public void setItems(List<Object> items) {
        Collections.sort(items, new Comparator<Object>() {
            @Override
            public int compare(Object o, Object t1) {
                return Integer.valueOf(((ShowRealVideo) o).question.index).compareTo(((ShowRealVideo) t1).question.index);
            }
        });
        super.setItems(items);
        setRecyclerWidth();
    }

    public void setVideos(List<Video> items, VideoHelper videoHelper, Profile profile) {
        List<ShowRealVideo> videos = new ArrayList<>(items.size());
        for (Video video : items) {
            String path = videoHelper.getPlaybackPath(profile, video);
            ShowRealVideo showRealVideo = new ShowRealVideo(video, video.question, path);
            showRealVideo.added = true;
            videos.add(showRealVideo);
        }
        setItems(new ArrayList<Object>(videos));
        notifyDataSetChanged();
    }

    public void addVideo(ShowRealVideo video) {
        items.add(items.size() == 0 ? 0 : items.size() - 1, video);
        setRecyclerWidth();
        notifyItemInserted(items.size() - 1);
    }

    public void moveSegment(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(items, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(items, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    public void removeSegment(int position) {
        items.remove(position);
        setRecyclerWidth();
        notifyItemRemoved(position);
    }

    public void selectSegment(int position) {
        int old = selected;
        selected = position;
        notifyItemChanged(old);
        notifyItemChanged(position);
    }

    private class VideoDelegate extends AbsListItemAdapterDelegate<ShowRealVideo, Object, SegmentViewHolder> {

        private final LayoutInflater inflater;
        private final Profile profile;
        private final View.OnTouchListener touchListener;

        private VideoDelegate(LayoutInflater inflater, Profile profile, View.OnTouchListener touchListener) {
            this.inflater = inflater;
            this.profile = profile;
            this.touchListener = touchListener;
        }

        @Override
        protected boolean isForViewType(@NonNull Object item, List<Object> items, int position) {
            return item instanceof ShowRealVideo;
        }

        @NonNull
        @Override
        public SegmentViewHolder onCreateViewHolder(ViewGroup parent) {
            ItemSegmentBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_segment, parent, false);
            return new SegmentViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull ShowRealVideo item, @NonNull SegmentViewHolder viewHolder) {
            viewHolder.binding.thumbnail.setTag(R.id.item, item);
            viewHolder.video = item;
            viewHolder.binding.overlay.setActivated(items.indexOf(item) == selected);
            viewHolder.binding.thumbnail.setOnTouchListener(touchListener);

            if (item.question.colour != null && !item.question.colour.equals("000000")) {
                String colorString = "#" + item.question.colour;
                try {
                    int color = Color.parseColor(colorString);

                    ShapeDrawable drawable = new ShapeDrawable();
                    drawable.getPaint().setStyle(Paint.Style.STROKE);
                    drawable.getPaint().setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, viewHolder.binding.getRoot().getResources().getDisplayMetrics()));
                    drawable.getPaint().setColor(color);
                    viewHolder.binding.overlay.setBackground(drawable);
                } catch (IllegalArgumentException ignored) {
                    viewHolder.binding.overlay.setBackground(null);
                }
            } else {
                viewHolder.binding.overlay.setBackground(null);
            }

            Glide.with(viewHolder.binding.getRoot().getContext())
                    .load(new VideoThumbailLoader.VideoThumbnail(profile, item))
                    .fitCenter()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .centerCrop()
                    .into(viewHolder.binding.thumbnail);
        }
    }

    private class DragListener extends MyRealActivity.MyRealFragment.SRDragListener {

        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            super.onDrag(view, dragEvent);
            SegmentAdapter.this.handlingDrag = isHandlingDrag();

            RecyclerView.ViewHolder holder = recyclerView.findContainingViewHolder(view);

            switch (dragEvent.getAction()) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (holder != null) {
                        ((SegmentViewHolder) holder).binding.overlay.setActivated(true);
                    }
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    if (holder != null) {
                        ((SegmentViewHolder) holder).binding.overlay.setActivated(false);
                    }
                    break;
                case DragEvent.ACTION_DROP:
                    if (dragType == MyRealActivity.MyRealFragment.DragType.Segment) {
                        if (holder != null) {
                            realView.moveSegment(position, holder.getAdapterPosition());
                        }
                        SegmentAdapter.this.handlingDrag = false;
                    }
                    break;
            }
            return true;
        }
    }

    private static class DropBox {

    }

    private static class EmptyDelegate extends AbsListItemAdapterDelegate<DropBox, Object, EmptyViewHolder> {
        private final LayoutInflater inflater;
        private final MyRealActivity.MyRealFragment.SegmentsDragListener dragListener;

        private EmptyDelegate(LayoutInflater inflater, MyRealActivity.MyRealFragment.SegmentsDragListener dragListener) {
            this.inflater = inflater;
            this.dragListener = dragListener;
        }

        @Override
        protected boolean isForViewType(@NonNull Object item, List<Object> items, int position) {
            return item instanceof DropBox;
        }

        @NonNull
        @Override
        public EmptyViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ItemSegmentEmptyBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_segment_empty, parent, false);
            return new EmptyViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull DropBox item, @NonNull EmptyViewHolder viewHolder) {
            viewHolder.binding.dropBox.setOnDragListener(dragListener);
        }
    }

    public static class SegmentViewHolder extends BindingViewHolder<ItemSegmentBinding> {

        ShowRealVideo video;

        public SegmentViewHolder(ItemSegmentBinding binding) {
            super(binding);
        }
    }

    private static class EmptyViewHolder extends BindingViewHolder<ItemSegmentEmptyBinding> {

        public EmptyViewHolder(ItemSegmentEmptyBinding binding) {
            super(binding);
        }
    }
}
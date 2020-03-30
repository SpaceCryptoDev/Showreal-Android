package com.showreal.app.features.conversations;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import com.showreal.app.BR;
import com.showreal.app.R;
import com.showreal.app.data.model.Match;
import com.showreal.app.data.model.Message;
import com.showreal.app.databinding.ItemChatDividerBinding;
import com.showreal.app.databinding.ItemChatLeftMediaBinding;
import com.showreal.app.databinding.ItemChatRightMediaBinding;
import com.showreal.app.databinding.MediaContentBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import uk.co.thedistance.components.lists.AbsSortedListItemAdapterDelegate;
import uk.co.thedistance.components.lists.BindingViewHolder;
import uk.co.thedistance.components.lists.SortedListDelegationAdapter;
import uk.co.thedistance.components.lists.Sorter;
import uk.co.thedistance.components.lists.interfaces.Sortable;

public class ConversationAdapter extends SortedListDelegationAdapter<Sortable> {

    private static final Sorter SORTER = new Sorter() {
        @Override
        public int compare(Sortable sortable, Sortable t1) {
            Date date1;
            Date date2;
            if (sortable instanceof Message) {
                date1 = ((Message) sortable).timeSent;
            } else {
                date1 = ((Divider) sortable).date;
            }
            if (t1 instanceof Message) {
                date2 = ((Message) t1).timeSent;
            } else {
                date2 = ((Divider) t1).date;
            }

            return date2.compareTo(date1);
        }
    };

    private SparseArray<Divider> dividers = new SparseArray<>();

    public ConversationAdapter(Context context, Match match, MediaViewModel.MediaView mediaView) {
        super(Sortable.class, SORTER);

        LayoutInflater inflater = LayoutInflater.from(context);
        delegatesManager.addDelegate(new MessageTextDelegate(inflater, match, true));
        delegatesManager.addDelegate(new MessageTextDelegate(inflater, match, false));
        delegatesManager.addDelegate(new MediaMessageLeftDelegate(inflater, match, mediaView));
        delegatesManager.addDelegate(new MediaMessageRightDelegate(inflater, match, mediaView));
        delegatesManager.addDelegate(new DividerDelegate(inflater));
    }

    private final Calendar calendar = Calendar.getInstance();

    private void addDividerForDate(Date date) {
        calendar.setTime(date);
        int day = calendar.get(Calendar.DAY_OF_YEAR) + (calendar.get(Calendar.YEAR) * 365);
        Log.i("DAY", "" + day);
        Divider divider = dividers.get(day);

        if (divider == null) {
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            divider = new Divider(calendar.getTime());
            items.add(divider);
            dividers.put(day, divider);
        }
    }

    @Override
    public void addItem(Sortable item) {
        super.addItem(item);

        if (item instanceof Message) {
            addDividerForDate(((Message) item).timeSent);
        }
    }

    @Override
    public void addItems(List<? extends Sortable> newItems) {
        super.addItems(newItems);

        for (Sortable item : newItems) {
            if (item instanceof Message) {
                addDividerForDate(((Message) item).timeSent);
            }
        }
    }

    private static class MessageTextDelegate extends AbsSortedListItemAdapterDelegate<Message, Sortable, MessageViewHolder> {

        final LayoutInflater inflater;
        private final Match match;
        private final boolean user;

        private MessageTextDelegate(LayoutInflater inflater, Match match, boolean user) {
            this.inflater = inflater;
            this.match = match;
            this.user = user;
        }

        @Override
        protected boolean isForViewType(@NonNull Sortable item, SortedList<Sortable> items, int position) {
            return item instanceof Message && ((Message) item).mediaType == null && ((Message) item).sender.equals(match.profile.chatId) != user;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ViewDataBinding binding = DataBindingUtil.inflate(inflater, user ? R.layout.item_chat_left : R.layout.item_chat_right, parent, false);
            return new MessageViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull Message item, @NonNull MessageViewHolder viewHolder) {
            viewHolder.binding.setVariable(BR.viewModel, new MessageViewModel(item));
        }
    }

    private static class MediaMessageLeftDelegate extends AbsSortedListItemAdapterDelegate<Message, Sortable, MediaMessageLeftViewHolder> {

        final LayoutInflater inflater;
        private final Match match;
        private final MediaViewModel.MediaView mediaView;

        private MediaMessageLeftDelegate(LayoutInflater inflater, Match match, MediaViewModel.MediaView mediaView) {
            this.inflater = inflater;
            this.match = match;
            this.mediaView = mediaView;
        }

        @Override
        protected boolean isForViewType(@NonNull Sortable item, SortedList<Sortable> items, int position) {
            return item instanceof Message && ((Message) item).mediaType != null && !((Message) item).sender.equals(match.profile.chatId);
        }

        @NonNull
        @Override
        public MediaMessageLeftViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ItemChatLeftMediaBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_chat_left_media, parent, false);
            return new MediaMessageLeftViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull final Message item, @NonNull MediaMessageLeftViewHolder viewHolder) {
            if (viewHolder.binding.stub.getViewStub() != null) {
                viewHolder.binding.stub.setOnInflateListener(new ViewStub.OnInflateListener() {
                    @Override
                    public void onInflate(ViewStub viewStub, View view) {
                        ViewDataBinding mediaBinding = DataBindingUtil.getBinding(view);
                        mediaBinding.setVariable(BR.viewModel, new MediaViewModel(item, mediaView));
                    }
                });

                viewHolder.binding.stub.getViewStub().inflate();
            } else if (viewHolder.binding.stub.getBinding() != null) {
                MediaContentBinding mediaBinding = (MediaContentBinding) viewHolder.binding.stub.getBinding();
                mediaBinding.setViewModel(new MediaViewModel(item, mediaView));
            }

            viewHolder.binding.setViewModel(new MessageViewModel(item));
        }
    }

    private static class MediaMessageRightDelegate extends AbsSortedListItemAdapterDelegate<Message, Sortable, MediaMessageRightViewHolder> {

        final LayoutInflater inflater;
        private final Match match;
        private final MediaViewModel.MediaView mediaView;

        private MediaMessageRightDelegate(LayoutInflater inflater, Match match, MediaViewModel.MediaView mediaView) {
            this.inflater = inflater;
            this.match = match;
            this.mediaView = mediaView;
        }

        @Override
        protected boolean isForViewType(@NonNull Sortable item, SortedList<Sortable> items, int position) {
            return item instanceof Message && ((Message) item).mediaType != null && ((Message) item).sender.equals(match.profile.chatId);
        }

        @NonNull
        @Override
        public MediaMessageRightViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ItemChatRightMediaBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_chat_right_media, parent, false);
            return new MediaMessageRightViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull final Message item, @NonNull MediaMessageRightViewHolder viewHolder) {
            if (viewHolder.binding.stub.getViewStub() != null) {
                viewHolder.binding.stub.setOnInflateListener(new ViewStub.OnInflateListener() {
                    @Override
                    public void onInflate(ViewStub viewStub, View view) {
                        ViewDataBinding mediaBinding = DataBindingUtil.getBinding(view);
                        mediaBinding.setVariable(BR.viewModel, new MediaViewModel(item, mediaView));
                    }
                });

                viewHolder.binding.stub.getViewStub().inflate();
            } else if (viewHolder.binding.stub.getBinding() != null) {
                MediaContentBinding mediaBinding = (MediaContentBinding) viewHolder.binding.stub.getBinding();
                mediaBinding.setViewModel(new MediaViewModel(item, mediaView));
            }

            viewHolder.binding.setViewModel(new MessageViewModel(item));
        }
    }

    private static class DividerDelegate extends AbsSortedListItemAdapterDelegate<Divider, Sortable, DividerViewHolder> {

        final LayoutInflater inflater;

        private DividerDelegate(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        @Override
        protected boolean isForViewType(@NonNull Sortable item, SortedList<Sortable> items, int position) {
            return item instanceof Divider;
        }

        @NonNull
        @Override
        public DividerViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            ItemChatDividerBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_chat_divider, parent, false);
            return new DividerViewHolder(binding);
        }

        @Override
        protected void onBindViewHolder(@NonNull Divider item, @NonNull DividerViewHolder viewHolder) {
            viewHolder.binding.setDivider(item);
        }
    }

    private static class MessageViewHolder extends BindingViewHolder<ViewDataBinding> {


        public MessageViewHolder(ViewDataBinding binding) {
            super(binding);
        }
    }

    private static class MediaMessageLeftViewHolder extends BindingViewHolder<ItemChatLeftMediaBinding> {

        public MediaMessageLeftViewHolder(ItemChatLeftMediaBinding binding) {
            super(binding);
        }
    }

    private static class MediaMessageRightViewHolder extends BindingViewHolder<ItemChatRightMediaBinding> {

        public MediaMessageRightViewHolder(ItemChatRightMediaBinding binding) {
            super(binding);
        }
    }

    public static class Divider implements Sortable {
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, MMM dd");

        final Date date;

        private Divider(Date date) {
            this.date = date;
        }

        public String getText() {
            return DATE_FORMAT.format(date);
        }

        @Override
        public boolean isSameItem(Sortable other) {
            return equals(other);
        }

        @Override
        public boolean isSameContent(Sortable other) {
            return equals(other);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Divider divider = (Divider) o;

            return date.equals(divider.date);

        }
    }

    private static class DividerViewHolder extends BindingViewHolder<ItemChatDividerBinding> {

        public DividerViewHolder(ItemChatDividerBinding binding) {
            super(binding);
        }
    }
}

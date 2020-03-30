package com.showreal.app.features.conversations;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;

import com.showreal.app.data.model.Message;

public class MediaViewModel extends BaseObservable {

    interface MediaView {
        void openMedia(Message message);
    }

    private final Message message;
    private final MediaView mediaView;

    public MediaViewModel(Message message, MediaView mediaView) {
        this.message = message;
        this.mediaView = mediaView;
    }

    @Bindable
    public String getUrl() {
        return message.mediaUrl;
    }

    @Bindable
    public Message getMessage() {
        return message;
    }

    public void onClick(View view) {
        mediaView.openMedia(message);
    }
}

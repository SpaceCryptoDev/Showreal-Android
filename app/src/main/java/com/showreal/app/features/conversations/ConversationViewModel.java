package com.showreal.app.features.conversations;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableField;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;

import com.showreal.app.BR;
import com.showreal.app.data.model.Match;

import java.util.Date;


public class ConversationViewModel extends BaseObservable {

    private boolean available;

    public void setAvailable(boolean available) {
        this.available = available;
        notifyPropertyChanged(BR.sendEnabled);
        notifyPropertyChanged(BR.cameraEnabled);

    }

    interface ConversationView {
        void sendMessage(String text);

        void chooseMedia();

        void openProfile(Match match);
    }

    final Match match;
    private final ConversationView conversationView;
    public final ObservableField<String> message = new ObservableField<>("");

    public ConversationViewModel(Match match, ConversationView conversationView) {
        this.match = match;
        this.conversationView = conversationView;

    }

    @Bindable
    public String getName() {
        return match.profile.firstName;
    }

    @Bindable
    public String getLastOnline() {
        long time = match.profile.lastOnline.getTime();
        return String.format("Last online: %s", (time < DateUtils.MINUTE_IN_MILLIS ? "Just now" :
                DateUtils.getRelativeTimeSpanString(time, new Date().getTime(), DateUtils.MINUTE_IN_MILLIS).toString()));
    }

    @Bindable
    public String getImage() {
        return match.profile.image;
    }


    public void onMessageChanged(CharSequence sequence, int start, int before, int count) {
        message.set(sequence.toString());
        notifyPropertyChanged(BR.sendEnabled);
    }

    public void onSend(View view) {
        if (TextUtils.isEmpty(message.get())) {
            return;
        }
        conversationView.sendMessage(message.get());
        message.set("");
    }

    public void onCamera(View view) {
        conversationView.chooseMedia();
    }

    @Bindable
    public boolean isSendEnabled() {
        return !TextUtils.isEmpty(message.get()) && available;
    }

    @Bindable
    public boolean isCameraEnabled() {
        return available;
    }

    public void onProfile(View view) {
        conversationView.openProfile(match);
    }
}

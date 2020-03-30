package com.showreal.app.features.conversations.matches;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;

import com.showreal.app.BR;
import com.showreal.app.R;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.SendBirdHelper;
import com.showreal.app.data.model.Match;


public class MatchViewModel extends BaseObservable {

    private final MatchView matchView;
    private final SendBirdHelper sendbird;

    interface MatchView {
        void openMatch(Match match);

        Context getImageContext();
    }

    private final Match match;

    public MatchViewModel(Match match, MatchView matchView) {
        this.match = match;
        this.matchView = matchView;

        sendbird = TheDistanceApplication.getApplicationComponent(matchView.getImageContext()).sendbird();
    }

    @Bindable
    public String getName() {
        return match.profile.firstName;
    }

    @Bindable
    public String getImage() {
        return match.profile.image;
    }

    public void onClick(View view) {
        matchView.openMatch(match);
    }

    @Bindable
    public Drawable getBubbleImage() {
        int count = sendbird.getUnreadCount(match.conversationUrl);
        return ContextCompat.getDrawable(matchView.getImageContext(), (!TextUtils.isEmpty(match.conversationUrl) && count > 0) ? R.drawable.ic_bubble_filled : R.drawable.ic_bubble_empty);
    }

    @Bindable
    public String getUnreadCount() {
        if (TextUtils.isEmpty(match.conversationUrl)) {
            return "0";
        }
        int count = sendbird.getUnreadCount(match.conversationUrl);
        return count > 0 ? String.valueOf(count) : "";
    }

    public void updateCount() {
        notifyPropertyChanged(BR.unreadCount);
    }
}

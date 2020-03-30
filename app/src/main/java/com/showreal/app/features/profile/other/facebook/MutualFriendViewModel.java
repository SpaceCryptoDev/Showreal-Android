package com.showreal.app.features.profile.other.facebook;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import com.showreal.app.data.model.MutualFriend;

public class MutualFriendViewModel extends BaseObservable {

    public MutualFriendViewModel(MutualFriend friend, MutualFriendView friendView) {
        this.friend = friend;
        this.friendView = friendView;
    }

    interface MutualFriendView {
        void openFriend(MutualFriend friend);
    }

    private final MutualFriend friend;
    private final MutualFriendView friendView;

    @Bindable
    public String getName() {
        return friend.name.split(" ")[0];
    }

    @Bindable
    public String getImage() {
        return friend.imageUrl;
    }
}

package com.showreal.app.features.profile.other.facebook;

import android.text.TextUtils;

import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.MutualFriend;
import com.showreal.app.data.model.MutualFriends;
import com.showreal.app.injection.ApplicationComponent;

import java.util.ArrayList;

import rx.Observable;
import rx.functions.Func1;
import uk.co.thedistance.components.lists.interfaces.ListDataSource;
import uk.co.thedistance.components.lists.model.ListContent;

public class MutualFriendsDataSource implements ListDataSource<MutualFriend> {

    private final ShowRealApi api;
    private final String token;
    private String after;
    private final String friendId;
    private int total = -1;
    private int count = 0;

    public MutualFriendsDataSource(ApplicationComponent component, String token, String friendId) {
        this.api = component.api();
        this.token = token;
        this.friendId = friendId;
    }

    @Override
    public boolean isListComplete() {
        return count >= total;
    }

    @Override
    public void reset() {
        total = -1;
        count = 0;
        after = null;
    }

    private Observable<MutualFriends> getFriendsObservable() {
        return TextUtils.isEmpty(after) ? api.getMutualFriends(token, friendId) : api.getNextMutualFriends(token, friendId, after);
    }

    @Override
    public Observable<ListContent<MutualFriend>> getData() {
        return getFriendsObservable()
                .map(new Func1<MutualFriends, ListContent<MutualFriend>>() {
                    @Override
                    public ListContent<MutualFriend> call(MutualFriends mutualFriends) {
                        if (mutualFriends == null) {
                            total = 0;
                            return new ListContent<>(new ArrayList<MutualFriend>(), count == 0);
                        }
                        total = mutualFriends.totalCount;

                        ListContent<MutualFriend> content = new ListContent<>(mutualFriends.friends, count == 0);
                        count += mutualFriends.friends.size();
                        after = mutualFriends.after;

                        return content;
                    }
                });
    }
}

package com.showreal.app.features.reviews;

import android.support.annotation.IntDef;

import com.showreal.app.data.model.Profile;

import uk.co.thedistance.components.lists.interfaces.Sortable;

class ReviewItem implements Sortable {

    public static final int Primary = 0;
    public static final int Secondary = 1;

    @Override
    public boolean isSameItem(Sortable other) {
        if (!(other instanceof ReviewItem)) {
            return false;
        }
        Profile otherProfile = ((ReviewItem) other).profile;
        return profile.id == otherProfile.id;
    }

    @Override
    public boolean isSameContent(Sortable other) {
        if (!(other instanceof ReviewItem)) {
            return false;
        }
        return equals(other);
    }

    @IntDef({Primary, Secondary})
    @interface ProfileType {
    }

    Profile profile;
    @ProfileType
    int type;

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != ReviewItem.class) {
            return false;
        }
        ReviewItem item = (ReviewItem) obj;
        return profile.equals(item.profile);
    }

    static ReviewItem with(Profile profile, @ProfileType int type) {
        ReviewItem item = new ReviewItem();
        item.profile = profile;
        item.type = type;
        return item;
    }
}

package com.showreal.app.features.reviews;

import android.content.Context;

import com.showreal.app.R;

import uk.co.thedistance.components.lists.interfaces.Sortable;

import static com.showreal.app.features.reviews.ReviewItem.Primary;
import static com.showreal.app.features.reviews.ReviewItem.Secondary;

public class ReviewHeader implements Sortable {

    @ReviewItem.ProfileType int type;

    static ReviewHeader with(@ReviewItem.ProfileType int type) {
        ReviewHeader item = new ReviewHeader();
        item.type = type;
        return item;
    }

    String getHeader(Context context) {
        switch (type) {
            case Primary:
                return context.getString(R.string.reviews_header_primary);
            case Secondary:
                return context.getString(R.string.reviews_header_secondary);
        }

        return null;
    }

    @Override
    public boolean isSameItem(Sortable other) {
        if (!(other instanceof ReviewHeader)) {
            return false;
        }
        return ((ReviewHeader) other).type == type;
    }

    @Override
    public boolean isSameContent(Sortable other) {
        if (!(other instanceof ReviewHeader)) {
            return false;
        }
        return ((ReviewHeader) other).type == type;
    }
}

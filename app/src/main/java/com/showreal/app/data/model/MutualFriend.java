package com.showreal.app.data.model;

import uk.co.thedistance.components.lists.interfaces.Sortable;

public class MutualFriend implements Sortable {

    public String imageUrl;
    public String name;
    public String id;

    public MutualFriend(String imageUrl, String name) {
        this.imageUrl = imageUrl;
        this.name = name;
    }

    @Override
    public boolean isSameItem(Sortable other) {
        return false;
    }

    @Override
    public boolean isSameContent(Sortable other) {
        return false;
    }
}

package com.showreal.app.features.profile.instagram;

import android.databinding.BaseObservable;
import android.view.View;

public class InstagramViewModel extends BaseObservable {

    public InstagramViewModel(InstagramView view) {
        this.instagramView = view;
    }

    interface InstagramView {
        void openImage(View image);
    }

    private final InstagramView instagramView;

    public void onClick(View view) {
        instagramView.openImage(view);
    }
}

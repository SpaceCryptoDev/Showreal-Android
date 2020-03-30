package com.showreal.app.features.real.myreal;

import com.showreal.app.data.model.Profile;

public class ReelUpdateContent {

    final Profile profile;
    final int incomplete;

    public ReelUpdateContent(Profile profile, int incomplete) {
        this.profile = profile;
        this.incomplete = incomplete;
    }
}

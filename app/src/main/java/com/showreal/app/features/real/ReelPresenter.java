package com.showreal.app.features.real;

import com.showreal.app.data.model.Profile;

import uk.co.thedistance.components.contentloading.ContentLoadingPresenter;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenterView;

public class ReelPresenter extends ContentLoadingPresenter<Profile, ReelDataSource, ContentLoadingPresenterView<Profile>> {

    public ReelPresenter(ReelDataSource dataSource) {
        super(dataSource);
    }
}

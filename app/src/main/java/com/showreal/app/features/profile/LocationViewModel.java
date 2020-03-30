package com.showreal.app.features.profile;

import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.view.View;


public class LocationViewModel extends BaseObservable {

    public LocationViewModel(LocationView locationView) {
        this.locationView = locationView;
    }

    protected interface LocationView {

        void onSearch(String text);
        void onLocation();
    }

    protected final LocationView locationView;
    public ObservableField<String> searchText = new ObservableField<>();

    public void onSearch(View view) {
        if (searchText.get() != null && searchText.get().length() > 0) {
            locationView.onSearch(searchText.get());
        }
    }

    public void onLocation(View view) {
        locationView.onLocation();
    }

    public void onClear(View view) {
        searchText.set("");
    }

    public void onSearchChanged(CharSequence sequence, int start, int before, int count) {
        searchText.set(sequence.toString());
    }

    public void setSearchText(String text) {
        searchText.set(text);
    }

}

package com.showreal.app.features.real.myreal;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;

import com.showreal.app.BR;
import com.showreal.app.data.model.Profile;
import com.showreal.app.databinding.ItemClipBinding;


public class MyRealViewModel extends BaseObservable {

    private final Profile profile;

    public MyRealViewModel(MyRealView realView, Profile profile) {
        this.realView = realView;
        this.profile = profile;
    }

    public void invalidateCount() {
        notifyPropertyChanged(BR.promptVisibility);
        notifyPropertyChanged(BR.emptyVisibility);
    }

    interface MyRealView {

        void publish();

        void addClip();

        void startLibraryDrag(View view, int pos);

        void startSegmentDrag(View view);

        void closeBar();

        void selectClip(int position, ItemClipBinding binding);

        int getClipCount();

        int getSegmentCount();

        boolean shouldShowPublish();

        void selectSegment(View view);

        void moveSegment(int fromPosition, int toPosition);
    }

    private final MyRealView realView;

    public void onPublish(View view) {
        realView.publish();
    }

    public void onAdd(View view) {
        realView.addClip();
    }

    public void onCloseBar(View view) {
        realView.closeBar();
    }

    @Bindable
    public int getPromptVisibility() {
        return realView.getClipCount() == 0 ? View.VISIBLE : View.GONE;
    }

    @Bindable
    public int getEmptyVisibility() {
        return realView.getSegmentCount() == 0 ? View.VISIBLE : View.GONE;
    }

    @Bindable
    public int getPublishVisibility() {
        return realView.shouldShowPublish() ? View.VISIBLE : View.GONE;
    }
}

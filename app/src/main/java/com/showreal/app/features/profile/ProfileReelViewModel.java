package com.showreal.app.features.profile;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import com.showreal.app.R;

import java.util.Random;

public class ProfileReelViewModel extends BaseObservable {

    private final ReelView reelView;
    private final int[] IMAGES = new int[]{R.drawable.pattern_1, R.drawable.pattern_2, R.drawable.pattern_3, R.drawable.pattern_4
            , R.drawable.pattern_4, R.drawable.pattern_5, R.drawable.pattern_6, R.drawable.pattern_7};
    private static final Random RANDOM = new Random();

    public interface ReelView {

        Context getImageContext();
    }

    public ProfileReelViewModel(ReelView reelView) {
        this.reelView = reelView;
    }

    @Bindable
    public Drawable getShapeOne() {
        return randomShape();
    }

    @Bindable
    public Drawable getShapeTwo() {
        return randomShape();
    }

    @Bindable
    public Drawable getShapeThree() {
        return randomShape();
    }

    private Drawable randomShape() {
        int rand = RANDOM.nextInt(7);
        return ContextCompat.getDrawable(reelView.getImageContext(), IMAGES[rand]);
    }
}

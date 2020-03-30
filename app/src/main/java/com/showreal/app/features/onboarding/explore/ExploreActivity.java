package com.showreal.app.features.onboarding.explore;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.showreal.app.BaseActivity;
import com.showreal.app.R;
import com.showreal.app.data.model.Profile;
import com.showreal.app.databinding.ActivityExploreBinding;
import com.showreal.app.features.real.myreal.MyRealActivity;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ExploreActivity extends BaseActivity {

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityExploreBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_explore);

        binding.buttonExploreAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Profile.dummyProfile(ExploreActivity.this)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<Profile>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onNext(Profile profile) {
                                Intent intent = new Intent(ExploreActivity.this, MyRealActivity.class);
                                intent.putExtra(MyRealActivity.EXTRA_PROFILE, profile);
                                intent.putExtra(MyRealActivity.EXTRA_EXPLORE, true);
                                startActivity(intent);
                            }
                        });
            }
        });
    }
}
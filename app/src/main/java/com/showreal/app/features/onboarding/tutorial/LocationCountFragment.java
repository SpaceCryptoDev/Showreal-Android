package com.showreal.app.features.onboarding.tutorial;

import android.Manifest;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.PixelFormat;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.showreal.app.BaseFragment;
import com.showreal.app.R;
import com.showreal.app.data.model.RevieweeCount;
import com.showreal.app.databinding.FragmentLogoBinding;
import com.tbruyelle.rxpermissions.Permission;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.util.concurrent.TimeUnit;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class LocationCountFragment extends BaseFragment implements MediaPlayer.OnCompletionListener {

    private long delay;
    FragmentLogoBinding binding;
    private LocationCountCallback callback;
    private ReactiveLocationProvider locationProvider;
    private int count = -1;
    private Subscription subscription;

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.seekTo(0);
        mp.start();
    }

    interface LocationCountCallback {
        void skipCount();

        void showCount(int count);
    }

    @Override
    protected String getScreenName() {
        return null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_logo, container, false);

        binding.video.getHolder().setFormat(PixelFormat.TRANSPARENT);
        playVideo();

        locationProvider = new ReactiveLocationProvider(getActivity());

        return binding.getRoot();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        callback = (LocationCountCallback) context;
    }

    private void playVideo() {
        String fileName = "android.resource://" + getActivity().getPackageName() + "/raw/logo_anim";
        binding.video.setVideoURI(Uri.parse(fileName));

        binding.video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                delay = mp.getDuration() - 400;

                if (RxPermissions.getInstance(getActivity()).isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    getCount(false);
                } else {
                    getCount(true);
                }

                mp.setOnCompletionListener(LocationCountFragment.this);

            }
        });
        binding.video.setZOrderOnTop(true);
        binding.video.start();
    }

    private void skipCount() {
        callback.skipCount();
    }

    private void getCount(boolean requestPermission) {
        Observable<Long> minimumTimer = Observable.timer(delay, TimeUnit.MILLISECONDS);
        Observable<Integer> count = requestPermission ? locationCountWithPermission() : locationCount();

        subscription = Observable.zip(minimumTimer, count, new Func2<Long, Integer, Integer>() {
            @Override
            public Integer call(Long aLong, Integer integer) {
                return integer;
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        skipCount();
                    }

                    @Override
                    public void onNext(Integer integer) {
                        showCount(integer);
                    }
                });
    }

    private Observable<Integer> locationCountWithPermission() {
        return RxPermissions.getInstance(getActivity())
                .requestEach(Manifest.permission.ACCESS_FINE_LOCATION)
                .flatMap(new Func1<Permission, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(Permission permission) {
                        if (permission.granted) {
                            return locationCount();
                        }
                        return Observable.just(0);
                    }
                });
    }

    private void showCount(int count) {
        callback.showCount(count);
    }

    private Observable<Integer> locationCount() {
        if (count != -1) {
            return Observable.just(count);
        }
        return SRlocationObservable.createObservable(getActivity())
                .flatMap(new Func1<Location, Observable<RevieweeCount>>() {
                    @Override
                    public Observable<RevieweeCount> call(Location location) {
                        if (location == null) {
                            return Observable.just(new RevieweeCount(0));
                        }
                        return getAppComponent().api().getLocalCount(location.getLatitude(), location.getLongitude())
                                .subscribeOn(Schedulers.io());
                    }
                }).map(new Func1<RevieweeCount, Integer>() {
                    @Override
                    public Integer call(RevieweeCount revieweeCount) {
                        LocationCountFragment.this.count = revieweeCount.resultCount;
                        return revieweeCount.resultCount;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void onStop() {
        super.onStop();

        binding.video.stopPlayback();
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (subscription == null) {
            playVideo();
        }
    }
}

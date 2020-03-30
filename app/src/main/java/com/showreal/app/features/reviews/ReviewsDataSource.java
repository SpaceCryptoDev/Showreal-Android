package com.showreal.app.features.reviews;

import android.content.Context;

import com.danikula.videocache.HttpProxyCacheServer;
import com.showreal.app.BuildConfig;
import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Reviewees;
import com.showreal.app.data.model.Video;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivecache.Provider;
import okhttp3.ResponseBody;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.contentloading.DataSource;
import uk.co.thedistance.thedistancecore.TDObservers;

public class ReviewsDataSource implements DataSource<Reviewees> {

    private final ShowRealApi api;
    private final Provider<List<Profile>> reviewCache;
    private final Provider<Profile> profileProvider;
    private final TheDistanceApplication context;
    private final HttpProxyCacheServer cacheServer;

    public ReviewsDataSource(Context context) {
        this.context = TheDistanceApplication.getApplicationComponent(context).appContext();
        this.api = TheDistanceApplication.getApplicationComponent(context).api();
        this.reviewCache = TheDistanceApplication.getApplicationComponent(context).cache().<List<Profile>>provider().expirable(false).withKey("profiles");
        profileProvider = TheDistanceApplication.getApplicationComponent(context).staleProfileProvider();
        this.cacheServer = TheDistanceApplication.getApplicationComponent(context).cacheServer();
    }

    @Override
    public void reset() {

    }

    private Observable<Reviewees> getPreviewData() {
        return api.getPreviewReviewees()
                .map(new Func1<List<Profile>, Reviewees>() {
                    @Override
                    public Reviewees call(List<Profile> profiles) {
                        Reviewees reviewees = new Reviewees();
                        reviewees.primary = profiles;
                        reviewees.secondary = Collections.emptyList();
                        return reviewees;
                    }
                });
    }

    @Override
    public Observable<Reviewees> getData() {
        if (BuildConfig.PREVIEW) {
            return getPreviewData();
        }
        return profileProvider.readNullable()
                .flatMap(new Func1<Profile, Observable<Reviewees>>() {
                    @Override
                    public Observable<Reviewees> call(Profile profile) {
                        return (profile == null || !profile.hasShowReal(context) ? getPreviewData() : api.getReviewees())
                                .doOnNext(new Action1<Reviewees>() {
                                    @Override
                                    public void call(Reviewees reviewees) {
                                        List<Profile> profiles = new ArrayList<>(reviewees.primary);
                                        profiles.addAll(reviewees.secondary);
                                        Observable.just(profiles).compose(reviewCache.replace()).subscribe(TDObservers.<List<Profile>>empty());

                                        precache(profiles);
                                    }
                                });
                    }
                });


    }

    private void precache(List<Profile> profiles) {
        if (profiles.size() > 5) {
            profiles = profiles.subList(0, 4);
        }

        List<String> toCache = new ArrayList<>();
        for (Profile profile : profiles) {
            for (Video video : profile.videos) {
                if (!cacheServer.isCached(video.url)) {
                    toCache.add(video.url);
                }
            }
        }

        if (toCache.isEmpty()) {
            return;
        }

        final ShowRealApi api = context.getApplicationComponent().api();
        Observable<Observable<ResponseBody>> downloads = Observable.from(toCache)
                .map(new Func1<String, Observable<ResponseBody>>() {
                    @Override
                    public Observable<ResponseBody> call(String string) {
                        String url = cacheServer.getProxyUrl(string);
                        return api.downloadFile(url);
                    }
                });
        Observable.concat(downloads)
                .subscribeOn(Schedulers.io())
                .subscribe(TDObservers.<ResponseBody>empty());
    }
}

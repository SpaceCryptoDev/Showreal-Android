package com.showreal.app.features.potential;

import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.Liked;
import com.showreal.app.data.model.LikedResponse;
import com.showreal.app.data.model.Match;
import com.showreal.app.injection.ApplicationComponent;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivecache.Provider;
import rx.Observable;
import rx.functions.Func1;
import uk.co.thedistance.components.contentloading.DataSource;

public class PotentialDataSource implements DataSource<List<Liked>> {

    private final ShowRealApi api;
    private final Provider<List<Liked>> cacheProvider;
    private boolean skipCache;

    public PotentialDataSource(ApplicationComponent component) {
        this.api = component.api();
        cacheProvider = component.cache().<List<Liked>>provider().lifeCache(30, TimeUnit.MINUTES).withKey("potential");
    }

    @Override
    public void reset() {
        skipCache = true;
    }

    @Override
    public Observable<List<Liked>> getData() {
        if (skipCache) {
            skipCache = false;
            return api.getPotential()
                    .map(new Func1<LikedResponse, List<Liked>>() {
                        @Override
                        public List<Liked> call(LikedResponse likedResponse) {
                            return likedResponse.liked;
                        }
                    })
                    .compose(cacheProvider.replace());
        }
        return api.getPotential()
                .map(new Func1<LikedResponse, List<Liked>>() {
                    @Override
                    public List<Liked> call(LikedResponse likedResponse) {
                        return likedResponse.liked;
                    }
                })
                .compose(cacheProvider.readWithLoader());
    }
}

package com.showreal.app.features.conversations.matches;

import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.Match;
import com.showreal.app.injection.ApplicationComponent;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivecache.Provider;
import rx.Observable;
import rx.functions.Func1;
import uk.co.thedistance.components.lists.interfaces.ListDataSource;
import uk.co.thedistance.components.lists.model.ListContent;

public class MatchesDataSource implements ListDataSource<Match> {

    private final ShowRealApi api;
    private final Provider<List<Match>> cache;
    private final Provider<List<Match>> cache_stale;
    private boolean skipCache;

    public MatchesDataSource(ApplicationComponent component) {
        api = component.api();
        cache = component.cache().<List<Match>>provider().lifeCache(30, TimeUnit.MINUTES).withKey("matches");
        cache_stale = component.cache().<List<Match>>provider().withKey("matches_stale");
    }

    @Override
    public boolean isListComplete() {
        return true;
    }

    @Override
    public void reset() {
        skipCache = true;
    }

    @Override
    public Observable<ListContent<Match>> getData() {
        return (skipCache ? api.getMatches() : api.getMatches()
                .compose(cache.readWithLoader()))
                .compose(cache_stale.replace())
                .onErrorResumeNext(cache_stale.read())
                .map(new Func1<List<Match>, ListContent<Match>>() {
                    @Override
                    public ListContent<Match> call(List<Match> matches) {
                        return new ListContent<>(matches, true);
                    }
                });
    }
}

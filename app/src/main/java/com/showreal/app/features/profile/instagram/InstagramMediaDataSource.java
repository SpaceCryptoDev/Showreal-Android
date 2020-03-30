package com.showreal.app.features.profile.instagram;

import com.showreal.app.data.InstagramApi;
import com.showreal.app.data.account.AccountHelper;
import com.showreal.app.data.model.InstagramMedia;
import com.showreal.app.injection.ApplicationComponent;

import rx.Observable;
import rx.functions.Func1;
import uk.co.thedistance.components.lists.interfaces.ListDataSource;
import uk.co.thedistance.components.lists.model.ListContent;

public class InstagramMediaDataSource implements ListDataSource<InstagramMedia.Image> {

    private final InstagramApi api;
    private final AccountHelper accountHelper;
    private boolean complete;
    private String next;

    public InstagramMediaDataSource(ApplicationComponent component) {
        api = component.instagram();
        this.accountHelper = component.accountHelper();
    }

    @Override
    public void reset() {
        next = null;
        complete = false;
    }

    private Observable<InstagramMedia> getObservable() {
        String token = accountHelper.getInstagramToken();
        return next == null ? api.getMedia(token) : api.getNextMedia(next);
    }

    @Override
    public Observable<ListContent<InstagramMedia.Image>> getData() {
        return getObservable()
                .map(new Func1<InstagramMedia, ListContent<InstagramMedia.Image>>() {
                    @Override
                    public ListContent<InstagramMedia.Image> call(InstagramMedia media) {
                        complete = media.pagination == null;
                        if (media.pagination != null) {
                            next = media.pagination.nextUrl;
                        }
                        return new ListContent<>(media.images, false);
                    }
                });
    }

    @Override
    public boolean isListComplete() {
        return complete;
    }
}

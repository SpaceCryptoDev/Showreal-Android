package com.showreal.app.data;

import android.util.Log;

import java.util.Map;
import java.util.Objects;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.base.Presenter;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenterView;
import uk.co.thedistance.components.contentloading.DataSource;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;

/**

 */
public class MultiUploadingPresenter implements Presenter<MultiUploadPresenterView> {

    protected MultiUploadPresenterView view;
    protected Map<Class, DataUploader> dataUploaders;
    protected Subscription dataSubscription;

    public MultiUploadingPresenter(Map<Class, DataUploader> dataUploaders) {
        this.dataUploaders = dataUploaders;
    }

    /**
     * This method will attempt to load content from the {@link DataSource} and call the appropriate methods
     * on the {@link ContentLoadingPresenterView} in the event of errors or completion
     */
    public void uploadContent(Object content) {
        unsubscribe();

        final Class cls = content.getClass();
        DataUploader dataUploader = dataUploaders.get(cls);

        dataUploader.setContent(content);

        showLoading(true);

        dataSubscription = dataUploader.getUpload()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("Error: ", e.getLocalizedMessage());
                        showLoading(false);
                        view.showError(e, e.getLocalizedMessage(), cls);
                        dataSubscription = null;
                    }

                    @Override
                    public void onNext(Object response) {
                        view.uploadComplete(response);
                        showLoading(false);
                        dataSubscription = null;
                    }
                });
    }

    private void showLoading(boolean show) {
        view.showUploading(show);
    }

    private void unsubscribe() {
        if (dataSubscription != null && !dataSubscription.isUnsubscribed()) {
            dataSubscription.unsubscribe();
        }
    }

    @Override
    public void onViewAttached(MultiUploadPresenterView view) {
        this.view = view;

        if (isUploading()) {
            view.showUploading(true);
        }
    }

    @Override
    public void onViewDetached() {
        unsubscribe();
    }

    @Override
    public void onDestroyed() {
        unsubscribe();
    }

    private boolean isUploading() {
        return dataSubscription != null;
    }
}

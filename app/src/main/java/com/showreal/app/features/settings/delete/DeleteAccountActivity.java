package com.showreal.app.features.settings.delete;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.showreal.app.BaseActivity;
import com.showreal.app.ErrorHandler;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.analytics.AppboyEvent;
import com.showreal.app.analytics.Events;
import com.showreal.app.databinding.ActivityDeleteAccountBinding;

import rx.functions.Action1;
import uk.co.thedistance.components.base.UploaderFactory;
import uk.co.thedistance.components.base.UploaderLoaderHelper;
import uk.co.thedistance.components.uploading.UploadingPresenter;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;

public class DeleteAccountActivity extends BaseActivity implements DeleteViewModel.DeleteView, UploadingPresenterView<Void> {

    private ActivityDeleteAccountBinding binding;
    private UploaderLoaderHelper<DeleteUploadPresenter> loaderHelper;
    private DeleteUploadPresenter uploader;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_delete_account);
        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.setViewModel(new DeleteViewModel(this));

        loaderHelper = new UploaderLoaderHelper<>(this, new DeleteUploadFactory());
        getSupportLoaderManager().initLoader(0, null, loaderHelper);
    }

    @Override
    protected void onResume() {
        super.onResume();

        uploader = loaderHelper.getUploader();
        uploader.onViewAttached(this);
    }

    @Override
    public void deleteAccount() {
        RxAlertDialog.with(this)
                .title(R.string.activity_delete_account)
                .message(R.string.alert_msg_delete)
                .positiveButton(R.string.button_delete)
                .negativeButton(R.string.button_cancel)
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer button) {
                        if (button == RxAlertDialog.ButtonPositive) {
                            getAppComponent().analytics().send(new AppboyEvent(Events.ACCOUNT_DELETE));
                            uploader.uploadContent(null);
                        }
                    }
                });
    }

    @Override
    public void cancel() {
        finish();
    }

    @Override
    public Context getTextContext() {
        return this;
    }

    @Override
    public void showUploading(boolean show) {
        if (show) {
            binding.loadingLayout.loadingLayout.show();
        } else {
            binding.loadingLayout.loadingLayout.hide();
        }
    }

    @Override
    public void uploadComplete(Void response) {
        getAppComponent().accountHelper().logout(this, false);
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, this)) {
            return;
        }

        FabricHelper.logException(throwable);
        RxAlertDialog.with(this)
                .title(R.string.alert_title_error_delete)
                .message(R.string.alert_msg_error_delete)
                .positiveButton(R.string.button_ok)
                .subscribe();
    }

    private class DeleteUploadPresenter extends UploadingPresenter<Void, Void, DeleteUploader, UploadingPresenterView<Void>> {

        public DeleteUploadPresenter(DeleteUploader dataUploader) {
            super(dataUploader);
        }
    }

    private class DeleteUploadFactory implements UploaderFactory<DeleteUploadPresenter> {

        @Override
        public DeleteUploadPresenter create() {
            return new DeleteUploadPresenter(new DeleteUploader(DeleteAccountActivity.this));
        }
    }
}

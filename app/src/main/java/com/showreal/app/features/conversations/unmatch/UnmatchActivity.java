package com.showreal.app.features.conversations.unmatch;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.showreal.app.BaseActivity;
import com.showreal.app.ErrorHandler;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.data.model.Match;
import com.showreal.app.databinding.ActivityUnmatchBinding;

import uk.co.thedistance.components.base.UploaderFactory;
import uk.co.thedistance.components.base.UploaderLoaderHelper;
import uk.co.thedistance.components.uploading.UploadingPresenter;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;

public class UnmatchActivity extends BaseActivity implements UnmatchViewModel.UnmatchView, UploadingPresenterView<Void> {

    public static final String EXTRA_MATCH = "match";
    private ActivityUnmatchBinding binding;
    private UploaderLoaderHelper<UnmatchPresenter> uploaderLoader;
    private UnmatchPresenter uploader;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_unmatch);
        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Match match = getIntent().getParcelableExtra(EXTRA_MATCH);
        binding.setViewModel(new UnmatchViewModel(match, this));

        uploaderLoader = new UploaderLoaderHelper<>(this, new UnmatchPresenterFactory(this));
        getSupportLoaderManager().initLoader(0, null, uploaderLoader);
    }

    @Override
    protected void onResume() {
        super.onResume();

        uploader = uploaderLoader.getUploader();
        uploader.onViewAttached(this);
    }

    @Override
    public void unmatch(Match match) {
        uploader.uploadContent(match);
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
            binding.loading.loadingLayout.show();
        } else {
            binding.loading.loadingLayout.hide();
        }
    }

    @Override
    public void uploadComplete(Void response) {
        Intent intent = getIntent();
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, this)) {
            return;
        }

        FabricHelper.logException(throwable);
        RxAlertDialog.with(this)
                .title(R.string.alert_title_unmatch)
                .message(R.string.alert_msg_unmatch)
                .positiveButton(R.string.button_ok)
                .subscribe();
    }

    private static class UnmatchPresenter extends UploadingPresenter<Match, Void, UnmatchUploader, UploadingPresenterView<Void>> {

        public UnmatchPresenter(UnmatchUploader dataUploader) {
            super(dataUploader);
        }
    }

    private static class UnmatchPresenterFactory implements UploaderFactory<UnmatchPresenter> {

        private final Context context;

        private UnmatchPresenterFactory(Context context) {
            this.context = context;
        }

        @Override
        public UnmatchPresenter create() {
            return new UnmatchPresenter(new UnmatchUploader(context));
        }
    }
}

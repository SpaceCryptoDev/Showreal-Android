package com.showreal.app.features.settings;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.showreal.app.BaseActivity;
import com.showreal.app.ErrorHandler;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.data.model.Report;
import com.showreal.app.databinding.ActivityReportBinding;

import uk.co.thedistance.components.base.UploaderFactory;
import uk.co.thedistance.components.base.UploaderLoaderHelper;
import uk.co.thedistance.components.uploading.UploadingPresenter;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;

public class ReportActivity extends BaseActivity implements UploadingPresenterView<Report>, ReportViewModel.ReportView {

    public static final String EXTRA_PROFILE_ID = "id";
    private UploaderLoaderHelper<ReportUploadingPresenter> loaderHelper;
    private ActivityReportBinding binding;
    private ReportUploadingPresenter uploader;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_report);
        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ReportViewModel viewModel = new ReportViewModel(this);
        if (getIntent().hasExtra(EXTRA_PROFILE_ID)) {
            viewModel.setUserId(getIntent().getIntExtra(EXTRA_PROFILE_ID, 0));
        }

        binding.setViewModel(viewModel);

        binding.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });

        loaderHelper = new UploaderLoaderHelper<>(this, new ReportUploaderFactory());
        getSupportLoaderManager().initLoader(0, null, loaderHelper);
    }

    @Override
    protected void onResume() {
        super.onResume();

        uploader = loaderHelper.getUploader();
        uploader.onViewAttached(this);
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
    public void uploadComplete(Report response) {
        binding.text.setText("");
        RxAlertDialog.with(this)
                .title(R.string.alert_title_thanks)
                .message(R.string.alert_msg_report)
                .positiveButton(R.string.button_ok)
                .subscribe();
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (ErrorHandler.handle(throwable, this)) {
            return;
        }

        RxAlertDialog.with(this)
                .title(R.string.alert_title_report)
                .message(R.string.alert_report_error)
                .positiveButton(R.string.button_ok)
                .subscribe();
    }

    @Override
    public void send(Report report) {
        if ((report.reported == null && TextUtils.isEmpty(report.message))
                || (report.reported != null && TextUtils.isEmpty(report.other))) {

            binding.text.setError(getString(R.string.error_field_empty));
            return;
        }
        uploader.uploadContent(report);
    }

    @Override
    public void clearErrors() {
        binding.text.setError(null);
    }

    @Override
    public Context getSpinnerContext() {
        return this;
    }

    private class ReportUploadingPresenter extends UploadingPresenter<Report, Report, ReportUploader, UploadingPresenterView<Report>> {

        public ReportUploadingPresenter(ReportUploader dataUploader) {
            super(dataUploader);
        }
    }

    private class ReportUploaderFactory implements UploaderFactory<ReportUploadingPresenter> {

        @Override
        public ReportUploadingPresenter create() {
            return new ReportUploadingPresenter(new ReportUploader(ReportActivity.this));
        }
    }
}

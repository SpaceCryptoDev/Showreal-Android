package com.showreal.app.features.settings;

import android.content.Context;

import com.showreal.app.TheDistanceApplication;
import com.showreal.app.data.ShowRealApi;
import com.showreal.app.data.model.Report;

import rx.Observable;
import uk.co.thedistance.components.uploading.interfaces.DataUploader;

public class ReportUploader implements DataUploader<Report, Report> {

    private final ShowRealApi api;
    private Report report;

    public ReportUploader(Context context) {
        this.api = TheDistanceApplication.getApplicationComponent(context).api();
    }

    @Override
    public void reset() {

    }

    @Override
    public void setContent(Report content) {
        this.report = content;
    }

    @Override
    public Observable<Report> getUpload() {
        return report.reported == null ? api.postReport(report) : api.reportUser(report);
    }
}

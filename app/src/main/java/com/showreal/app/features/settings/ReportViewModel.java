package com.showreal.app.features.settings;

import android.content.Context;
import android.databinding.BaseObservable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.showreal.app.R;
import com.showreal.app.data.model.Report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportViewModel extends BaseObservable {

    final ReportView reportView;
    public final ArrayAdapter<String> spinnerAdapter;
    private Report report = new Report();


    public ReportViewModel(ReportView reportView) {
        this.reportView = reportView;

        List<String> reportTitles = new ArrayList<>(Arrays.asList(reportView.getSpinnerContext().getResources().getStringArray(R.array.report_titles)));
        spinnerAdapter = new ArrayAdapter<>(reportView.getSpinnerContext(), R.layout.spinner_item_dropdown, reportTitles);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item);
    }

    public void setUserId(int userId) {
        report.reported = userId;
        spinnerAdapter.clear();
        spinnerAdapter.addAll(Arrays.asList(reportView.getSpinnerContext().getResources().getStringArray(R.array.report_user_titles)));
    }

    interface ReportView {
        void send(Report report);

        Context getSpinnerContext();

        void clearErrors();
    }

    public void onSend(View view) {
        reportView.send(report);
    }

    public AdapterView.OnItemSelectedListener selectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (report.reported == null) {
                report.topic = position;
            } else {
                report.reason = position;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    public void onMessageChanged(CharSequence sequence, int start, int before, int count) {
        reportView.clearErrors();

        if (report.reported == null) {
            report.message = sequence.toString();
        } else {
            report.other = sequence.toString();
        }
    }
}

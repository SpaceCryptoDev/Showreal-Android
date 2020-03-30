package com.showreal.app.features.status;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.showreal.app.BaseActivity;
import com.showreal.app.R;
import com.showreal.app.databinding.ActivityStatusBinding;

public class StatusActivity extends BaseActivity {

    private ActivityStatusBinding binding;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_status);
        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}

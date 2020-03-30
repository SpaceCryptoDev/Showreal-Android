package com.showreal.app.features.profile;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import com.bumptech.glide.Glide;
import com.showreal.app.BaseActivity;
import com.showreal.app.R;
import com.showreal.app.databinding.ActivityImageCropBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class CropActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_URI = "image";
    private ActivityImageCropBinding binding;
    private Uri imageUrl;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_image_crop);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String image = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        imageUrl = Uri.parse(image);
        Glide.with(this)
                .load(imageUrl)
                .asBitmap()
                .into(binding.crop);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_crop, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_choose:
                finishCrop();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private Observable<String> getCroppedPath() {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                FileOutputStream stream = null;
                File output = new File(getCacheDir(), "profile.jpg");
                if (output.exists()) {
                    output.delete();
                }
                try {
                    stream = new FileOutputStream(output);
                    Bitmap bitmap = binding.crop.getCroppedBitmap();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);

                    subscriber.onNext(output.getAbsolutePath());
                    subscriber.onCompleted();

                } catch (FileNotFoundException e) {
                    subscriber.onError(e);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void finishCrop() {
        getCroppedPath()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String s) {
                        Intent intent = getIntent();
                        intent.putExtra(EXTRA_IMAGE_URI, s);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
    }
}

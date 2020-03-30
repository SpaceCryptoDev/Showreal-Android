package com.showreal.app.features.profile.instagram;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.transition.Fade;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.showreal.app.BaseActivity;
import com.showreal.app.R;
import com.showreal.app.data.model.InstagramMedia;
import com.showreal.app.databinding.ActivityPhotoBinding;
import com.showreal.app.databinding.ItemPhotoBinding;

import java.util.ArrayList;
import java.util.List;

import uk.co.thedistance.thedistancecore.Version;

public class PhotoActivity extends BaseActivity {

    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_MEDIA = "media";
    private ActivityPhotoBinding binding;
    private int position;
    private List<InstagramMedia.Image> images;

    public static void startWith(Activity activity, List<InstagramMedia.Image> images, View view) {
        if (view.getTag(R.id.photo_position) == null) {
            return;
        }

        Intent intent = new Intent(activity, PhotoActivity.class);
        intent.putExtra(EXTRA_MEDIA, new ArrayList<>(images));

        int position = (int) view.getTag(R.id.photo_position);
        intent.putExtra(EXTRA_POSITION, position);

        if (Version.isLollipop()) {
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                    activity,
                    Pair.create(view, activity.getString(R.string.transition_name_photo))
            );

            activity.startActivity(intent, options.toBundle());
            return;
        }

        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        images = getIntent().getParcelableArrayListExtra(EXTRA_MEDIA);

        position = getIntent().getIntExtra(EXTRA_POSITION, 0);

        supportPostponeEnterTransition();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_photo);

        if (Version.isLollipop()) {

            String url = images.get(position).standardResolution.url;

            Glide.with(this)
                    .load(url)
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            setupPager();
                            supportStartPostponedEnterTransition();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            supportStartPostponedEnterTransition();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    setupPager();
                                }
                            }, getResources().getInteger(android.R.integer.config_mediumAnimTime));
                            return false;
                        }
                    })
                    .into(binding.dummyImage);
        } else {
            binding.dummyImage.setVisibility(View.GONE);
            setupPager();
        }
    }


    private void setupPager() {
        ImageAdapter adapter = new ImageAdapter(this, images);
        binding.pager.setAdapter(adapter);
        binding.pager.setCurrentItem(position);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                binding.dummyImage.setVisibility(View.GONE);
            }
        }, 100);
    }

    @Override
    protected String getScreenName() {
        return null;
    }

    private class ImageAdapter extends PagerAdapter {

        private final List<InstagramMedia.Image> items;
        private final LayoutInflater inflater;

        public ImageAdapter(Context context, List<InstagramMedia.Image> items) {
            this.items = items;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ItemPhotoBinding photoBinding = ItemPhotoBinding.inflate(inflater, container, false);
            container.addView(photoBinding.getRoot());
            photoBinding.setUrl(items.get(position).standardResolution.url);

            ViewCompat.setTransitionName(photoBinding.image, getResources().getString(R.string.transition_name_photo, position));

            return photoBinding.getRoot();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}

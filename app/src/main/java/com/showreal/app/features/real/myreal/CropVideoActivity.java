package com.showreal.app.features.real.myreal;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.adapter.BottomSheetItemClickListener;
import com.showreal.app.BaseActivity;
import com.showreal.app.R;
import com.showreal.app.data.VideoHelper;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Video;
import com.showreal.app.databinding.ActivityCropClipBinding;
import com.showreal.app.databinding.ItemThumbnailBinding;
import com.showreal.app.features.conversations.EasyVideo;
import com.showreal.app.features.real.ReelPlayer;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pl.aprilapps.easyphotopicker.EasyImage;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.lists.BindingViewHolder;


public class CropVideoActivity extends BaseActivity {

    public static final String EXTRA_VIDEO = "video";
    public static final String EXTRA_PROFILE = "profile";
    public static final String EXTRA_IS_EDIT = "is_edit";
    private static final int RC_RECORD = 0x0;
    private static final int RC_CHOOSE = 0x2;
    private ActivityCropClipBinding binding;
    private Video video;
    private ReelPlayer player;
    private MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
    private Profile profile;
    private boolean showCrop;
    private boolean isEdit;
    private ThumbnailAdapter adapter;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_crop_clip);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        video = getIntent().getParcelableExtra(EXTRA_VIDEO);
        if (video != null && !video.url.startsWith("file:")) {
            video.url = String.format("file://%s", video.url);
        }
        profile = getIntent().getParcelableExtra(EXTRA_PROFILE);
        isEdit = getIntent().getBooleanExtra(EXTRA_IS_EDIT, false);

        binding.question.setText(video.question.text);

        ensureOriginalVideo();

        if (!showCrop) {
            binding.recycler.setVisibility(View.GONE);
            binding.crop.getRoot().setVisibility(View.GONE);
        } else {
            binding.recycler.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    binding.recycler.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                    float thumbnailWidth = (binding.recycler.getWidth() - (binding.recycler.getPaddingLeft() * 2)) / 5;

                    binding.recycler.setAdapter(adapter = new ThumbnailAdapter(CropVideoActivity.this, video, thumbnailWidth));
                }
            });

            binding.recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    int start = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
                    if (player.getStartTime() != start) {
                        player.setStartTime(start);
                    }
                }
            });
        }

        binding.buttonFilter.setActivated(video.isLightFilterEnabled);
        binding.buttonFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null) {
                    player.toggleFilter();
                    binding.buttonFilter.setActivated(player.isFilterEnabled());
                }
            }
        });

        if (isEdit) {
            binding.buttonEdit.setVisibility(View.VISIBLE);
            binding.buttonEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chooseVideo();
                }
            });
        }

        setupVideo();

        binding.buttonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trimVideo();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (player != null) {
            player.pause();
            player.clear();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (player != null) {
            player.ready();
        }
    }

    private void setupVideo() {
        List<Video> videoList = new ArrayList<>(1);
        videoList.add(video);

        if (player != null) {
            player.destroy();
        }

        player = ReelPlayer.with(this, -1)
                .videoOnly(true)
                .videos(videoList)
                .audio(true)
                .useOriginal(true)
                .binding(binding)
                .profile(profile)
                .create();
        player.setup();
    }

    private void ensureOriginalVideo() {
        File original = getAppComponent().videoHelper().getFileForVideo(profile, video.question.id, VideoHelper.VideoSuffix.Original);
        if (original.exists()) {
            video.url = "file:///" + original.getPath();
        }

        String name = video.url.substring(0, video.url.lastIndexOf('.'));
        showCrop = name.endsWith(VideoHelper.VideoSuffix.Original.value) && !video.url.startsWith("http") && video.duration > 5;
    }

    private void trimVideo() {
        binding.loadingLayout.loadingLayout.show();
        video.isLightFilterEnabled = player.isFilterEnabled();
        if (!showCrop) {
            compressAndFinish(video);
            return;
        }
        Observable.create(new Observable.OnSubscribe<Video>() {
            @Override
            public void call(Subscriber<? super Video> subscriber) {
                int start = (player.getStartTime()) * 1000;
                int end = (int) Math.min(start + 5000, video.duration * 1000);

                File dest = getAppComponent().videoHelper().getFileForVideo(profile, video.question.id, null);
                if (dest.exists()) {
                    dest.delete();
                }
                File file = new File(video.url.substring(video.url.indexOf(":///") + 3));
                try {
                    TrimVideoUtils.startTrim(file, dest, start, end);
                    video.url = String.format("file://%s", dest.getAbsolutePath());
                    metadataRetriever.setDataSource(CropVideoActivity.this, Uri.fromFile(dest));

                    String time = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    long duration = Long.parseLong(time);
                    video.duration = duration / 1000;
                    if (video.duration == 0) {
                        video.duration = 5;
                    }

                    subscriber.onNext(video);
                } catch (IOException e) {
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Video>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        binding.loadingLayout.loadingLayout.hide();
                    }

                    @Override
                    public void onNext(Video video) {
                        File published = getAppComponent().videoHelper().getFileForVideo(profile, video.question.id, VideoHelper.VideoSuffix.Published);
                        if (published.exists()) {
                            published.delete();
                        }
                        compressAndFinish(video);
                    }
                });
    }

    private void compressAndFinish(final Video video) {
        File file = new File(video.url.substring(video.url.indexOf(":///") + 3));
        VideoCompressor compressor = new VideoCompressor(profile);
        compressor.compress(file, this)
                .subscribe(new Subscriber<VideoCompressor.Result>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        binding.loadingLayout.loadingLayout.hide();
                        Intent intent = getIntent();
                        intent.putExtra(EXTRA_VIDEO, video);
                        setResult(RESULT_OK, intent);
                        finish();
                    }

                    @Override
                    public void onNext(VideoCompressor.Result result) {
                        binding.loadingLayout.loadingLayout.hide();
                        Intent intent = getIntent();
                        video.url = result.file.getPath();

                        intent.putExtra(EXTRA_VIDEO, video);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_RECORD) {
            Intent intent = getIntent();
            if (resultCode == RESULT_OK) {
                Video video = data.getParcelableExtra(RecordActivity.EXTRA_VIDEO);
                intent.putExtra(RecordActivity.EXTRA_VIDEO, video);
                setResult(resultCode, intent);
                finish();
            }
        }

        if (requestCode == RC_CHOOSE) {
            Intent intent = getIntent();
            if (resultCode == RESULT_OK) {
                Video video = data.getParcelableExtra(RecordActivity.EXTRA_VIDEO);
                intent.putExtra(RecordActivity.EXTRA_VIDEO, video);
                setResult(resultCode, intent);
                finish();
            }
        }

        EasyVideo.handleActivityResult(requestCode, resultCode, data, this, new EasyImage.Callbacks() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {

            }

            @Override
            public void onImagePicked(File file, EasyImage.ImageSource source, int type) {

                File renamed = getAppComponent().videoHelper().getFileForVideo(profile, video.question.id, VideoHelper.VideoSuffix.Original);
                renamed.getParentFile().mkdirs();
                if (renamed.exists()) {
                    renamed.delete();
                }

                try {
                    FileUtils.moveFile(file, renamed);
                    file = renamed;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                video.url = String.format("file://%s", file.getAbsolutePath());

                metadataRetriever.setDataSource(CropVideoActivity.this, Uri.fromFile(file));
                String time = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long duration = Long.parseLong(time);
                video.duration = duration / 1000;
                setupVideo();
                adapter.setVideo(video, CropVideoActivity.this);
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {

            }
        });
    }

    private void chooseVideo() {
        new BottomSheetBuilder(this, binding.coordinator)
                .setMode(BottomSheetBuilder.MODE_LIST)
                .setMenu(R.menu.sheet_reel)
                .setItemClickListener(new BottomSheetItemClickListener() {
                    @Override
                    public void onBottomSheetItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_record:
                                Intent intent = new Intent(CropVideoActivity.this, RecordActivity.class);
                                intent.putExtra(RecordActivity.EXTRA_QUESTION, video.question);
                                intent.putExtra(RecordActivity.EXTRA_PROFILE, profile);
                                startActivityForResult(intent, RC_RECORD);
                                break;
                            case R.id.action_choose:
                                Intent chooseIntent = new Intent(CropVideoActivity.this, ClipAdjustActivity.class);
                                chooseIntent.putExtra(ClipAdjustActivity.EXTRA_QUESTION, video.question);
                                chooseIntent.putExtra(ClipAdjustActivity.EXTRA_PROFILE, profile);
                                startActivityForResult(chooseIntent, RC_CHOOSE);
                                break;
                        }
                    }
                }).createDialog().show();

    }

    private static class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailHolder> {

        private final LayoutInflater inflator;
        private Video video;
        private final MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        private final float thumbnailWidth;

        public ThumbnailAdapter(Context context, Video video, float thumbnailWidth) {
            this.inflator = LayoutInflater.from(context);
            this.video = video;
            this.thumbnailWidth = thumbnailWidth;

            metadataRetriever.setDataSource(context, Uri.parse(video.url));
            String time = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long duration = Long.parseLong(time);
            this.video.duration = duration / 1000;

        }

        @Override
        public ThumbnailHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ItemThumbnailBinding binding = DataBindingUtil.inflate(inflator, R.layout.item_thumbnail, parent, false);
            return new ThumbnailHolder(binding);
        }

        @Override
        public void onBindViewHolder(final ThumbnailHolder holder, int position) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.binding.image.getLayoutParams();
            params.width = (int) thumbnailWidth;

            Observable.create(new Observable.OnSubscribe<Bitmap>() {
                @Override
                public void call(Subscriber<? super Bitmap> subscriber) {

                    Bitmap bitmap = metadataRetriever.getFrameAtTime(holder.getAdapterPosition() * 1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);

                    if (bitmap != null) {
                        subscriber.onNext(bitmap);
                    } else {
                        subscriber.onError(new IllegalArgumentException("Could not get thumbnail"));
                    }
                    subscriber.onCompleted();
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<Bitmap>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(Bitmap bitmap) {
                            holder.binding.image.setImageBitmap(bitmap);
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return (int) video.duration;
        }

        public void setVideo(Video video, Context context) {
            this.video = video;
            metadataRetriever.setDataSource(context, Uri.parse(video.url));
            String time = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long duration = Long.parseLong(time);
            this.video.duration = duration / 1000;
            notifyDataSetChanged();
        }
    }

    private static class ThumbnailHolder extends BindingViewHolder<ItemThumbnailBinding> {

        public ThumbnailHolder(ItemThumbnailBinding binding) {
            super(binding);
        }
    }
}

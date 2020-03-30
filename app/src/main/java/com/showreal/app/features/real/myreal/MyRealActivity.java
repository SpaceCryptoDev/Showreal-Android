package com.showreal.app.features.real.myreal;

import android.accounts.AccountManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.DragEvent;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.appboy.Appboy;
import com.showreal.app.BaseActivity;
import com.showreal.app.BaseFragment;
import com.showreal.app.ErrorHandler;
import com.showreal.app.FabricHelper;
import com.showreal.app.MainActivity;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.analytics.Attributes;
import com.showreal.app.analytics.Screens;
import com.showreal.app.data.model.DeviceReel;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Question;
import com.showreal.app.data.model.Video;
import com.showreal.app.databinding.ActivityMyrealBinding;
import com.showreal.app.databinding.FragmentShowrealBinding;
import com.showreal.app.databinding.ItemClipBinding;
import com.showreal.app.features.real.ReelPlayer;
import com.showreal.app.features.real.VideoDownloader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import uk.co.thedistance.components.base.PresenterFactory;
import uk.co.thedistance.components.base.PresenterLoaderHelper;
import uk.co.thedistance.components.base.UploaderFactory;
import uk.co.thedistance.components.base.UploaderLoaderHelper;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenter;
import uk.co.thedistance.components.contentloading.ContentLoadingPresenterView;
import uk.co.thedistance.components.uploading.UploadingPresenter;
import uk.co.thedistance.components.uploading.interfaces.UploadingPresenterView;
import uk.co.thedistance.thedistancecore.TDSubscribers;

public class MyRealActivity extends BaseActivity {

    public static final String EXTRA_PROFILE = "profile";
    public static final String EXTRA_SIGN_UP = "sign_up";
    public static final String EXTRA_EXPLORE = "explore";
    private ActivityMyrealBinding binding;
    MyRealFragment fragment;

    @Override
    protected String getScreenName() {
        return Screens.LIBRARY;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_myreal);

        setSupportActionBar(binding.toolbarLayout.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        boolean signup = getIntent().getBooleanExtra(EXTRA_SIGN_UP, false);
        boolean explore = getIntent().getBooleanExtra(EXTRA_EXPLORE, false);

        if (signup) {
            binding.toolbarLayout.toolbar.setBackgroundResource(R.drawable.toolbar_background_buttons);
        }

        binding.signupSteps.setVisibility(signup ? View.VISIBLE : View.GONE);

        if (savedInstanceState == null) {
            Profile profile = getIntent().getParcelableExtra(EXTRA_PROFILE);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment, fragment = MyRealFragment.newInstance(profile, signup, explore))
                    .commit();
        } else {
            fragment = (MyRealFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        }

        binding.stepStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.binding.getViewModel().onPublish(null);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (fragment.onBackPressed()) {
            return;
        }

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class MyRealFragment extends BaseFragment implements MyRealViewModel.MyRealView, ContentLoadingPresenterView<ReelData>, UploadingPresenterView<ReelUpdateContent> {

        private static final int RC_ADD_CLIP = 0x0;
        private FragmentShowrealBinding binding;
        private ReelPlayer player;
        private LibraryAdapter adapter;
        private SegmentAdapter segmentAdapter;
        private PresenterLoaderHelper<VideosPresenter> loaderHelper;
        private UploaderLoaderHelper<ReelUploadPresenter> uploaderHelper;
        private VideosPresenter presenter;
        private List<Question> questions;
        private Profile profile;
        private ReelUploadPresenter uploader;
        private int selection = -1;
        private Profile originalProfile;
        private DeviceReel reel;
        private boolean fromSignup;
        private boolean showAdd;
        private boolean downloadPrompted;
        private boolean exploreOnly;

        private enum Action {
            None,
            Upload,
        }

        enum DragType {
            Library,
            Segment
        }

        private Action action = Action.None;

        @Override
        protected String getScreenName() {
            return null;
        }

        public static MyRealFragment newInstance(Profile profile, boolean signup, boolean explore) {

            Bundle args = new Bundle();

            MyRealFragment fragment = new MyRealFragment();
            args.putParcelable(EXTRA_PROFILE, profile);
            args.putBoolean(EXTRA_SIGN_UP, signup);
            args.putBoolean(EXTRA_EXPLORE, explore);
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_showreal, container, false);

            originalProfile = profile = getArguments().getParcelable(EXTRA_PROFILE);
            fromSignup = showAdd = getArguments().getBoolean(EXTRA_SIGN_UP);
            exploreOnly = getArguments().getBoolean(EXTRA_EXPLORE);

            setHasOptionsMenu(fromSignup || exploreOnly);

            binding.setViewModel(new MyRealViewModel(this, profile));

            binding.clipRecycler.setAdapter(adapter = new LibraryAdapter(getActivity(), profile, this));
            binding.segmentRecycler.setAdapter(segmentAdapter = new SegmentAdapter(getActivity(), new SegmentsDragListener(), profile, this));

            loaderHelper = new PresenterLoaderHelper<>(getActivity(), new VideosPresenterFactory(getActivity(), profile));
            getLoaderManager().initLoader(0, null, loaderHelper);

            uploaderHelper = new UploaderLoaderHelper<>(getActivity(), new ReelUploadFactory(getActivity(), profile));
            getLoaderManager().initLoader(2, null, uploaderHelper);

            binding.frame.setOnDragListener(new RealDragListener());

            binding.buttonDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteClip();
                }
            });

            binding.buttonEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editClip();
                }
            });

            return binding.getRoot();
        }


        @Override
        public void onResume() {
            super.onResume();

            presenter = loaderHelper.getPresenter();
            presenter.onViewAttached(this);

            uploader = uploaderHelper.getUploader();
            uploader.onViewAttached(this);
        }

        @Override
        public void onPause() {
            super.onPause();

            if (player != null) {
                player.pause();
                player.clear();
                player.destroy();
                player = null;
            }
        }

        @Override
        public void showLoading(boolean show, boolean isRefresh) {
            if (show) {
                binding.loading.loadingLayout.show();
            } else {
                binding.loading.loadingLayout.hide();
            }
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);

            if (fromSignup) {
                inflater.inflate(R.menu.menu_showreal, menu);
            } else if (exploreOnly) {
                inflater.inflate(R.menu.menu_showreal_explore, menu);
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_done:
                    publish();
                    break;
                case R.id.action_preview:
                    getActivity().finish();
                    break;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void showContent(ReelData content, boolean refresh) {
            reel = content.reel;
            if (refresh) {
                profile = originalProfile;
                adapter.setItems(content.videos);
                segmentAdapter.setVideos(content.reel.videos, getAppComponent().videoHelper(), profile);
                binding.getViewModel().invalidateCount();

            }
            if (refresh || player == null) {
                setupReal(reel, 0);
            }
            questions = content.questions;

            if (fromSignup && showAdd && content.videos.isEmpty()) {
                Intent intent = new Intent(getActivity(), QuestionActivity.class);
                intent.putExtra(QuestionActivity.EXTRA_QUESTIONS, new ArrayList<>(questions));
                intent.putExtra(QuestionActivity.EXTRA_PROFILE, profile);
                intent.putExtra(QuestionActivity.EXTRA_EXISTING, new ArrayList<>(adapter.getItems()));
                intent.putExtra(QuestionActivity.EXTRA_SHOW_UP, false);
                startActivityForResult(intent, RC_ADD_CLIP);
            }
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
        public void uploadComplete(ReelUpdateContent response) {
            originalProfile = this.profile = response.profile;
            presenter.dataSource.profile = response.profile;
            uploader.dataUploader.setProfile(response.profile);

            presenter.loadContent(true);

            if (response.incomplete > 0) {
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_reel_updated)
                        .message(response.incomplete == 1 ? R.string.alert_msg_reel_failed_single : R.string.alert_msg_reel_failed)
                        .positiveButton(R.string.button_ok)
                        .subscribe();
            } else {
                Appboy.getInstance(getActivity()).getCurrentUser().setCustomUserAttribute(Attributes.VIDEOS_PUBLISHED, response.profile.videos.size());

                RxAlertDialog.with(this)
                        .title(R.string.alert_title_reel_updated)
                        .message(R.string.alert_msg_reel_updated)
                        .positiveButton(R.string.button_ok)
                        .cancelable(false)
                        .subscribe(new Action1<Integer>() {
                            @Override
                            public void call(Integer integer) {
                                if (fromSignup) {
                                    getAppComponent().accountHelper().setRealNeeded(false);
                                    Intent intent = new Intent(getActivity(), MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    getActivity().finish();
                                }
                            }
                        });
            }
            action = Action.None;
        }

        @Override
        public void showError(Throwable throwable, String error) {
            if (ErrorHandler.handle(throwable, getActivity())) {
                return;
            }

            if (!(throwable instanceof ReelUploader.ReelUploadException)) {
                FabricHelper.logException(throwable);
            }

            if (action == Action.Upload && throwable instanceof ReelUploader.ReelUploadException) {
                switch (((ReelUploader.ReelUploadException) throwable).type) {
                    case 0:
                        RxAlertDialog.with(this)
                                .title(R.string.alert_title_reel_updated)
                                .message(R.string.alert_msg_reel_empty)
                                .positiveButton(R.string.button_ok)
                                .subscribe();
                        break;
                    case 1:
                        RxAlertDialog.with(this)
                                .title(R.string.alert_title_reel_updated)
                                .message(R.string.alert_msg_reel_incomplete)
                                .positiveButton(R.string.button_ok)
                                .subscribe();
                        break;
                }

            }
            action = Action.None;
        }

        static class SRDragListener implements View.OnDragListener {
            private boolean handlingDrag = false;
            DragType dragType;
            int position;

            public boolean isHandlingDrag() {
                return handlingDrag;
            }

            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        String label = event.getClipDescription().getLabel().toString();
                        String typeString = label.substring(0, label.indexOf("_"));
                        dragType = DragType.valueOf(typeString);
                        position = Integer.parseInt(label.substring(label.indexOf("_") + 1));
                        break;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        handlingDrag = true;
                        break;
                    case DragEvent.ACTION_DRAG_EXITED:
                        handlingDrag = false;
                        break;
                }
                return false;
            }
        }

        class SegmentsDragListener extends SRDragListener {

            private final Drawable normal = getResources().getDrawable(R.drawable.etched_box);
            private final Drawable hover = getResources().getDrawable(R.drawable.hover_box);

            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                super.onDrag(view, dragEvent);

                switch (dragEvent.getAction()) {
                    case DragEvent.ACTION_DRAG_ENTERED:
                        if (dragType == DragType.Library) {
                            view.setBackground(hover);
                        }
                        break;
                    case DragEvent.ACTION_DRAG_EXITED:
                    case DragEvent.ACTION_DRAG_ENDED:
                        view.setBackground(normal);
                        break;
                    case DragEvent.ACTION_DROP:
                        if (dragType == DragType.Library) {
                            addSegment(dragEvent.getClipData(), (ShowRealVideo) dragEvent.getLocalState());
                            return true;
                        }
                        return false;
                }
                return true;
            }
        }

        class RealDragListener extends SRDragListener {

            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                super.onDrag(view, dragEvent);

                switch (dragEvent.getAction()) {
                    case DragEvent.ACTION_DRAG_LOCATION:
                        if (dragType == DragType.Library) {
                            binding.overlay.setHighlight(pointInCircle(dragEvent.getX(), dragEvent.getY()));
                        }
                        break;
                    case DragEvent.ACTION_DRAG_ENDED:
                        binding.overlay.setHighlight(false);
                        if (dragType == DragType.Segment && !dragEvent.getResult()) {
                            removeSegment(position);
                        }
                        break;
                    case DragEvent.ACTION_DRAG_EXITED:
                        binding.overlay.setHighlight(false);
                        break;
                    case DragEvent.ACTION_DROP:
                        if (pointInCircle(dragEvent.getX(), dragEvent.getY())) {
                            if (dragType == DragType.Library) {
                                addSegment(dragEvent.getClipData(), (ShowRealVideo) dragEvent.getLocalState());
                                return true;
                            }
                            return false;
                        }
                        return false;
                }
                return true;
            }
        }

        private boolean pointInCircle(float x, float y) {
            if (player == null) {
                return true;
            }
            ReelPlayer.CircleProperties circle = player.getCircleProperties();
            double d = Math.sqrt((Math.pow((x - circle.centerX), 2) + Math.pow((y - circle.centerY), 2)));
            return d <= circle.radius;
        }

        @Override
        public void moveSegment(int fromPosition, int toPosition) {
            if (toPosition >= segmentAdapter.getItemCount() - 1) {
                return;
            }
            segmentAdapter.moveSegment(fromPosition, toPosition);
            if (fromPosition < toPosition) {
                for (int i = fromPosition; i < toPosition; i++) {
                    Collections.swap(reel.videos, i, i + 1);
                }
            } else {
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(reel.videos, i, i - 1);
                }
            }
            for (int i = 0; i < reel.videos.size(); i++) {
                reel.videos.get(i).question.index = i;
            }

            getAppComponent().reelHelper().updateIndexes(reel.videos)
                    .subscribe(TDSubscribers.ignorant(new Action1<DeviceReel>() {
                        @Override
                        public void call(DeviceReel reel) {
                            setupReal(reel, 0);
                        }
                    }));
        }

        private void removeSegment(int position) {
            RecyclerView.ViewHolder viewHolder = binding.segmentRecycler.findViewHolderForLayoutPosition(position);
            if (viewHolder != null && viewHolder instanceof SegmentAdapter.SegmentViewHolder) {
                removeSegment((SegmentAdapter.SegmentViewHolder) viewHolder);
            }
            binding.getViewModel().invalidateCount();
        }

        private void removeSegment(SegmentAdapter.SegmentViewHolder viewHolder) {
            int position = viewHolder.getAdapterPosition();

            segmentAdapter.removeSegment(position);
            adapter.setItemAdded(viewHolder.video, false);

            Video video = reel.videos.get(position);
            reel.videos.remove(position);
            for (int i = 0; i < reel.videos.size(); i++) {
                reel.videos.get(i).question.index = i;
            }

            getAppComponent().reelHelper().removeVideo(video)
                    .flatMap(new Func1<Boolean, Observable<DeviceReel>>() {
                        @Override
                        public Observable<DeviceReel> call(Boolean aBoolean) {
                            return getAppComponent().reelHelper().updateIndexes(reel.videos);
                        }
                    }).subscribe(TDSubscribers.ignorant(new Action1<DeviceReel>() {
                @Override
                public void call(DeviceReel reel) {
                    setupReal(reel, 0);
                }
            }));
        }

        private final MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();

        private void addSegment(ClipData data, final ShowRealVideo video) {
            ClipData.Item item = data.getItemAt(0);
            String text = item.getText().toString();
            int position = Integer.parseInt(text);
            adapter.setItemAdded(position, true);

            video.question.index = reel.videos.size();
            video.video.question.index = reel.videos.size();

            segmentAdapter.addVideo(video);
            binding.segmentRecycler.smoothScrollToPosition(segmentAdapter.getItemCount() - 1);

            binding.getViewModel().invalidateCount();
            Observable.create(new Observable.OnSubscribe<Video>() {
                @Override
                public void call(Subscriber<? super Video> subscriber) {

                    if (video.path.startsWith("http")) {
                        metadataRetriever.setDataSource(video.path, new HashMap<String, String>());
                    } else {
                        metadataRetriever.setDataSource(getActivity(), Uri.parse(video.path));
                    }
                    String time = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    long duration = Long.parseLong(time);

                    video.video.duration = duration / 1000;

                    subscriber.onNext(video.video);
                    subscriber.onCompleted();
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(new Func1<Video, Observable<DeviceReel>>() {
                        @Override
                        public Observable<DeviceReel> call(Video video) {
                            return getAppComponent().reelHelper()
                                    .addVideoToReel(video);
                        }
                    }).subscribe(new Subscriber<DeviceReel>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(DeviceReel reel) {
                    setupReal(reel, 0);
                }
            });

        }

        private void addSegmentInit(int position, final ShowRealVideo video) {
            adapter.setItemAdded(position, true);

            video.question.index = reel.videos.size();
            video.video.question.index = reel.videos.size();

            segmentAdapter.addVideo(video);
            binding.segmentRecycler.smoothScrollToPosition(segmentAdapter.getItemCount() - 1);

            binding.getViewModel().invalidateCount();
            Observable.create(new Observable.OnSubscribe<Video>() {
                @Override
                public void call(Subscriber<? super Video> subscriber) {

                    if (video.path.startsWith("http")) {
                        metadataRetriever.setDataSource(video.path, new HashMap<String, String>());
                    } else {
                        metadataRetriever.setDataSource(getActivity(), Uri.parse(video.path));
                    }
                    String time = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    long duration = Long.parseLong(time);

                    video.video.duration = duration / 1000;

                    subscriber.onNext(video.video);
                    subscriber.onCompleted();
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap(new Func1<Video, Observable<DeviceReel>>() {
                        @Override
                        public Observable<DeviceReel> call(Video video) {
                            return getAppComponent().reelHelper()
                                    .addVideoToReel(video);
                        }
                    }).subscribe(new Subscriber<DeviceReel>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(DeviceReel reel) {
                    setupReal(reel, 0);
                }
            });

        }

        private void setupReal(DeviceReel reel, int index) {
            binding.getViewModel().invalidateCount();
            if (reel == null) {
                return;
            }

            this.reel = reel;
            if (presenter.content != null) {
                presenter.content.reel = reel;
            }

            if (player != null) {
                player.destroy();
            }

            if (reel.videos.isEmpty()) {
                binding.questionOverlay.setBackgroundResource(R.color.grey_light);
                binding.questionOverlay.setAlpha(1.0f);
                binding.questionOverlay.setVisibility(View.VISIBLE);
                return;
            }

            if (!downloadPrompted) {
                downloadPrompted = true;

                List<Video> checkDownloaded = new ArrayList<>();
                for (Video video : reel.videos) {
                    if (video.published) {
                        checkDownloaded.add(video);
                    }
                }

                downloadPrompt(checkDownloaded);
            }


            player = ReelPlayer.with(getActivity(), -1)
                    .binding(binding)
                    .videos(reel.videos)
                    .audio(true)
                    .profile(profile)
                    .create();
            player.setup(index);
        }

        private void downloadPrompt(final List<Video> checkDownloaded) {
            if (!downloadedNeeded(checkDownloaded)) {
                return;
            }
            RxAlertDialog.with(this)
                    .title(R.string.alert_title_showreal)
                    .message(R.string.alert_msg_download)
                    .positiveButton(R.string.button_download)
                    .negativeButton(R.string.button_not_now)
                    .subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer integer) {
                            if (integer == RxAlertDialog.ButtonPositive) {
                                downloadVideos(checkDownloaded);
                            }
                        }
                    });

        }

        private void downloadVideos(List<Video> videos) {
            showLoading(true, fromSignup);
            final int count = videos.size();
            VideoDownloader downloader = getAppComponent().videoDownloader();
            downloader.downloadUserVideos(profile, videos)
                    .subscribe(new Subscriber<List<String>>() {
                        @Override
                        public void onCompleted() {
                            showLoading(false, fromSignup);
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(List<String> finished) {
                            adapter.notifyDataSetChanged();
                            setupReal(reel, 0);
                            if (finished.size() < count) {
                                showDownloadError();
                            }
                        }
                    });
        }

        private void showDownloadError() {
            RxAlertDialog.with(this)
                    .title(R.string.alert_title_showreal)
                    .message(R.string.alert_msg_download_error)
                    .positiveButton(R.string.button_ok)
                    .subscribe();
        }

        private boolean downloadedNeeded(List<Video> videos) {
            VideoDownloader downloader = getAppComponent().videoDownloader();
            for (Video video : videos) {
                if (!downloader.userVideoDownloaded(video, profile)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void publish() {
            if (exploreOnly) {
                RxAlertDialog.with(this)
                        .title(R.string.alert_title_sign_up)
                        .message(R.string.alert_msg_sign_up)
                        .positiveButton(R.string.button_sign_up)
                        .negativeButton(R.string.button_cancel)
                        .create()
                        .subscribe(new Action1<Integer>() {
                            @Override
                            public void call(Integer integer) {
                                if (integer == RxAlertDialog.ButtonPositive) {
                                    AccountManager.get(getActivity())
                                            .addAccount(getString(R.string.account_type), "session",
                                                    null, null, getActivity(), null, new Handler());
                                    getActivity().finish();
                                }
                            }
                        });
                return;
            }
            action = Action.Upload;
            uploader.uploadContent(reel.videos);
        }

        @Override
        public void addClip() {
            Intent intent = new Intent(getActivity(), QuestionActivity.class);
            intent.putExtra(QuestionActivity.EXTRA_QUESTIONS, new ArrayList<>(questions));
            intent.putExtra(QuestionActivity.EXTRA_PROFILE, profile);
            intent.putExtra(QuestionActivity.EXTRA_EXISTING, new ArrayList<>(adapter.getItems()));
            startActivityForResult(intent, RC_ADD_CLIP);
        }

        @Override
        public void startLibraryDrag(View view, int position) {
            String label = String.format("%s_%d", DragType.Library.name(), position);
            ClipData data = ClipData.newPlainText(label, String.valueOf(position));
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            ShowRealVideo video = (ShowRealVideo) view.getTag(R.id.item);
            view.startDrag(data, shadowBuilder, video, 0);
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }

        @Override
        public void startSegmentDrag(View view) {
            RecyclerView.ViewHolder vh = binding.segmentRecycler.findContainingViewHolder(view);

            if (vh == null || !(vh instanceof SegmentAdapter.SegmentViewHolder)) {
                return;
            }

            SegmentAdapter.SegmentViewHolder holder = (SegmentAdapter.SegmentViewHolder) vh;

            int position = holder.getAdapterPosition();
            String label = String.format("%s_%d", DragType.Segment.name(), position);
            ClipData data = ClipData.newPlainText(label, String.valueOf(position));

            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(holder.binding.thumbnail);
            holder.binding.thumbnail.startDrag(data, shadowBuilder, holder.video, 0);
        }

        @Override
        public void selectClip(int position, ItemClipBinding binding) {
            RecyclerView.ViewHolder holder = this.binding.clipRecycler.findViewHolderForLayoutPosition(selection);
            if (holder != null) {
                ((LibraryAdapter.VideoViewHolder) holder).binding.circle.setHighlight(false);
            }

            ShowRealVideo video = adapter.getItems().get(position);
            this.binding.clipQuestion.setText(video.question.text);
            this.binding.topBar.setVisibility(View.VISIBLE);
            this.binding.scrollView.scrollTo(0, 0);
            binding.circle.setHighlight(true);
            selection = position;
        }

        @Override
        public void selectSegment(View view) {
            RecyclerView.ViewHolder holder = binding.segmentRecycler.findContainingViewHolder(view);
            if (holder != null) {
                segmentAdapter.selectSegment(holder.getAdapterPosition());
                setupReal(reel, holder.getAdapterPosition());
            }

        }

        @Override
        public boolean shouldShowPublish() {
            return !fromSignup;
        }

        public void editClip() {
            ShowRealVideo video = adapter.getItems().get(selection);

            Intent intent = new Intent(getActivity(), CropVideoActivity.class);
            intent.putExtra(CropVideoActivity.EXTRA_VIDEO, video.video);
            intent.putExtra(CropVideoActivity.EXTRA_PROFILE, profile);
            intent.putExtra(CropVideoActivity.EXTRA_IS_EDIT, true);
            startActivityForResult(intent, RC_ADD_CLIP);

            closeBar();
        }

        public void deleteClip() {
            RxAlertDialog.with(this)
                    .title(R.string.alert_title_delete_video)
                    .message(R.string.alert_msg_delete_video)
                    .positiveButton(R.string.button_delete_video)
                    .negativeButton(R.string.button_cancel)
                    .subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer integer) {
                            if (integer == RxAlertDialog.ButtonPositive) {
                                ShowRealVideo video = adapter.getItems().get(selection);

                                getAppComponent().reelHelper().deleteVideo(video.video)
                                        .subscribe(new Subscriber<DeviceReel>() {
                                            @Override
                                            public void onCompleted() {
                                                closeBar();
                                            }

                                            @Override
                                            public void onError(Throwable e) {

                                            }

                                            @Override
                                            public void onNext(DeviceReel reel) {
                                                presenter.loadContent(true);
                                                setupReal(reel, 0);
                                            }
                                        });
                            }
                        }
                    });


        }

        @Override
        public void closeBar() {
            binding.topBar.setVisibility(View.GONE);
            RecyclerView.ViewHolder holder = binding.clipRecycler.findViewHolderForLayoutPosition(selection);
            if (holder != null) {
                ((LibraryAdapter.VideoViewHolder) holder).binding.circle.setHighlight(false);
            }
            selection = -1;
        }

        @Override
        public int getClipCount() {
            return adapter.getItemCount();
        }

        @Override
        public int getSegmentCount() {
            return segmentAdapter.getItemCount();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            showAdd = false;

            if (requestCode == RC_ADD_CLIP) {

                if (resultCode == RESULT_OK) {
                    Video video = data.getParcelableExtra(RecordActivity.EXTRA_VIDEO);

                    getAppComponent().reelHelper().addVideo(video, profile)
                            .subscribe(TDSubscribers.ignorant(new Action1<List<Video>>() {
                                @Override
                                public void call(List<Video> videos) {
                                    adapter.setItems(videos);
                                    binding.getViewModel().invalidateCount();

                                    getAppComponent().reelHelper().getReel()
                                            .subscribe(TDSubscribers.ignorant(new Action1<DeviceReel>() {
                                                @Override
                                                public void call(DeviceReel reel) {
                                                    setupReal(reel, 0);
                                                    segmentAdapter.setVideos(reel.videos, getAppComponent().videoHelper(), profile);
                                                }
                                            }));
                                }
                            }));
                }

                return;
            }
        }

        private static class VideosPresenter extends ContentLoadingPresenter<ReelData, VideoDataSource, ContentLoadingPresenterView<ReelData>> {

            public VideosPresenter(VideoDataSource dataSource) {
                super(dataSource);
            }

            @Override
            public void onViewAttached(@NonNull ContentLoadingPresenterView<ReelData> view) {
                this.view = view;
                if (content != null) {
                    view.showContent(content, false);
                } else {
                    loadContent(true);
                }
            }
        }

        private static class VideosPresenterFactory implements PresenterFactory<VideosPresenter> {

            private final Context context;
            private final Profile profile;

            private VideosPresenterFactory(Context context, Profile profile) {
                this.context = context;
                this.profile = profile;
            }

            @Override
            public VideosPresenter create() {
                return new VideosPresenter(new VideoDataSource(context, profile));
            }
        }

        private static class ReelUploadPresenter extends UploadingPresenter<List<Video>, ReelUpdateContent, ReelUploader, UploadingPresenterView<ReelUpdateContent>> {

            public ReelUploadPresenter(ReelUploader dataUploader) {
                super(dataUploader);
            }

            public boolean checkContent(List<Video> videos) {
                return !dataUploader.getUploads(videos).isEmpty();
            }
        }

        private static class ReelUploadFactory implements UploaderFactory<ReelUploadPresenter> {

            final Context context;
            final Profile profile;

            private ReelUploadFactory(Context context, Profile profile) {
                this.context = context;
                this.profile = profile;
            }

            @Override
            public ReelUploadPresenter create() {
                return new ReelUploadPresenter(new ReelUploader(context, profile));
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            if (player != null) {
                player.destroy();
            }
        }

        @Override
        public boolean onBackPressed() {
            if (selection != -1) {
                closeBar();
                return true;
            }
            if (!fromSignup && !exploreOnly) {
                if (uploader.checkContent(reel.videos)) {
                    RxAlertDialog.with(this)
                            .title(R.string.alert_title_publish)
                            .message(R.string.alert_msg_publish)
                            .positiveButton(R.string.button_publish_now)
                            .negativeButton(R.string.button_remind_me)
                            .subscribe(new Action1<Integer>() {
                                @Override
                                public void call(Integer integer) {
                                    switch (integer) {
                                        case RxAlertDialog.ButtonPositive:
                                            publish();
                                            break;
                                        default:
                                            getActivity().finish();
                                    }
                                }
                            });
                    return true;
                }
                return false;
            }

            return super.onBackPressed();
        }
    }
}

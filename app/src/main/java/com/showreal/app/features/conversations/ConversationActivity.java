package com.showreal.app.features.conversations;

import android.Manifest;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.adapter.BottomSheetItemClickListener;
import com.sendbird.android.GroupChannel;
import com.showreal.app.BaseActivity;
import com.showreal.app.ErrorHandler;
import com.showreal.app.FabricHelper;
import com.showreal.app.R;
import com.showreal.app.RxAlertDialog;
import com.showreal.app.data.model.Match;
import com.showreal.app.data.model.Message;
import com.showreal.app.databinding.ActivityConversationBinding;
import com.showreal.app.features.conversations.unmatch.UnmatchActivity;
import com.showreal.app.features.profile.SREasyImage;
import com.showreal.app.features.profile.other.OtherProfileActivity;
import com.showreal.app.features.settings.ReportActivity;
import com.tbruyelle.rxpermissions.Permission;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;
import java.io.IOException;
import java.util.List;

import nl.nl2312.rxcupboard.DatabaseChange;
import pl.aprilapps.easyphotopicker.EasyImage;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
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

public class ConversationActivity extends BaseActivity implements ContentLoadingPresenterView<List<Message>>, UploadingPresenterView<Message>, ConversationViewModel.ConversationView, MediaViewModel.MediaView {

    public static final String EXTRA_MATCH = "match";
    private static final int RC_UNMATCH = 0x0;
    public static final String EXTRA_FROM_NOTIFICATION = "from_notification";

    private ActivityConversationBinding binding;
    private PresenterLoaderHelper<ConversationPresenter> loaderHelper;
    private UploaderLoaderHelper<ConversationUploader> uploaderHelper;
    private ConversationPresenter presenter;
    private ConversationUploader uploader;
    private ConversationAdapter adapter;
    private Subscription changesSubscription;
    private MessagesDataSource dataSource;
    private boolean firstLoad = true;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Match match = getIntent().getParcelableExtra(EXTRA_MATCH);
        binding.setViewModel(new ConversationViewModel(match, this));

        if (getIntent().getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
            String channel = match.conversationUrl;
            NotificationManagerCompat.from(this).cancel(channel.hashCode());
        }

        ((LinearLayoutManager) binding.recycler.getLayoutManager()).setReverseLayout(true);
        binding.recycler.setAdapter(adapter = new ConversationAdapter(this, match, this));

        dataSource = new MessagesDataSource(this, match);

        loaderHelper = new PresenterLoaderHelper<>(this, new ConversationPresenterFactory(dataSource));
        uploaderHelper = new UploaderLoaderHelper<>(this, new ConversationUploaderFactory(dataSource));

        getSupportLoaderManager().initLoader(0, null, loaderHelper);
        getSupportLoaderManager().initLoader(2, null, uploaderHelper);

        getCached();
        trackChanges();
    }

    private void trackChanges() {
        changesSubscription = dataSource.getChanges()
                .subscribe(TDSubscribers.ignorant(new Action1<DatabaseChange<Message>>() {
                    @Override
                    public void call(DatabaseChange<Message> change) {
                        if (change instanceof DatabaseChange.DatabaseDelete) {
                            adapter.removeItem(change.entity());
                        } else {
                            adapter.addItem(change.entity());
                            binding.recycler.smoothScrollToPosition(0);
                        }
                    }
                }));
    }

    private void getCached() {
        dataSource.getCached()
                .subscribe(TDSubscribers.ignorant(new Action1<List<Message>>() {
                    @Override
                    public void call(List<Message> messages) {
                        adapter.addItems(messages);
                        binding.recycler.smoothScrollToPosition(0);
                    }
                }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        changesSubscription.unsubscribe();
    }

    final android.databinding.Observable.OnPropertyChangedCallback availabilityChange = new android.databinding.Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(android.databinding.Observable observable, int i) {
            binding.getViewModel().setAvailable(((ObservableBoolean) observable).get());
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        presenter = loaderHelper.getPresenter();

        if (firstLoad) {
            firstLoad = false;
            showLoading(true, false);
            presenter.dataSource.getGroupChannel()
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<GroupChannel>() {
                        @Override
                        public void onCompleted() {
                            if (adapter.getItemCount() > 0) {
                                showLoading(false, false);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            showError(e, null);
                        }

                        @Override
                        public void onNext(GroupChannel channel) {
                            getCached();
                            presenter.onViewAttached(ConversationActivity.this);
                        }
                    });
        } else {
            presenter.onViewAttached(this);
        }

        uploader = uploaderHelper.getUploader();
        uploader.onViewAttached(this);

        binding.getViewModel().setAvailable(getAppComponent().sendbird().sendbirdAvailable.get());
        getAppComponent().sendbird().sendbirdAvailable.addOnPropertyChangedCallback(availabilityChange);
    }

    @Override
    protected void onPause() {
        super.onPause();

        getAppComponent().sendbird().sendbirdAvailable.removeOnPropertyChangedCallback(availabilityChange);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_profile:
                openProfile(binding.getViewModel().match);
                return true;
            case R.id.action_unmatch:
                unmatch();
                return true;
            case R.id.action_report:
                report();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void report() {
        Intent intent = new Intent(this, ReportActivity.class);
        intent.putExtra(ReportActivity.EXTRA_PROFILE_ID, binding.getViewModel().match.profile.id);
        startActivity(intent);
    }

    private void unmatch() {
        Intent unmatchIntent = new Intent(this, UnmatchActivity.class);
        unmatchIntent.putExtra(UnmatchActivity.EXTRA_MATCH, binding.getViewModel().match);
        startActivityForResult(unmatchIntent, RC_UNMATCH);
    }

    @Override
    public void showLoading(boolean show, boolean isRefresh) {
        binding.progress.setVisibility(show && adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showContent(List<Message> messages, boolean refresh) {
        adapter.addItems(messages);
    }

    @Override
    public void showUploading(boolean show) {
        binding.sendProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.buttonSend.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void uploadComplete(Message response) {
        adapter.addItem(response);
        binding.recycler.smoothScrollToPosition(0);
    }

    @Override
    public void showError(Throwable throwable, String error) {
        if (dataSource.getMessage() != null) {
            binding.getViewModel().message.set(dataSource.getMessage());
        }
        if (throwable instanceof IOException || ErrorHandler.isSendbirdConnectionError(throwable)) {
            RxAlertDialog.with(this)
                    .title(R.string.alert_title_offline)
                    .message(R.string.alert_msg_offline)
                    .positiveButton(R.string.button_ok)
                    .subscribe();
            return;
        }

        if (ErrorHandler.handle(throwable, this)) {
            return;
        }

        FabricHelper.logException(throwable);
    }

    @Override
    public void sendMessage(String text) {
        uploader.uploadContent(new Message(text));
    }

    @Override
    public void chooseMedia() {
        new BottomSheetBuilder(this, binding.coordinator)
                .setMode(BottomSheetBuilder.MODE_LIST)
                .setMenu(R.menu.sheet_media)
                .setItemClickListener(new BottomSheetItemClickListener() {
                    @Override
                    public void onBottomSheetItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_image:
                                chooseImage();
                                break;
                            case R.id.action_video:
                                chooseVideo();
                                break;

                        }
                    }
                }).createDialog().show();
    }

    private void chooseVideo() {
        if (RxPermissions.getInstance(this).isGranted(Manifest.permission.CAMERA)) {
            EasyVideo.openChooserWithDocuments(ConversationActivity.this, true);
        } else {
            getPermissionVideo();
        }
    }

    private void getPermissionVideo() {
        RxPermissions.getInstance(this)
                .requestEach(Manifest.permission.CAMERA)
                .subscribe(new Action1<Permission>() {
                    @Override
                    public void call(Permission permission) {
                        if (permission.granted) {
                            chooseImage();
                        } else {
                            EasyVideo.openChooserWithDocuments(ConversationActivity.this, false);
                        }
                    }
                });
    }

    public void chooseImage() {
        if (RxPermissions.getInstance(this).isGranted(Manifest.permission.CAMERA)) {
            EasyImage.openChooserWithDocuments(this, getString(R.string.choose_image), 0);
        } else {
            getPermission();
        }
    }

    private void getPermission() {
        RxPermissions.getInstance(this)
                .requestEach(Manifest.permission.CAMERA)
                .subscribe(new Action1<Permission>() {
                    @Override
                    public void call(Permission permission) {
                        if (permission.granted) {
                            chooseImage();
                        } else {
                            SREasyImage.openChooserWithDocuments(ConversationActivity.this, getString(R.string.choose_image), 0, false);
                        }
                    }
                });
    }


    @Override
    public void openMedia(Message message) {
        if (message.mediaType.startsWith("video")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(message.mediaUrl), message.mediaType);
            startActivity(intent);
            return;
        }
        PhotoDialog dialog = PhotoDialog.newInstance(message);
        dialog.show(getSupportFragmentManager(), "photo");
    }

    @Override
    public void openProfile(Match match) {
        Intent intent = new Intent(this, OtherProfileActivity.class);
        intent.putExtra(OtherProfileActivity.EXTRA_PROFILE, match.profile);
        intent.putExtra(OtherProfileActivity.EXTRA_SOURCE, OtherProfileActivity.SOURCE_MATCHES);
        startActivity(intent);
    }

    private static class ConversationPresenter extends ContentLoadingPresenter<List<Message>, MessagesDataSource, ContentLoadingPresenterView<List<Message>>> {

        ConversationPresenter(MessagesDataSource dataSource) {
            super(dataSource);
        }

        Observable<List<Message>> getCached() {
            return dataSource.getCached()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        }

        public void reset() {
            content = null;
        }
    }

    private static class ConversationPresenterFactory implements PresenterFactory<ConversationPresenter> {


        private final MessagesDataSource dataSource;

        private ConversationPresenterFactory(MessagesDataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public ConversationPresenter create() {
            return new ConversationPresenter(dataSource);
        }
    }

    private static class ConversationUploader extends UploadingPresenter<Message, Message, MessagesDataSource, UploadingPresenterView<Message>> {

        ConversationUploader(MessagesDataSource dataSource) {
            super(dataSource);
        }
    }

    private static class ConversationUploaderFactory implements UploaderFactory<ConversationUploader> {

        private final MessagesDataSource dataSource;

        private ConversationUploaderFactory(MessagesDataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public ConversationUploader create() {
            return new ConversationUploader(dataSource);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_UNMATCH && resultCode == RESULT_OK) {
            finish();
            return;
        }

        EasyVideo.handleActivityResult(requestCode, resultCode, data, this, new EasyImage.Callbacks() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {

            }

            @Override
            public void onImagePicked(File imageFile, EasyImage.ImageSource source, int type) {
                uploader.uploadContent(new Message(imageFile));
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {

            }
        });
    }
}

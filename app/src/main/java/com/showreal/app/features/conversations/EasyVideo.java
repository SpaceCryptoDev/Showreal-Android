package com.showreal.app.features.conversations;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import pl.aprilapps.easyphotopicker.EasyImage;
import pl.aprilapps.easyphotopicker.EasyImageConfig;
import uk.co.thedistance.thedistancecore.Version;

public class EasyVideo {

    private static final String PREFS_VIDEO_URI = "pl.aprilapps.easyphotopicker.photo_uri";
    private static final String PREFS_LAST_URI = "pl.aprilapps.easyphotopicker.last_photo";

    static Intent videoChooserIntent(Context context, File file) throws IOException {

        Uri outputFileUri = file == null ? createVideoFile(context) : Uri.fromFile(file);
        List<Intent> cameraIntents = new ArrayList<>();
        Intent captureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> camList = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : camList) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            grantWritePermission(context, intent, outputFileUri);
            cameraIntents.add(intent);
        }
        Intent docsIntent = createDocumentsIntent();

        Intent chooserIntent = Intent.createChooser(docsIntent, "Choose video");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

        return chooserIntent;
    }

    static Intent videoChooserIntent(Context context) throws IOException {
        Intent docsIntent = createDocumentsIntent();
        return Intent.createChooser(docsIntent, "Choose video");
    }

    private static void grantWritePermission(Context context, Intent intent, Uri uri) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    private static void revokeWritePermission(Context context, Uri uri) {
        context.revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private static Intent createDocumentsIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        return intent;
    }

    private static Uri createVideoFile(Context context) throws IOException {
        File mediaFile = getMediaFile(context);

        Uri uri;
        if (Version.isNougat()) {
            String packageName = context.getApplicationContext().getPackageName();
            String authority = packageName + ".easyphotopicker.fileprovider";
            uri = FileProvider.getUriForFile(context, authority, mediaFile);
        } else {
            uri = Uri.fromFile(mediaFile);
        }

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(PREFS_VIDEO_URI, uri.toString())
                .putString(PREFS_LAST_URI, mediaFile.toString())
                .apply();



        return uri;
    }

    public static File getMediaFile(Context context) throws IOException {

        File cacheDir = context.getCacheDir();

        if (isExternalStorageWritable()) {
            cacheDir = context.getExternalCacheDir();
        }

        File dir = new File(cacheDir, "EasyImage");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File imageFile = File.createTempFile(UUID.randomUUID().toString(), ".mp4", dir);
        return imageFile;
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static void handleActivityResult(int requestCode, int resultCode, Intent data, final Activity activity, final EasyImage.Callbacks callbacks) {
        EasyImage.handleActivityResult(requestCode, resultCode, data, activity, new EasyImage.Callbacks() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                callbacks.onImagePickerError(e, source, type);
            }

            @Override
            public void onImagePicked(File imageFile, EasyImage.ImageSource source, int type) {

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                String videoUri = preferences.getString(PREFS_VIDEO_URI, null);
                if (!TextUtils.isEmpty(videoUri)) {
                    revokeWritePermission(activity, Uri.parse(videoUri));
                    preferences
                            .edit().remove(PREFS_VIDEO_URI)
                            .apply();
                }

                callbacks.onImagePicked(imageFile, source, type);
            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                if (source == EasyImage.ImageSource.CAMERA) {
                    File photoFile = EasyImage.lastlyTakenButCanceledPhoto(activity);
                    if (photoFile != null) {
                        photoFile.delete();
                    }
                    String uri = PreferenceManager.getDefaultSharedPreferences(activity).getString(PREFS_LAST_URI, null);
                    if (uri != null) {
                        File file = new File(uri);
                        if (file != null && file.exists()) {
                            file.delete();
                        }
                    }
                }
                callbacks.onCanceled(source, type);
            }
        });
    }

    public static void openChooserWithDocuments(Activity context, boolean includeCamera) {
        try {
            Intent intent = includeCamera ? EasyVideo.videoChooserIntent(context, null) : EasyVideo.videoChooserIntent(context);
            context.startActivityForResult(intent, EasyImageConfig.REQ_SOURCE_CHOOSER);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void openChooserWithDocuments(Activity context, File file) {
        try {
            Intent intent = EasyVideo.videoChooserIntent(context, file);
            context.startActivityForResult(intent, EasyImageConfig.REQ_SOURCE_CHOOSER);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

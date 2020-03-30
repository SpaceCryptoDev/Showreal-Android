package com.showreal.app.features.profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import java.io.IOException;

import pl.aprilapps.easyphotopicker.EasyImage;


/**
 * Created by Jacek Kwiecień on 16.10.2015.
 */
@SuppressWarnings({"unused", "FieldCanBeLocal", "ResultOfMethodCallIgnored"})
public class SREasyImage extends EasyImage {

    private static final String KEY_TYPE = "pl.aprilapps.easyphotopicker.type";

    private static void storeType(Context context, int type) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(KEY_TYPE, type).commit();
    }

    private static Intent createChooserIntentWithoutCamera(Context context, String chooserTitle, int type) throws IOException {
        Intent galleryIntent = createDocumentsIntent(context, type);
        return Intent.createChooser(galleryIntent, chooserTitle);
    }

    public static void openChooserWithDocuments(Fragment fragment, String chooserTitle, int type, boolean showCamera) {
        if (showCamera) {
            openChooserWithDocuments(fragment, chooserTitle, type);
            return;
        }
        try {
            Intent intent = createChooserIntentWithoutCamera(fragment.getActivity(), chooserTitle, type);
            fragment.startActivityForResult(intent, REQ_SOURCE_CHOOSER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void openChooserWithDocuments(Activity activity, String chooserTitle, int type, boolean showCamera) {
        if (showCamera) {
            openChooserWithDocuments(activity, chooserTitle, type);
            return;
        }
        try {
            Intent intent = createChooserIntentWithoutCamera(activity, chooserTitle, type);
            activity.startActivityForResult(intent, REQ_SOURCE_CHOOSER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Intent createDocumentsIntent(Context context, int type) {
        storeType(context, type);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        return intent;
    }
}
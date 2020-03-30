package com.showreal.app;

import android.app.Activity;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.showreal.app.databinding.ToastNotificationBinding;

public class SRToast {

    private static final int TOAST_DURATION = 2000;
    private static View toastView;

    public static void showToast(final BaseActivity activity, String text) {
        if (activity.isActivityUnavailable()) {
            return;
        }
        WindowManager windowManager = activity.getWindowManager();

        ToastNotificationBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity), R.layout.toast_notification, null, false);

        float statusBarHeight = getStatusBarHeight(activity);

        binding.text.setText(text);

        toastView = binding.getRoot();
        toastView.setMinimumHeight((int) (binding.getRoot().getMinimumHeight() + statusBarHeight));
        toastView.setPadding(0, (int) statusBarHeight, 0, 0);
        windowManager.addView(toastView, getLayoutParams());

        final Runnable dismissRunnable = new Runnable() {
            @Override
            public void run() {
                toastView.setOnClickListener(null);
                if (!activity.isActivityUnavailable()) {
                    activity.getWindowManager().removeView(toastView);
                    toastView = null;
                }
            }
        };
        final Handler dismissHandler = new Handler(Looper.getMainLooper());
        dismissHandler.postDelayed(dismissRunnable, TOAST_DURATION + activity.getResources().getInteger(android.R.integer.config_mediumAnimTime));

        toastView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissHandler.removeCallbacksAndMessages(null);
                dismissHandler.post(dismissRunnable);
            }
        });

    }

    private static WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();

        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        layoutParams.format = PixelFormat.RGB_888;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.gravity = Gravity.TOP;
        layoutParams.windowAnimations = R.style.Animation_Toast;
        return layoutParams;
    }

    private static float getScreenHeight(Activity activity) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager()
                .getDefaultDisplay()
                .getMetrics(displaymetrics);
        return displaymetrics.heightPixels;
    }

    private static float getStatusBarHeight(Context context) {
        float result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}

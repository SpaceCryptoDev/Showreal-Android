package com.showreal.app;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import uk.co.thedistance.thedistancetheming.fonts.FontCache;

public class SRSnackbar {

    @IntDef({LENGTH_INDEFINITE, LENGTH_SHORT, LENGTH_LONG})
    @IntRange(from = 1)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Duration {
    }

    public static final int LENGTH_INDEFINITE = -2;
    public static final int LENGTH_SHORT = -1;
    public static final int LENGTH_LONG = 0;

    public static void show(@NonNull View view, @StringRes int resId, @Duration int duration) {
        make(view, resId, duration).show();
    }

    public static void show(@NonNull View view, String text, @Duration int duration) {
        make(view, text, duration).show();
    }

    public static Snackbar make(@NonNull View view, String text, @Duration int duration) {
        Snackbar snackbar = Snackbar.make(view, text, duration);
        customise(view.getContext(), snackbar);
        return snackbar;
    }

    public static Snackbar make(@NonNull View view, @StringRes int resId, @Duration int duration) {
        Snackbar snackbar = Snackbar.make(view, resId, duration);
        customise(view.getContext(), snackbar);
        return snackbar;
    }

    private static void customise(Context context, Snackbar snackbar) {
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();

        layout.setBackgroundColor(ContextCompat.getColor(context, R.color.tabs_color));
        TextView tv = (TextView) layout.findViewById(R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        tv.setTypeface(FontCache.getInstance(context).get(context.getString(R.string.FontBody1)));
    }

    public static Snackbar setAction(Snackbar snackbar, @StringRes int resId, View.OnClickListener listener) {
        snackbar.setAction(resId, listener);

        Context context = snackbar.getView().getContext();
        TextView action = (TextView) snackbar.getView().findViewById(R.id.snackbar_action);
        action.setTypeface(FontCache.getInstance(context).get(context.getString(R.string.FontButtonBold)));
        return snackbar;
    }
}

package com.showreal.app;

import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.support.annotation.IntDef;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.showreal.app.databinding.DialogShowrealBinding;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import uk.co.thedistance.thedistancetheming.fonts.Font;

public class RxAlertDialog {

    final AlertDialog.Builder builder;

    public static final int ButtonNegative = 0;
    public static final int ButtonPositive = 1;
    public static final int ButtonNeutral = 2;
    private final Context context;
    private Subscriber<? super Integer> subscriber;
    private int title;
    private int message;
    private String messageString;

    @IntDef({ButtonNegative, ButtonPositive, ButtonNeutral})
    @interface ButtonType {
    }

    private RxAlertDialog(Context context) {
        builder = new AlertDialog.Builder(context);
        this.context = context;
    }

    public static RxAlertDialog with(Context context) {
        return new RxAlertDialog(context);
    }

    public static RxAlertDialog with(Fragment fragment) {
        return new RxAlertDialog(fragment.getActivity());
    }

    public static RxAlertDialog with(android.app.Fragment fragment) {
        return new RxAlertDialog(fragment.getActivity());
    }

    public RxAlertDialog title(@StringRes int title) {
        this.title = title;
        return this;
    }

    public RxAlertDialog message(@StringRes int message) {
        this.message = message;
        return this;
    }

    public RxAlertDialog message(String message) {
        this.messageString = message;
        return this;
    }

    public RxAlertDialog negativeButton(@StringRes int negative) {
        builder.setNegativeButton(negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                subscriber.onNext(ButtonNegative);
                subscriber.onCompleted();
            }
        });
        return this;
    }

    public RxAlertDialog positiveButton(@StringRes int positive) {
        builder.setPositiveButton(positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                subscriber.onNext(ButtonPositive);
                subscriber.onCompleted();
            }
        });

        return this;
    }

    public RxAlertDialog neutralButton(@StringRes int neutral) {
        builder.setNegativeButton(neutral, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                subscriber.onNext(ButtonNeutral);
                subscriber.onCompleted();
            }
        });

        return this;
    }

    public RxAlertDialog cancelable(boolean cancelable) {
        builder.setCancelable(cancelable);
        return this;
    }

    public Observable<Integer> create() {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                RxAlertDialog.this.subscriber = subscriber;


                DialogShowrealBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_showreal, null, false);
                binding.title.setText(title);

                if (messageString != null) {
                    binding.text.setText(messageString);
                } else {
                    binding.text.setText(message);
                }

                builder.setView(binding.getRoot());
                AlertDialog dialog = builder.create();
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.show();

                dialog.getWindow().setLayout((int) (280 * context.getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT);

                Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (positive != null) {
                    Font.setFont(positive, positive.getResources().getString(R.string.FontBody2));
                }
                Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                if (negative != null) {
                    Font.setFont(negative, negative.getResources().getString(R.string.FontButton));
                }
                Button neutral = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                if (neutral != null) {
                    Font.setFont(neutral, neutral.getResources().getString(R.string.FontButton));
                }
            }
        }).observeOn(AndroidSchedulers.mainThread());
    }

    public Subscription subscribe(Action1<Integer> action1) {
        return create().subscribe(action1);
    }

    public Subscription subscribe() {
        return create().subscribe();
    }
}

package com.showreal.app.features.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.showreal.app.R;
import com.showreal.app.databinding.PreferenceSegmentBinding;

import uk.co.thedistance.thedistancetheming.fonts.Font;

public class SegmentPreference extends Preference implements RadioGroup.OnCheckedChangeListener {

    private final
    @StringRes
    int firstResId;
    private final
    @StringRes
    int secondResId;
    private int selection = 0;
    private PreferenceSegmentBinding binding;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SegmentPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SegmentPreference, defStyleAttr, 0);

        firstResId = a.getResourceId(R.styleable.SegmentPreference_first, R.string.height_cm);
        secondResId = a.getResourceId(R.styleable.SegmentPreference_second, R.string.height_feet);

        a.recycle();
    }

    public SegmentPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SegmentPreference, defStyleAttr, 0);

        firstResId = a.getResourceId(R.styleable.SegmentPreference_first, R.string.height_cm);
        secondResId = a.getResourceId(R.styleable.SegmentPreference_second, R.string.height_feet);

        a.recycle();
    }

    public SegmentPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.segmentPreferenceStyle);
    }

    public SegmentPreference(Context context) {
        this(context, null);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        final LayoutInflater layoutInflater =
                (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.preference_segment, parent, false);

        Font.setFont(binding.getRoot().findViewById(android.R.id.title), getContext().getString(R.string.FontTitle));
        Font.setFont(binding.unitFirst, getContext().getString(R.string.FontButton));
        Font.setFont(binding.unitSecond, getContext().getString(R.string.FontButton));

        binding.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

        return binding.getRoot();
    }

    private void toggle() {
        setSelection(selection == 0 ? 1 : 0);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        binding.unitFirst.setText(firstResId);
        binding.unitSecond.setText(secondResId);
        binding.units.setOnCheckedChangeListener(this);
        setSelection(selection);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setSelection(restorePersistedValue ? getPersistedInt(selection) : (int) defaultValue);
    }

    public void setSelection(int selection) {
        if (binding != null && binding.unitFirst != null) {
            binding.unitFirst.setChecked(selection == 0);
            binding.unitSecond.setChecked(selection == 1);
        }
        this.selection = selection;
        persistInt(selection);
        notifyChanged();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.unit_first:
                selection = 0;
                break;
            case R.id.unit_second:
                selection = 1;
                break;
        }
        persistInt(selection);
        notifyChanged();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        /*
         * Suppose a client uses this preference type without persisting. We
         * must save the instance state so it is able to, for example, survive
         * orientation changes.
         */

        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        // Save the instance state
        final SavedState myState = new SavedState(superState);
        myState.selection = selection;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        selection = myState.selection;
        notifyChanged();
    }

    private static class SavedState extends BaseSavedState {

        int selection;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeInt(selection);
        }

        public SavedState(Parcel source) {
            super(source);

            selection = source.readInt();
        }

        @SuppressWarnings("unused")
        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}

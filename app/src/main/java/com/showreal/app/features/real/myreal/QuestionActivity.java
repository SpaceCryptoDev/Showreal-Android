package com.showreal.app.features.real.myreal;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.bumptech.glide.Glide;
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.github.rubensousa.bottomsheetbuilder.adapter.BottomSheetItemClickListener;
import com.showreal.app.BaseActivity;
import com.showreal.app.R;
import com.showreal.app.data.model.Profile;
import com.showreal.app.data.model.Question;
import com.showreal.app.data.model.Video;
import com.showreal.app.databinding.ActivityQuestionBinding;
import com.showreal.app.databinding.PageQuestionBinding;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

public class QuestionActivity extends BaseActivity {

    static final String EXTRA_QUESTIONS = "questions";
    public static final String EXTRA_SHOW_UP = "show_up";
    private static final int RC_RECORD = 0x0;
    private static final int RC_CHOOSE = 0x2;
    public static final String EXTRA_EXISTING = "existing";
    public static final String EXTRA_PROFILE = "profile";
    private ActivityQuestionBinding binding;
    private QuestionAdapter adapter;
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM d, yyyy");
    private SparseArray<Video> answered = new SparseArray<>();
    private Profile profile;
    private float pageWidth;
    private float endSpaceWidth;

    @Override
    protected String getScreenName() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_question);
        setSupportActionBar(binding.toolbar);

        boolean showUp = getIntent().getBooleanExtra(EXTRA_SHOW_UP, true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(showUp);

        final List<Question> questions = getIntent().getParcelableArrayListExtra(EXTRA_QUESTIONS);
        Collections.sort(questions);

        List<ShowRealVideo> existingVideos = getIntent().getParcelableArrayListExtra(EXTRA_EXISTING);

        for (ShowRealVideo video : existingVideos) {
            answered.put(video.question.id, video.video);
        }

        profile = getIntent().getParcelableExtra(EXTRA_PROFILE);

        binding.pager.setPageMargin((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, getResources().getDisplayMetrics()));

        binding.pager.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.pager.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                int padding = (int) (binding.getRoot().getWidth() * 0.235f);
                pageWidth = (binding.pager.getWidth() * 0.53f) / (binding.pager.getWidth() - padding);
                binding.pager.setPadding(padding, 0, 0, 0);
                binding.pager.setAdapter(adapter = new QuestionAdapter(QuestionActivity.this, questions));
                endSpaceWidth = (float) padding / (binding.pager.getWidth() - padding);

                if (!questions.isEmpty()) {
                    setSubtitle(0);
                }
            }
        });

        binding.pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                setAlpha(position, 1 - positionOffset);
                setAlpha(position + 1, positionOffset);
            }

            @Override
            public void onPageSelected(int position) {
                setSubtitle(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void openQuestion(final Question question) {
        if (answered.get(question.id) != null) {
            Intent intent = new Intent(this, CropVideoActivity.class);
            intent.putExtra(CropVideoActivity.EXTRA_VIDEO, answered.get(question.id));
            intent.putExtra(CropVideoActivity.EXTRA_PROFILE, profile);
            intent.putExtra(CropVideoActivity.EXTRA_IS_EDIT, true);
            startActivityForResult(intent, RC_CHOOSE);
            return;
        }

        new BottomSheetBuilder(this, binding.coordinator)
                .setMode(BottomSheetBuilder.MODE_LIST)
                .setMenu(R.menu.sheet_reel)
                .setItemClickListener(new BottomSheetItemClickListener() {
                    @Override
                    public void onBottomSheetItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_record:
                                Intent intent = new Intent(QuestionActivity.this, RecordActivity.class);
                                intent.putExtra(RecordActivity.EXTRA_QUESTION, question);
                                intent.putExtra(RecordActivity.EXTRA_PROFILE, profile);
                                startActivityForResult(intent, RC_RECORD);
                                break;
                            case R.id.action_choose:
                                Intent chooseIntent = new Intent(QuestionActivity.this, ClipAdjustActivity.class);
                                chooseIntent.putExtra(ClipAdjustActivity.EXTRA_QUESTION, question);
                                chooseIntent.putExtra(ClipAdjustActivity.EXTRA_PROFILE, profile);
                                startActivityForResult(chooseIntent, RC_CHOOSE);
                                break;
                        }
                    }
                }).createDialog().show();
    }

    private void setSubtitle(int position) {
        Question question = adapter.questions.get(position);
        if (question.questionType == 1) {
            binding.subtitle.setText(getString(R.string.question_featured, DATE_FORMAT.format(question.expiryDate)));
        } else {
            binding.subtitle.setText(R.string.question_choose);
        }
    }

    public void setAlpha(int position, float alpha) {
        String activeTag = String.format("active_%d", position);
        String normalTag = String.format("normal_%d", position);
        String textTag = String.format("text_%d", position);
        String imageTag = String.format("image_%d", position);
        String iconTag = String.format("icon_%d", position);

        float height = 0.39f + ((0.1f * alpha));
        float imageHeight = 0.24f + ((0.1f * alpha));

        View active = binding.pager.findViewWithTag(activeTag);
        if (active != null) {
            active.setAlpha(alpha);
            PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) active.getLayoutParams();
            params.getPercentLayoutInfo().heightPercent = height;
            active.requestLayout();
        }
        View normal = binding.pager.findViewWithTag(normalTag);
        if (normal != null) {
            PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) normal.getLayoutParams();
            params.getPercentLayoutInfo().heightPercent = height;
            normal.requestLayout();
        }
        View image = binding.pager.findViewWithTag(imageTag);
        if (image != null) {
            PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) image.getLayoutParams();
            params.getPercentLayoutInfo().heightPercent = imageHeight;
            image.requestLayout();
        }
        View question = binding.pager.findViewWithTag(textTag);
        if (question != null) {
            float scale = 1f + ((0.2f * alpha));
            ViewCompat.setScaleY(question, scale);
            ViewCompat.setScaleX(question, scale);
        }
        View icon = binding.pager.findViewWithTag(iconTag);
        if (icon != null) {
            PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) icon.getLayoutParams();
            params.getPercentLayoutInfo().topMarginPercent = 0.09f + (0.02f * alpha);
            icon.requestLayout();
        }
    }

    private class QuestionAdapter extends PagerAdapter {

        private final List<Question> questions;
        private final LayoutInflater inflater;
        private final int gradientBottom;

        private QuestionAdapter(Context context, List<Question> questions) {
            inflater = LayoutInflater.from(context);
            this.questions = questions;

            gradientBottom = ContextCompat.getColor(context, R.color.purple_dark);
        }

        @Override
        public float getPageWidth(int position) {
            if (position == getCount() - 1) {
                return 1f;
            }
            return pageWidth;
        }

        @Override
        public int getCount() {
            return questions.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            PageQuestionBinding binding = DataBindingUtil.inflate(inflater, R.layout.page_question, container, true);
            final Question question = questions.get(position);
            binding.question.setText(question.text);
            binding.active.setTag(String.format("active_%d", position));
            binding.normal.setTag(String.format("normal_%d", position));
            binding.question.setTag(String.format("text_%d", position));
            binding.image.setTag(String.format("image_%d", position));
            binding.videoIcon.setTag(String.format("icon_%d", position));

            if (question.colour != null && !question.colour.equals("000000")) {
                String colorString = "#" + question.colour;
                try {
                    int color = Color.parseColor(colorString);
                    binding.active.setGradient(color, gradientBottom);
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (!TextUtils.isEmpty(question.largeImage)) {
                Glide.with(QuestionActivity.this)
                        .load(question.largeImage)
                        .dontTransform()
                        .into(binding.image);
            }

            binding.videoIcon.setVisibility(answered.get(question.id) != null ? View.VISIBLE : View.GONE);

            binding.active.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openQuestion(question);
                }
            });

            if (position == getCount() - 1) {
                PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) binding.space.getLayoutParams();
                params.getPercentLayoutInfo().widthPercent = endSpaceWidth;
                binding.space.setVisibility(View.VISIBLE);
            }

            return binding.getRoot();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
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
    }
}

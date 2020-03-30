package com.showreal.app;

import android.support.v4.view.ViewPager;

import uk.co.thedistance.components.analytics.model.ScreenEvent;

/**
 * Extend this class and implement a {@link PageChangeListener}
 * to automatically send Google Analytics screen views for each item in a {@link ViewPager}.
 */
public abstract class BasePagerFragment extends BaseFragment {

    protected PageChangeListener pageChangeListener;
    private int currentPage = 0;

    @Override
    protected void sendScreen() {
        super.sendScreen();

        if (!trackingEnabled) {
            return;
        }

        // If fragment uses PageChangeListener, send current page on resume
        if (pageChangeListener != null) {
            pageChangeListener.onPageSelected(currentPage);
        }
    }


    protected class PageChangeListener implements ViewPager.OnPageChangeListener {

        private String[] screenNames;

        public PageChangeListener(String... screenNames) {
            this.screenNames = screenNames;

            pageChangeListener = this;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            currentPage = position;
            if (screenNames.length > position) {
                getAppComponent().analytics().send(new ScreenEvent(screenNames[position]));
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }
}

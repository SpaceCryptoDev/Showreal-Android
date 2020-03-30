package com.showreal.app.features.reviews;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.showreal.app.databinding.ItemRealBinding;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

public class RealLayoutManager extends LinearLayoutManager {
    public RealLayoutManager(Context context) {
        super(context);
    }

    public RealLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public RealLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public int findLastCompletelyVisibleItemPosition() {
        final View child = findOneVisibleChild(getChildCount() - 1, -1, true, false);
        return child == null ? NO_POSITION : getPosition(child);
    }

    View findOneVisibleChild(int fromIndex, int toIndex, boolean completelyVisible,
                             boolean acceptPartiallyVisible) {

        final int start = orientationHelper.getStartAfterPadding();
        final int end = orientationHelper.getEndAfterPadding();

        final int next = toIndex > fromIndex ? 1 : -1;
        View partiallyVisible = null;
        for (int i = fromIndex; i != toIndex; i += next) {
            final View child = getChildAt(i);
            final int childStart = orientationHelper.getDecoratedStart(child);
            final int childEnd = orientationHelper.getDecoratedEnd(child);
            if (childStart < end && childEnd > start) {
                if (completelyVisible) {
                    if (childStart >= start && childEnd <= end) {
                        return child;
                    } else if (acceptPartiallyVisible && partiallyVisible == null) {
                        partiallyVisible = child;
                    }
                } else {
                    return child;
                }
            }
        }
        return partiallyVisible;
    }

    OrientationHelper orientationHelper = new OrientationHelper(this) {
        @Override
        public int getEndAfterPadding() {
            return mLayoutManager.getHeight() - mLayoutManager.getPaddingBottom();
        }

        @Override
        public int getEnd() {
            return mLayoutManager.getHeight();
        }

        @Override
        public void offsetChildren(int amount) {
            mLayoutManager.offsetChildrenVertical(amount);
        }

        @Override
        public int getStartAfterPadding() {
            return mLayoutManager.getPaddingTop();
        }

        @Override
        public int getDecoratedMeasurement(View view) {
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();
            return mLayoutManager.getDecoratedMeasuredHeight(view) + params.topMargin
                    + params.bottomMargin;
        }

        @Override
        public int getDecoratedMeasurementInOther(View view) {
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();
            return mLayoutManager.getDecoratedMeasuredWidth(view) + params.leftMargin
                    + params.rightMargin;
        }

        @Override
        public int getDecoratedEnd(View view) {
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();

            ViewDataBinding binding = DataBindingUtil.getBinding(view);
            if (binding != null && binding instanceof ItemRealBinding) {
                ItemRealBinding realBinding = (ItemRealBinding) binding;

                int realTop = realBinding.frame.getTop();
                int childTop = mLayoutManager.getDecoratedTop(view) - params.topMargin;

                int top = childTop + realTop;
                return top + realBinding.frame.getHeight();
            }

            return mLayoutManager.getDecoratedBottom(view) + params.bottomMargin;
        }

        @Override
        public int getDecoratedStart(View view) {
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                    view.getLayoutParams();

            ViewDataBinding binding = DataBindingUtil.getBinding(view);
            if (binding != null && binding instanceof ItemRealBinding) {
                ItemRealBinding realBinding = (ItemRealBinding) binding;
                int realTop = realBinding.frame.getTop();
                int childTop = mLayoutManager.getDecoratedTop(view) - params.topMargin;

                return childTop + realTop;

            }

            return mLayoutManager.getDecoratedTop(view) - params.topMargin;
        }

        @Override
        public int getTransformedEndWithDecoration(View view) {
            mLayoutManager.getTransformedBoundingBox(view, true, mTmpRect);
            return mTmpRect.bottom;
        }

        @Override
        public int getTransformedStartWithDecoration(View view) {
            mLayoutManager.getTransformedBoundingBox(view, true, mTmpRect);
            return mTmpRect.top;
        }

        @Override
        public int getTotalSpace() {
            return mLayoutManager.getHeight() - mLayoutManager.getPaddingTop()
                    - mLayoutManager.getPaddingBottom();
        }

        @Override
        public void offsetChild(View view, int offset) {
            view.offsetTopAndBottom(offset);
        }

        @Override
        public int getEndPadding() {
            return mLayoutManager.getPaddingBottom();
        }

        @Override
        public int getMode() {
            return mLayoutManager.getHeightMode();
        }

        @Override
        public int getModeInOther() {
            return mLayoutManager.getWidthMode();
        }
    };
}

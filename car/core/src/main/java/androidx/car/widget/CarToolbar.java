/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.car.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.car.R;
import androidx.core.view.MarginLayoutParamsCompat;

import java.util.List;

/**
 * A toolbar for building car applications.
 *
 * <p>CarToolbar provides a subset of features of {@link Toolbar} through a driving safe UI. From
 * start to end, a CarToolbar provides the following elements:
 * <ul>
 *      <li><em>A navigation button.</em> Similar to that in Toolbar, navigation button should
 *          always provide access to other navigational destinations. If navigation button is to
 *          be used as Up Button, its {@code OnClickListener} needs to explicitly invoke
 *          {@link AppCompatActivity#onSupportNavigateUp()}
 *      <li><em>A title icon.</em> An @{@code Icon} shown before the title.
 *      <li><em>A title.</em> A single line primary text that ellipsizes at the end.
 *      <li><em>A subtitle.</em> A single line secondary text that ellipsizes at the end.
 *      <li><em>An overflow button.</em> A button that opens the overflow menu.
 * </ul>
 *
 * <p>One distinction between CarToolbar and Toolbar is that CarToolbar cannot be used as action bar
 * through {@link androidx.appcompat.app.AppCompatActivity#setSupportActionBar(Toolbar)}.
 *
 * <p>The CarToolbar has a fixed height of {@code R.dimen.car_app_bar_height}.
 */
public class CarToolbar extends ViewGroup {

    private static final String TAG = "Toolbar";

    private final ImageButton mNavButtonView;
    private final int mEdgeButtonIconSize;
    private final ImageView mTitleIconView;
    private final ImageButton mOverflowButtonView;
    private final int mToolbarHeight;
    private final int mTextVerticalPadding;
    private int mTitleIconSize;
    // There is no actual container for edge buttons (Navigation / Overflow). This value is used
    // to calculate a horizontal margin on both ends of the edge buttons so that they're centered.
    // We use dedicated attribute over horizontal margin so that the API for setting space before
    // title (i.e. @dimen/car_margin) is simpler.
    private int mEdgeButtonContainerWidth;

    private final TextView mTitleTextView;
    private CharSequence mTitleText;

    private final TextView mSubtitleTextView;
    private CharSequence mSubtitleText;

    @Nullable
    private List<CarMenuItem> mMenuItems;

    public CarToolbar(Context context) {
        this(context, /* attrs= */ null);
    }

    public CarToolbar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.carToolbarStyle);
    }

    public CarToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, /* defStyleRes= */ 0);
    }

    public CarToolbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        Resources res = context.getResources();
        mToolbarHeight = res.getDimensionPixelSize(R.dimen.car_app_bar_height);
        mEdgeButtonIconSize = res.getDimensionPixelSize(R.dimen.car_primary_icon_size);

        mTextVerticalPadding = getResources().getDimensionPixelSize(R.dimen.car_padding_1);
        LayoutInflater.from(context).inflate(R.layout.car_toolbar, this);

        // Ensure min touch target size for nav button.
        mNavButtonView = findViewById(R.id.nav_button);
        int minTouchSize = getContext().getResources().getDimensionPixelSize(
                R.dimen.car_touch_target_size);
        MinTouchTargetHelper.ensureThat(mNavButtonView).hasMinTouchSize(minTouchSize);

        mTitleTextView = findViewById(R.id.title);
        mTitleIconView = findViewById(R.id.title_icon);
        mSubtitleTextView = findViewById(R.id.subtitle);
        mOverflowButtonView = findViewById(R.id.overflow_menu);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CarToolbar, defStyleAttr,
                /* defStyleRes= */ 0);
        try {
            CharSequence title = a.getText(R.styleable.CarToolbar_title);
            setTitle(title);

            setTitleTextAppearance(a.getResourceId(R.styleable.CarToolbar_titleTextAppearance,
                    R.style.TextAppearance_Car_Body1_Medium));

            setNavigationIcon(Icon.createWithResource(getContext(),
                    a.getResourceId(R.styleable.CarToolbar_navigationIcon,
                            R.drawable.ic_nav_arrow_back)));

            int titleIconResId = a.getResourceId(R.styleable.CarToolbar_titleIcon, -1);
            setTitleIcon(titleIconResId != -1 ? Icon.createWithResource(context, titleIconResId)
                    : null);

            setTitleIconSize(a.getDimensionPixelSize(R.styleable.CarToolbar_titleIconSize,
                    res.getDimensionPixelSize(R.dimen.car_application_icon_size)));

            CharSequence subtitle = a.getText(R.styleable.CarToolbar_subtitle);
            setSubtitle(subtitle);

            setSubtitleTextAppearance(a.getResourceId(R.styleable.CarToolbar_subtitleTextAppearance,
                    R.style.TextAppearance_Car_Body2));

            setOverflowIcon(Icon.createWithResource(getContext(),
                    a.getResourceId(R.styleable.CarToolbar_overflowIcon, R.drawable.ic_more_vert)));

            mEdgeButtonContainerWidth = a.getDimensionPixelSize(
                    R.styleable.CarToolbar_navigationIconContainerWidth,
                    res.getDimensionPixelSize(R.dimen.car_margin));
        } finally {
            a.recycle();
        }
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        // Car Toolbar uses fixed height.
        return mToolbarHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Desired height should be the height constraint for all child views.
        int desiredHeight = getPaddingTop() + getSuggestedMinimumHeight() + getPaddingBottom();
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                desiredHeight, MeasureSpec.AT_MOST);

        int width = 0;
        // Measure the two edge buttons first because they have a higher
        // display priority than the title, subtitle, or the titleIcon.
        if (mNavButtonView.getVisibility() != GONE) {
            // Size of nav button is fixed.
            int measureSpec = MeasureSpec.makeMeasureSpec(mEdgeButtonIconSize, MeasureSpec.EXACTLY);
            mNavButtonView.measure(measureSpec, measureSpec);

            // Nav button width includes its container.
            int navWidth = Math.max(mEdgeButtonContainerWidth, mNavButtonView.getMeasuredWidth());
            width += navWidth + getHorizontalMargins(mNavButtonView);
        }

        if (mOverflowButtonView.getVisibility() != GONE) {
            int measureSpec = MeasureSpec.makeMeasureSpec(mEdgeButtonIconSize, MeasureSpec.EXACTLY);
            mOverflowButtonView.measure(measureSpec, measureSpec);
            width += Math.max(mEdgeButtonContainerWidth, mOverflowButtonView.getMeasuredWidth())
                    + getHorizontalMargins(mOverflowButtonView);
        }

        if (mTitleIconView.getVisibility() != GONE) {
            int measureSpec = MeasureSpec.makeMeasureSpec(mTitleIconSize, MeasureSpec.EXACTLY);
            mTitleIconView.measure(measureSpec, measureSpec);

            width += mTitleIconView.getMeasuredWidth();
        }

        int titleLength = 0;
        int subtitleLength = 0;
        if (mTitleTextView.getVisibility() != GONE) {
            measureChild(mTitleTextView, widthMeasureSpec, width, childHeightMeasureSpec, 0);
            titleLength = mTitleTextView.getMeasuredWidth() + getHorizontalMargins(mTitleTextView);
        }
        if (mSubtitleTextView.getVisibility() != GONE) {
            measureChild(mSubtitleTextView, widthMeasureSpec, width, childHeightMeasureSpec, 0);
            subtitleLength = mSubtitleTextView.getMeasuredWidth()
                    + getHorizontalMargins(mSubtitleTextView);
        }
        width += Math.max(titleLength, subtitleLength);

        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int height = bottom - top;
        int layoutLeft = getPaddingLeft();
        int layoutRight = getPaddingRight();

        if (mNavButtonView.getVisibility() != GONE) {
            // Nav button is centered in container.
            int navButtonWidth = mNavButtonView.getMeasuredWidth();
            int containerWidth = Math.max(mEdgeButtonContainerWidth, navButtonWidth);
            int navButtonLeft = (containerWidth - navButtonWidth) / 2;

            layoutViewFromLeftVerticallyCentered(mNavButtonView, navButtonLeft, height);
            layoutLeft += containerWidth;
        }

        if (mOverflowButtonView.getVisibility() != GONE) {
            int horizontalMargin = (mEdgeButtonContainerWidth
                    - mOverflowButtonView.getMeasuredWidth()) / 2;
            layoutViewFromRightVerticallyCentered(mOverflowButtonView,
                    right - horizontalMargin, height);
            layoutRight += Math.max(mEdgeButtonContainerWidth,
                    mOverflowButtonView.getMeasuredWidth());
        }

        if (mTitleIconView.getVisibility() != GONE) {
            layoutViewFromLeftVerticallyCentered(mTitleIconView, layoutLeft, height);
            layoutLeft += mTitleIconView.getMeasuredWidth();
        }

        if (mTitleTextView.getVisibility() != GONE && mSubtitleTextView.getVisibility() != GONE) {
            layoutTextViewsVerticallyCentered(mTitleTextView, mSubtitleTextView, layoutLeft,
                    height);
        } else if (mTitleTextView.getVisibility() != GONE) {
            layoutViewFromLeftVerticallyCentered(mTitleTextView, layoutLeft, height);
        } else if (mSubtitleTextView.getVisibility() != GONE) {
            layoutViewFromLeftVerticallyCentered(mSubtitleTextView, layoutLeft, height);
        }
    }


    /**
     * Set the icon to use for the toolbar's navigation button.
     *
     * <p>The navigation button appears at the start of the toolbar if present. Setting an icon
     * will make the navigation button visible.
     *
     * @param icon Icon to set; {@code null} will hide the icon.
     * @attr ref R.styleable#CarToolbar_navigationIcon
     */
    public void setNavigationIcon(@Nullable Icon icon) {
        if (icon == null) {
            mNavButtonView.setVisibility(GONE);
            mNavButtonView.setImageDrawable(null);
            return;
        }
        mNavButtonView.setVisibility(VISIBLE);
        mNavButtonView.setImageDrawable(icon.loadDrawable(getContext()));
    }

    /**
     * Sets a listener to respond to navigation events.
     *
     * <p>This listener will be called whenever the user clicks the navigation button
     * at the start of the toolbar. An icon must be set for the navigation button to appear.
     *
     * @param listener Listener to set.
     * @see #setNavigationIcon(Icon)
     */
    public void setNavigationIconOnClickListener(@Nullable View.OnClickListener listener) {
        mNavButtonView.setOnClickListener(listener);
    }

    /**
     * Sets the width of container for navigation icon.
     *
     * <p>Navigation icon will be horizontally centered in its container. If the width of container
     * is less than that of navigation icon, there will be no space on both ends of navigation icon.
     *
     * @param width Width of container in pixels.
     */
    public void setNavigationIconContainerWidth(@Px int width) {
        mEdgeButtonContainerWidth = width;
        requestLayout();
    }

    /**
     * Sets the title icon to use in the toolbar.
     *
     * <p>The title icon is positioned between the navigation button and the title.
     *
     * @param icon Icon to set; {@code null} will hide the icon.
     * @attr ref R.styleable#CarToolbar_titleIcon
     */
    public void setTitleIcon(@Nullable Icon icon) {
        if (icon == null) {
            mTitleIconView.setVisibility(GONE);
            mTitleIconView.setImageDrawable(null);
            return;
        }
        mTitleIconView.setVisibility(VISIBLE);
        mTitleIconView.setImageDrawable(icon.loadDrawable(getContext()));
    }

    /**
     * Sets a new size for the title icon.
     *
     * @param size Size of the title icon dimensions in pixels.
     * @attr ref R.styleable#CarToolbar_titleIconSize
     */
    public void setTitleIconSize(@Px int size) {
        mTitleIconSize = size;
        requestLayout();
    }

    /**
     * Returns the title of this toolbar.
     *
     * @return The current title.
     */
    public CharSequence getTitle() {
        return mTitleText;
    }

    /**
     * Sets the title of this toolbar.
     *
     * <p>A title should be used as the anchor for a section of content. It should
     * describe or name the content being viewed.
     *
     * @param resId Resource ID of a string to set as the title.
     */
    public void setTitle(@StringRes int resId) {
        setTitle(getContext().getText(resId));
    }

    /**
     * Sets the title of this toolbar.
     *
     * <p>A title should be used as the anchor for a section of content. It should
     * describe or name the content being viewed.
     *
     * <p>{@code null} or empty string will hide the title.
     *
     * @param title Title to set.
     */
    public void setTitle(CharSequence title) {
        mTitleText = title;
        mTitleTextView.setText(title);
        mTitleTextView.setVisibility(TextUtils.isEmpty(title) ? GONE : VISIBLE);
    }
    /**
     * Returns the subtitle of this toolbar.
     *
     * @return The current subtitle, or {@code null} if none has been set.
     */
    @Nullable
    public CharSequence getSubtitle() {
        return mSubtitleText;
    }

    /**
     * Sets the subtitle of this toolbar.
     *
     * <p>Subtitles should express extended information about the current content.
     * Subtitle will appear underneath the title if the title exists.
     * @param resId Resource ID of a string to set as the subtitle.
     */
    public void setSubtitle(@StringRes int resId) {
        setSubtitle(getContext().getText(resId));
    }

    /**
     * Sets the subtitle of this toolbar.
     *
     * <p>Subtitle should express extended information about the current content.
     * Subtitle will appear underneath the title if the title exists.
     *
     * @param subtitle Subtitle to set. {@code null} or empty string will hide the subtitle.
     */
    public void setSubtitle(@Nullable CharSequence subtitle) {
        mSubtitleText = subtitle;
        mSubtitleTextView.setText(subtitle);
        mSubtitleTextView.setVisibility(TextUtils.isEmpty(subtitle) ? GONE : VISIBLE);
    }

    /**
     * Sets the text color, size, style, hint color, and highlight color
     * from the specified TextAppearance resource.
     *
     * @param resId Resource id of TextAppearance.
     */
    public void setTitleTextAppearance(@StyleRes int resId) {
        mTitleTextView.setTextAppearance(resId);
    }

    /**
     * Sets the text color, size, style, hint color, and highlight color
     * from the specified TextAppearance resource.
     *
     * @param resId Resource id of TextAppearance.
     */
    public void setSubtitleTextAppearance(@StyleRes int resId) {
        mSubtitleTextView.setTextAppearance(resId);
    }

    /**
     * Sets the icon of the overflow menu button.
     *
     * @param icon Icon to set.
     * @attr ref R.styleable#CarToolbar_overflowIcon
     */
    public void setOverflowIcon(@NonNull Icon icon) {
        mOverflowButtonView.setImageDrawable(icon.loadDrawable(getContext()));
    }

    /**
     * Sets the list of {@link CarMenuItem}s that will be displayed on this {@code CarToolbar}.
     *
     * @param items List of {@link CarMenuItem}s to display, {@code null} to remove all items.
     */
    public void setMenuItems(@Nullable List<CarMenuItem> items) {
        mMenuItems = items;

        // If there are items to be displayed in the overflow menu, show the overflow icon.
        boolean containsOverflowItems = false;
        for (CarMenuItem item : items) {
            if (item.getDisplayBehavior() == CarMenuItem.DisplayBehavior.NEVER) {
                containsOverflowItems = true;
                break;
            }
        }
        mOverflowButtonView.setVisibility(containsOverflowItems ? View.VISIBLE : View.GONE);
        requestLayout();
    }

    /**
     * Returns a list of this {@code CarToolbar}'s {@link CarMenuItem}s, or {@code null} if
     * none were set.
     */
    @Nullable
    public List<CarMenuItem> getMenuItems() {
        return mMenuItems;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    /**
     * Lays out a view on the left side so that it's vertically centered in its parent.
     *
     * @param view The view to layout.
     * @param left Position from the left.
     * @param parentHeight Height of the parent view.
     */
    private void layoutViewFromLeftVerticallyCentered(View view, int left, int parentHeight) {
        int height = view.getMeasuredHeight();
        int top = (parentHeight - height) / 2;

        view.layout(left, top, left + view.getMeasuredWidth(), top + height);
    }

    /**
     * Lays out a view on the right side so that it's vertically centered in its parent.
     *
     * @param view The view to layout.
     * @param right Position from the right.
     * @param parentHeight Height of the parent view.
     */
    private void layoutViewFromRightVerticallyCentered(View view, int right, int parentHeight) {
        int height = view.getMeasuredHeight();
        int top = (parentHeight - height) / 2;

        view.layout(right - view.getMeasuredWidth(), top, right, top + height);
    }

    private void layoutTextViewsVerticallyCentered(View title, View subtitle, int left,
            int height) {
        int titleHeight = title.getMeasuredHeight();
        int titleWidth = title.getMeasuredWidth();

        int subtitleHeight = subtitle.getMeasuredHeight();
        int subtitleWidth = subtitle.getMeasuredWidth();

        int titleTop = (height - titleHeight - subtitleHeight - mTextVerticalPadding) / 2;
        title.layout(left, titleTop, left + titleWidth, titleTop + titleHeight);

        int subtitleTop = title.getBottom() + mTextVerticalPadding;
        subtitle.layout(left, subtitleTop, left + subtitleWidth, subtitleTop + subtitleHeight);
    }

    private int getHorizontalMargins(View v) {
        MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
        return MarginLayoutParamsCompat.getMarginStart(mlp)
                + MarginLayoutParamsCompat.getMarginEnd(mlp);
    }

    /**
     * Measure child view.
     *
     * @param child Child view to measure.
     * @param parentWidthSpec Parent width MeasureSpec.
     * @param widthUsed Width used so far by other child views; used as part of padding for current
     * child view in MeasureSpec calculation.
     * @param parentHeightSpec Parent height MeasureSpec.
     * @param heightUsed Height used so far by other child views; used as part of padding for
     * current child view in MeasureSpec calculation.
     */
    private void measureChild(View child, int parentWidthSpec, int widthUsed,
            int parentHeightSpec, int heightUsed) {
        // Calculate the padding and margin of current dimension, including
        // the width/height used by other child views.
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        int childWidthSpec = getChildMeasureSpec(parentWidthSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin + widthUsed,
                lp.width);
        int childHeightSpec = getChildMeasureSpec(parentHeightSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin + heightUsed,
                lp.height);
        child.measure(childWidthSpec, childHeightSpec);
    }
}

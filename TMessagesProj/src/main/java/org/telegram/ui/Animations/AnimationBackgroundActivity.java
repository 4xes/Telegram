package org.telegram.ui.Animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Animations.Background.GradientSurfaceView;
import org.telegram.ui.Animations.Cells.BottomTextLayout;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScrollSlidingTextTabStrip;

import java.util.ArrayList;

public class AnimationBackgroundActivity extends BaseFragment {

    private int additionalPadding;
    private final Paint backgroundPaint = new Paint();


    public AnimationPreferences preferences;

    private ScrollSlidingTextTabStrip scrollSlidingTextTabStrip;
    private int initialTab;

    private FrameLayout contentView;

    private FragmentContextView fragmentContextView;

    private GradientSurfaceView surfaceView;

    private BottomTextLayout bottomOverlayAnimate;

    @Override
    public View createView(Context context) {
        preferences = new AnimationPreferences(context);

        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setAddToContainer(false);
        actionBar.setClipContent(true);
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setTitle("Animation Settings");
        actionBar.setExtraHeight(AndroidUtilities.dp(44));
        actionBar.setAllowOverlayTitle(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (getParentActivity() == null) {
                    return;
                }
                if (id == -1) {
                    if (!closeActionMode()) {
                        finishFragment();
                    }
                }
            }
        });

        surfaceView = new GradientSurfaceView(context);
        surfaceView.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        surfaceView.animatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                bottomOverlayAnimate.setEnabled(true, true);
            }
        };

        bottomOverlayAnimate = new BottomTextLayout(context);
        bottomOverlayAnimate.setText("ANIMATE");
        bottomOverlayAnimate.setOnClickListener(v -> {
            int tabId = scrollSlidingTextTabStrip.getCurrentTabId();
            Interpolator[] interpolators =  AnimationType.Background.params;
            if (tabId < AnimationType.Background.params.length )  {
                bottomOverlayAnimate.setEnabled(false, true);
                surfaceView.requestPositionAnimation(interpolators[tabId]);
            }
        });
        bottomOverlayAnimate.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 51, Gravity.BOTTOM));

        if (scrollSlidingTextTabStrip != null) {
            initialTab = scrollSlidingTextTabStrip.getCurrentTabId();
        }
        scrollSlidingTextTabStrip = new ScrollSlidingTextTabStrip(context);
        if (initialTab != -1) {
            scrollSlidingTextTabStrip.setInitialTabId(initialTab);
            initialTab = -1;
        }
        actionBar.addView(scrollSlidingTextTabStrip, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 44, Gravity.START | Gravity.BOTTOM));

        scrollSlidingTextTabStrip.setDelegate(new ScrollSlidingTextTabStrip.ScrollSlidingTabStripDelegate() {
            @Override
            public void onPageSelected(int id, boolean forward) {
            }

            @Override
            public void onPageScrolled(float progress) {

            }
        });

        FrameLayout frameLayout;
        fragmentView = frameLayout = new FrameLayout(context) {

            private boolean globalIgnoreLayout;

            @Override
            public void forceHasOverlappingRendering(boolean hasOverlappingRendering) {
                super.forceHasOverlappingRendering(hasOverlappingRendering);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                int heightSize = MeasureSpec.getSize(heightMeasureSpec);

                setMeasuredDimension(widthSize, heightSize);

                measureChildWithMargins(actionBar, widthMeasureSpec, 0, heightMeasureSpec, 0);
                globalIgnoreLayout = true;
                setPaddingToSurface();
                globalIgnoreLayout = false;

                int childCount = getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View child = getChildAt(i);
                    if (child == null || child.getVisibility() == GONE || child == actionBar) {
                        continue;
                    }
                    measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                }
            }

            private void setPaddingToSurface() {
                if (surfaceView != null) {
                    LayoutParams params = (FrameLayout.LayoutParams) surfaceView.getLayoutParams();
                    params.topMargin = actionBar.getMeasuredHeight() + additionalPadding;
                    params.bottomMargin = bottomOverlayAnimate.getMeasuredHeight() - bottomOverlayAnimate.getPaddingTop();
                }
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                if (fragmentContextView != null) {
                    int y = actionBar.getMeasuredHeight();
                    fragmentContextView.layout(fragmentContextView.getLeft(), fragmentContextView.getTop() + y, fragmentContextView.getRight(), fragmentContextView.getBottom() + y);
                }
            }

            @Override
            public void setPadding(int left, int top, int right, int bottom) {
                additionalPadding = top;
                if (fragmentContextView != null) {
                    fragmentContextView.setTranslationY(top + actionBar.getTranslationY());
                }
                setPaddingToSurface();
            }

            @Override
            protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                if (parentLayout != null) {
                    parentLayout.drawHeaderShadow(canvas, actionBar.getMeasuredHeight() + (int) actionBar.getTranslationY());
                }
                if (fragmentContextView != null && fragmentContextView.isCallStyle()) {
                    canvas.save();
                    canvas.translate(fragmentContextView.getX(), fragmentContextView.getY());
                    fragmentContextView.setDrawOverlay(true);
                    fragmentContextView.draw(canvas);
                    fragmentContextView.setDrawOverlay(false);
                    canvas.restore();
                }
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (child == fragmentContextView && fragmentContextView.isCallStyle()) {
                    return true;
                }
                return super.drawChild(canvas, child, drawingTime);
            }

            @Override
            public void requestLayout() {
                if (globalIgnoreLayout) {
                    return;
                }
                super.requestLayout();
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                return scrollSlidingTextTabStrip.isAnimatingIndicator() || onTouchEvent(ev);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                backgroundPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                canvas.drawRect(0, actionBar.getMeasuredHeight() + actionBar.getTranslationY(), getMeasuredWidth(), getMeasuredHeight(), backgroundPaint);
            }

        };
        frameLayout.setWillNotDraw(false);
        frameLayout.addView(surfaceView);

        frameLayout.addView(bottomOverlayAnimate);

        if (!AndroidUtilities.isTablet()) {
            frameLayout.addView(fragmentContextView = new FragmentContextView(context, this, false), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.TOP | Gravity.LEFT, 0, 8, 0, 0));
        }

        frameLayout.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        updateTabs();

        contentView = (FrameLayout) fragmentView;
        return fragmentView;
    }

    private boolean closeActionMode() {
        if (actionBar.isActionModeShowed()) {
            actionBar.hideActionMode();
            return true;
        } else {
            return false;
        }
    }

    private void updateTabs() {
        if (scrollSlidingTextTabStrip == null) {
            return;
        }
        final Interpolator[] params = AnimationType.Background.params;

        boolean changed = false;
        for (int i = 0; i < params.length; i++) {
            if (!scrollSlidingTextTabStrip.hasTab(i)) {
                changed = true;
            }
        }

        if (changed) {
            scrollSlidingTextTabStrip.removeTabs();
            for (int i = 0; i < params.length; i++) {
                if (!scrollSlidingTextTabStrip.hasTab(i)) {
                    scrollSlidingTextTabStrip.addTextTab(i, params[i].title);
                }
            }
        }
        if (scrollSlidingTextTabStrip.getTabsCount() <= 1) {
            scrollSlidingTextTabStrip.setVisibility(View.GONE);
            actionBar.setExtraHeight(0);
        } else {
            scrollSlidingTextTabStrip.setVisibility(View.VISIBLE);
            actionBar.setExtraHeight(AndroidUtilities.dp(44));
        }
        scrollSlidingTextTabStrip.finishAddingTabs();
    }


    @Override
    public boolean isSwipeBackEnabled(MotionEvent event) {
        return scrollSlidingTextTabStrip.getCurrentPosition() == scrollSlidingTextTabStrip.getFirstTabId();
    }

    @Override
    public boolean onBackPressed() {
        return actionBar.isEnabled() && !closeActionMode();
    }
    

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> arrayList = new ArrayList<>();

        arrayList.add(new ThemeDescription(fragmentView, 0, null, null, null, null, Theme.key_windowBackgroundWhite));

        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));

        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM | ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_actionBarDefaultSubmenuItemIcon));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_AM_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_AM_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        arrayList.add(new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_inappPlayerBackground));
        arrayList.add(new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{FragmentContextView.class}, new String[]{"playButton"}, null, null, null, Theme.key_inappPlayerPlayPause));
        arrayList.add(new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_inappPlayerTitle));
        arrayList.add(new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_FASTSCROLL, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_inappPlayerPerformer));
        arrayList.add(new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{FragmentContextView.class}, new String[]{"closeButton"}, null, null, null, Theme.key_inappPlayerClose));

        arrayList.add(new ThemeDescription(scrollSlidingTextTabStrip, 0, new Class[]{ScrollSlidingTextTabStrip.class}, new String[]{"selectorDrawable"}, null, null, null, Theme.key_actionBarTabLine));
        arrayList.add(new ThemeDescription(scrollSlidingTextTabStrip.getTabsContainer(), ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{TextView.class}, null, null, null, Theme.key_actionBarTabActiveText));
        arrayList.add(new ThemeDescription(scrollSlidingTextTabStrip.getTabsContainer(), ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{TextView.class}, null, null, null, Theme.key_actionBarTabUnactiveText));
        arrayList.add(new ThemeDescription(scrollSlidingTextTabStrip.getTabsContainer(), ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, new Class[]{TextView.class}, null, null, null, Theme.key_actionBarTabSelector));
        return arrayList;
    }
}

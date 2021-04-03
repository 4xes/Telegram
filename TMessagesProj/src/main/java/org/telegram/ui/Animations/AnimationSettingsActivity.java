package org.telegram.ui.Animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Property;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Animations.Cells.GradientSurfaceCell;
import org.telegram.ui.Cells.EditTextSettingsCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.SharedMediaSectionCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.ColorPicker;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScrollSlidingTextTabStrip;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.ShareAlert;

import java.util.ArrayList;

public class AnimationSettingsActivity extends BaseFragment {


    private final Paint backgroundPaint = new Paint();

    private int additionalPadding;
    private boolean scrolling;
    private boolean disableActionBarScrolling;

    private AnimatorSet scrimAnimatorSet;
    private ActionBarPopupWindow scrimPopupWindow;
    private ActionBarMenuSubItem[] scrimPopupWindowItems;
    private FrameLayout contentView;

    private static class SettingsPage extends FrameLayout {
        private RecyclerListView listView;
        private LinearLayoutManager layoutManager;
        private int selectedType;

        public SettingsPage(Context context) {
            super(context);
        }
    }

    private AnimationPageAdapter[] adapters;
    private AnimationPageAdapter backgroundAdapter;

    private final SettingsPage[] settingsPages = new SettingsPage[2];

    private ScrollSlidingTextTabStrip scrollSlidingTextTabStrip;
    private int initialTab;

    private AnimatorSet tabsAnimation;
    private boolean tabsAnimationInProgress;
    private boolean animatingForward;
    private boolean backAnimation;

    private FragmentContextView fragmentContextView;
    private int maximumVelocity;

    private ActionBarMenuItem otherItem;
    private final static int share_parameters = 1;
    private final static int import_parameters = 2;
    private final static int restore_parameters = 3;

    private FrameLayout pickerLayout;
    public ColorPicker colorPicker;
    public int lastIndexColor;
    public int lastPickedColor;

    private float currentIntensity = 0.5f;
    private float previousIntensity;

    private AnimatorSet pickerViewAnimation;

    private static final Interpolator interpolator = t -> {
        --t;
        return t * t * t * t * t + 1.0F;
    };


    public final Property<AnimationSettingsActivity, Float> SCROLL_Y = new AnimationProperties.FloatProperty<AnimationSettingsActivity>("animationValue") {
        @Override
        public void setValue(AnimationSettingsActivity object, float value) {
            object.setScrollY(value);
            for (int a = 0; a < settingsPages.length; a++) {
                settingsPages[a].listView.checkSection();
            }
        }

        @Override
        public Float get(AnimationSettingsActivity object) {
            return actionBar.getTranslationY();
        }
    };

    public void showColorPicker(boolean show) {
        colorPicker.setColor(lastPickedColor, 0);
        pickerViewAnimation = new AnimatorSet();
        ArrayList<Animator> animators = new ArrayList<>();
        if (show) {
            pickerLayout.setVisibility(View.VISIBLE);
            animators.add(ObjectAnimator.ofFloat(pickerLayout, View.TRANSLATION_Y, 0));
        } else {
            pickerLayout.setVisibility(View.VISIBLE);
            animators.add(ObjectAnimator.ofFloat(pickerLayout, View.TRANSLATION_Y, pickerLayout.getMeasuredHeight()));
        }
        pickerViewAnimation.playTogether(animators);
        pickerViewAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                pickerViewAnimation = null;
                if (!show) {
                    pickerLayout.setVisibility(View.GONE);
                }
            }
        });
        pickerViewAnimation.setInterpolator(CubicBezierInterpolator.EASE_OUT);
        pickerViewAnimation.setDuration(200);
        pickerViewAnimation.start();
    }

    @Override
    public View createView(Context context) {
        ViewConfiguration configuration = ViewConfiguration.get(context);
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();


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
                switch (id) {
                    case -1:
                        if (!closeActionMode()) {
                            finishFragment();
                        }
                        break;
                    case share_parameters:
                        shareDialog(context);
                        break;
                    case import_parameters:
                        importDialog(context);
                        break;
                    case restore_parameters:
                        AnimationManager.getInstance().resetSettings();
                        onUpdateSettings();
                        colorPicker.setColor(AnimationManager.getPreferences().getColor(lastIndexColor), 0);
                        Toast.makeText(context, "Settings restored", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        otherItem = menu.addItem(10, R.drawable.ic_ab_other);
        otherItem.setContentDescription(LocaleController.getString("AccDescrMoreOptions", R.string.AccDescrMoreOptions));
        otherItem.addSubItem(share_parameters, 0, "Share Parameters");
        otherItem.addSubItem(import_parameters, 0, "Import Parameters");
        ActionBarMenuSubItem restore = otherItem.addSubItem(restore_parameters, 0, "Restore to Defaults");
        restore.setTextColor(0xffff3e3e);

        if (scrollSlidingTextTabStrip != null) {
            initialTab = scrollSlidingTextTabStrip.getCurrentTabId();
        }
        scrollSlidingTextTabStrip = new ScrollSlidingTextTabStrip(context);
        if (initialTab != -1) {
            scrollSlidingTextTabStrip.setInitialTabId(initialTab);
            initialTab = -1;
        }
        actionBar.addView(scrollSlidingTextTabStrip, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 44, Gravity.START | Gravity.BOTTOM));

        AnimationType[] types = AnimationType.values();
        adapters = new AnimationPageAdapter[types.length];
        for (int i = 0; i < types.length; i++) {
            adapters[i] = new AnimationPageAdapter(context, types[i], this);
            if (types[i] == AnimationType.Background) {
                backgroundAdapter = adapters[i];
            }
        }

        scrollSlidingTextTabStrip.setDelegate(new ScrollSlidingTextTabStrip.ScrollSlidingTabStripDelegate() {
            @Override
            public void onPageSelected(int id, boolean forward) {
                if (settingsPages[0].selectedType == id) {
                    return;
                }
                settingsPages[1].selectedType = id;
                settingsPages[1].setVisibility(View.VISIBLE);
                switchToCurrentSelectedMode(true);
                animatingForward = forward;
            }

            @Override
            public void onPageScrolled(float progress) {
                if (progress == 1 && settingsPages[1].getVisibility() != View.VISIBLE) {
                    return;
                }
                if (animatingForward) {
                    settingsPages[0].setTranslationX(-progress * settingsPages[0].getMeasuredWidth());
                    settingsPages[1].setTranslationX(settingsPages[0].getMeasuredWidth() - progress * settingsPages[0].getMeasuredWidth());
                } else {
                    settingsPages[0].setTranslationX(progress * settingsPages[0].getMeasuredWidth());
                    settingsPages[1].setTranslationX(progress * settingsPages[0].getMeasuredWidth() - settingsPages[0].getMeasuredWidth());
                }
                if (progress == 1) {
                    SettingsPage tempPage = settingsPages[0];
                    settingsPages[0] = settingsPages[1];
                    settingsPages[1] = tempPage;
                    settingsPages[1].setVisibility(View.GONE);
                }
            }
        });


        FrameLayout frameLayout;
        fragmentView = frameLayout = new FrameLayout(context) {

            private int startedTrackingPointerId;
            private boolean startedTracking;
            private boolean maybeStartTracking;
            private int startedTrackingX;
            private int startedTrackingY;
            private VelocityTracker velocityTracker;
            private boolean globalIgnoreLayout;

            private boolean prepareForMoving(MotionEvent ev, boolean forward) {
                int id = scrollSlidingTextTabStrip.getNextPageId(forward);
                if (id < 0) {
                    return false;
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                maybeStartTracking = false;
                startedTracking = true;
                startedTrackingX = (int) ev.getX();
                actionBar.setEnabled(false);
                scrollSlidingTextTabStrip.setEnabled(false);
                settingsPages[1].selectedType = id;
                settingsPages[1].setVisibility(View.VISIBLE);
                animatingForward = forward;
                switchToCurrentSelectedMode(true);
                if (forward) {
                    settingsPages[1].setTranslationX(settingsPages[0].getMeasuredWidth());
                } else {
                    settingsPages[1].setTranslationX(-settingsPages[0].getMeasuredWidth());
                }
                return true;
            }

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
                int actionBarHeight = actionBar.getMeasuredHeight();
                globalIgnoreLayout = true;
                for (int a = 0; a < settingsPages.length; a++) {
                    if (settingsPages[a] == null) {
                        continue;
                    }
                    if (settingsPages[a].listView != null) {
                        settingsPages[a].listView.setPadding(0, actionBarHeight + additionalPadding, 0, AndroidUtilities.dp(4));
                    }
                }
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
                int actionBarHeight = actionBar.getMeasuredHeight();
                for (SettingsPage settingsPage : settingsPages) {
                    if (settingsPage == null) {
                        continue;
                    }
                    if (settingsPage.listView != null) {
                        settingsPage.listView.setPadding(0, actionBarHeight + additionalPadding, 0, AndroidUtilities.dp(4));
                        settingsPage.listView.checkSection();
                    }
                }
                fixScrollOffset();
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

            public boolean checkTabsAnimationInProgress() {
                if (tabsAnimationInProgress) {
                    boolean cancel = false;
                    if (backAnimation) {
                        if (Math.abs(settingsPages[0].getTranslationX()) < 1) {
                            settingsPages[0].setTranslationX(0);
                            settingsPages[1].setTranslationX(settingsPages[0].getMeasuredWidth() * (animatingForward ? 1 : -1));
                            cancel = true;
                        }
                    } else if (Math.abs(settingsPages[1].getTranslationX()) < 1) {
                        settingsPages[0].setTranslationX(settingsPages[0].getMeasuredWidth() * (animatingForward ? -1 : 1));
                        settingsPages[1].setTranslationX(0);
                        cancel = true;
                    }
                    if (cancel) {
                        if (tabsAnimation != null) {
                            tabsAnimation.cancel();
                            tabsAnimation = null;
                        }
                        tabsAnimationInProgress = false;
                    }
                    return tabsAnimationInProgress;
                }
                return false;
            }



            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                return checkTabsAnimationInProgress() || scrollSlidingTextTabStrip.isAnimatingIndicator() || onTouchEvent(ev);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                backgroundPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                canvas.drawRect(0, actionBar.getMeasuredHeight() + actionBar.getTranslationY(), getMeasuredWidth(), getMeasuredHeight(), backgroundPaint);
            }

            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                if (!parentLayout.checkTransitionAnimation() && !checkTabsAnimationInProgress()) {
                    if (ev != null) {
                        if (velocityTracker == null) {
                            velocityTracker = VelocityTracker.obtain();
                        }
                        velocityTracker.addMovement(ev);
                    }
                    if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN && !startedTracking && !maybeStartTracking) {
                        startedTrackingPointerId = ev.getPointerId(0);
                        maybeStartTracking = true;
                        startedTrackingX = (int) ev.getX();
                        startedTrackingY = (int) ev.getY();
                        velocityTracker.clear();
                    } else if (ev != null && ev.getAction() == MotionEvent.ACTION_MOVE && ev.getPointerId(0) == startedTrackingPointerId) {
                        int dx = (int) (ev.getX() - startedTrackingX);
                        int dy = Math.abs((int) ev.getY() - startedTrackingY);
                        if (startedTracking && (animatingForward && dx > 0 || !animatingForward && dx < 0)) {
                            if (!prepareForMoving(ev, dx < 0)) {
                                maybeStartTracking = true;
                                startedTracking = false;
                                settingsPages[0].setTranslationX(0);
                                settingsPages[1].setTranslationX(animatingForward ? settingsPages[0].getMeasuredWidth() : -settingsPages[0].getMeasuredWidth());
                                scrollSlidingTextTabStrip.selectTabWithId(settingsPages[1].selectedType, 0);
                            }
                        }
                        if (maybeStartTracking && !startedTracking) {
                            float touchSlop = AndroidUtilities.getPixelsInCM(0.3f, true);
                            if (Math.abs(dx) >= touchSlop && Math.abs(dx) > dy) {
                                prepareForMoving(ev, dx < 0);
                            }
                        } else if (startedTracking) {
                            settingsPages[0].setTranslationX(dx);
                            if (animatingForward) {
                                settingsPages[1].setTranslationX(settingsPages[0].getMeasuredWidth() + dx);
                            } else {
                                settingsPages[1].setTranslationX(dx - settingsPages[0].getMeasuredWidth());
                            }
                            float scrollProgress = Math.abs(dx) / (float) settingsPages[0].getMeasuredWidth();
                            scrollSlidingTextTabStrip.selectTabWithId(settingsPages[1].selectedType, scrollProgress);
                        }
                    } else if (ev == null || ev.getPointerId(0) == startedTrackingPointerId && (ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_POINTER_UP)) {
                        velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
                        float velX;
                        float velY;
                        if (ev != null && ev.getAction() != MotionEvent.ACTION_CANCEL) {
                            velX = velocityTracker.getXVelocity();
                            velY = velocityTracker.getYVelocity();
                            if (!startedTracking) {
                                if (Math.abs(velX) >= 3000 && Math.abs(velX) > Math.abs(velY)) {
                                    prepareForMoving(ev, velX < 0);
                                }
                            }
                        } else {
                            velX = 0;
                            velY = 0;
                        }
                        if (startedTracking) {
                            float x = settingsPages[0].getX();
                            tabsAnimation = new AnimatorSet();
                            backAnimation = Math.abs(x) < settingsPages[0].getMeasuredWidth() / 3.0f && (Math.abs(velX) < 3500 || Math.abs(velX) < Math.abs(velY));
                            float distToMove;
                            float dx;
                            if (backAnimation) {
                                dx = Math.abs(x);
                                if (animatingForward) {
                                    tabsAnimation.playTogether(
                                            ObjectAnimator.ofFloat(settingsPages[0], View.TRANSLATION_X, 0),
                                            ObjectAnimator.ofFloat(settingsPages[1], View.TRANSLATION_X, settingsPages[1].getMeasuredWidth())
                                    );
                                } else {
                                    tabsAnimation.playTogether(
                                            ObjectAnimator.ofFloat(settingsPages[0], View.TRANSLATION_X, 0),
                                            ObjectAnimator.ofFloat(settingsPages[1], View.TRANSLATION_X, -settingsPages[1].getMeasuredWidth())
                                    );
                                }
                            } else {
                                dx = settingsPages[0].getMeasuredWidth() - Math.abs(x);
                                if (animatingForward) {
                                    tabsAnimation.playTogether(
                                            ObjectAnimator.ofFloat(settingsPages[0], View.TRANSLATION_X, -settingsPages[0].getMeasuredWidth()),
                                            ObjectAnimator.ofFloat(settingsPages[1], View.TRANSLATION_X, 0)
                                    );
                                } else {
                                    tabsAnimation.playTogether(
                                            ObjectAnimator.ofFloat(settingsPages[0], View.TRANSLATION_X, settingsPages[0].getMeasuredWidth()),
                                            ObjectAnimator.ofFloat(settingsPages[1], View.TRANSLATION_X, 0)
                                    );
                                }
                            }
                            tabsAnimation.setInterpolator(interpolator);

                            int width = getMeasuredWidth();
                            int halfWidth = width / 2;
                            float distanceRatio = Math.min(1.0f, 1.0f * dx / (float) width);
                            float distance = (float) halfWidth + (float) halfWidth * AndroidUtilities.distanceInfluenceForSnapDuration(distanceRatio);
                            velX = Math.abs(velX);
                            int duration;
                            if (velX > 0) {
                                duration = 4 * Math.round(1000.0f * Math.abs(distance / velX));
                            } else {
                                float pageDelta = dx / getMeasuredWidth();
                                duration = (int) ((pageDelta + 1.0f) * 100.0f);
                            }
                            duration = Math.max(150, Math.min(duration, 600));

                            tabsAnimation.setDuration(duration);
                            tabsAnimation.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animator) {
                                    tabsAnimation = null;
                                    if (backAnimation) {
                                        settingsPages[1].setVisibility(View.GONE);
                                    } else {
                                        SettingsPage tempPage = settingsPages[0];
                                        settingsPages[0] = settingsPages[1];
                                        settingsPages[1] = tempPage;
                                        settingsPages[1].setVisibility(View.GONE);
                                        scrollSlidingTextTabStrip.selectTabWithId(settingsPages[0].selectedType, 1.0f);
                                    }
                                    tabsAnimationInProgress = false;
                                    maybeStartTracking = false;
                                    startedTracking = false;
                                    actionBar.setEnabled(true);
                                    scrollSlidingTextTabStrip.setEnabled(true);
                                }
                            });
                            tabsAnimation.start();
                            tabsAnimationInProgress = true;
                            startedTracking = false;
                        } else {
                            maybeStartTracking = false;
                            actionBar.setEnabled(true);
                            scrollSlidingTextTabStrip.setEnabled(true);
                        }
                        if (velocityTracker != null) {
                            velocityTracker.recycle();
                            velocityTracker = null;
                        }
                    }
                    return startedTracking;
                }
                return false;
            }
        };
        frameLayout.setWillNotDraw(false);

        int scrollToPositionOnRecreate = -1;
        int scrollToOffsetOnRecreate = 0;

        for (int a = 0; a < settingsPages.length; a++) {
            if (a == 0) {
                if (settingsPages[a] != null && settingsPages[a].layoutManager != null) {
                    scrollToPositionOnRecreate = settingsPages[a].layoutManager.findFirstVisibleItemPosition();
                    if (scrollToPositionOnRecreate != settingsPages[a].layoutManager.getItemCount() - 1) {
                        RecyclerListView.Holder holder = (RecyclerListView.Holder) settingsPages[a].listView.findViewHolderForAdapterPosition(scrollToPositionOnRecreate);
                        if (holder != null) {
                            scrollToOffsetOnRecreate = holder.itemView.getTop();
                        } else {
                            scrollToPositionOnRecreate = -1;
                        }
                    } else {
                        scrollToPositionOnRecreate = -1;
                    }
                }
            }
            final SettingsPage settingsPage = new SettingsPage(context) {
                @Override
                public void setTranslationX(float translationX) {
                    super.setTranslationX(translationX);
                    if (tabsAnimationInProgress) {
                        if (settingsPages[0] == this) {
                            float scrollProgress = Math.abs(settingsPages[0].getTranslationX()) / (float) settingsPages[0].getMeasuredWidth();
                            scrollSlidingTextTabStrip.selectTabWithId(settingsPages[1].selectedType, scrollProgress);
                        }
                    }
                }
            };
            frameLayout.addView(settingsPage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            settingsPages[a] = settingsPage;

            final LinearLayoutManager layoutManager = settingsPages[a].layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
                @Override
                public boolean supportsPredictiveItemAnimations() {
                    return false;
                }

                @Override
                protected void calculateExtraLayoutSpace(@NonNull RecyclerView.State state, @NonNull int[] extraLayoutSpace) {
                    super.calculateExtraLayoutSpace(state, extraLayoutSpace);
                    extraLayoutSpace[1] = Math.max(extraLayoutSpace[1], AndroidUtilities.dp(56f) * 2);
                }
            };
            settingsPages[a].listView = new RecyclerListView(context) {
                @Override
                protected void onLayout(boolean changed, int l, int t, int r, int b) {
                    super.onLayout(changed, l, t, r, b);
                    updateSections(this, true);
                }
            };
            settingsPages[a].listView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
            settingsPages[a].listView.setItemAnimator(null);
            settingsPages[a].listView.setClipToPadding(false);
            settingsPages[a].listView.setSectionsType(2);
            settingsPages[a].listView.setLayoutManager(layoutManager);
            settingsPages[a].addView(settingsPages[a].listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            settingsPages[a].listView.setOnItemClickListener((view, position) -> {

            });
            settingsPages[a].listView.setOnScrollListener(new RecyclerView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                    }
                    scrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
                    if (newState != RecyclerView.SCROLL_STATE_DRAGGING) {
                        int scrollY = (int) -actionBar.getTranslationY();
                        int actionBarHeight = ActionBar.getCurrentActionBarHeight();
                        if (scrollY != 0 && scrollY != actionBarHeight) {
                            if (scrollY < actionBarHeight / 2) {
                                settingsPages[0].listView.smoothScrollBy(0, -scrollY);
                            } else if (settingsPages[0].listView.canScrollVertically(1)) {
                                settingsPages[0].listView.smoothScrollBy(0, actionBarHeight - scrollY);
                            }
                        }
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (recyclerView == settingsPages[0].listView && !actionBar.isActionModeShowed() && !disableActionBarScrolling) {
                        float currentTranslation = actionBar.getTranslationY();
                        float newTranslation = currentTranslation - dy;
                        if (newTranslation < -ActionBar.getCurrentActionBarHeight()) {
                            newTranslation = -ActionBar.getCurrentActionBarHeight();
                        } else if (newTranslation > 0) {
                            newTranslation = 0;
                        }
                        if (newTranslation != currentTranslation) {
                            setScrollY(newTranslation);
                        }
                    }
                    updateSections(recyclerView, false);
                }
            });
            settingsPages[a].listView.setOnItemLongClickListener((view, position) -> {

                return false;
            });
            if (a == 0 && scrollToPositionOnRecreate != -1) {
                layoutManager.scrollToPositionWithOffset(scrollToPositionOnRecreate, scrollToOffsetOnRecreate);
            }

            if (a != 0) {
                settingsPages[a].setVisibility(View.GONE);
            }
        }

        if (!AndroidUtilities.isTablet()) {
            frameLayout.addView(fragmentContextView = new FragmentContextView(context, this, false), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.TOP | Gravity.LEFT, 0, 8, 0, 0));
        }

        frameLayout.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        updateTabs();
        switchToCurrentSelectedMode(false);

        scrimPaint = new Paint() {
            @Override
            public void setAlpha(int a) {
                super.setAlpha(a);
                if (fragmentView != null) {
                    fragmentView.invalidate();
                }
            }
        };

        Rect paddings = new Rect();
        Drawable sheetDrawable = context.getResources().getDrawable(R.drawable.sheet_shadow).mutate();
        sheetDrawable.getPadding(paddings);
        sheetDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhite), PorterDuff.Mode.MULTIPLY));

        pickerLayout = new FrameLayout(context) {
            @Override
            public void onDraw(Canvas canvas) {
                sheetDrawable.setBounds(colorPicker.getLeft() - paddings.left, 0, colorPicker.getRight() + paddings.right, getMeasuredHeight());
                sheetDrawable.draw(canvas);
            }
        };
        pickerLayout.setVisibility(View.INVISIBLE);
        pickerLayout.setWillNotDraw(false);
        FrameLayout.LayoutParams layoutParams = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 288, Gravity.LEFT | Gravity.BOTTOM);
        layoutParams.height += paddings.top;
        pickerLayout.setPadding(0, paddings.top, 0, 0);
        frameLayout.addView(pickerLayout, layoutParams);

        FrameLayout patternsButtonsContainer = new FrameLayout(context) {
            @Override
            public void onDraw(Canvas canvas) {
                int bottom = Theme.chat_composeShadowDrawable.getIntrinsicHeight();
                Theme.chat_composeShadowDrawable.setBounds(0, 0, getMeasuredWidth(), bottom);
                Theme.chat_composeShadowDrawable.draw(canvas);
                canvas.drawRect(0, bottom, getMeasuredWidth(), getMeasuredHeight(), Theme.chat_composeBackgroundPaint);
            }
        };
        patternsButtonsContainer.setWillNotDraw(false);
        patternsButtonsContainer.setPadding(0, AndroidUtilities.dp(3), 0, 0);
        patternsButtonsContainer.setClickable(true);

        colorPicker = new ColorPicker(context, false, new ColorPicker.ColorPickerDelegate() {
            @Override
            public void setColor(int color, int num, boolean applyNow) {
                lastPickedColor = color;
                int index = backgroundAdapter.currentPickerIndex;
                if (index >= 0 && index < 4) {
                    backgroundAdapter.setColor(index, color);
                }
            }

            @Override
            public void openThemeCreate(boolean share) {

            }

            @Override
            public void deleteTheme() {

            }

            @Override
            public void rotateColors() {

            }

            @Override
            public int getDefaultColor(int num) {

                return 0;
            }

            @Override
            public boolean hasChanges() {
                return false;
            }
        });
        pickerLayout.addView(colorPicker, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_HORIZONTAL));
        pickerLayout.addView(patternsButtonsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 51, Gravity.BOTTOM));

        TextView patternsCancelButton = new TextView(context);
        patternsCancelButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        patternsCancelButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        patternsCancelButton.setTextColor(Theme.getColor(Theme.key_chat_fieldOverlayText));
        patternsCancelButton.setText(LocaleController.getString("Cancel", R.string.Cancel).toUpperCase());
        patternsCancelButton.setGravity(Gravity.CENTER);
        patternsCancelButton.setPadding(AndroidUtilities.dp(21), 0, AndroidUtilities.dp(21), 0);
        patternsCancelButton.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 0));
        patternsCancelButton.setOnClickListener(v -> {
            if (pickerViewAnimation != null) {
                return;
            }
            showColorPicker(false);
            backgroundAdapter.restoreColors();
        }
        );
        patternsButtonsContainer.addView(patternsCancelButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP));

        TextView patternsSaveButton = new TextView(context);
        patternsSaveButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        patternsSaveButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        patternsSaveButton.setTextColor(Theme.getColor(Theme.key_chat_fieldOverlayText));
        patternsSaveButton.setText(LocaleController.getString("ApplyTheme", R.string.ApplyTheme).toUpperCase());
        patternsSaveButton.setGravity(Gravity.CENTER);
        patternsSaveButton.setPadding(AndroidUtilities.dp(21), 0, AndroidUtilities.dp(21), 0);
        patternsSaveButton.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 0));
        patternsSaveButton.setOnClickListener(v -> {
            if (pickerViewAnimation != null) {
                return;
            }
            backgroundAdapter.applyColors();
            showColorPicker(false);
        });
        patternsButtonsContainer.addView(patternsSaveButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.TOP));


        colorPicker.setType(0, false, false, false, false, 0, false);
        colorPicker.setHasEdits(false);

        contentView = (FrameLayout) fragmentView;
        return fragmentView;
    }

    private void onUpdateSettings() {
        for (SettingsPage page: settingsPages) {
            if (page.listView != null && page.listView.getAdapter() != null) {
                page.listView.getAdapter().notifyDataSetChanged();
            }
        }
    }

    private void shareDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Share settings");
        TextInfoPrivacyCell textDetailCell = new TextInfoPrivacyCell (context);
        String settings = AnimationManager.getPreferences().toString();
        textDetailCell.setText(settings);
        builder.setView(textDetailCell);
        builder.setNeutralButton("Copy", (dialogInterface, i) -> {
            AndroidUtilities.addToClipboard(settings);
            dismissCurrentDialog();
            Toast.makeText(context, "Settings copied", Toast.LENGTH_SHORT).show();
        });
        builder.setPositiveButton("Share", (dialogInterface, i) -> {
            ShareAlert alert = new ShareAlert(context, null, settings, false, null, false);
            alert.setDelegate(new ShareAlert.ShareAlertDelegate() {
                @Override
                public void didShare() {
                    Toast.makeText(context, "Settings shared", Toast.LENGTH_SHORT).show();
                    dismissCurrentDialog();
                }

                @Override
                public boolean didCopy() {
                    return true;
                }
            });
            alert.show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialogInterface, i) -> {
            dismissCurrentDialog();
        });
        AlertDialog dialog = builder.create();
        dialog.setDismissDialogByButtons(false);
        showDialog(dialog);
    }

    private void importDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Import settings");
        final EditTextSettingsCell editTextSettingsCell = new EditTextSettingsCell(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(120) + 1);

                int availableWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - AndroidUtilities.dp(42);
                getTextView().measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
            }
        };
        editTextSettingsCell.getTextView().setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        editTextSettingsCell.getTextView().setMaxLines(Integer.MAX_VALUE);
        editTextSettingsCell.getTextView().setLines(8);
        editTextSettingsCell.getTextView().setHint("Paste settings here");
        editTextSettingsCell.getTextView().setSingleLine(false);
        editTextSettingsCell.getTextView().setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        editTextSettingsCell.setText("", true);
        builder.setView(editTextSettingsCell);
        builder.setNeutralButton("Get from clipboard", (dialogInterface, i) -> {
            String clipboard = AndroidUtilities.getFromClipboard();
            if (clipboard == null) {
                editTextSettingsCell.setText("", true);
            } else {

                editTextSettingsCell.setText(clipboard, true);
            }
        });
        builder.setPositiveButton("Import", (dialogInterface, i) -> {
            if (editTextSettingsCell.getText().trim().isEmpty()) {
                Toast.makeText(context, "Nothing to import", Toast.LENGTH_SHORT).show();
            }
            if (AnimationManager.getPreferences().setSettingsFromString(editTextSettingsCell.getText())){
                onUpdateSettings();
                Toast.makeText(context, "Settings imported", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Settings error", Toast.LENGTH_SHORT).show();
            }
            dismissCurrentDialog();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialogInterface, i) -> {
            dismissCurrentDialog();
        });
        AlertDialog dialog = builder.create();
        dialog.setDismissDialogByButtons(false);
        showDialog(dialog);
    }

    private boolean closeActionMode() {
        if (actionBar.isActionModeShowed()) {
            actionBar.hideActionMode();
            return true;
        } else {
            return false;
        }
    }

    private void setScrollY(float value) {
        actionBar.setTranslationY(value);
        if (fragmentContextView != null) {
            fragmentContextView.setTranslationY(additionalPadding + value);
        }
        for (int a = 0; a < settingsPages.length; a++) {
            settingsPages[a].listView.setPinnedSectionOffsetY((int) value);
        }
        fragmentView.invalidate();
    }


    private void updateTabs() {
        if (scrollSlidingTextTabStrip == null) {
            return;
        }
        boolean changed = false;
        for (AnimationPageAdapter page: adapters) {
            if (!scrollSlidingTextTabStrip.hasTab(page.type.ordinal())) {
                changed = true;
            }
        }

        if (changed) {
            scrollSlidingTextTabStrip.removeTabs();
            for (AnimationPageAdapter page: adapters) {
                if (!scrollSlidingTextTabStrip.hasTab(page.type.ordinal())) {
                    scrollSlidingTextTabStrip.addTextTab(page.type.ordinal(), page.getTitle());
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
        int id = scrollSlidingTextTabStrip.getCurrentTabId();
        if (id >= 0) {
            settingsPages[0].selectedType = id;
        }
        scrollSlidingTextTabStrip.finishAddingTabs();
    }

    private void switchToCurrentSelectedMode(boolean animated) {
        for (int a = 0; a < settingsPages.length; a++) {
            settingsPages[a].listView.stopScroll();
        }
        int a = animated ? 1 : 0;
        if (settingsPages[a].listView != null) {
            RecyclerView.Adapter currentAdapter = settingsPages[a].listView.getAdapter();
            for (AnimationPageAdapter adapter : adapters) {
                if (settingsPages[a].selectedType == adapter.type.ordinal()) {
                    if (currentAdapter != adapter) {
                        settingsPages[a].listView.setAdapter(adapter);
                        settingsPages[a].listView.setOnItemClickListener(adapter);
                    }
                }
            }
        }
    }

    private void fixScrollOffset() {
        if (actionBar.getTranslationY() != 0f) {
            final RecyclerListView listView = settingsPages[0].listView;
            final View child = listView.getChildAt(0);
            if (child != null) {
                final int offset = (int) (child.getY() - (actionBar.getMeasuredHeight() + actionBar.getTranslationY() + additionalPadding));
                if (offset > 0) {
                    scrollWithoutActionBar(listView, offset);
                }
            }
        }
    }

    private void scrollWithoutActionBar(RecyclerView listView, int dy) {
        disableActionBarScrolling = true;
        listView.scrollBy(0, dy);
        disableActionBarScrolling = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        scrolling = true;
    }

    @Override
    public boolean isSwipeBackEnabled(MotionEvent event) {
        return scrollSlidingTextTabStrip.getCurrentPosition() == scrollSlidingTextTabStrip.getFirstTabId();
    }

    @Override
    public boolean onBackPressed() {
        return actionBar.isEnabled() && !closeActionMode();
    }

    private void updateSections(RecyclerView listView, boolean checkTopBottom) {
        int count = listView.getChildCount();
        int minPositionDateHolder = Integer.MAX_VALUE;
        View minDateChild = null;
        float padding = listView.getPaddingTop() + actionBar.getTranslationY();
        int minTop = Integer.MAX_VALUE;
        int maxBottom = 0;

        for (int a = 0; a < count; a++) {
            View view = listView.getChildAt(a);
            int bottom = view.getBottom();
            minTop = Math.min(minTop, view.getTop());
            maxBottom = Math.max(bottom, maxBottom);
            if (bottom <= padding) {
                continue;
            }
            int position = view.getBottom();
            if (view instanceof SharedMediaSectionCell || view instanceof GraySectionCell) {
                if (view.getAlpha() != 1.0f) {
                    view.setAlpha(1.0f);
                }
                if (position < minPositionDateHolder) {
                    minPositionDateHolder = position;
                    minDateChild = view;
                }
            }
        }
        if (minDateChild != null) {
            if (minDateChild.getTop() > padding) {
                if (minDateChild.getAlpha() != 1.0f) {
                    minDateChild.setAlpha(1.0f);
                }
            } else {
                if (minDateChild.getAlpha() != 0.0f) {
                    minDateChild.setAlpha(0.0f);
                }
            }
        }
        if (checkTopBottom) {
            if (maxBottom != 0 && maxBottom < (listView.getMeasuredHeight() - listView.getPaddingBottom())) {
                resetScroll();
            } else if (minTop != Integer.MAX_VALUE && minTop > listView.getPaddingTop() + actionBar.getTranslationY()) {
                scrollWithoutActionBar(listView, -listView.computeVerticalScrollOffset());
                resetScroll();
            }
        }
    }

    private void resetScroll() {
        if (actionBar.getTranslationY() == 0) {
            return;
        }
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofFloat(this, SCROLL_Y, 0));
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.setDuration(180);
        animatorSet.start();
    }

    private Paint scrimPaint;

    interface OnMenuItemClickListener {
        void onMenuClick(int i);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void createMenu(View v, ArrayList<CharSequence> items, float x, float y, OnMenuItemClickListener listener) {
        if (actionBar.isActionModeShowed()) {
            return;
        }

        if (items.isEmpty()) {
            return;
        }

        if (scrimPopupWindow != null) {
            scrimPopupWindow.dismiss();
            scrimPopupWindow = null;
            scrimPopupWindowItems = null;
            return;
        }

        Rect rect = new Rect();

        ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(getParentActivity());
        popupLayout.setOnTouchListener(new View.OnTouchListener() {

            private int[] pos = new int[2];

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (scrimPopupWindow != null && scrimPopupWindow.isShowing()) {
                        View contentView = scrimPopupWindow.getContentView();
                        contentView.getLocationInWindow(pos);
                        rect.set(pos[0], pos[1], pos[0] + contentView.getMeasuredWidth(), pos[1] + contentView.getMeasuredHeight());
                        if (!rect.contains((int) event.getX(), (int) event.getY())) {
                            scrimPopupWindow.dismiss();
                        }
                    }
                } else if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
                    if (scrimPopupWindow != null && scrimPopupWindow.isShowing()) {
                        scrimPopupWindow.dismiss();
                    }
                }
                return false;
            }
        });
        popupLayout.setDispatchKeyEventListener(keyEvent -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && scrimPopupWindow != null && scrimPopupWindow.isShowing()) {
                scrimPopupWindow.dismiss();
            }
        });
        Rect backgroundPaddings = new Rect();
        Drawable shadowDrawable = getParentActivity().getResources().getDrawable(R.drawable.popup_fixed_alert).mutate();
        shadowDrawable.getPadding(backgroundPaddings);
        popupLayout.setBackgroundDrawable(shadowDrawable);
        popupLayout.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground));

        LinearLayout linearLayout = new LinearLayout(getParentActivity());
        ScrollView scrollView;
        if (Build.VERSION.SDK_INT >= 21) {
            scrollView = new ScrollView(getParentActivity(), null, 0, R.style.scrollbarShapeStyle) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    setMeasuredDimension(linearLayout.getMeasuredWidth(), getMeasuredHeight());
                }
            };
        } else {
            scrollView = new ScrollView(getParentActivity());
        }
        scrollView.setClipToPadding(false);
        popupLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        linearLayout.setMinimumWidth(AndroidUtilities.dp(115));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        RecyclerListView chatListView = settingsPages[0].listView;
        scrimPopupWindowItems = new ActionBarMenuSubItem[items.size()];
        for (int a = 0, N = items.size(); a < N; a++) {
            ActionBarMenuSubItem cell = new ActionBarMenuSubItem(getParentActivity(), a == 0, a == N - 1);
            cell.setText(items.get(a).toString());
            scrimPopupWindowItems[a] = cell;
            linearLayout.addView(cell);
            final int i = a;
            cell.setOnClickListener(v1 -> {
                listener.onMenuClick(i);
                if (scrimPopupWindow != null) {
                    scrimPopupWindow.dismiss();
                }
            });
        }
        scrollView.addView(linearLayout, LayoutHelper.createScroll(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP));
        scrimPopupWindow = new ActionBarPopupWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT) {
            @Override
            public void dismiss() {
                super.dismiss();
                if (scrimPopupWindow != this) {
                    return;
                }
                scrimPopupWindow = null;
                scrimPopupWindowItems = null;
                if (scrimAnimatorSet != null) {
                    scrimAnimatorSet.cancel();
                    scrimAnimatorSet = null;
                }
                scrimAnimatorSet = new AnimatorSet();
                ArrayList<Animator> animators = new ArrayList<>();
                animators.add(ObjectAnimator.ofInt(scrimPaint, AnimationProperties.PAINT_ALPHA, 0));
                scrimAnimatorSet.playTogether(animators);
                scrimAnimatorSet.setDuration(220);
                scrimAnimatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        contentView.invalidate();
                        chatListView.invalidate();
                    }
                });
                scrimAnimatorSet.start();
            }
        };
        scrimPopupWindow.setPauseNotifications(true);
        scrimPopupWindow.setDismissAnimationDuration(220);
        scrimPopupWindow.setOutsideTouchable(true);
        scrimPopupWindow.setClippingEnabled(true);
        scrimPopupWindow.setAnimationStyle(R.style.PopupContextAnimation);
        scrimPopupWindow.setFocusable(true);
        popupLayout.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST));
        scrimPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        scrimPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        scrimPopupWindow.getContentView().setFocusableInTouchMode(true);
        int popupX = v.getRight() + (int) x - popupLayout.getMeasuredWidth() + backgroundPaddings.left - AndroidUtilities.dp(28);
        if (popupX < AndroidUtilities.dp(6)) {
            popupX = AndroidUtilities.dp(6);
        } else if (popupX > chatListView.getMeasuredWidth() - AndroidUtilities.dp(6) - popupLayout.getMeasuredWidth()) {
            popupX = chatListView.getMeasuredWidth() - AndroidUtilities.dp(6) - popupLayout.getMeasuredWidth();
        }
        if (AndroidUtilities.isTablet()) {
            int[] location = new int[2];
            fragmentView.getLocationInWindow(location);
            popupX += location[0];
        }
        int totalHeight = contentView.getHeight();
        int height = popupLayout.getMeasuredHeight();
        int popupY;
        if (height < totalHeight) {
            popupY = (int) (chatListView.getY() + v.getTop() + y);
            if (height - backgroundPaddings.top - backgroundPaddings.bottom > AndroidUtilities.dp(240)) {
                popupY += AndroidUtilities.dp(240) - height;
            }
            if (popupY < chatListView.getY() + AndroidUtilities.dp(24)) {
                popupY = (int) (chatListView.getY() + AndroidUtilities.dp(24));
            } else if (popupY > totalHeight - height - AndroidUtilities.dp(8)) {
                popupY = totalHeight - height - AndroidUtilities.dp(8);
            }
        } else {
            popupY = inBubbleMode ? 0 : AndroidUtilities.statusBarHeight;
        }
        scrimPopupWindow.showAtLocation(chatListView, Gravity.LEFT | Gravity.TOP, popupX, popupY);
        chatListView.stopScroll();
        contentView.invalidate();
        chatListView.invalidate();
        if (scrimAnimatorSet != null) {
            scrimAnimatorSet.cancel();
        }
        scrimAnimatorSet = new AnimatorSet();
        ArrayList<Animator> animators = new ArrayList<>();
        animators.add(ObjectAnimator.ofInt(scrimPaint, AnimationProperties.PAINT_ALPHA, 0, 50));
        scrimAnimatorSet.playTogether(animators);
        scrimAnimatorSet.setDuration(150);
        scrimAnimatorSet.start();
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

        for (int a = 0; a < settingsPages.length; a++) {
            arrayList.add(new ThemeDescription(settingsPages[a].listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
            arrayList.add(new ThemeDescription(settingsPages[a].listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
            arrayList.add(new ThemeDescription(settingsPages[a].listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));
        }

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

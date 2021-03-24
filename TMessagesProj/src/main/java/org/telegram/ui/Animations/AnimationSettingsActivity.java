package org.telegram.ui.Animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Property;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.LoadingCell;
import org.telegram.ui.Cells.SharedAudioCell;
import org.telegram.ui.Cells.SharedDocumentCell;
import org.telegram.ui.Cells.SharedLinkCell;
import org.telegram.ui.Cells.SharedMediaSectionCell;
import org.telegram.ui.Cells.SharedPhotoVideoCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScrollSlidingTextTabStrip;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.MediaActivity;

import java.util.ArrayList;
import java.util.Collections;

import static org.telegram.ui.Animations.AnimationPageAdapter.PAGE_BACKGROUND;
import static org.telegram.ui.Animations.AnimationPageAdapter.PAGE_EMOJI;
import static org.telegram.ui.Animations.AnimationPageAdapter.PAGE_LINKS;
import static org.telegram.ui.Animations.AnimationPageAdapter.PAGE_SHORT_TEXT;
import static org.telegram.ui.Animations.AnimationPageAdapter.PAGE_VIDEO;
import static org.telegram.ui.Animations.AnimationPageAdapter.PAGE_VOICE;

public class AnimationSettingsActivity extends BaseFragment {


    private final Paint backgroundPaint = new Paint();

    private int additionalPadding;
    private boolean scrolling;
    private boolean disableActionBarScrolling;

    private static class MediaPage extends FrameLayout {
        private RecyclerListView listView;
        private LinearLayoutManager layoutManager;
        private int selectedType;

        public MediaPage(Context context) {
            super(context);
        }
    }

    private AnimationPageAdapter[] adapters;

    private final MediaPage[] mediaPages = new MediaPage[2];

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

    private static final Interpolator interpolator = t -> {
        --t;
        return t * t * t * t * t + 1.0F;
    };

    public final Property<AnimationSettingsActivity, Float> SCROLL_Y = new AnimationProperties.FloatProperty<AnimationSettingsActivity>("animationValue") {
        @Override
        public void setValue(AnimationSettingsActivity object, float value) {
            object.setScrollY(value);
            for (int a = 0; a < mediaPages.length; a++) {
                mediaPages[a].listView.checkSection();
            }
        }

        @Override
        public Float get(AnimationSettingsActivity object) {
            return actionBar.getTranslationY();
        }
    };

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
                if (id == -1) {
                    if (!closeActionMode()) {
                        finishFragment();
                    }
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

        adapters = new AnimationPageAdapter[]{
                new AnimationPageAdapter(context, PAGE_BACKGROUND),
                new AnimationPageAdapter(context, PAGE_SHORT_TEXT),
                new AnimationPageAdapter(context, PAGE_LINKS),
                new AnimationPageAdapter(context, PAGE_EMOJI),
                new AnimationPageAdapter(context, PAGE_VOICE),
                new AnimationPageAdapter(context, PAGE_VIDEO)
        };

        scrollSlidingTextTabStrip.setDelegate(new ScrollSlidingTextTabStrip.ScrollSlidingTabStripDelegate() {
            @Override
            public void onPageSelected(int id, boolean forward) {
                if (mediaPages[0].selectedType == id) {
                    return;
                }
                mediaPages[1].selectedType = id;
                mediaPages[1].setVisibility(View.VISIBLE);
                switchToCurrentSelectedMode(true);
                animatingForward = forward;
            }

            @Override
            public void onPageScrolled(float progress) {
                if (progress == 1 && mediaPages[1].getVisibility() != View.VISIBLE) {
                    return;
                }
                if (animatingForward) {
                    mediaPages[0].setTranslationX(-progress * mediaPages[0].getMeasuredWidth());
                    mediaPages[1].setTranslationX(mediaPages[0].getMeasuredWidth() - progress * mediaPages[0].getMeasuredWidth());
                } else {
                    mediaPages[0].setTranslationX(progress * mediaPages[0].getMeasuredWidth());
                    mediaPages[1].setTranslationX(progress * mediaPages[0].getMeasuredWidth() - mediaPages[0].getMeasuredWidth());
                }
                if (progress == 1) {
                    MediaPage tempPage = mediaPages[0];
                    mediaPages[0] = mediaPages[1];
                    mediaPages[1] = tempPage;
                    mediaPages[1].setVisibility(View.GONE);
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
                mediaPages[1].selectedType = id;
                mediaPages[1].setVisibility(View.VISIBLE);
                animatingForward = forward;
                switchToCurrentSelectedMode(true);
                if (forward) {
                    mediaPages[1].setTranslationX(mediaPages[0].getMeasuredWidth());
                } else {
                    mediaPages[1].setTranslationX(-mediaPages[0].getMeasuredWidth());
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
                for (int a = 0; a < mediaPages.length; a++) {
                    if (mediaPages[a] == null) {
                        continue;
                    }
                    if (mediaPages[a].listView != null) {
                        mediaPages[a].listView.setPadding(0, actionBarHeight + additionalPadding, 0, AndroidUtilities.dp(4));
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
                for (MediaPage mediaPage : mediaPages) {
                    if (mediaPage == null) {
                        continue;
                    }
                    if (mediaPage.listView != null) {
                        mediaPage.listView.setPadding(0, actionBarHeight + additionalPadding, 0, AndroidUtilities.dp(4));
                        mediaPage.listView.checkSection();
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
                        if (Math.abs(mediaPages[0].getTranslationX()) < 1) {
                            mediaPages[0].setTranslationX(0);
                            mediaPages[1].setTranslationX(mediaPages[0].getMeasuredWidth() * (animatingForward ? 1 : -1));
                            cancel = true;
                        }
                    } else if (Math.abs(mediaPages[1].getTranslationX()) < 1) {
                        mediaPages[0].setTranslationX(mediaPages[0].getMeasuredWidth() * (animatingForward ? -1 : 1));
                        mediaPages[1].setTranslationX(0);
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
                                mediaPages[0].setTranslationX(0);
                                mediaPages[1].setTranslationX(animatingForward ? mediaPages[0].getMeasuredWidth() : -mediaPages[0].getMeasuredWidth());
                                scrollSlidingTextTabStrip.selectTabWithId(mediaPages[1].selectedType, 0);
                            }
                        }
                        if (maybeStartTracking && !startedTracking) {
                            float touchSlop = AndroidUtilities.getPixelsInCM(0.3f, true);
                            if (Math.abs(dx) >= touchSlop && Math.abs(dx) > dy) {
                                prepareForMoving(ev, dx < 0);
                            }
                        } else if (startedTracking) {
                            mediaPages[0].setTranslationX(dx);
                            if (animatingForward) {
                                mediaPages[1].setTranslationX(mediaPages[0].getMeasuredWidth() + dx);
                            } else {
                                mediaPages[1].setTranslationX(dx - mediaPages[0].getMeasuredWidth());
                            }
                            float scrollProgress = Math.abs(dx) / (float) mediaPages[0].getMeasuredWidth();
                            scrollSlidingTextTabStrip.selectTabWithId(mediaPages[1].selectedType, scrollProgress);
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
                            float x = mediaPages[0].getX();
                            tabsAnimation = new AnimatorSet();
                            backAnimation = Math.abs(x) < mediaPages[0].getMeasuredWidth() / 3.0f && (Math.abs(velX) < 3500 || Math.abs(velX) < Math.abs(velY));
                            float distToMove;
                            float dx;
                            if (backAnimation) {
                                dx = Math.abs(x);
                                if (animatingForward) {
                                    tabsAnimation.playTogether(
                                            ObjectAnimator.ofFloat(mediaPages[0], View.TRANSLATION_X, 0),
                                            ObjectAnimator.ofFloat(mediaPages[1], View.TRANSLATION_X, mediaPages[1].getMeasuredWidth())
                                    );
                                } else {
                                    tabsAnimation.playTogether(
                                            ObjectAnimator.ofFloat(mediaPages[0], View.TRANSLATION_X, 0),
                                            ObjectAnimator.ofFloat(mediaPages[1], View.TRANSLATION_X, -mediaPages[1].getMeasuredWidth())
                                    );
                                }
                            } else {
                                dx = mediaPages[0].getMeasuredWidth() - Math.abs(x);
                                if (animatingForward) {
                                    tabsAnimation.playTogether(
                                            ObjectAnimator.ofFloat(mediaPages[0], View.TRANSLATION_X, -mediaPages[0].getMeasuredWidth()),
                                            ObjectAnimator.ofFloat(mediaPages[1], View.TRANSLATION_X, 0)
                                    );
                                } else {
                                    tabsAnimation.playTogether(
                                            ObjectAnimator.ofFloat(mediaPages[0], View.TRANSLATION_X, mediaPages[0].getMeasuredWidth()),
                                            ObjectAnimator.ofFloat(mediaPages[1], View.TRANSLATION_X, 0)
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
                                        mediaPages[1].setVisibility(View.GONE);
                                    } else {
                                        MediaPage tempPage = mediaPages[0];
                                        mediaPages[0] = mediaPages[1];
                                        mediaPages[1] = tempPage;
                                        mediaPages[1].setVisibility(View.GONE);
                                        scrollSlidingTextTabStrip.selectTabWithId(mediaPages[0].selectedType, 1.0f);
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

        for (int a = 0; a < mediaPages.length; a++) {
            if (a == 0) {
                if (mediaPages[a] != null && mediaPages[a].layoutManager != null) {
                    scrollToPositionOnRecreate = mediaPages[a].layoutManager.findFirstVisibleItemPosition();
                    if (scrollToPositionOnRecreate != mediaPages[a].layoutManager.getItemCount() - 1) {
                        RecyclerListView.Holder holder = (RecyclerListView.Holder) mediaPages[a].listView.findViewHolderForAdapterPosition(scrollToPositionOnRecreate);
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
            final MediaPage mediaPage = new MediaPage(context) {
                @Override
                public void setTranslationX(float translationX) {
                    super.setTranslationX(translationX);
                    if (tabsAnimationInProgress) {
                        if (mediaPages[0] == this) {
                            float scrollProgress = Math.abs(mediaPages[0].getTranslationX()) / (float) mediaPages[0].getMeasuredWidth();
                            scrollSlidingTextTabStrip.selectTabWithId(mediaPages[1].selectedType, scrollProgress);
                        }
                    }
                }
            };
            frameLayout.addView(mediaPage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            mediaPages[a] = mediaPage;

            final LinearLayoutManager layoutManager = mediaPages[a].layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
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
            mediaPages[a].listView = new RecyclerListView(context) {
                @Override
                protected void onLayout(boolean changed, int l, int t, int r, int b) {
                    super.onLayout(changed, l, t, r, b);
                    updateSections(this, true);
                }
            };
            mediaPages[a].listView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
            mediaPages[a].listView.setItemAnimator(null);
            mediaPages[a].listView.setClipToPadding(false);
            mediaPages[a].listView.setSectionsType(2);
            mediaPages[a].listView.setLayoutManager(layoutManager);
            mediaPages[a].addView(mediaPages[a].listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            mediaPages[a].listView.setOnItemClickListener((view, position) -> {

            });
            mediaPages[a].listView.setOnScrollListener(new RecyclerView.OnScrollListener() {

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
                                mediaPages[0].listView.smoothScrollBy(0, -scrollY);
                            } else if (mediaPages[0].listView.canScrollVertically(1)) {
                                mediaPages[0].listView.smoothScrollBy(0, actionBarHeight - scrollY);
                            }
                        }
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (recyclerView == mediaPages[0].listView && !actionBar.isActionModeShowed() && !disableActionBarScrolling) {
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
            mediaPages[a].listView.setOnItemLongClickListener((view, position) -> {

                return false;
            });
            if (a == 0 && scrollToPositionOnRecreate != -1) {
                layoutManager.scrollToPositionWithOffset(scrollToPositionOnRecreate, scrollToOffsetOnRecreate);
            }

            if (a != 0) {
                mediaPages[a].setVisibility(View.GONE);
            }
        }

        if (!AndroidUtilities.isTablet()) {
            frameLayout.addView(fragmentContextView = new FragmentContextView(context, this, false), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.TOP | Gravity.LEFT, 0, 8, 0, 0));
        }

        frameLayout.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        updateTabs();
        switchToCurrentSelectedMode(false);

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

    private void setScrollY(float value) {
        actionBar.setTranslationY(value);
        if (fragmentContextView != null) {
            fragmentContextView.setTranslationY(additionalPadding + value);
        }
        for (int a = 0; a < mediaPages.length; a++) {
            mediaPages[a].listView.setPinnedSectionOffsetY((int) value);
        }
        fragmentView.invalidate();
    }


    private void updateTabs() {
        if (scrollSlidingTextTabStrip == null) {
            return;
        }
        boolean changed = false;
        for (AnimationPageAdapter page: adapters) {
            if (!scrollSlidingTextTabStrip.hasTab(page.pageType)) {
                changed = true;
            }
        }

        if (changed) {
            scrollSlidingTextTabStrip.removeTabs();
            for (AnimationPageAdapter page: adapters) {
                if (!scrollSlidingTextTabStrip.hasTab(page.pageType)) {
                    scrollSlidingTextTabStrip.addTextTab(page.pageType, page.getTitle());
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
            mediaPages[0].selectedType = id;
        }
        scrollSlidingTextTabStrip.finishAddingTabs();
    }

    private void switchToCurrentSelectedMode(boolean animated) {
        for (int a = 0; a < mediaPages.length; a++) {
            mediaPages[a].listView.stopScroll();
        }
        int a = animated ? 1 : 0;
        if (mediaPages[a].listView != null) {
            RecyclerView.Adapter currentAdapter = mediaPages[a].listView.getAdapter();
            for (AnimationPageAdapter adapter : adapters) {
                if (mediaPages[a].selectedType == adapter.pageType) {
                    if (currentAdapter != adapter) {
                        mediaPages[a].listView.setAdapter(adapter);
                    }
                }
            }
        }
    }

    private void fixScrollOffset() {
        if (actionBar.getTranslationY() != 0f) {
            final RecyclerListView listView = mediaPages[0].listView;
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

        for (int a = 0; a < mediaPages.length; a++) {
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
            arrayList.add(new ThemeDescription(mediaPages[a].listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));
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

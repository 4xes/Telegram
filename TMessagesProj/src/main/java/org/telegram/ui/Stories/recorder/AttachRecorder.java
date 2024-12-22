package org.telegram.ui.Stories.recorder;

import static android.view.View.VISIBLE;
import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.drawView;
import static org.telegram.messenger.AndroidUtilities.rectTmp;
import static org.telegram.messenger.AndroidUtilities.rectTmp2;
import static org.telegram.messenger.AndroidUtilities.snapshotView;
import static org.telegram.messenger.AndroidUtilities.touchSlop;
import static org.telegram.messenger.AndroidUtilities.updateViewVisibilityAnimated;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.Components.Bulletin.DURATION_PROLONG;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AnimationNotificationsLocker;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.messenger.camera.CameraController;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.PhotoAttachPhotoCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.Attach.AttachCameraDelegate;
import org.telegram.ui.Components.Attach.CameraRecyclerView;
import org.telegram.ui.Components.Attach.CameraThumbUtils;
import org.telegram.ui.Components.Attach.CollageAttachLayoutView;
import org.telegram.ui.Components.Attach.CounterTextView;
import org.telegram.ui.Components.BlurringShader;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatAttachAlertPhotoLayout;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.GestureDetectorFixDoubleTap;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ProgressView;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.ThanosEffect;
import org.telegram.ui.Components.WaveDrawable;
import org.telegram.ui.Components.ZoomControlView;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.Stories.DarkThemeResourceProvider;
import org.telegram.ui.Stories.StoriesController;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class AttachRecorder implements NotificationCenter.NotificationCenterDelegate {

    public static final String SETTINGS_KEY_CAMERA_HINT = "attach_camera_hint_1";
    public static final String SETTINGS_KEY_CAMERA_DUAL_HINT = "attach_camera_dual_hint_1";
    private final Theme.ResourcesProvider resourcesProvider = new DarkThemeResourceProvider();

    private final Activity activity;


    public WindowView windowView;
    private ContainerView containerView;
    private FlashViews flashViews;
    private ThanosEffect thanosEffect;

    private final int currentAccount;
    private boolean cameraPhotoRecyclerViewIgnoreLayout = true;
    private RadialProgressView radialProgressView;

    private final AnimationNotificationsLocker notificationsLocker = new AnimationNotificationsLocker();


    @SuppressLint("WrongConstant")
    public AttachRecorder(Activity activity, int currentAccount, AttachCameraDelegate attachCameraDelegate) {
        this.activity = activity;
        this.currentAccount = currentAccount;
        this.attachDelegate = attachCameraDelegate;

        initViews();
        initPhotoRecycler(activity);
    }

    public void initPhotoRecycler(Context context) {
        cameraRecyclerView = new CameraRecyclerView(context, resourcesProvider, attachDelegate.getAlert(), attachDelegate.getAlert().parentAlert) {
            @Override
            public void requestLayout() {
                if (cameraPhotoRecyclerViewIgnoreLayout) {
                    return;
                }
                super.requestLayout();
            }
        };
        cameraRecyclerView.setAdapter(cameraAttachAdapter = attachDelegate.getAlert().createPhotoAttachAdapter(context));
        cameraAttachAdapter.createCache();
        cameraPhotoLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        cameraRecyclerView.setLayoutManager(cameraPhotoLayoutManager);
        cameraRecyclerView.setOnItemClickListener((view, position) -> {
            if (view instanceof PhotoAttachPhotoCell) {
                ((PhotoAttachPhotoCell) view).callDelegate();
            }
        });
        cameraRecyclerView.setVisibility(View.GONE);
        containerView.addView(cameraRecyclerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 80));

        counterTextView = new CounterTextView(context);
        counterTextView.setVisibility(View.GONE);
        counterTextView.setOnClickListener(v -> {
            if (cameraView == null) {
                return;
            }
            attachDelegate.getAlert().openPhotoViewer(null, false, false);
            attachDelegate.stopCameraPreview();
        });
        containerView.addView(counterTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 38));

        radialProgressView = new RadialProgressView(context);
        containerView.addView(radialProgressView);
        radialProgressView.setVisibility(View.GONE);
    }

    public void checkVisibleViewer() {
        setVisibleViewer(!ChatAttachAlertPhotoLayout.cameraPhotos.isEmpty());
    }

    public void setVisibleViewer(boolean isVisible) {
        if (isVisible) {
            counterTextView.setVisibility(VISIBLE);
            cameraRecyclerView.setVisibility(VISIBLE);
        } else {
            counterTextView.setVisibility(View.GONE);
            cameraRecyclerView.setVisibility(View.GONE);
        }
    }

    private void lock() {
        notificationsLocker.lock();
    }

    private void unlock() {
        notificationsLocker.unlock();
    }

    public void onOpen(boolean animated) {
        showControls(true, animated);
        lock();
        addNotificationObservers();
    }

    public void onClose(boolean animated) {
        clearCollage();
        showControls(false, animated);
        unlock();
        removeNotificationObservers();
    }

    public WindowView getWindowView() {
        return windowView;
    }

    public ZoomControlView getZoomControlView() {
        return zoomControlView;
    }

    public DualCameraView getCameraView() {
        return cameraView;
    }

    public ChatAttachAlertPhotoLayout.PhotoAttachAdapter getCameraAdapter() {
        return cameraAttachAdapter;
    }

    public LinearLayoutManager getCameraPhotoLayoutManager() {
        return cameraPhotoLayoutManager;
    }

    public CameraRecyclerView getCameraRecyclerView() {
        return cameraRecyclerView;
    }

    public CollageLayoutView2 getCameraContainer() {
        return collageLayoutView;
    }

    public TextView getCounterTextView() {
        return counterTextView;
    }

    private float dismissProgress;
    long selectedDialogId;

    @Nullable
    Runnable dualHintRunnable;
    @Nullable
    Runnable savedDualHintRunnable;

    private boolean scrollingY, scrollingX;

    private int insetLeft, insetRight;
    private int insetTop = AndroidUtilities.statusBarHeight;
    private int insetBottom = AndroidUtilities.navigationBarHeight;

    public class WindowView extends SizeNotifierFrameLayout {

        private final GestureDetectorFixDoubleTap gestureDetector;
        private final ScaleGestureDetector scaleGestureDetector;

        public WindowView(Context context) {
            super(context);
            gestureDetector = new GestureDetectorFixDoubleTap(context, new GestureListener());
            scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        }

        @Override
        public int getBottomPadding() {
            return getHeight() - containerView.getBottom() + insetTop;
        }

        private boolean flingDetected;
        private boolean touchInCollageList;

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            if (isCameraRecyclerVisible() && !wasGalleryOpen) {
                cameraRecyclerView.getGlobalVisibleRect(rectTmp2);
                if (rectTmp2.contains((int) ev.getX(), (int) ev.getY())){
                    return super.dispatchTouchEvent(ev);
                }
            }
            flingDetected = false;
            if (collageListView != null && collageListView.isVisible()) {
                final float y = containerView.getY() + actionBarContainer.getY() + collageListView.getY();
                if (ev.getY() >= y && ev.getY() <= y + collageListView.getHeight() || touchInCollageList) {
                    touchInCollageList = ev.getAction() != MotionEvent.ACTION_UP && ev.getAction() != MotionEvent.ACTION_CANCEL;
                    return super.dispatchTouchEvent(ev);
                } else {
                    collageListView.setVisible(false, true);
                    updateActionBarButtons(true);
                }
            }
            if (touchInCollageList && (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL)) {
                touchInCollageList = false;
            }
            scaleGestureDetector.onTouchEvent(ev);
            gestureDetector.onTouchEvent(ev);
            if (ev.getAction() == MotionEvent.ACTION_UP && !flingDetected) {
                if (containerView.getTranslationY() > 0) {
                    if (dismissProgress > .4f) {
                        animateContainerBack();
                        attachDelegate.onClose();
                    } else {
                        animateContainerBack();
                        showControls(true, true);
                    }
                } else if (galleryListView != null && galleryListView.getTranslationY() > 0 && !galleryClosing) {
                    animateGalleryListView(!takingVideo && galleryListView.getTranslationY() < galleryListView.getPadding());
                }
                galleryClosing = false;
                modeSwitcherView.stopScroll(0);
                scrollingY = false;
                scrollingX = false;
            }
            return super.dispatchTouchEvent(ev);
        }

        public void cancelGestures() {
            scaleGestureDetector.onTouchEvent(AndroidUtilities.emptyMotionEvent());
            gestureDetector.onTouchEvent(AndroidUtilities.emptyMotionEvent());
        }

        @Override
        public boolean dispatchKeyEventPreIme(KeyEvent event) {
            if (event != null && event.getKeyCode()
                    == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                return onBackPressed();
            }
            return super.dispatchKeyEventPreIme(event);
        }

        private boolean scaling = false;
        private final class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (!scaling || cameraView == null || currentPage != PAGE_CAMERA || cameraView.isDualTouch() || collageLayoutView.getFilledProgress() >= 1) {
                    return false;
                }
                final float deltaScaleFactor = (detector.getScaleFactor() - 1.0f) * .75f;
                cameraZoom += deltaScaleFactor;
                cameraZoom = Utilities.clamp(cameraZoom, 1, 0);
                cameraView.setZoom(cameraZoom);
                if (zoomControlView != null) {
                    zoomControlView.setZoom(cameraZoom, false);
                }
                showZoomControls(true, true);
                return true;
            }

            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                if (cameraView == null || currentPage != PAGE_CAMERA || wasGalleryOpen) {
                    return false;
                }
                scaling = true;
                return super.onScaleBegin(detector);
            }

            @Override
            public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
                scaling = false;
                animateGalleryListView(false);
                animateContainerBack();
                showControls(true, true);
                super.onScaleEnd(detector);
            }
        }

        private float sty;
        private float stx;

        private final class GestureListener extends GestureDetectorFixDoubleTap.OnGestureListener {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                sty = 0;
                stx = 0;
                return false;
            }

            @Override
            public void onShowPress(@NonNull MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                scrollingY = false;
                scrollingX = false;
                if (!hasDoubleTap(e)) {
                    if (onSingleTapConfirmed(e)) {
                        return true;
                    }
                }
                if (isGalleryOpen() && e.getY() < galleryListView.top()) {
                    animateGalleryListView(false);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                if (galleryOpenCloseSpringAnimator != null || galleryOpenCloseAnimator != null || recordControl.isTouch() || cameraView != null && cameraView.isDualTouch() || scaling || zoomControlView != null && zoomControlView.isTouch() || inCheck()) {
                    return false;
                }
                if (takingVideo || takingPhoto || currentPage != PAGE_CAMERA) {
                    return false;
                }
                if (!scrollingX) {
                    sty += distanceY;
                    if (!scrollingY && Math.abs(sty) >= touchSlop) {
                        if (collageLayoutView != null) {
                            collageLayoutView.cancelTouch();
                        }
                        scrollingY = true;
                    }
                }
                if (scrollingY) {
                    int galleryMax = windowView.getMeasuredHeight() - (int) (AndroidUtilities.displaySize.y * 0.35f) - (AndroidUtilities.statusBarHeight + ActionBar.getCurrentActionBarHeight());
                    float ty;
                    if (galleryListView == null || galleryListView.getTranslationY() >= galleryMax) {
                        ty = containerView.getTranslationY1();
                    } else {
                        ty = galleryListView.getTranslationY() - galleryMax;
                    }
                    if (galleryListView != null && galleryListView.listView.canScrollVertically(-1)) {
                        distanceY = Math.max(0, distanceY);
                    }
                    ty -= distanceY;
                    ty = Math.max(-galleryMax, ty);
                    if (currentPage == PAGE_PREVIEW) {
                        ty = Math.max(0, ty);
                    }
                    if (ty >= 0) {
                        containerView.setTranslationY(ty);
                        if (galleryListView != null) {
                            galleryListView.setTranslationY(galleryMax);
                        }
                    } else {
                        containerView.setTranslationY(0);
                        if (galleryListView == null) {
                            createGalleryListView();
                        }
                        galleryListView.setTranslationY(galleryMax + ty);
                    }
                }
                if (!scrollingY) {
                    stx += distanceX;
                    if (!scrollingX && Math.abs(stx) >= touchSlop) {
                        if (collageLayoutView != null) {
                            collageLayoutView.cancelTouch();
                        }
                        scrollingX = true;
                    }
                }
                if (scrollingX) {
                    modeSwitcherView.scrollX(distanceX);
                }
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {

            }

            @Override
            public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                if (recordControl.isTouch() || cameraView != null && cameraView.isDualTouch() || scaling || zoomControlView != null && zoomControlView.isTouch() || inCheck()) {
                    return false;
                }
                flingDetected = true;
                boolean r = false;
                if (scrollingY) {
                    if (Math.abs(containerView.getTranslationY1()) >= dp(1)) {
                        if (velocityY > 0 && Math.abs(velocityY) > 2000 && Math.abs(velocityY) > Math.abs(velocityX) || dismissProgress > .4f) {
                            attachDelegate.onClose();
                            animateContainerBack();
                        } else {
                            animateContainerBack();
                            showControls(true, true);
                        }
                        r = true;
                    } else if (galleryListView != null && !galleryClosing) {
                        if (Math.abs(velocityY) > 200 && (!galleryListView.listView.canScrollVertically(-1) || !wasGalleryOpen)) {
                            animateGalleryListView(!takingVideo && velocityY < 0);
                            r = true;
                        } else {
                            animateGalleryListView(!takingVideo && galleryListView.getTranslationY() < galleryListView.getPadding());
                            r = true;
                        }
                    }
                }
                if (scrollingX) {
                    r = modeSwitcherView.stopScroll(velocityX) || r;
                }
                galleryClosing = false;
                scrollingY = false;
                scrollingX = false;
                if (r && collageLayoutView != null) {
                    collageLayoutView.cancelTouch();
                }
                return r;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (cameraView != null) {
                    cameraView.allowToTapFocus();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (cameraView == null || takingPhoto || !cameraView.isInited() || currentPage != PAGE_CAMERA) {
                    return false;
                }
                cameraView.switchCamera();
                recordControl.rotateFlip(180);
                saveCameraFace(cameraView.isFrontface());
                if (useDisplayFlashlight()) {
                    flashViews.flashIn(null);
                } else {
                    flashViews.flashOut();
                }
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                if (cameraView != null) {
                    cameraView.clearTapFocus();
                }
                return false;
            }

            @Override
            public boolean hasDoubleTap(MotionEvent e) {
                return currentPage == PAGE_CAMERA && cameraView != null && cameraView.isInited() && !takingPhoto && !recordControl.isTouch() && !isGalleryOpen() && galleryListViewOpening == null;
            }
        }


        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (Build.VERSION.SDK_INT < 21) {
                insetTop = AndroidUtilities.statusBarHeight;
                insetBottom = AndroidUtilities.navigationBarHeight;
            }

            final int W = MeasureSpec.getSize(widthMeasureSpec);
            final int H = MeasureSpec.getSize(heightMeasureSpec);
            final int w = W - insetLeft - insetRight;

            containerView.measure(
                MeasureSpec.makeMeasureSpec(W, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(H, MeasureSpec.EXACTLY)
            );
            flashViews.backgroundView.measure(
                MeasureSpec.makeMeasureSpec(W, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(H, MeasureSpec.EXACTLY)
            );
            if (thanosEffect != null) {
                thanosEffect.measure(
                    MeasureSpec.makeMeasureSpec(W, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(H, MeasureSpec.EXACTLY)
                );
            }

            if (galleryListView != null) {
                galleryListView.measure(MeasureSpec.makeMeasureSpec(W, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(H, MeasureSpec.EXACTLY));
            }

            for (int i = 0; i < getChildCount(); ++i) {
                View child = getChildAt(i);
                if (child instanceof DownloadButton.PreparingVideoToast) {
                    child.measure(
                        MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(H, MeasureSpec.EXACTLY)
                    );
                } else if (child instanceof Bulletin.ParentLayout) {
                    child.measure(
                        MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(Math.min(dp(340), H - insetTop), MeasureSpec.EXACTLY)
                    );
                }
            }

            setMeasuredDimension(W, H);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            final int W = right - left;
            final int H = bottom - top;

            containerView.layout(0, 0, W, H);
            flashViews.backgroundView.layout(0, 0, W, H);
            if (thanosEffect != null) {
                thanosEffect.layout(0, 0, W, H);
            }

            if (galleryListView != null) {
                galleryListView.layout((W - galleryListView.getMeasuredWidth()) / 2, 0, (W + galleryListView.getMeasuredWidth()) / 2, H);
            }

            for (int i = 0; i < getChildCount(); ++i) {
                View child = getChildAt(i);
                if (child instanceof DownloadButton.PreparingVideoToast) {
                    child.layout(0, 0, W, H);
                } else if (child instanceof Bulletin.ParentLayout) {
                    child.layout(0, insetTop, child.getMeasuredWidth(), insetTop + child.getMeasuredHeight());
                }
            }
        }
    }

    private class ContainerView extends FrameLayout {
        public ContainerView(Context context) {
            super(context);
        }

        private float translationY1;
        private float translationY2;

        public void setTranslationY2(float translationY2) {
            super.setTranslationY(this.translationY1 + (this.translationY2 = translationY2));
        }

        public float getTranslationY1() {
            return translationY1;
        }

        public float getTranslationY2() {
            return translationY2;
        }

        @Override
        public void setTranslationY(float translationY) {
            super.setTranslationY((this.translationY1 = translationY) + translationY2);

//            controlContainer.setTranslationY(-this.getTranslationY());
//            navbarContainer.setTranslationY(-this.getTranslationY());
//            actionBarButtons.setTranslationY(-this.getTranslationY());

            dismissProgress = Utilities.clamp(translationY / getMeasuredHeight() * 4, 1, 0);

            windowView.invalidate();

            if (dismissProgress > 0.1f && translationY > 0f) {
                if (containerViewBackAnimator == null) {
                    showControls(false, true);
                }
            }

            final float scale = 1f - .1f * Utilities.clamp(getTranslationY() / AndroidUtilities.dp(320), 1, 0);
            setScaleX(scale);
            setScaleY(scale);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            final int w = right - left;
            final int h = bottom - top;

            boolean isPortrait = true;


            previewContainer.layout(0, 0, w, h);
            if (isPortrait) {
                previewContainer.setPivotX(w * .5f);
            } else {
                previewContainer.setPivotY(h * .5f);
            }
            actionBarContainer.layout(0, insetTop, w, insetTop + actionBarContainer.getMeasuredHeight());

            int b = h - Math.max(attachDelegate.getAlert().parentAlert.getBottomInset(), dp(16));;
            int t = b - cameraRecyclerView.getMeasuredHeight();
            cameraRecyclerView.layout(0, t, w, b);
            if (isCameraRecyclerVisible()) {
                b = t;
            } else {
                b = h - attachDelegate.getAlert().parentAlert.getBottomInset();
            }
            t = b - navbarContainer.getMeasuredHeight();
            navbarContainer.layout(0, t, w, b);
            b = t;
            t = b - controlContainer.getMeasuredHeight();
            controlContainer.layout(0, t, w, b);
            b = b - dp(90);
            t = b - counterTextView.getMeasuredHeight();
            int cx = (w - counterTextView.getMeasuredWidth()) / 2;
            counterTextView.layout(cx, t, cx + counterTextView.getMeasuredWidth(), b);

            cx = w / 2;
            int cy = h / 2;
            int halfProgress = radialProgressView.getMeasuredHeight() / 2;
            radialProgressView.layout(cx - halfProgress, cy - halfProgress, cx + halfProgress, cy + halfProgress);

            flashViews.foregroundView.layout(0, 0, w, h);
            for (int i = 0; i < getChildCount(); ++i) {
                View child = getChildAt(i);
                if (child instanceof ItemOptions.DimView) {
                    child.layout(0, 0, w, h);
                }
            }

            if (isPortrait) {
                setPivotX((right - left) / 2f);
                setPivotY(-h * .2f);
            } else {
                setPivotX(-w * .2f);
                setPivotY(bottom - top);
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final int W = MeasureSpec.getSize(widthMeasureSpec);
            final int H = MeasureSpec.getSize(heightMeasureSpec);
            boolean isPortrait = true;

            measureChildExactly(previewContainer, H, W);
            if (isPortrait) {
                measureChildExactly(actionBarContainer, W, dp(56 + 56 + 38));
            } else {
                measureChildExactly(actionBarContainer, dp(56 + 56 + 38), H);
            }
            if (isPortrait) {
                measureChildExactly(controlContainer, W, dp(220));
            } else {
                measureChildExactly(controlContainer, dp(220), H);
            }
            if (isPortrait) {
                measureChildExactly(navbarContainer, W, dp(48));
            } else {
                measureChildExactly(navbarContainer, dp(48), H);
            }
            measureChild(counterTextView, widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(dp(38), View.MeasureSpec.EXACTLY));
            measureChildExactly(radialProgressView, dp(56), dp(56));

            cameraPhotoRecyclerViewIgnoreLayout = true;
            if (isPortrait) {
                cameraRecyclerView.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(80), View.MeasureSpec.EXACTLY));
                if (cameraPhotoLayoutManager.getOrientation() != LinearLayoutManager.HORIZONTAL) {
                    cameraRecyclerView.setPadding(dp(8), 0, dp(8), 0);
                    cameraPhotoLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
                    cameraAttachAdapter.notifyDataSetChanged();
                }
            } else {
                cameraRecyclerView.measure(View.MeasureSpec.makeMeasureSpec(dp(80), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(H, View.MeasureSpec.EXACTLY));
                if (cameraPhotoLayoutManager.getOrientation() != LinearLayoutManager.VERTICAL) {
                    cameraRecyclerView.setPadding(0, dp(8), 0, dp(8));
                    cameraPhotoLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                    cameraAttachAdapter.notifyDataSetChanged();
                }
            }
            cameraPhotoRecyclerViewIgnoreLayout = false;

            measureChildExactly(flashViews.foregroundView, W, H);

            for (int i = 0; i < getChildCount(); ++i) {
                View child = getChildAt(i);
                if (child instanceof ItemOptions.DimView) {
                    measureChildExactly(child, W, H);
                }
            }

            setMeasuredDimension(W, H);
        }

        private void measureChildExactly(View child, int width, int height) {
            child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }

        private final Paint topGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private LinearGradient topGradient;

        @Override
        protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
            boolean r = super.drawChild(canvas, child, drawingTime);
            if (child == previewContainer) {
                final float top = AndroidUtilities.statusBarHeight;
                if (topGradient == null) {
                    topGradient = new LinearGradient(0, top, 0, top + dp(72), new int[] {0x40000000, 0x00000000}, new float[] { top / (top + dp(72)), 1 }, Shader.TileMode.CLAMP );
                    topGradientPaint.setShader(topGradient);
                }
                topGradientPaint.setAlpha(0xFF);
                AndroidUtilities.rectTmp.set(0, 0, getWidth(), dp(72 + 12) + top);
                canvas.drawRoundRect(AndroidUtilities.rectTmp, dp(12), dp(12), topGradientPaint);
            }
            return r;
        }
    }

    public static final int PAGE_CAMERA = 0;
    public static final int PAGE_PREVIEW = 1;
    private int currentPage = PAGE_CAMERA;

    private FrameLayout previewContainer;
    private FrameLayout actionBarContainer;
    private LinearLayout actionBarButtons;
    private FrameLayout controlContainer;
    private FrameLayout navbarContainer;

    private FlashViews.ImageViewInvertable backButton;
    private BlurringShader.BlurManager blurManager;

    private CollageLayout lastCollageLayout;

    /* PAGE_CAMERA */
    private CollageLayoutView2 collageLayoutView;
    private ImageView cameraViewThumb;
    private DualCameraView cameraView;

    private ToggleButton2 flashButton;
    private ToggleButton dualButton;
    private CollageLayoutButton collageButton;
    private ToggleButton2 collageRemoveButton;
    private CollageLayoutButton.CollageLayoutListView collageListView;
    private VideoTimerView videoTimerView;
    private boolean wasGalleryOpen;
    private boolean galleryClosing;
    private GalleryListView galleryListView;
    private RecordControl recordControl;
    private PhotoVideoSwitcherView modeSwitcherView;
    private HintTextView hintTextView;
    private HintTextView collageHintTextView;
    private ZoomControlView zoomControlView;
    public CameraRecyclerView cameraRecyclerView;
    public LinearLayoutManager cameraPhotoLayoutManager;
    public ChatAttachAlertPhotoLayout.PhotoAttachAdapter cameraAttachAdapter;
    private TextView counterTextView;
    private HintView2 cameraHint;
    private HintView2 dualHint;
    private HintView2 savedDualHint;

    private File outputFile;
    private StoryEntry outputEntry;
    private MessageObject collageVideoMessage;
    private String collageThumbPath;
    private boolean fromGallery;

    private boolean videoError;

    private boolean isVideo = false;
    private boolean takingPhoto = false;
    private boolean takingVideo = false;
    private boolean stoppingTakingVideo = false;

    private float cameraZoom;


    public Context getContext() {
        return activity;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initViews() {
        Context context = getContext();

        windowView = new WindowView(context);
        if (Build.VERSION.SDK_INT >= 21) {
            //windowView.setFitsSystemWindows(true);
            windowView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @NonNull
                @Override
                public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets insets) {
                    final WindowInsetsCompat insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets, v);
                    final androidx.core.graphics.Insets i = insetsCompat.getInsets(WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.systemBars());
                    insetTop    = Math.max(i.top, insets.getStableInsetTop());
                    insetBottom = Math.max(i.bottom, insets.getStableInsetBottom());
                    insetLeft   = Math.max(i.left, insets.getStableInsetLeft());
                    insetRight  = Math.max(i.right, insets.getStableInsetRight());
                    insetTop = Math.max(insetTop, AndroidUtilities.statusBarHeight);
                    windowView.requestLayout();
                    if (Build.VERSION.SDK_INT >= 30) {
                        return WindowInsets.CONSUMED;
                    } else {
                        return insets.consumeSystemWindowInsets();
                    }
                }
            });
        }
        windowView.setFocusable(true);

        flashViews = new FlashViews(context, null, null, null);
        flashViews.add(new FlashViews.Invertable() {
            @Override
            public void setInvert(float invert) {
                AndroidUtilities.setLightNavigationBar(windowView, invert > 0.5f);
                AndroidUtilities.setLightStatusBar(windowView, invert > 0.5f);
            }
            @Override
            public void invalidate() {}
        });
        windowView.addView(flashViews.backgroundView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        windowView.addView(containerView = new ContainerView(context));
        containerView.addView(previewContainer = new FrameLayout(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (!attachDelegate.isCameraExpanded()) {
                    return false;
                }
                return super.onTouchEvent(event);
            }

            private final Rect leftExclRect = new Rect();
            private final Rect rightExclRect = new Rect();

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    final int w = right - left;
                    final int h = bottom - top;
                    leftExclRect.set(0, h - dp(120), dp(40), h);
                    rightExclRect.set(w - dp(40), h - dp(120), w, h);
                    setSystemGestureExclusionRects(Arrays.asList(leftExclRect, rightExclRect));
                }
            }

            private RenderNode renderNode;
            @Override
            protected void dispatchDraw(@NonNull Canvas c) {
                boolean endRecording = false;
                Canvas canvas = c;
                if (Build.VERSION.SDK_INT >= 31 && c.isHardwareAccelerated() && !AndroidUtilities.makingGlobalBlurBitmap) {
                    if (renderNode == null) {
                        renderNode = new RenderNode("StoryRecorder.PreviewView");
                    }
                    renderNode.setPosition(0, 0, getWidth(), getHeight());
                    canvas = renderNode.beginRecording();
                    endRecording = true;
                }
                super.dispatchDraw(canvas);
                if (endRecording && Build.VERSION.SDK_INT >= 31) {
                    renderNode.endRecording();
                    if (blurManager != null) {
                        blurManager.setRenderNode(this, renderNode, 0xFF1F1F1F);
                    }
                    c.drawRenderNode(renderNode);
                }
            }
        });
        containerView.addView(flashViews.foregroundView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        blurManager = new BlurringShader.BlurManager(previewContainer);

        actionBarContainer = new FrameLayout(context);// 150dp
        actionBarContainer.setVisibility(View.GONE);
        containerView.addView(actionBarContainer);

        controlContainer = new FrameLayout(context);
        controlContainer.setVisibility(View.GONE);
        containerView.addView(controlContainer); // 220dp

        navbarContainer = new FrameLayout(context);
        navbarContainer.setVisibility(View.GONE);
        containerView.addView(navbarContainer); // 48dp

        Bulletin.addDelegate(windowView, new Bulletin.Delegate() {
            @Override
            public int getTopOffset(int tag) {
                return dp(56);
            }

            @Override
            public int getBottomOffset(int tag) {
                return Bulletin.Delegate.super.getBottomOffset(tag);
            }

            @Override
            public boolean clipWithGradient(int tag) {
                return true;
            }
        });

        collageLayoutView = new CollageAttachLayoutView(context, blurManager, containerView, resourcesProvider, attachDelegate) {
            @Override
            protected void onLayoutUpdate(CollageLayout layout) {
                collageListView.setVisible(false, true);
                if (layout != null && layout.parts.size() > 1) {
                    collageButton.setIcon(new CollageLayoutButton.CollageLayoutDrawable(lastCollageLayout = layout), true);
                    collageButton.setSelected(true, true);
                } else {
                    collageButton.setSelected(false, true);
                }
                updateActionBarButtons(true);
            }
        };
        collageLayoutView.setCancelGestures(windowView::cancelGestures);
        collageLayoutView.setResetState(() -> {
            updateActionBarButtons(true);
        });

        previewContainer.addView(collageLayoutView, new FrameLayout.LayoutParams(attachDelegate.getItemSize(), attachDelegate.getItemSize()));

        cameraViewThumb = new ImageView(context);
        cameraViewThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cameraViewThumb.setVisibility(View.GONE);
        //previewContainer.addView(cameraViewThumb, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        backButton = new FlashViews.ImageViewInvertable(context);
        backButton.setContentDescription(getString(R.string.AccDescrGoBack));
        backButton.setScaleType(ImageView.ScaleType.CENTER);
        backButton.setImageResource(R.drawable.ic_close_white);
        backButton.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY));
        backButton.setBackground(Theme.createSelectorDrawable(0x20ffffff));
        backButton.setOnClickListener(e -> {
           attachDelegate.onClose();
        });
        actionBarContainer.addView(backButton, LayoutHelper.createFrame(56, 56, Gravity.TOP | Gravity.LEFT));
        flashViews.add(backButton);

        actionBarButtons = new LinearLayout(context);
        actionBarButtons.setOrientation(LinearLayout.HORIZONTAL);
        actionBarButtons.setGravity(Gravity.RIGHT);
        actionBarContainer.addView(actionBarButtons, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56, Gravity.RIGHT | Gravity.FILL_HORIZONTAL, 0, 0, 8, 0));

        flashButton = new ToggleButton2(context);
        flashButton.setBackground(Theme.createSelectorDrawable(0x20ffffff));
        flashButton.setOnClickListener(e -> {
            if (cameraView == null) {
                return;
            }
            String current = getCurrentFlashMode();
            String next = getNextFlashMode();
            if (current == null || current.equals(next)) {
                return;
            }
            setCurrentFlashMode(next);
            setCameraFlashModeIcon(next, true);
        });
        flashButton.setOnLongClickListener(e -> {
            if (cameraView == null || !cameraView.isFrontface()) {
                return false;
            }

            checkFrontfaceFlashModes();
            flashButton.setSelected(true);
            flashViews.previewStart();
            ItemOptions.makeOptions(containerView, resourcesProvider, flashButton)
                .addView(
                    new SliderView(getContext(), SliderView.TYPE_WARMTH)
                        .setValue(flashViews.warmth)
                        .setOnValueChange(v -> {
                            flashViews.setWarmth(v);
                        })
                )
                .addSpaceGap()
                .addView(
                    new SliderView(getContext(), SliderView.TYPE_INTENSITY)
                        .setMinMax(.65f, 1f)
                        .setValue(flashViews.intensity)
                        .setOnValueChange(v -> {
                            flashViews.setIntensity(v);
                        })
                )
                .setOnDismiss(() -> {
                    saveFrontFaceFlashMode();
                    flashViews.previewEnd();
                    flashButton.setSelected(false);
                })
                .setDimAlpha(0)
                .setGravity(Gravity.RIGHT)
                .translate(dp(46), -dp(4))
                .setBackgroundColor(0xbb1b1b1b)
                .show();
            return true;
        });
        flashButton.setVisibility(View.GONE);
        flashButton.setAlpha(0f);
        flashViews.add(flashButton);
        actionBarContainer.addView(flashButton, LayoutHelper.createFrame(56, 56, Gravity.TOP | Gravity.RIGHT));

        dualButton = new ToggleButton(context, R.drawable.media_dual_camera2_shadow, R.drawable.media_dual_camera2);
        dualButton.setOnClickListener(v -> {
            if (cameraView == null || currentPage != PAGE_CAMERA) {
                return;
            }
            cameraView.toggleDual();
            dualButton.setValue(cameraView.isDual());

            dualHint.hide();
            MessagesController.getGlobalMainSettings().edit().putInt("storydualhint", 2).apply();
            if (savedDualHint.shown()) {
                MessagesController.getGlobalMainSettings().edit().putInt(SETTINGS_KEY_CAMERA_DUAL_HINT, 2).apply();
            }
            savedDualHint.hide();
        });
        final boolean dualCameraAvailable = DualCameraView.dualAvailableStatic(context);
        dualButton.setVisibility(attachDelegate.getAlert().isChatActivity() && dualCameraAvailable ? View.VISIBLE : View.GONE);
        dualButton.setAlpha(dualCameraAvailable ? 1.0f : 0.0f);
        if (attachDelegate.getAlert().isChatActivity()) {
            flashViews.add(dualButton);
        }
        actionBarContainer.addView(dualButton, LayoutHelper.createFrame(56, 56, Gravity.TOP | Gravity.RIGHT));

        collageButton = new CollageLayoutButton(context);
        collageButton.setBackground(Theme.createSelectorDrawable(0x20ffffff));
        if (lastCollageLayout == null) {
            lastCollageLayout = CollageLayout.getLayouts().get(6);
        }
        collageButton.setOnClickListener(v -> {
            if (currentPage != PAGE_CAMERA || animatedRecording) return;
            if (cameraView != null && cameraView.isDual()) {
                cameraView.toggleDual();
            }
            if (!collageListView.isVisible() && !collageLayoutView.hasLayout()) {
                collageLayoutView.setLayout(lastCollageLayout, true);
                collageListView.setSelected(lastCollageLayout);
                collageButton.setIcon(new CollageLayoutButton.CollageLayoutDrawable(lastCollageLayout), true);
                collageButton.setSelected(true);
                if (cameraView != null) {
                    cameraView.recordHevc = !collageLayoutView.hasLayout();
                }
            }
            collageListView.setVisible(!collageListView.isVisible(), true);
            updateActionBarButtons(true);
        });
        collageButton.setIcon(new CollageLayoutButton.CollageLayoutDrawable(lastCollageLayout), false);
        collageButton.setSelected(false);
        collageButton.setVisibility(attachDelegate.getAlert().isChatActivity() ? View.VISIBLE: View.GONE);
        collageButton.setAlpha(1.0f);
        if (attachDelegate.getAlert().isChatActivity()) {
            flashViews.add(dualButton);
        }
        actionBarContainer.addView(collageButton, LayoutHelper.createFrame(56, 56, Gravity.TOP | Gravity.RIGHT));

        collageRemoveButton = new ToggleButton2(context);
        collageRemoveButton.setBackground(Theme.createSelectorDrawable(0x20ffffff));
        collageRemoveButton.setIcon(new CollageLayoutButton.CollageLayoutDrawable(new CollageLayout("../../.."), true), false);
        collageRemoveButton.setVisibility(View.GONE);
        collageRemoveButton.setAlpha(0.0f);
        collageRemoveButton.setOnClickListener(v -> {
            collageLayoutView.setLayout(null, true);
            collageLayoutView.clear(true);
            collageListView.setSelected(null);
            if (cameraView != null) {
                cameraView.recordHevc = !collageLayoutView.hasLayout();
            }
            collageListView.setVisible(false, true);
            updateActionBarButtons(true);
        });
        flashViews.add(collageRemoveButton);
        actionBarContainer.addView(collageRemoveButton, LayoutHelper.createFrame(56, 56, Gravity.TOP | Gravity.RIGHT));

        collageListView = new CollageLayoutButton.CollageLayoutListView(context, flashViews);
        collageListView.listView.scrollToPosition(6);
        collageListView.setSelected(null);
        collageListView.setOnLayoutClick(layout -> {
            collageLayoutView.setLayout(lastCollageLayout = layout, true);
            collageListView.setSelected(layout);
            if (cameraView != null) {
                cameraView.recordHevc = !collageLayoutView.hasLayout();
            }
            collageButton.setDrawable(new CollageLayoutButton.CollageLayoutDrawable(layout));
            setActionBarButtonVisible(collageRemoveButton, collageListView.isVisible(), true);
            recordControl.setCollageProgress(collageLayoutView.hasLayout() ? collageLayoutView.getFilledProgress() : 0.0f, true);
        });
        actionBarContainer.addView(collageListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56, Gravity.TOP | Gravity.RIGHT));

        dualHint = new HintView2(activity, HintView2.DIRECTION_TOP)
            .setJoint(1, -20)
            .setDuration(5000)
            .setCloseButton(true)
            .setText(getString(R.string.StoryCameraDualHint))
            .setOnHiddenListener(() -> MessagesController.getGlobalMainSettings().edit().putInt("storydualhint", MessagesController.getGlobalMainSettings().getInt("storydualhint", 0) + 1).apply());
        dualHint.setPadding(dp(8), 0, dp(8), 0);
        if (attachDelegate.getAlert().isChatActivity()) {
            actionBarContainer.addView(dualHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 52, 0, 0));
        }

        savedDualHint = new HintView2(activity, HintView2.DIRECTION_RIGHT)
                .setJoint(0, 56 / 2)
                .setDuration(5000)
                .setMultilineText(true);
        actionBarContainer.addView(savedDualHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 0, 52, 0));

        videoTimerView = new VideoTimerView(context);
        showVideoTimer(false, false);
        actionBarContainer.addView(videoTimerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 45, Gravity.TOP | Gravity.FILL_HORIZONTAL, 56, 0, 56, 0));
        flashViews.add(videoTimerView);

        if (Build.VERSION.SDK_INT >= 21) {
            MediaController.loadGalleryPhotosAlbums(0);
        }

        recordControl = new RecordControl(context);
        recordControl.setMaxDuration(Long.MAX_VALUE);
        recordControl.setDelegate(recordControlDelegate);
        recordControl.setAttachCameraDelegate(attachDelegate);
        recordControl.startAsVideo(isVideo);
        controlContainer.addView(recordControl, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 100, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL));
        flashViews.add(recordControl);
        recordControl.setCollageProgress(collageLayoutView.hasLayout() ? collageLayoutView.getFilledProgress() : 0.0f, true);

        cameraHint = new HintView2(activity, HintView2.DIRECTION_BOTTOM)
                .setMultilineText(true)
                .setText(getString(R.string.StoryCameraHint2))
                .setMaxWidth(320)
                .setDuration(5000L)
                .setTextAlign(Layout.Alignment.ALIGN_CENTER);
        controlContainer.addView(cameraHint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.BOTTOM, 0, 0, 0, 100));

        zoomControlView = new ZoomControlView(context);
        zoomControlView.enabledTouch = false;
        zoomControlView.setAlpha(0.0f);
        controlContainer.addView(zoomControlView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0, 0, 100 + 8));

        zoomControlView.setDelegate(zoom -> {
            if (cameraView != null) {
                cameraView.setZoom(cameraZoom = zoom);
            }
            showZoomControls(true, true);
        });
        zoomControlView.setZoom(cameraZoom = 0, false);

        modeSwitcherView = new PhotoVideoSwitcherView(context) {
            @Override
            protected boolean allowTouch() {
                return !inCheck();
            }
        };
        modeSwitcherView.setOnSwitchModeListener(newIsVideo -> {
            if (takingPhoto || takingVideo) {
                return;
            }

            isVideo = newIsVideo;
            showVideoTimer(isVideo && !collageListView.isVisible(), true);
            modeSwitcherView.switchMode(isVideo);
            recordControl.startAsVideo(isVideo);
        });
        modeSwitcherView.setOnSwitchingModeListener(t -> {
            recordControl.startAsVideoT(t);
        });
        navbarContainer.addView(modeSwitcherView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL));
        flashViews.add(modeSwitcherView);

        hintTextView = new HintTextView(context);
        navbarContainer.addView(hintTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 32, Gravity.CENTER, 8, 0, 8, 8));
        flashViews.add(hintTextView);

        collageHintTextView = new HintTextView(context);
        collageHintTextView.setText(LocaleController.getString(R.string.StoryCollageReorderHint), false);
        collageHintTextView.setAlpha(0.0f);
        navbarContainer.addView(collageHintTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 32, Gravity.CENTER, 8, 0, 8, 8));
        if (attachDelegate.getAlert().isChatActivity()) {
            flashViews.add(collageHintTextView);
        }
        updateActionBarButtonsOffsets();
    }

    private boolean isCameraRecyclerVisible() {
        return cameraRecyclerView.getVisibility() == VISIBLE;
    }

    private String flashButtonMode;
    private void setCameraFlashModeIcon(String mode, boolean animated) {
        flashButton.clearAnimation();
        if (cameraView != null && cameraView.isDual() || animatedRecording) {
            mode = null;
        }
        flashButtonMode = mode;
        if (mode == null) {
            setActionBarButtonVisible(flashButton, false, animated);
            return;
        }
        final int resId;
        switch (mode) {
            case Camera.Parameters.FLASH_MODE_ON:
                resId = R.drawable.media_photo_flash_on2;
                flashButton.setContentDescription(getString(R.string.AccDescrCameraFlashOn));
                break;
            case Camera.Parameters.FLASH_MODE_AUTO:
                resId = R.drawable.media_photo_flash_auto2;
                flashButton.setContentDescription(getString(R.string.AccDescrCameraFlashAuto));
                break;
            default:
            case Camera.Parameters.FLASH_MODE_OFF:
                resId = R.drawable.media_photo_flash_off2;
                flashButton.setContentDescription(getString(R.string.AccDescrCameraFlashOff));
                break;
        }
        flashButton.setIcon(resId, animated);
        setActionBarButtonVisible(flashButton, currentPage == PAGE_CAMERA && !collageListView.isVisible() && flashButtonMode != null && !inCheck(), animated);
    }

    private boolean requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity != null) {
            boolean granted = activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 112);
                return false;
            }
        }
        return true;
    }

    private final RecordControl.Delegate recordControlDelegate = new RecordControl.Delegate() {
        @Override
        public boolean canRecordAudio() {
            return requestAudioPermission();
        }

        @Override
        public void onPhotoShoot() {
            if (takingPhoto || currentPage != PAGE_CAMERA || cameraView == null || !cameraView.isInited()) {
                return;
            }
            cameraHint.hide();
            if (outputFile != null) {
                try {
                    outputFile.delete();
                } catch (Exception ignore) {}
                outputFile = null;
            }


            outputFile = attachDelegate.generatePhotoOutput();
            takingPhoto = true;
            checkFrontfaceFlashModes();
            isDark = false;
            if (cameraView.isFrontface() && frontfaceFlashMode == 1) {
                checkIsDark();
            }
            if (useDisplayFlashlight()) {
                flashViews.flash(this::takePicture);
            } else {
                takePicture(null);
            }
        }

        @Override
        public void onCheckClick() {
            ArrayList<StoryEntry> entries = collageLayoutView.getContent();
            if (entries.size() == 1) {
                outputEntry = entries.get(0);
            } else {
                outputEntry = StoryEntry.asCollage(collageLayoutView.getLayout(), collageLayoutView.getContent());
            }
            isVideo = outputEntry != null && outputEntry.isVideo;
            if (modeSwitcherView != null) {
                modeSwitcherView.switchMode(isVideo);
            }

            if (isVideo) {
                outputFile = attachDelegate.generateVideoOutput();

                String thumbFile = getCollageThumbPath();
                getCollageVideo(outputFile, thumbFile, outputEntry);
            } else {
                collageLayoutView.clear(true);
                attachDelegate.getAlert().onTakePicture(getCollageImage(), 0, true);
            }
        }

        private String getCollageThumbPath() {
            Bitmap bitmap = drawView(collageLayoutView);
            Bitmap bitmapFinal = Bitmap.createScaledBitmap(bitmap, 80, (int) (bitmap.getHeight() / (bitmap.getWidth() / 80.0f)), true);
            bitmap.recycle();

            String fileName = Integer.MIN_VALUE + "_" + SharedConfig.getLastLocalId() + ".jpg";
            File cacheFile = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), fileName);
            String path = cacheFile.getAbsolutePath();
            ImageLoader.getInstance().putImageToCache(new BitmapDrawable(bitmapFinal), Utilities.MD5(path), false);
            return path;
        }

        private File getCollageImage() {
            outputFile = attachDelegate.generatePhotoOutput();
            Bitmap bitmap = drawView(collageLayoutView);
            saveToFile(outputFile, bitmap, false);
            return outputFile;
        }

        public boolean saveToFile(File file, Bitmap bitmap, boolean thumb) {
            Bitmap resultBitmap = bitmap;
            try (FileOutputStream out = new FileOutputStream(file.getAbsoluteFile())) {
                if (thumb) {
                    resultBitmap = Bitmap.createScaledBitmap(bitmap, 80, (int) (bitmap.getHeight() / (bitmap.getWidth() / 80.0f)), true);
                }
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                return true;
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                bitmap.recycle();
                if (resultBitmap != bitmap) {
                    resultBitmap.recycle();
                }
            }
            return false;
        }

        public void getCollageVideo(File file, String thumbPath, StoryEntry entry) {
            TLRPC.TL_message message = new TLRPC.TL_message();
            message.id = 1;
            message.attachPath = file.getPath();
            collageVideoMessage = new MessageObject(currentAccount, message, (MessageObject) null, false, false);
            collageThumbPath = thumbPath;
            final MessageObject messageObject = collageVideoMessage;
            entry.getVideoEditedInfo(info -> {
                messageObject.videoEditedInfo = info;
                Log.e("COLLAGE", "scheduleVideoMessage");

                collageLayoutView.setPreview(true);
                recordControl.setVisibility(View.GONE);
                radialProgressView.setVisibility(VISIBLE);
                collageHintTextView.setAlpha(0.0f);
                MediaController.getInstance().scheduleVideoConvert(messageObject, false, false, false);
            });
        }

        private void takePicture(Utilities.Callback<Runnable> done) {
            boolean savedFromTextureView = false;
            if (!useDisplayFlashlight()) {
                cameraView.startTakePictureAnimation(true);
            }
            if (cameraView.isDual() && TextUtils.equals(cameraView.getCameraSession().getCurrentFlashMode(), Camera.Parameters.FLASH_MODE_OFF) || collageLayoutView.hasLayout()) {
                if (!collageLayoutView.hasLayout()) {
                    cameraView.pauseAsTakingPicture();
                }
                final Bitmap bitmap = cameraView.getTextureView().getBitmap();
                savedFromTextureView = saveToFile(outputFile, bitmap, false);
            }
            if (!savedFromTextureView) {
                AndroidUtilities.lockOrientation(activity);
                final boolean sameTakePictureOrientation = cameraView.getCameraSession().isSameTakePictureOrientation();
                takingPhoto = CameraController.getInstance().takePicture(outputFile, false, cameraView.getCameraSessionObject(), (orientation) -> {
                    if (useDisplayFlashlight()) {
                        try {
                            windowView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                        } catch (Exception ignore) {}
                    }
                    AndroidUtilities.unlockOrientation(activity);
                    takingPhoto = false;
                    if (outputFile == null) {
                        return;
                    }
                    int w = -1, h = -1;
                    try {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(outputFile.getAbsolutePath(), opts);
                        w = opts.outWidth;
                        h = opts.outHeight;
                    } catch (Exception ignore) {}

                    int rotate = orientation == -1 ? 0 : 90;
                    if (orientation == -1) {
                        if (w > h) {
                            rotate = 270;
                        }
                    } else if (h > w && rotate != 0) {
                        rotate = 0;
                    }
                    StoryEntry entry = StoryEntry.fromPhotoShoot(outputFile, rotate);
                    if (collageLayoutView.hasLayout()) {
                        outputFile = null;
                        if (collageLayoutView.push(entry)) {
                            outputEntry = StoryEntry.asCollage(collageLayoutView.getLayout(), collageLayoutView.getContent());
                            fromGallery = false;

                            if (done != null) {
                                done.run(null);
                            }
                        } else if (done != null) {
                            done.run(null);
                        }
                        updateActionBarButtons(true);
                    } else {
                        outputEntry = entry;
                        fromGallery = false;

                        if (done != null) {
                            done.run(() -> {
                                attachDelegate.getAlert().onTakePicture(outputFile, orientation, sameTakePictureOrientation);
                            });
                        } else {
                            attachDelegate.getAlert().onTakePicture(outputFile, orientation, sameTakePictureOrientation);
                        }
                    }
                });
            } else {
                takingPhoto = false;
                final StoryEntry entry = StoryEntry.fromPhotoShoot(outputFile, 0);
                if (collageLayoutView.hasLayout()) {
                    outputFile = null;
                    if (collageLayoutView.push(entry)) {
                        outputEntry = StoryEntry.asCollage(collageLayoutView.getLayout(), collageLayoutView.getContent());
                        fromGallery = false;
                        if (done != null) {
                            done.run(null);
                        }
                    } else if (done != null) {
                        done.run(null);
                    }
                    updateActionBarButtons(true);
                } else {
                    outputEntry = entry;
                    fromGallery = false;

                    if (done != null) {
                        done.run(() -> {
                            attachDelegate.getAlert().onTakePicture(outputFile, 0, true);
                        });
                    } else {
                        attachDelegate.getAlert().onTakePicture(outputFile, 0, true);
                    }
                }
            }
        }

        @Override
        public void onVideoRecordStart(boolean byLongPress, Runnable whenStarted) {
            if (takingVideo || stoppingTakingVideo || currentPage != PAGE_CAMERA || cameraView == null || cameraView.getCameraSession() == null) {
                return;
            }
            if (dualHint != null) {
                dualHint.hide();
            }
            if (savedDualHint != null) {
                savedDualHint.hide();
            }
            cameraHint.hide();
            takingVideo = true;
            if (outputFile != null) {
                try {
                    outputFile.delete();
                } catch (Exception ignore) {}
                outputFile = null;
            }
            if (collageLayoutView.hasLayout()) {
                outputFile = StoryEntry.makeCacheFile(currentAccount, true);
            } else {
                outputFile = attachDelegate.generateVideoOutput();
            }
            checkFrontfaceFlashModes();
            isDark = false;
            if (cameraView.isFrontface() && frontfaceFlashMode == 1) {
                checkIsDark();
            }
            if (useDisplayFlashlight()) {
                flashViews.flashIn(() -> startRecording(byLongPress, whenStarted));
            } else {
                startRecording(byLongPress, whenStarted);
            }
        }

        private void startRecording(boolean byLongPress, Runnable whenStarted) {
            if (cameraView == null) {
                return;
            }
            AndroidUtilities.lockOrientation(activity);
            CameraController.getInstance().recordVideo(cameraView.getCameraSessionObject(), outputFile, false, (thumbPath, duration) -> {
                AndroidUtilities.unlockOrientation(activity);
                if (recordControl != null) {
                    recordControl.stopRecordingLoading(true);
                }
                if (useDisplayFlashlight()) {
                    flashViews.flashOut();
                }
                if (outputFile == null || cameraView == null) {
                    return;
                }

                takingVideo = false;
                stoppingTakingVideo = false;

                if (duration <= 800) {
                    animateRecording(false, true);
                    setAwakeLock(false);
                    videoTimerView.setRecording(false, true);
                    if (recordControl != null) {
                        recordControl.stopRecordingLoading(true);
                    }
                    try {
                        outputFile.delete();
                        outputFile = null;
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    if (thumbPath != null) {
                        try {
                            new File(thumbPath).delete();
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                    return;
                }

                showVideoTimer(false, true);

                StoryEntry entry = StoryEntry.fromVideoShoot(outputFile, thumbPath, duration);
                animateRecording(false, true);
                setAwakeLock(false);
                videoTimerView.setRecording(false, true);
                if (recordControl != null) {
                    recordControl.stopRecordingLoading(true);
                }
                if (collageLayoutView.hasLayout()) {
                    outputFile = null;
                    entry.videoVolume = 1.0f;
                    if (collageLayoutView.push(entry)) {
                        outputEntry = StoryEntry.asCollage(collageLayoutView.getLayout(), collageLayoutView.getContent());
                        fromGallery = false;
                        int width = cameraView.getVideoWidth(), height = cameraView.getVideoHeight();
                        if (width > 0 && height > 0) {
                            outputEntry.width = width;
                            outputEntry.height = height;
                            outputEntry.setupMatrix();
                        }
                    }
                    updateActionBarButtons(true);
                } else {
                    outputEntry = null;
                    attachDelegate.getAlert().onRecordedVideo(thumbPath, duration, outputFile);
                }
            }, () /* onVideoStart */ -> {
                whenStarted.run();

                hintTextView.setText(getString(byLongPress ? R.string.StoryHintSwipeToZoom : R.string.StoryHintPinchToZoom), false);
                animateRecording(true, true);
                setAwakeLock(true);

                collageListView.setVisible(false, true);
                videoTimerView.setRecording(true, true);
                showVideoTimer(true, true);
            }, cameraView, true);

            if (!isVideo) {
                isVideo = true;
                collageListView.setVisible(false, true);
                showVideoTimer(isVideo, true);
                modeSwitcherView.switchMode(isVideo);
                recordControl.startAsVideo(isVideo);
            }
        }

        @Override
        public void onVideoRecordLocked() {
            hintTextView.setText(getString(R.string.StoryHintPinchToZoom), true);
        }

        @Override
        public void onVideoRecordPause() {

        }

        @Override
        public void onVideoRecordResume() {

        }

        @Override
        public void onVideoRecordEnd(boolean byDuration) {
            if (stoppingTakingVideo || !takingVideo) {
                return;
            }
            stoppingTakingVideo = true;
            AndroidUtilities.runOnUIThread(() -> {
                if (takingVideo && stoppingTakingVideo && cameraView != null) {
                    showZoomControls(false, true);
                    setAwakeLock(false);
                    CameraController.getInstance().stopVideoRecording(cameraView.getCameraSessionRecording(), false, false);
                }
            }, byDuration ? 0 : 400);
        }

        @Override
        public void onVideoDuration(long duration) {
            videoTimerView.setDuration(duration, true);
        }

        @Override
        public void onGalleryClick() {
            if (currentPage == PAGE_CAMERA && !takingPhoto && !takingVideo && requestGalleryPermission()) {
                animateGalleryListView(true);
            }
        }

        @Override
        public void onFlipClick() {
            if (cameraView == null || takingPhoto || !cameraView.isInited() || currentPage != PAGE_CAMERA) {
                return;
            }
            if (savedDualHint != null) {
                savedDualHint.hide();
            }
            if (useDisplayFlashlight() && frontfaceFlashModes != null && !frontfaceFlashModes.isEmpty()) {
                final String mode = frontfaceFlashModes.get(frontfaceFlashMode);
                SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("camera", Activity.MODE_PRIVATE);
                sharedPreferences.edit().putString("flashMode", mode).commit();
            }
            cameraView.switchCamera();
            saveCameraFace(cameraView.isFrontface());
            if (useDisplayFlashlight()) {
                flashViews.flashIn(null);
            } else {
                flashViews.flashOut();
            }
        }

        @Override
        public void onFlipLongClick() {
            if (cameraView != null) {
                cameraView.toggleDual();
            }
        }

        @Override
        public void onZoom(float zoom) {
            zoomControlView.setZoom(zoom, true);
            showZoomControls(false, true);
        }
    };

    private void setAwakeLock(boolean lock) {
        Activity activity = AndroidUtilities.findActivity(getContext());
        if (activity == null) activity = LaunchActivity.instance;
        if (activity == null || activity.isFinishing()) return;
        final Window window = activity.getWindow();
        if (window == null) return;
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        if (lock) {
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        } else {
            layoutParams.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        }
        window.setAttributes(layoutParams);
    }

    private AnimatorSet recordingAnimator;
    private boolean animatedRecording;
    private boolean animatedRecordingWasInCheck;
    private void animateRecording(boolean recording, boolean animated) {
        if (recording) {
            if (dualHint != null) {
                dualHint.hide();
            }
            if (savedDualHint != null) {
                savedDualHint.hide();
            }
            if (cameraHint != null) {
                cameraHint.hide();
            }
        }
        if (animatedRecording == recording && animatedRecordingWasInCheck == inCheck()) {
            return;
        }
        if (recordingAnimator != null) {
            recordingAnimator.cancel();
            recordingAnimator = null;
        }
        animatedRecording = recording;
        animatedRecordingWasInCheck = inCheck();
        if (recording && collageListView != null && collageListView.isVisible()) {
            collageListView.setVisible(false, animated);
        }
        updateActionBarButtons(animated);
        if (animated) {
            recordingAnimator = new AnimatorSet();
            recordingAnimator.playTogether(
                ObjectAnimator.ofFloat(hintTextView, View.ALPHA, recording && currentPage == PAGE_CAMERA && !inCheck() ? 1 : 0),
                ObjectAnimator.ofFloat(hintTextView, View.TRANSLATION_Y, recording && currentPage == PAGE_CAMERA && !inCheck() ? 0 : dp(16)),
                ObjectAnimator.ofFloat(collageHintTextView, View.ALPHA, !recording && currentPage == PAGE_CAMERA && inCheck() ? 0.6f : 0),
                ObjectAnimator.ofFloat(collageHintTextView, View.TRANSLATION_Y, !recording && currentPage == PAGE_CAMERA && inCheck() ? 0 : dp(16)),
                ObjectAnimator.ofFloat(modeSwitcherView, View.ALPHA, recording || currentPage != PAGE_CAMERA || inCheck() ? 0 : 1),
                ObjectAnimator.ofFloat(modeSwitcherView, View.TRANSLATION_Y, recording || currentPage != PAGE_CAMERA || inCheck() ? dp(16) : 0)
            );
            recordingAnimator.setDuration(260);
            recordingAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            recordingAnimator.start();
        } else {
            hintTextView.setAlpha(recording && currentPage == PAGE_CAMERA && !inCheck() ? 1f : 0);
            hintTextView.setTranslationY(recording && currentPage == PAGE_CAMERA && !inCheck() ? 0 : dp(16));
            collageHintTextView.setAlpha(!recording && currentPage == PAGE_CAMERA && inCheck() ? 0.6f : 0);
            collageHintTextView.setTranslationY(!recording && currentPage == PAGE_CAMERA && inCheck() ? 0 : dp(16));
            modeSwitcherView.setAlpha(recording || currentPage != PAGE_CAMERA || inCheck() ? 0 : 1f);
            modeSwitcherView.setTranslationY(recording || currentPage != PAGE_CAMERA || inCheck() ? dp(16) : 0);
        }
    }

    private boolean isDark;
    private void checkIsDark() {
        if (cameraView == null || cameraView.getTextureView() == null) {
            isDark = false;
            return;
        }
        final Bitmap bitmap = cameraView.getTextureView().getBitmap();
        if (bitmap == null) {
            isDark = false;
            return;
        }
        float l = 0;
        final int sx = bitmap.getWidth() / 12;
        final int sy = bitmap.getHeight() / 12;
        for (int x = 0; x < 10; ++x) {
            for (int y = 0; y < 10; ++y) {
                l += AndroidUtilities.computePerceivedBrightness(bitmap.getPixel((1 + x) * sx, (1 + y) * sy));
            }
        }
        l /= 100;
        bitmap.recycle();
        isDark = l < .22f;
    }

    private boolean useDisplayFlashlight() {
        return (takingPhoto || takingVideo) && (cameraView != null && cameraView.isFrontface()) && (frontfaceFlashMode == 2 || frontfaceFlashMode == 1 && isDark);
    }

    private boolean videoTimerShown = true;
    private void showVideoTimer(boolean show, boolean animated) {
        if (videoTimerShown == show) {
            return;
        }

        videoTimerShown = show;
        if (animated) {
            videoTimerView.animate().alpha(show ? 1 : 0).setDuration(350).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).withEndAction(() -> {
                if (!show) {
                    videoTimerView.setRecording(false, false);
                }
            }).start();
        } else {
            videoTimerView.clearAnimation();
            videoTimerView.setAlpha(show ? 1 : 0);
            if (!show) {
                videoTimerView.setRecording(false, false);
            }
        }
    }

    private Runnable zoomControlHideRunnable;
    private AnimatorSet zoomControlAnimation;

    private void showZoomControls(boolean show, boolean animated) {
        if (zoomControlView.getTag() != null && show || zoomControlView.getTag() == null && !show) {
            if (show) {
                if (zoomControlHideRunnable != null) {
                    AndroidUtilities.cancelRunOnUIThread(zoomControlHideRunnable);
                }
                AndroidUtilities.runOnUIThread(zoomControlHideRunnable = () -> {
                    showZoomControls(false, true);
                    zoomControlHideRunnable = null;
                }, 2000);
            }
            return;
        }
        if (zoomControlAnimation != null) {
            zoomControlAnimation.cancel();
        }
        zoomControlView.setTag(show ? 1 : null);
        zoomControlAnimation = new AnimatorSet();
        zoomControlAnimation.setDuration(180);
        if (show) {
            zoomControlView.setVisibility(View.VISIBLE);
        }
        zoomControlAnimation.playTogether(ObjectAnimator.ofFloat(zoomControlView, View.ALPHA, show ? 1.0f : 0.0f));
        zoomControlAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!show) {
                    zoomControlView.setVisibility(View.GONE);
                }
                zoomControlAnimation = null;
            }
        });
        zoomControlAnimation.start();
        if (show) {
            AndroidUtilities.runOnUIThread(zoomControlHideRunnable = () -> {
                showZoomControls(false, true);
                zoomControlHideRunnable = null;
            }, 2000);
        }
    }

    public boolean onBackPressed() {
        if (takingVideo) {
            recordControl.stopRecording();
            return true;
        }
        if (takingPhoto) {
            return true;
        }
        if (galleryListView != null) {
            if (galleryListView.onBackPressed()) {
                return true;
            }
            animateGalleryListView(false);
            lastGallerySelectedAlbum = null;
            return true;
        } else if( collageLayoutView.hasContent()) {
            clearCollage();

            return true;
        } {
            return false;
        }
    }

    public void clearCollage() {
        radialProgressView.setVisibility(View.GONE);
        collageVideoMessage = null;
        collageLayoutView.setPreview(false);
        collageLayoutView.clear(true);
        collageHintTextView.setVisibility(VISIBLE);
        recordControl.setVisibility(VISIBLE);
        updateActionBarButtons(true);
    }

    private AnimatorSet pageAnimator;
    public void navigateTo(int page, boolean animated) {
        if (page == currentPage) {
            return;
        }

        final int oldPage = currentPage;
        currentPage = page;

        if (pageAnimator != null) {
            pageAnimator.cancel();
        }

        onNavigateStart(oldPage, page);
        showVideoTimer(page == PAGE_CAMERA && isVideo && !collageListView.isVisible() && !inCheck(), animated);
        setActionBarButtonVisible(backButton, !collageListView.isVisible(), animated);
        setActionBarButtonVisible(flashButton, page == PAGE_CAMERA && !collageListView.isVisible() && flashButtonMode != null && !inCheck(), animated);
        setActionBarButtonVisible(dualButton, page == PAGE_CAMERA && cameraView != null && cameraView.dualAvailable() && !collageListView.isVisible() && !collageLayoutView.hasLayout(), true);
        setActionBarButtonVisible(collageButton, page == PAGE_CAMERA && !collageListView.isVisible(), animated);
        updateActionBarButtons(animated);
        if (animated) {
            pageAnimator = new AnimatorSet();

            ArrayList<Animator> animators = new ArrayList<>();

            if (cameraView != null) {
                animators.add(ObjectAnimator.ofFloat(cameraView, View.ALPHA, page == PAGE_CAMERA ? 1 : 0));
            }
            cameraViewThumb.setVisibility(View.VISIBLE);
            animators.add(ObjectAnimator.ofFloat(cameraViewThumb, View.ALPHA, page == PAGE_CAMERA ? 1 : 0));
            animators.add(ObjectAnimator.ofFloat(collageLayoutView, View.ALPHA, page == PAGE_CAMERA || page == PAGE_PREVIEW && collageLayoutView.hasLayout() ? 1 : 0));

            animators.add(ObjectAnimator.ofFloat(recordControl, View.ALPHA, page == PAGE_CAMERA ? 1 : 0));
            animators.add(ObjectAnimator.ofFloat(recordControl, View.TRANSLATION_Y, page == PAGE_CAMERA ? 0 : dp(24)));
            animators.add(ObjectAnimator.ofFloat(modeSwitcherView, View.ALPHA, page == PAGE_CAMERA && !inCheck() ? 1 : 0));
            animators.add(ObjectAnimator.ofFloat(modeSwitcherView, View.TRANSLATION_Y, page == PAGE_CAMERA && !inCheck() ? 0 : dp(24)));
            animators.add(ObjectAnimator.ofFloat(hintTextView, View.ALPHA, page == PAGE_CAMERA && animatedRecording && !inCheck() ? 1 : 0));
            animators.add(ObjectAnimator.ofFloat(collageHintTextView, View.ALPHA, page == PAGE_CAMERA && !animatedRecording && inCheck() ? 0.6f : 0));

            animators.add(ObjectAnimator.ofFloat(zoomControlView, View.ALPHA, 0));

            pageAnimator.playTogether(animators);
            pageAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    onNavigateEnd();
                }
            });
            pageAnimator.setDuration(460);
            pageAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            pageAnimator.start();
        } else {
            if (cameraView != null) {
                cameraView.setAlpha(page == PAGE_CAMERA ? 1 : 0);
            }
            cameraViewThumb.setAlpha(page == PAGE_CAMERA ? 1f : 0);
            cameraViewThumb.setVisibility(page == PAGE_CAMERA ? View.VISIBLE : View.GONE);
            collageLayoutView.setAlpha(page == PAGE_CAMERA || page == PAGE_PREVIEW && collageLayoutView.hasLayout() ? 1 : 0);
            recordControl.setAlpha(page == PAGE_CAMERA ? 1f : 0);
            recordControl.setTranslationY(page == PAGE_CAMERA ? 0 : dp(16));
            modeSwitcherView.setAlpha(page == PAGE_CAMERA && !inCheck() ? 1f : 0);
            modeSwitcherView.setTranslationY(page == PAGE_CAMERA && !inCheck() ? 0 : dp(16));
            hintTextView.setAlpha(page == PAGE_CAMERA && animatedRecording && !inCheck() ? 1f : 0);
            collageHintTextView.setAlpha(page == PAGE_CAMERA && !animatedRecording && inCheck() ? 0.6f : 0);
            onNavigateEnd();
        }
    }

    private ValueAnimator containerViewBackAnimator;
    private boolean applyContainerViewTranslation2 = true;

    private Animator showControlsAnimator;
    private Animator hideControlsAnimator;

    public void setGoneControls() {
        controlContainer.setVisibility(View.GONE);
        navbarContainer.setVisibility(View.GONE);
        actionBarContainer.setVisibility(View.GONE);
    }

    private void animateContainerBack() {
        if (containerViewBackAnimator != null) {
            containerViewBackAnimator.cancel();
            containerViewBackAnimator = null;
        }
        applyContainerViewTranslation2 = false;
        float y1 = containerView.getTranslationY1(), y2 = containerView.getTranslationY2(), a = containerView.getAlpha();
        containerViewBackAnimator = ValueAnimator.ofFloat(1, 0);
        containerViewBackAnimator.addUpdateListener(anm -> {
            final float t = (float) anm.getAnimatedValue();
            containerView.setTranslationY(y1 * t);
            containerView.setTranslationY2(y2 * t);
        });
        containerViewBackAnimator.setDuration(340);
        containerViewBackAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        containerViewBackAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                containerViewBackAnimator = null;
                containerView.setTranslationY(0);
                containerView.setTranslationY2(0);
            }
        });
        containerViewBackAnimator.start();
    }

    private void showControls(boolean isVisible, boolean animated) {
        showControls(isVisible, animated, null);
    }

    private void showControls(boolean isVisible, boolean animated, Interpolator interpolator) {
        Interpolator interpolatorShow = interpolator != null ? interpolator : CubicBezierInterpolator.DEFAULT;
        if (isVisible) {
            if (hideControlsAnimator != null) {
                hideControlsAnimator.cancel();
                hideControlsAnimator = null;
            }
            if (animated) {
                if (showControlsAnimator != null) {
                    return;
                }
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(
                        ObjectAnimator.ofFloat(controlContainer, View.ALPHA, 1.0f),
                        ObjectAnimator.ofFloat(navbarContainer, View.ALPHA, 1.0f),
                        ObjectAnimator.ofFloat(actionBarContainer, View.ALPHA, 1.0f),
                        ObjectAnimator.ofFloat(counterTextView, View.ALPHA, 1.0f),
                        ObjectAnimator.ofFloat(cameraRecyclerView, View.ALPHA, 1.0f));
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        controlContainer.setVisibility(View.VISIBLE);
                        navbarContainer.setVisibility(View.VISIBLE);
                        actionBarContainer.setVisibility(View.VISIBLE);
                    }
                });
                animatorSet.setDuration(220);
                animatorSet.setInterpolator(interpolatorShow);
                animatorSet.start();
                showControlsAnimator = animatorSet;
            } else {
                if (showControlsAnimator != null) {
                    showControlsAnimator.cancel();
                    showControlsAnimator = null;
                }
                controlContainer.setVisibility(View.VISIBLE);
                navbarContainer.setVisibility(View.VISIBLE);
                actionBarContainer.setVisibility(View.VISIBLE);
            }
        } else {
            if (showControlsAnimator != null) {
                showControlsAnimator.cancel();
                showControlsAnimator = null;
            }
            if (animated) {
                if (hideControlsAnimator != null) {
                    return;
                }
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(
                        ObjectAnimator.ofFloat(controlContainer, View.ALPHA, 0.0f),
                        ObjectAnimator.ofFloat(navbarContainer, View.ALPHA, 0.0f),
                        ObjectAnimator.ofFloat(actionBarContainer, View.ALPHA, 0.0f),
                        ObjectAnimator.ofFloat(counterTextView, View.ALPHA, 0.0f),
                        ObjectAnimator.ofFloat(cameraRecyclerView, View.ALPHA, 0.0f));
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        controlContainer.setVisibility(View.GONE);
                        navbarContainer.setVisibility(View.GONE);
                        actionBarContainer.setVisibility(View.GONE);
                    }
                });
                animatorSet.setDuration(220);
                animatorSet.setInterpolator(interpolatorShow);
                animatorSet.start();
                hideControlsAnimator = animatorSet;
            } else {
                if (hideControlsAnimator != null) {
                    hideControlsAnimator.cancel();
                    hideControlsAnimator = null;
                    return;
                }
                controlContainer.setVisibility(View.GONE);
                navbarContainer.setVisibility(View.GONE);
                actionBarContainer.setVisibility(View.GONE);
            }
        }
    }

    private Parcelable lastGalleryScrollPosition;
    private MediaController.AlbumEntry lastGallerySelectedAlbum;

    private void createGalleryListView() {
        createGalleryListView(false);
    }

    private void destroyGalleryListView() {
        if (galleryListView == null) {
            return;
        }
        windowView.removeView(galleryListView);
        galleryListView = null;
        if (galleryOpenCloseAnimator != null) {
            galleryOpenCloseAnimator.cancel();
            galleryOpenCloseAnimator = null;
        }
        if (galleryOpenCloseSpringAnimator != null) {
            galleryOpenCloseSpringAnimator.cancel();
            galleryOpenCloseSpringAnimator = null;
        }
        galleryListViewOpening = null;
    }

    private void createGalleryListView(boolean forAddingPart) {
        if (galleryListView != null || getContext() == null) {
            return;
        }

        galleryListView = new GalleryListView(currentAccount, getContext(), resourcesProvider, lastGallerySelectedAlbum, forAddingPart) {
            @Override
            public void setTranslationY(float translationY) {
                super.setTranslationY(translationY);
                if (applyContainerViewTranslation2) {
                    final float amplitude = windowView.getMeasuredHeight() - galleryListView.top();
                    float t = Utilities.clamp(1f - translationY / amplitude, 1, 0);
                    if (t == 0f) {
                        setBackground(null);
                    } else {
                        windowView.setBackgroundColor(0xff000000);
                    }
                    containerView.setTranslationY2(t * dp(-32));
                    containerView.setAlpha(1 - .6f * t);
                    actionBarContainer.setAlpha(1f - t);
                }
            }

            @Override
            public void updateDrafts() {
            }

            @Override
            public void firstLayout() {
                galleryListView.setTranslationY(windowView.getMeasuredHeight() - galleryListView.top());
                if (galleryLayouted != null) {
                    galleryLayouted.run();
                    galleryLayouted = null;
                }
            }

            @Override
            protected void onFullScreen(boolean isFullscreen) {
                if (currentPage == PAGE_CAMERA && isFullscreen) {
//                    AndroidUtilities.runOnUIThread(() -> {
//                        destroyCameraView(true);
//                        removeFromParent(cameraViewThumb);
//                        cameraViewThumb.setVisibility(View.VISIBLE);
//                        cameraViewThumb.setImageDrawable(getCameraThumb());
//                        previewContainer.addView(cameraViewThumb, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//                    });
                }
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getY() < top()) {
                    galleryClosing = true;
                    animateGalleryListView(false);
                    return true;
                }
                return super.dispatchTouchEvent(ev);
            }
        };
        galleryListView.allowSearch(false);
        galleryListView.setOnBackClickListener(() -> {
            animateGalleryListView(false);
            lastGallerySelectedAlbum = null;
        });
        galleryListView.setOnSelectListener((entry, blurredBitmap) -> {
            if (entry == null || galleryListViewOpening != null || scrollingY || !isGalleryOpen()) {
                return;
            }

            if (forAddingPart) {
                if (outputEntry == null) {
                    return;
                }
                outputEntry.editedMedia = true;
                //open entry
                animateGalleryListView(false);
            } else {
                StoryEntry storyEntry;
                showVideoTimer(false, true);
                modeSwitcherView.switchMode(isVideo);
                recordControl.startAsVideo(isVideo);

                animateGalleryListView(false);
                if (entry instanceof MediaController.PhotoEntry) {
                    MediaController.PhotoEntry photoEntry = (MediaController.PhotoEntry) entry;
                    isVideo = photoEntry.isVideo;
                    storyEntry = StoryEntry.fromPhotoEntry(photoEntry);
                    storyEntry.blurredVideoThumb = blurredBitmap;
                    storyEntry.setupMatrix();
                    fromGallery = true;

                    if (collageLayoutView.hasLayout()) {
                        outputFile = null;
                        storyEntry.videoVolume = 1.0f;
                        if (collageLayoutView.push(storyEntry)) {
                            outputEntry = StoryEntry.asCollage(collageLayoutView.getLayout(), collageLayoutView.getContent());
                        }
                        updateActionBarButtons(true);
                    } else {
                        attachDelegate.getAlert().openPhotoViewer((MediaController.PhotoEntry) entry, true, false);
                        outputEntry = storyEntry;
                    }
                } else {
                    return;
                }
            }

            if (galleryListView != null) {
                lastGalleryScrollPosition = galleryListView.layoutManager.onSaveInstanceState();
                lastGallerySelectedAlbum = galleryListView.getSelectedAlbum();
            }
        });
        if (lastGalleryScrollPosition != null) {
            galleryListView.layoutManager.onRestoreInstanceState(lastGalleryScrollPosition);
        }
        windowView.addView(galleryListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));
    }

    private boolean isGalleryOpen() {
        return !scrollingY && galleryListView != null && galleryListView.getTranslationY() < (windowView.getMeasuredHeight() - (int) (AndroidUtilities.displaySize.y * 0.35f) - (AndroidUtilities.statusBarHeight + ActionBar.getCurrentActionBarHeight()));
    }

    private ValueAnimator galleryOpenCloseAnimator;
    private SpringAnimation galleryOpenCloseSpringAnimator;
    private Boolean galleryListViewOpening;
    private Runnable galleryLayouted;

    private void animateGalleryListView(boolean open) {
        wasGalleryOpen = open;
        if (galleryListViewOpening != null && galleryListViewOpening == open) {
            return;
        }

        if (galleryListView == null) {
            if (open) {
                createGalleryListView();
            }
            if (galleryListView == null) {
                return;
            }
        }

        if (galleryListView.firstLayout) {
            galleryLayouted = () -> animateGalleryListView(open);
            return;
        }

        if (galleryOpenCloseAnimator != null) {
            galleryOpenCloseAnimator.cancel();
            galleryOpenCloseAnimator = null;
        }
        if (galleryOpenCloseSpringAnimator != null) {
            galleryOpenCloseSpringAnimator.cancel();
            galleryOpenCloseSpringAnimator = null;
        }

        if (galleryListView == null) {
            if (open) {
                createGalleryListView();
            }
            if (galleryListView == null) {
                return;
            }
        }
        if (galleryListView != null) {
            galleryListView.ignoreScroll = false;
        }

        galleryListViewOpening = open;

        float from = galleryListView.getTranslationY();
        float to = open ? 0 : windowView.getHeight() - galleryListView.top() + AndroidUtilities.navigationBarHeight * 2.5f;

        galleryListView.ignoreScroll = !open;

        applyContainerViewTranslation2 = containerViewBackAnimator == null;
        if (open) {
            windowView.setBackgroundColor(0xff000000);
            galleryOpenCloseSpringAnimator = new SpringAnimation(galleryListView, DynamicAnimation.TRANSLATION_Y, to);
            galleryOpenCloseSpringAnimator.getSpring().setDampingRatio(0.75f);
            galleryOpenCloseSpringAnimator.getSpring().setStiffness(350.0f);
            galleryOpenCloseSpringAnimator.addEndListener((a, canceled, c, d) -> {
                if (canceled) {
                    return;
                }
                galleryListView.setTranslationY(to);
                galleryListView.ignoreScroll = false;
                galleryOpenCloseSpringAnimator = null;
                galleryListViewOpening = null;
            });
            galleryOpenCloseSpringAnimator.start();
        } else {
            galleryOpenCloseAnimator = ValueAnimator.ofFloat(from, to);
            galleryOpenCloseAnimator.addUpdateListener(anm -> {
                galleryListView.setTranslationY((float) anm.getAnimatedValue());
            });
            galleryOpenCloseAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    windowView.setBackground(null);
                    windowView.removeView(galleryListView);
                    galleryListView = null;
                    galleryOpenCloseAnimator = null;
                    galleryListViewOpening = null;
                }
            });
            galleryOpenCloseAnimator.setDuration(450L);
            galleryOpenCloseAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            galleryOpenCloseAnimator.start();
        }

        if (!open) {
            lastGalleryScrollPosition = null;
        }

        if (!open && currentPage == PAGE_CAMERA && !noCameraPermission) {
            createCameraView();
        }
    }

    private void onNavigateStart(int fromPage, int toPage) {
        if (toPage == PAGE_CAMERA) {
            requestCameraPermission(false);
            recordControl.setVisibility(View.VISIBLE);
            if (recordControl != null) {
                recordControl.stopRecordingLoading(false);
            }
            modeSwitcherView.setVisibility(View.VISIBLE);
            zoomControlView.setVisibility(View.VISIBLE);
            zoomControlView.setAlpha(0);
            videoTimerView.setDuration(0, true);

            if (outputEntry != null) {
                outputEntry.destroy(false);
                outputEntry = null;
            }
            if (collageLayoutView != null) {
                collageLayoutView.clear(true);
                recordControl.setCollageProgress(0.0f, false);
            }
        }
        if (fromPage == PAGE_CAMERA) {
            setCameraFlashModeIcon(null, true);
            saveLastCameraBitmap(() -> cameraViewThumb.setImageDrawable(getCameraThumb()));
            cameraHint.hide();
            if (dualHint != null) {
                dualHint.hide();
            }
        }

        cameraViewThumb.setClickable(false);
        if (savedDualHint != null) {
            savedDualHint.hide();
        }
        Bulletin.hideVisible();
    }

    private void onNavigateEnd() {
        destroyCameraView(false);
        recordControl.setVisibility(View.GONE);
        zoomControlView.setVisibility(View.GONE);
        modeSwitcherView.setVisibility(View.GONE);
        animateRecording(false, false);
        setAwakeLock(false);
        cameraViewThumb.setClickable(true);
    }

    private boolean noCameraPermission;

    private final AttachCameraDelegate attachDelegate;

    @SuppressLint("ClickableViewAccessibility")
    public void createCameraView() {
        if (cameraView != null || getContext() == null) {
            return;
        }
        final boolean lazy = !LiteMode.isEnabled(LiteMode.FLAGS_CHAT);
        cameraView = new DualCameraView(getContext(), getCameraFace(), lazy, attachDelegate) {

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (!attachDelegate.isCameraExpanded()) {
                    return false;
                }
                return super.onTouchEvent(event);
            }

            @Override
            public void onEntityDraggedTop(boolean value) {
            }

            @Override
            public void onEntityDraggedBottom(boolean value) {

            }

            @Override
            public void toggleDual() {
                super.toggleDual();
                dualButton.setValue(isDual());
                setCameraFlashModeIcon(getCurrentFlashMode(), true);
            }

            @Override
            protected void onSavedDualCameraSuccess() {
                if (takingVideo || takingPhoto || cameraView == null || currentPage != PAGE_CAMERA) {
                    return;
                }
                savedDualHintRunnable = AttachRecorder.this::onDualSuccessHint;
                dualButton.setValue(isDual());
            }

            @Override
            public boolean isSavedDual() {
                return false;
            }

            @Override
            public void saveDual() {
                //
            }

            @Override
            protected void receivedAmplitude(double amplitude) {
                if (recordControl != null) {
                    recordControl.setAmplitude(Utilities.clamp((float) (amplitude / WaveDrawable.MAX_AMPLITUDE), 1, 0), true);
                }
            }
        };
        if (recordControl != null) {
            recordControl.setAmplitude(0, false);
        }
        cameraView.recordHevc = !collageLayoutView.hasLayout();
        cameraView.setThumbDrawable(attachDelegate.getThumbCamera());
        cameraView.setDelegate(() -> {
            String currentFlashMode = getCurrentFlashMode();
            if (TextUtils.equals(currentFlashMode, getNextFlashMode())) {
                currentFlashMode = null;
            }
            setCameraFlashModeIcon(currentFlashMode, true);
            if (zoomControlView != null) {
                zoomControlView.setZoom(cameraZoom = 0, false);
            }
            updateActionBarButtons(true);
            attachDelegate.onInitCamera();
        });
        setActionBarButtonVisible(dualButton, cameraView.dualAvailable(), true);
        collageButton.setTranslationX(cameraView.dualAvailable() ? 0 : dp(46));

        cameraView.setFocusable(true);
        cameraView.setFpsLimit(30);
        collageLayoutView.setCameraView(cameraView);
        attachDelegate.setCameraView(cameraView);

        dualHintRunnable = this::showDualHint;
    }

    public void showHints() {
        if (dualHintRunnable != null) {
            dualHintRunnable.run();
            dualHintRunnable = null;
        }

        if (savedDualHintRunnable != null) {
            savedDualHintRunnable.run();
            savedDualHintRunnable = null;
        }
    }

    private void onDualSuccessHint() {
        if (MessagesController.getGlobalMainSettings().getInt(SETTINGS_KEY_CAMERA_DUAL_HINT, 0) < 2) {
            AndroidUtilities.runOnUIThread(() -> {
                if (savedDualHint != null && cameraView != null && attachDelegate.isCameraExpanded()) {
                    CharSequence text = cameraView.isFrontface() ? getString(R.string.StoryCameraSavedDualBackHint) : getString(R.string.StoryCameraSavedDualFrontHint);
                    savedDualHint.setMaxWidthPx(HintView2.cutInFancyHalf(text, savedDualHint.getTextPaint()));
                    savedDualHint.setText(text);
                    savedDualHint.show();
                    MessagesController.getGlobalMainSettings().edit().putInt(SETTINGS_KEY_CAMERA_DUAL_HINT, MessagesController.getGlobalMainSettings().getInt(SETTINGS_KEY_CAMERA_DUAL_HINT, 0) + 1).apply();
                }
            }, 340);
        }
    }


    private void showDualHint() {
        if (MessagesController.getGlobalMainSettings().getInt(SETTINGS_KEY_CAMERA_HINT, 0) < 1) {
            cameraHint.show();
            MessagesController.getGlobalMainSettings().edit().putInt(SETTINGS_KEY_CAMERA_HINT, MessagesController.getGlobalMainSettings().getInt(SETTINGS_KEY_CAMERA_HINT, 0) + 1).apply();
        } else if (!cameraView.isSavedDual() && cameraView.dualAvailable() && MessagesController.getGlobalMainSettings().getInt(SETTINGS_KEY_CAMERA_DUAL_HINT, 0) < 2) {
            dualHint.show();
        }
    }

    private int frontfaceFlashMode = -1;
    private ArrayList<String> frontfaceFlashModes;
    private void checkFrontfaceFlashModes() {
        if (frontfaceFlashMode < 0) {
            frontfaceFlashMode = MessagesController.getGlobalMainSettings().getInt("frontflash", 1);
            frontfaceFlashModes = new ArrayList<>();
            frontfaceFlashModes.add(Camera.Parameters.FLASH_MODE_OFF);
            frontfaceFlashModes.add(Camera.Parameters.FLASH_MODE_AUTO);
            frontfaceFlashModes.add(Camera.Parameters.FLASH_MODE_ON);

            flashViews.setWarmth(MessagesController.getGlobalMainSettings().getFloat("frontflash_warmth", .9f));
            flashViews.setIntensity(MessagesController.getGlobalMainSettings().getFloat("frontflash_intensity", 1));
        }
    }
    private void saveFrontFaceFlashMode() {
        if (frontfaceFlashMode >= 0) {
            MessagesController.getGlobalMainSettings().edit()
                .putFloat("frontflash_warmth", flashViews.warmth)
                .putFloat("frontflash_intensity", flashViews.intensity)
                .apply();
        }
    }

    private String getCurrentFlashMode() {
        if (cameraView == null || cameraView.getCameraSession() == null) {
            return null;
        }
        if (cameraView.isFrontface() && !cameraView.getCameraSession().hasFlashModes()) {
            checkFrontfaceFlashModes();
            return frontfaceFlashModes.get(frontfaceFlashMode);
        }
        return cameraView.getCameraSession().getCurrentFlashMode();
    }

    private String getNextFlashMode() {
        if (cameraView == null || cameraView.getCameraSession() == null) {
            return null;
        }
        if (cameraView.isFrontface() && !cameraView.getCameraSession().hasFlashModes()) {
            checkFrontfaceFlashModes();
            return frontfaceFlashModes.get(frontfaceFlashMode + 1 >= frontfaceFlashModes.size() ? 0 : frontfaceFlashMode + 1);
        }
        return cameraView.getCameraSession().getNextFlashMode();
    }

    private void setCurrentFlashMode(String mode) {
        if (cameraView == null || cameraView.getCameraSession() == null) {
            return;
        }
        if (cameraView.isFrontface() && !cameraView.getCameraSession().hasFlashModes()) {
            int index = frontfaceFlashModes.indexOf(mode);
            if (index >= 0) {
                frontfaceFlashMode = index;
                MessagesController.getGlobalMainSettings().edit().putInt("frontflash", frontfaceFlashMode).apply();
            }
            return;
        }
        cameraView.getCameraSession().setCurrentFlashMode(mode);
    }


    private Drawable getCameraThumb() {
        Bitmap bitmap = null;
        try {
            File file = new File(ApplicationLoader.getFilesDirFixed(), "cthumb.jpg");
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Throwable ignore) {}
        if (bitmap != null) {
            return new BitmapDrawable(bitmap);
        } else {
            return ContextCompat.getDrawable(getContext(), R.drawable.icplaceholder);
        }
    }

    private void saveLastCameraBitmap(Runnable whenDone) {
        CameraThumbUtils.saveThumbInQueue(cameraView, whenDone);
    }

    private void destroyCameraView(boolean waitForThumb) {
        if (cameraView != null) {
            if (waitForThumb) {
                saveLastCameraBitmap(() -> {
                    cameraViewThumb.setImageDrawable(getCameraThumb());
                    if (cameraView != null) {
                        cameraView.destroy(true, null);

                        AndroidUtilities.removeFromParent(cameraView);
                        if (collageLayoutView != null) {
                            collageLayoutView.setCameraView(null);
                        }
                        cameraView = null;
                    }
                });
            } else {
                saveLastCameraBitmap(() -> {
                    cameraViewThumb.setImageDrawable(getCameraThumb());
                });
                cameraView.destroy(true, null);
                AndroidUtilities.removeFromParent(cameraView);
                if (collageLayoutView != null) {
                    collageLayoutView.setCameraView(null);
                }
                cameraView = null;
            }
        }
    }

    private boolean requestedCameraPermission;

    private void requestCameraPermission(boolean force) {
        if (requestedCameraPermission && !force) {
            return;
        }
        noCameraPermission = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity != null) {
            noCameraPermission = activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
            if (noCameraPermission) {
                Drawable iconDrawable = Objects.requireNonNull(ContextCompat.getDrawable(getContext(), R.drawable.story_camera)).mutate();
                iconDrawable.setColorFilter(new PorterDuffColorFilter(0x3dffffff, PorterDuff.Mode.MULTIPLY));
                CombinedDrawable drawable = new CombinedDrawable(new ColorDrawable(0xff222222), iconDrawable);
                drawable.setIconSize(dp(64), dp(64));
                cameraViewThumb.setImageDrawable(drawable);
                if (activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    new AlertDialog.Builder(getContext(), resourcesProvider)
                        .setTopAnimation(R.raw.permission_request_camera, AlertsCreator.PERMISSIONS_REQUEST_TOP_ICON_SIZE, false, Theme.getColor(Theme.key_dialogTopBackground))
                        .setMessage(AndroidUtilities.replaceTags(getString(R.string.PermissionNoCameraWithHint)))
                        .setPositiveButton(getString(R.string.PermissionOpenSettings), (dialogInterface, i) -> {
                            try {
                                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + ApplicationLoader.applicationContext.getPackageName()));
                                activity.startActivity(intent);
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                        })
                        .setNegativeButton(getString(R.string.ContactsPermissionAlertNotNow), null)
                        .create()
                        .show();
                    return;
                }
                activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, 111);
                requestedCameraPermission = true;
            }
        }

        if (!noCameraPermission) {
            if (CameraController.getInstance().isCameraInitied()) {
                createCameraView();
            } else {
                CameraController.getInstance().initCamera(this::createCameraView);
            }
        }
    }

    private boolean requestGalleryPermission() {
        if (activity != null) {
            boolean noGalleryPermission = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                noGalleryPermission = (
                    activity.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED
                );
                if (noGalleryPermission) {
                    activity.requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO}, 114);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                noGalleryPermission = activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
                if (noGalleryPermission) {
                    activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 114);
                }
            }
            return !noGalleryPermission;
        }
        return true;
    }

    private void saveCameraFace(boolean frontface) {
        MessagesController.getGlobalMainSettings().edit().putBoolean("attach_camera", frontface).apply();
    }

    private boolean getCameraFace() {
        return MessagesController.getGlobalMainSettings().getBoolean("attach_camera", false);
    }

    public void updateGallery() {
        if (recordControl != null) {
            recordControl.updateGalleryImage();
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.albumsDidLoad) {
            updateGallery();
            if (lastGallerySelectedAlbum != null && MediaController.allMediaAlbums != null) {
                for (int a = 0; a < MediaController.allMediaAlbums.size(); a++) {
                    MediaController.AlbumEntry entry = MediaController.allMediaAlbums.get(a);
                    if (entry.bucketId == lastGallerySelectedAlbum.bucketId && entry.videoOnly == lastGallerySelectedAlbum.videoOnly) {
                        lastGallerySelectedAlbum = entry;
                        break;
                    }
                }
            }
        }
        if (id == NotificationCenter.fileLoaded) {
            final String path = (String) args[0];
            Log.e("COLLAGE", (String) path);
        }
        if (id == NotificationCenter.fileLoadFailed) {
        } else if (id == NotificationCenter.fileLoadProgressChanged) {
        } else if (id == NotificationCenter.filePreparingStarted) {
        } else if (id == NotificationCenter.filePreparingFailed) {
            MessageObject messageObject = (MessageObject) args[0];
            if (collageVideoMessage == messageObject) {
                clearCollage();
                BulletinFactory.of(attachDelegate.getAlert().parentAlert.getContainer(), resourcesProvider)
                        .createSimpleBulletin(R.raw.error, "Error processing collage")
                        .setDuration(DURATION_PROLONG)
                        .show(true);
                collageVideoMessage = null;
            }
        } else if (id == NotificationCenter.fileNewChunkAvailable) {
            MessageObject messageObject = (MessageObject) args[0];
            if (messageObject == collageVideoMessage) {
                String finalPath = (String) args[1];
                long finalSize = (Long) args[3];
                float progress = (float) args[4];

                radialProgressView.setProgress(progress);
                if (finalSize != 0) {
                    clearCollage();
                    attachDelegate.getAlert().onRecordedVideo(collageThumbPath, (long) messageObject.getDuration(), new File(finalPath));
                }
            }
        }
    }

    private void addNotificationObservers() {
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.filePreparingFailed);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.filePreparingStarted);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileNewChunkAvailable);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoadFailed);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoadProgressChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.albumsDidLoad);
    }

    private void removeNotificationObservers() {
        int currentAccount = UserConfig.selectedAccount;
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.filePreparingFailed);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.filePreparingStarted);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileNewChunkAvailable);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoadFailed);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoadProgressChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.albumsDidLoad);
    }

    public interface ClosingViewProvider {
        void preLayout(long dialogId, Runnable runnable);
        SourceView getView(long dialogId);
    }

    public AttachRecorder selectedPeerId(long dialogId) {
        this.selectedDialogId = dialogId;
        return this;
    }

    public static CharSequence cameraBtnSpan(Context context) {
        SpannableString cameraStr = new SpannableString("c");
        Drawable cameraDrawable = Objects.requireNonNull(ContextCompat.getDrawable(context, R.drawable.story_camera)).mutate();
        final int sz = AndroidUtilities.dp(35);
        cameraDrawable.setBounds(-sz / 4, -sz, sz / 4 * 3, 0);
        cameraStr.setSpan(new ImageSpan(cameraDrawable) {
            @Override
            public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
                return super.getSize(paint, text, start, end, fm) / 3 * 2;
            }

            @Override
            public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
                canvas.save();
                canvas.translate(0, (bottom - top) / 2 + dp(1));
                cameraDrawable.setAlpha(paint.getAlpha());
                super.draw(canvas, text, start, end, x, top, y, bottom, paint);
                canvas.restore();
            }
        }, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return cameraStr;
    }

    public ThanosEffect getThanosEffect() {
        if (!ThanosEffect.supports()) {
            return null;
        }
        if (thanosEffect == null) {
            windowView.addView(thanosEffect = new ThanosEffect(getContext(), () -> {
                ThanosEffect thisThanosEffect = thanosEffect;
                if (thisThanosEffect != null) {
                    thanosEffect = null;
                    windowView.removeView(thisThanosEffect);
                }
            }));
        }
        return thanosEffect;
    }

    public void setActionBarButtonVisible(View view, boolean visible, boolean animated) {
        if (view == dualButton || view == collageButton) {
            if (!attachDelegate.getAlert().isChatActivity()) {
                return;
            }
        }
        if (view == null) return;
        if (animated) {
            view.setVisibility(View.VISIBLE);
            view.animate()
                .alpha(visible ? 1.0f : 0.0f)
                .setUpdateListener(animation -> updateActionBarButtonsOffsets())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        updateActionBarButtonsOffsets();
                        if (!visible) {
                            view.setVisibility(View.GONE);
                        }
                    }
                })
                .setDuration(320)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                .start();
        } else {
            view.animate().cancel();
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
            view.setAlpha(visible ? 1.0f : 0.0f);
            updateActionBarButtonsOffsets();
        }
    }

    private boolean inCheck() {
        final float collageProgress = collageLayoutView.hasLayout() ? collageLayoutView.getFilledProgress() : 0.0f;
        return !animatedRecording && collageProgress >= 1.0f;
    }

    private void updateActionBarButtons(boolean animated) {
        showVideoTimer(currentPage == PAGE_CAMERA && isVideo && !collageListView.isVisible() && !inCheck(), animated);
        collageButton.setSelected(collageLayoutView.hasLayout());
        setActionBarButtonVisible(backButton, collageListView == null || !collageListView.isVisible(), animated);
        setActionBarButtonVisible(flashButton, !animatedRecording && currentPage == PAGE_CAMERA && flashButtonMode != null && !collageListView.isVisible() && !inCheck(), animated);
        setActionBarButtonVisible(dualButton, !animatedRecording && currentPage == PAGE_CAMERA && cameraView != null && cameraView.dualAvailable() && !collageListView.isVisible() && !collageLayoutView.hasLayout(), animated);
        setActionBarButtonVisible(collageButton, currentPage == PAGE_CAMERA && !collageListView.isVisible(), animated);
        setActionBarButtonVisible(collageRemoveButton, collageListView.isVisible(), animated);
        final float collageProgress = collageLayoutView.hasLayout() ? collageLayoutView.getFilledProgress() : 0.0f;
        recordControl.setCollageProgress(collageProgress, animated);
        animateRecording(animatedRecording, animated);
    }

    private void updateActionBarButtonsOffsets() {
        float right = 0;
        collageRemoveButton.setTranslationX(-right); right += dp(46) * collageRemoveButton.getAlpha();
        if (attachDelegate.getAlert().isChatActivity()) {
            dualButton.setTranslationX(-right);          right += dp(46) * dualButton.getAlpha();
            collageButton.setTranslationX(-right);       right += dp(46) * collageButton.getAlpha();
        }
        flashButton.setTranslationX(-right);         right += dp(46) * flashButton.getAlpha();

        float left = 0;
        backButton.setTranslationX(left); left += dp(46) * backButton.getAlpha();

        collageListView.setBounds(left + dp(8), right + dp(8));
    }
}

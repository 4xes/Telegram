package org.telegram.ui.Components.Attach;

import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static org.telegram.messenger.AndroidUtilities.dp;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Build;
import android.util.Property;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AnimationNotificationsLocker;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.camera.CameraController;
import org.telegram.messenger.camera.CameraView;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.PhotoAttachCameraCell;
import org.telegram.ui.Cells.PhotoAttachPhotoCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatAttachAlert;
import org.telegram.ui.Components.ChatAttachAlertPhotoLayout;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ShutterButton;
import org.telegram.ui.Components.ZoomControlView;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PhotoViewer;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class AttachCameraView extends FrameLayout {

    protected final Theme.ResourcesProvider resourcesProvider;
    private final DecelerateInterpolator interpolator = new DecelerateInterpolator(1.5f);
    private final boolean needCamera;
    private final Drawable cameraDrawable;
    private final ChatAttachAlertPhotoLayout alert;
    private final TextView recordTime;
    private final ImageView[] flashModeButton = new ImageView[2];
    private final float[] cameraViewLocation = new float[2];
    private final int[] viewPosition = new int[2];
    private final ShutterButton shutterButton;
    private final FrameLayout cameraPanel;
    private final ZoomControlView zoomControlView;
    private final TextView counterTextView;
    private final TextView tooltipTextView;
    private final ImageView switchCameraButton;
    private final ChatAttachAlert parentAlert;
    public CameraRecyclerView cameraPhotoRecyclerView;
    public LinearLayoutManager cameraPhotoLayoutManager;
    public ChatAttachAlertPhotoLayout.PhotoAttachAdapter cameraAttachAdapter;
    public AnimationNotificationsLocker notificationsLocker = new AnimationNotificationsLocker();
    protected FrameLayout cameraIcon;
    boolean cameraExpanded;
    float additionCloseCameraY;
    float animationClipTop;
    float animationClipBottom;
    float animationClipRight;
    float animationClipLeft;
    private boolean isHidden;
    private boolean canSaveCameraPreview;
    private boolean cameraOpened;
    private Boolean isCameraFrontfaceBeforeEnteringEditMode;
    private boolean deviceHasGoodCamera;
    private boolean noCameraPermissions;
    private boolean checkCameraWhenShown;
    private AnimatorSet cameraInitAnimation;
    private CameraView cameraView;
    private float cameraZoom;
    private boolean mediaEnabled;
    private boolean requestingPermissions;
    private boolean flashAnimationInProgress;
    private float currentPanTranslationY;
    private float cameraViewOffsetX;
    private float cameraViewOffsetY;
    private float cameraViewOffsetBottomY;
    private boolean cameraAnimationInProgress;
    @Keep
    private float cameraOpenProgress;
    private int videoRecordTime;
    private Runnable videoRecordRunnable;
    private AnimatorSet zoomControlAnimation;
    private Runnable zoomControlHideRunnable;
    private boolean takingPhoto;
    private boolean cameraPhotoRecyclerViewIgnoreLayout;
    private float pinchStartDistance;
    private boolean zooming;
    private boolean zoomWas;
    private android.graphics.Rect hitRect = new Rect();
    private float lastY;
    private boolean pressed;
    private boolean maybeStartDraging;
    private boolean dragging;

    public AttachCameraView(@NonNull Context context, ChatAttachAlertPhotoLayout alert, Theme.ResourcesProvider resourcesProvider, boolean needCamera) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.needCamera = needCamera;
        this.alert = alert;
        this.parentAlert = alert.parentAlert;

        cameraDrawable = Objects.requireNonNull(ContextCompat.getDrawable(context, R.drawable.instant_camera)).mutate();

        recordTime = new RecordTimeView(context);
        AndroidUtilities.updateViewVisibilityAnimated(recordTime, false, 1f, false);
        getContainer().addView(recordTime, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 16, 0, 0));

        cameraPanel = new FrameLayout(context) {
            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                int cx;
                int cy;
                int cx2;
                int cy2;
                int cx3;
                int cy3;

                if (getMeasuredWidth() == dp(126)) {
                    cx = getMeasuredWidth() / 2;
                    cy = getMeasuredHeight() / 2;
                    cx3 = cx2 = getMeasuredWidth() / 2;
                    cy2 = cy + cy / 2 + dp(17);
                    cy3 = cy / 2 - dp(17);
                } else {
                    cx = getMeasuredWidth() / 2;
                    cy = getMeasuredHeight() / 2 - dp(13);
                    cx2 = cx + cx / 2 + dp(17);
                    cx3 = cx / 2 - dp(17);
                    cy3 = cy2 = getMeasuredHeight() / 2 - dp(13);
                }

                int y = getMeasuredHeight() - tooltipTextView.getMeasuredHeight() - dp(12);
                if (getMeasuredWidth() == dp(126)) {
                    tooltipTextView.layout(cx - tooltipTextView.getMeasuredWidth() / 2, getMeasuredHeight(), cx + tooltipTextView.getMeasuredWidth() / 2, getMeasuredHeight() + tooltipTextView.getMeasuredHeight());
                } else {
                    tooltipTextView.layout(cx - tooltipTextView.getMeasuredWidth() / 2, y, cx + tooltipTextView.getMeasuredWidth() / 2, y + tooltipTextView.getMeasuredHeight());
                }
                shutterButton.layout(cx - shutterButton.getMeasuredWidth() / 2, cy - shutterButton.getMeasuredHeight() / 2, cx + shutterButton.getMeasuredWidth() / 2, cy + shutterButton.getMeasuredHeight() / 2);
                switchCameraButton.layout(cx2 - switchCameraButton.getMeasuredWidth() / 2, cy2 - switchCameraButton.getMeasuredHeight() / 2, cx2 + switchCameraButton.getMeasuredWidth() / 2, cy2 + switchCameraButton.getMeasuredHeight() / 2);
                for (int a = 0; a < 2; a++) {
                    flashModeButton[a].layout(cx3 - flashModeButton[a].getMeasuredWidth() / 2, cy3 - flashModeButton[a].getMeasuredHeight() / 2, cx3 + flashModeButton[a].getMeasuredWidth() / 2, cy3 + flashModeButton[a].getMeasuredHeight() / 2);
                }
            }
        };
        cameraPanel.setVisibility(View.GONE);
        cameraPanel.setAlpha(0.0f);
        getContainer().addView(cameraPanel, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 126, Gravity.LEFT | Gravity.BOTTOM));

        counterTextView = new CounterTextView(context);
        counterTextView.setVisibility(View.GONE);
        getContainer().addView(counterTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 38, Gravity.LEFT | Gravity.TOP, 0, 0, 0, 100 + 16));
        counterTextView.setOnClickListener(v -> {
            if (cameraView == null) {
                return;
            }
            alert.openPhotoViewer(null, false, false);
            stopPreview();
        });

        zoomControlView = new ZoomControlView(context);
        zoomControlView.setVisibility(View.GONE);
        zoomControlView.setAlpha(0.0f);
        getContainer().addView(zoomControlView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 50, Gravity.LEFT | Gravity.TOP, 0, 0, 0, 100 + 16));
        zoomControlView.setDelegate(zoom -> {
            if (cameraView != null) {
                cameraView.setZoom(cameraZoom = zoom);
            }
            showZoomControls(true);
        });

        ChatAttachAlert parentAlert = parentAlert();
        shutterButton = new ShutterButton(context);
        cameraPanel.addView(shutterButton, LayoutHelper.createFrame(84, 84, Gravity.CENTER));
        shutterButton.setDelegate(new ShutterButton.ShutterButtonDelegate() {

            private File outputFile;
            private boolean zoomingWas;

            @Override
            public boolean shutterLongPressed() {
                if (parentAlert.avatarPicker != 2 && !(parentAlert.baseFragment instanceof ChatActivity) || takingPhoto || parentAlert.destroyed || cameraView == null) {
                    return false;
                }
                if (parentAlert.isStickerMode) {
                    return false;
                }
                BaseFragment baseFragment = parentAlert.baseFragment;
                if (baseFragment == null) {
                    baseFragment = LaunchActivity.getLastFragment();
                }
                if (baseFragment == null || baseFragment.getParentActivity() == null) {
                    return false;
                }
                if (!alert.videoEnabled) {
                    BulletinFactory.of(cameraView, resourcesProvider).createErrorBulletin(LocaleController.getString(R.string.GlobalAttachVideoRestricted)).show();
                    return false;
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    if (getContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestingPermissions = true;
                        baseFragment.getParentActivity().requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 21);
                        return false;
                    }
                }
                for (int a = 0; a < 2; a++) {
                    flashModeButton[a].animate().alpha(0f).translationX(dp(30)).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
                }
                switchCameraButton.animate().alpha(0f).translationX(-dp(30)).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
                tooltipTextView.animate().alpha(0f).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
                outputFile = AndroidUtilities.generateVideoPath(parentAlert.baseFragment instanceof ChatActivity && ((ChatActivity) parentAlert.baseFragment).isSecretChat());
                AndroidUtilities.updateViewVisibilityAnimated(recordTime, true);
                recordTime.setText(AndroidUtilities.formatLongDuration(0));
                videoRecordTime = 0;
                videoRecordRunnable = () -> {
                    if (videoRecordRunnable == null) {
                        return;
                    }
                    videoRecordTime++;
                    recordTime.setText(AndroidUtilities.formatLongDuration(videoRecordTime));
                    AndroidUtilities.runOnUIThread(videoRecordRunnable, 1000);
                };
                AndroidUtilities.lockOrientation(baseFragment.getParentActivity());
                CameraController.getInstance().recordVideo(cameraView.getCameraSessionObject(), outputFile, parentAlert.avatarPicker != 0, (thumbPath, duration) -> {
                    if (outputFile == null || parentAlert.destroyed || cameraView == null) {
                        return;
                    }
                    alert.onRecordedVideo(thumbPath, duration, outputFile);
                }, () -> AndroidUtilities.runOnUIThread(videoRecordRunnable, 1000), cameraView);
                shutterButton.setState(ShutterButton.State.RECORDING, true);
                cameraView.runHaptic();
                return true;
            }

            @Override
            public void shutterCancel() {
                if (outputFile != null) {
                    outputFile.delete();
                    outputFile = null;
                }
                resetRecordState();
                CameraController.getInstance().stopVideoRecording(cameraView.getCameraSession(), true);
            }

            @Override
            public void shutterReleased() {
                if (takingPhoto || cameraView == null || cameraView.getCameraSession() == null) {
                    return;
                }
                if (shutterButton.getState() == ShutterButton.State.RECORDING) {
                    resetRecordState();
                    CameraController.getInstance().stopVideoRecording(cameraView.getCameraSession(), false);
                    shutterButton.setState(ShutterButton.State.DEFAULT, true);
                    return;
                }
                if (!alert.photoEnabled) {
                    BulletinFactory.of(cameraView, resourcesProvider).createErrorBulletin(LocaleController.getString(R.string.GlobalAttachPhotoRestricted)).show();
                    return;
                }
                final File cameraFile = AndroidUtilities.generatePicturePath(parentAlert.baseFragment instanceof ChatActivity && ((ChatActivity) parentAlert.baseFragment).isSecretChat(), null);
                final boolean sameTakePictureOrientation = cameraView.getCameraSession().isSameTakePictureOrientation();
                cameraView.getCameraSession().setFlipFront(parentAlert.baseFragment instanceof ChatActivity || parentAlert.avatarPicker == 2);
                takingPhoto = CameraController.getInstance().takePicture(cameraFile, false, cameraView.getCameraSessionObject(), (orientation) -> {
                    takingPhoto = false;
                    if (cameraFile == null || parentAlert.destroyed) {
                        return;
                    }
                    alert.onTakePicture(cameraFile, orientation, sameTakePictureOrientation);
                });
                cameraView.startTakePictureAnimation(true);
            }


            @Override
            public boolean onTranslationChanged(float x, float y) {
                boolean isPortrait = getContainer().getWidth() < getContainer().getHeight();
                float val1 = isPortrait ? x : y;
                float val2 = isPortrait ? y : x;
                if (!zoomingWas && Math.abs(val1) > Math.abs(val2)) {
                    return zoomControlView.getTag() == null;
                }
                if (val2 < 0) {
                    showZoomControls(true);
                    zoomControlView.setZoom(-val2 / dp(200), true);
                    zoomingWas = true;
                    return false;
                }
                if (zoomingWas) {
                    zoomControlView.setZoom(0, true);
                }
                if (x == 0 && y == 0) {
                    zoomingWas = false;
                }
                return !zoomingWas && (x != 0 || y != 0);
            }
        });
        shutterButton.setFocusable(true);
        shutterButton.setContentDescription(LocaleController.getString(R.string.AccDescrShutter));

        switchCameraButton = new ImageView(context);
        switchCameraButton.setScaleType(ImageView.ScaleType.CENTER);
        cameraPanel.addView(switchCameraButton, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        switchCameraButton.setOnClickListener(v -> {
            if (takingPhoto || cameraView == null || !cameraView.isInited()) {
                return;
            }
            canSaveCameraPreview = false;
            cameraView.switchCamera();
            ObjectAnimator animator = ObjectAnimator.ofFloat(switchCameraButton, View.SCALE_X, 0.0f).setDuration(100);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    switchCameraButton.setImageResource(cameraView != null && cameraView.isFrontface() ? R.drawable.camera_revert1 : R.drawable.camera_revert2);
                    ObjectAnimator.ofFloat(switchCameraButton, View.SCALE_X, 1.0f).setDuration(100).start();
                }
            });
            animator.start();

        });
        switchCameraButton.setContentDescription(LocaleController.getString(R.string.AccDescrSwitchCamera));

        for (int a = 0; a < 2; a++) {
            flashModeButton[a] = new ImageView(context);
            flashModeButton[a].setScaleType(ImageView.ScaleType.CENTER);
            flashModeButton[a].setVisibility(View.INVISIBLE);
            cameraPanel.addView(flashModeButton[a], LayoutHelper.createFrame(48, 48, Gravity.LEFT | Gravity.TOP));
            flashModeButton[a].setOnClickListener(currentImage -> {
                if (flashAnimationInProgress || cameraView == null || !cameraView.isInited() || !cameraOpened) {
                    return;
                }
                String current = cameraView.getCameraSession().getCurrentFlashMode();
                String next = cameraView.getCameraSession().getNextFlashMode();
                if (current.equals(next)) {
                    return;
                }
                cameraView.getCameraSession().setCurrentFlashMode(next);
                flashAnimationInProgress = true;
                ImageView nextImage = flashModeButton[0] == currentImage ? flashModeButton[1] : flashModeButton[0];
                nextImage.setVisibility(View.VISIBLE);
                setCameraFlashModeIcon(nextImage, next);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(
                        ObjectAnimator.ofFloat(currentImage, View.TRANSLATION_Y, 0, dp(48)),
                        ObjectAnimator.ofFloat(nextImage, View.TRANSLATION_Y, -dp(48), 0),
                        ObjectAnimator.ofFloat(currentImage, View.ALPHA, 1.0f, 0.0f),
                        ObjectAnimator.ofFloat(nextImage, View.ALPHA, 0.0f, 1.0f));
                animatorSet.setDuration(220);
                animatorSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        flashAnimationInProgress = false;
                        currentImage.setVisibility(View.INVISIBLE);
                        nextImage.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    }
                });
                animatorSet.start();
            });
            flashModeButton[a].setContentDescription("flash mode " + a);
        }

        tooltipTextView = new TextView(context);
        tooltipTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        tooltipTextView.setTextColor(0xffffffff);
        tooltipTextView.setText(LocaleController.getString(R.string.TapForVideo));
        tooltipTextView.setShadowLayer(dp(3.33333f), 0, dp(0.666f), 0x4c000000);
        tooltipTextView.setPadding(dp(6), 0, dp(6), 0);
        cameraPanel.addView(tooltipTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0, 0, 16));

        initPhotoRecycler(context);
    }

    public void initPhotoRecycler(Context context) {
        cameraPhotoRecyclerView = new CameraRecyclerView(context, resourcesProvider, alert, parentAlert) {
            @Override
            public void requestLayout() {
                if (cameraPhotoRecyclerViewIgnoreLayout) {
                    return;
                }
                super.requestLayout();
            }
        };
        cameraPhotoRecyclerView.setAdapter(cameraAttachAdapter = alert.createPhotoAttachAdapter(context));
        cameraAttachAdapter.createCache();
        getContainer().addView(cameraPhotoRecyclerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 80));
        cameraPhotoLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        cameraPhotoRecyclerView.setLayoutManager(cameraPhotoLayoutManager);
        cameraPhotoRecyclerView.setOnItemClickListener((view, position) -> {
            if (view instanceof PhotoAttachPhotoCell) {
                ((PhotoAttachPhotoCell) view).callDelegate();
            }
        });
    }

    public FrameLayout getContainer() {
        return alert.parentAlert.container;
    }

    public boolean isCameraCreated() {
        return cameraView != null;
    }

    public void stopPreview() {
        if (isCameraCreated()) {
            CameraController.getInstance().stopPreview(cameraView.getCameraSessionObject());
        }
    }

    public void startPreview() {
        if (isCameraCreated()) {
            CameraController.getInstance().startPreview(cameraView.getCameraSessionObject());
        }
    }

    public ChatAttachAlert parentAlert() {
        return alert.parentAlert;
    }

    public void pauseCamera(boolean pause) {
        if (needCamera && !noCameraPermissions) {
            if (pause) {
                if (cameraView != null) {
                    isCameraFrontfaceBeforeEnteringEditMode = cameraView.isFrontface();
                    hideCamera(true);
                }
            } else {
                showCamera();
            }
        }
    }

    public void onOpenAnimationEnd() {
        ChatAttachAlert parentAlert = parentAlert();
        checkCamera(parentAlert != null && parentAlert.baseFragment instanceof ChatActivity);
    }

    public void checkCamera(boolean request) {
        ChatAttachAlert parentAlert = parentAlert();
        if (parentAlert.destroyed || !needCamera) {
            return;
        }
        boolean old = deviceHasGoodCamera;
        boolean old2 = noCameraPermissions;
        BaseFragment fragment = parentAlert.baseFragment;
        if (fragment == null) {
            fragment = LaunchActivity.getLastFragment();
        }
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        if (!SharedConfig.inappCamera) {
            deviceHasGoodCamera = false;
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                if (noCameraPermissions = (fragment.getParentActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
                    if (request) {
                        try {
                            parentAlert().baseFragment.getParentActivity().requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 17);
                        } catch (Exception ignore) {

                        }
                    }
                    deviceHasGoodCamera = false;
                } else {
                    if (request || SharedConfig.hasCameraCache) {
                        CameraController.getInstance().initCamera(null);
                    }
                    deviceHasGoodCamera = CameraController.getInstance().isCameraInitied();
                }
            } else {
                if (request || SharedConfig.hasCameraCache) {
                    CameraController.getInstance().initCamera(null);
                }
                deviceHasGoodCamera = CameraController.getInstance().isCameraInitied();
            }
        }
        if ((old != deviceHasGoodCamera || old2 != noCameraPermissions) && alert.adapter != null) {
            alert.adapter.notifyDataSetChanged();
        }
        if (!parentAlert.destroyed && parentAlert.isShowing() && deviceHasGoodCamera && parentAlert.getBackDrawable().getAlpha() != 0 && !cameraOpened) {
            showCamera();
        }
    }

    public void showCamera() {
        ChatAttachAlert parentAlert = parentAlert();
        if (parentAlert.paused || !mediaEnabled) {
            return;
        }
        if (cameraView == null) {
            final boolean lazy = !LiteMode.isEnabled(LiteMode.FLAGS_CHAT);
            cameraView = new CameraView(getContext(), isCameraFrontfaceBeforeEnteringEditMode != null ? isCameraFrontfaceBeforeEnteringEditMode : parentAlert.openWithFrontFaceCamera, lazy) {

                final Bulletin.Delegate bulletinDelegate = new Bulletin.Delegate() {
                    @Override
                    public int getBottomOffset(int tag) {
                        return dp(126) + parentAlert.getBottomInset();
                    }
                };

                @Override
                protected void dispatchDraw(Canvas canvas) {
                    if (AndroidUtilities.makingGlobalBlurBitmap) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 21) {
                        super.dispatchDraw(canvas);
                    } else {
                        int maxY = (int) Math.min(parentAlert.getCommentTextViewTop() + currentPanTranslationY + parentAlert.getContainerView().getTranslationY() - cameraView.getTranslationY() - (parentAlert.mentionContainer != null ? parentAlert.mentionContainer.clipBottom() + dp(8) : 0), alert.getMeasuredHeight());
                        if (cameraAnimationInProgress) {
                            AndroidUtilities.rectTmp.set(animationClipLeft + cameraViewOffsetX * (1f - cameraOpenProgress), animationClipTop + cameraViewOffsetY * (1f - cameraOpenProgress), animationClipRight, Math.min(maxY, animationClipBottom));
                        } else if (!cameraOpened) {
                            AndroidUtilities.rectTmp.set(cameraViewOffsetX, cameraViewOffsetY, alert.getMeasuredWidth(), Math.min(maxY, alert.getMeasuredHeight()));
                        } else {
                            AndroidUtilities.rectTmp.set(0, 0, alert.getMeasuredWidth(), Math.min(maxY, alert.getMeasuredHeight()));
                        }
                        canvas.save();
                        canvas.clipRect(AndroidUtilities.rectTmp);
                        super.dispatchDraw(canvas);
                        canvas.restore();
                    }
                }

                @Override
                protected void onAttachedToWindow() {
                    super.onAttachedToWindow();
                    Bulletin.addDelegate(cameraView, bulletinDelegate);
                }

                @Override
                protected void onDetachedFromWindow() {
                    super.onDetachedFromWindow();
                    Bulletin.removeDelegate(cameraView);
                }
            };

            PhotoAttachCameraCell cameraCell = alert.cameraCell;
            if (cameraCell != null && lazy) {
                cameraView.setThumbDrawable(cameraCell.getDrawable());
            }
            cameraView.setRecordFile(AndroidUtilities.generateVideoPath(parentAlert.baseFragment instanceof ChatActivity && ((ChatActivity) parentAlert.baseFragment).isSecretChat()));
            cameraView.setFocusable(true);
            cameraView.setFpsLimit(30);
            if (Build.VERSION.SDK_INT >= 21) {
                cameraView.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        int maxY = (int) Math.min(parentAlert.getCommentTextViewTop() - (parentAlert.mentionContainer != null ? parentAlert.mentionContainer.clipBottom() + dp(8) : 0) + currentPanTranslationY + parentAlert.getContainerView().getTranslationY() - cameraView.getTranslationY(), view.getMeasuredHeight());
                        if (cameraOpened) {
                            maxY = view.getMeasuredHeight();
                        } else if (cameraAnimationInProgress) {
                            maxY = AndroidUtilities.lerp(maxY, view.getMeasuredHeight(), cameraOpenProgress);
                        }
                        if (cameraAnimationInProgress) {
                            AndroidUtilities.rectTmp.set(animationClipLeft + cameraViewOffsetX * (1f - cameraOpenProgress), animationClipTop + cameraViewOffsetY * (1f - cameraOpenProgress), animationClipRight, animationClipBottom);
                            outline.setRect((int) AndroidUtilities.rectTmp.left, (int) AndroidUtilities.rectTmp.top, (int) AndroidUtilities.rectTmp.right, Math.min(maxY, (int) AndroidUtilities.rectTmp.bottom));
                        } else if (!cameraOpened) {
                            int rad = dp(8 * parentAlert.cornerRadius);
                            outline.setRoundRect((int) cameraViewOffsetX, (int) cameraViewOffsetY, view.getMeasuredWidth() + rad, Math.min(maxY, view.getMeasuredHeight()) + rad, rad);
                        } else {
                            outline.setRect(0, 0, view.getMeasuredWidth(), Math.min(maxY, view.getMeasuredHeight()));
                        }
                    }
                });
                cameraView.setClipToOutline(true);
            }
            cameraView.setContentDescription(LocaleController.getString(R.string.AccDescrInstantCamera));
            parentAlert.getContainer().addView(cameraView, 1, new FrameLayout.LayoutParams(getItemSize(), getItemSize()));
            cameraView.setDelegate(() -> {
                String current = cameraView.getCameraSession().getCurrentFlashMode();
                String next = cameraView.getCameraSession().getNextFlashMode();
                if (current == null || next == null) return;
                if (current.equals(next)) {
                    for (int a = 0; a < 2; a++) {
                        flashModeButton[a].setVisibility(View.INVISIBLE);
                        flashModeButton[a].setAlpha(0.0f);
                        flashModeButton[a].setTranslationY(0.0f);
                    }
                } else {
                    setCameraFlashModeIcon(flashModeButton[0], cameraView.getCameraSession().getCurrentFlashMode());
                    for (int a = 0; a < 2; a++) {
                        flashModeButton[a].setVisibility(a == 0 ? View.VISIBLE : View.INVISIBLE);
                        flashModeButton[a].setAlpha(a == 0 && cameraOpened ? 1.0f : 0.0f);
                        flashModeButton[a].setTranslationY(0.0f);
                    }
                }
                switchCameraButton.setImageResource(cameraView.isFrontface() ? R.drawable.camera_revert1 : R.drawable.camera_revert2);
                switchCameraButton.setVisibility(cameraView.hasFrontFaceCamera() ? View.VISIBLE : View.INVISIBLE);
                if (!cameraOpened) {
                    cameraInitAnimation = new AnimatorSet();
                    cameraInitAnimation.playTogether(
                            ObjectAnimator.ofFloat(cameraView, View.ALPHA, 0.0f, 1.0f),
                            ObjectAnimator.ofFloat(cameraIcon, View.ALPHA, 0.0f, 1.0f));
                    cameraInitAnimation.setDuration(180);
                    cameraInitAnimation.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (animation.equals(cameraInitAnimation)) {
                                canSaveCameraPreview = true;
                                cameraInitAnimation = null;
                                if (!isHidden) {
                                    alert.hidePhotoAttachCameraCell();
                                }
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            cameraInitAnimation = null;
                        }
                    });
                    cameraInitAnimation.start();
                }
            });

            if (cameraIcon == null) {
                cameraIcon = new FrameLayout(getContext()) {
                    @Override
                    protected void onDraw(Canvas canvas) {
                        int maxY = (int) Math.min(parentAlert.getCommentTextViewTop() + currentPanTranslationY + parentAlert.getContainerView().getTranslationY() - cameraView.getTranslationY(), alert.getMeasuredHeight());
                        if (cameraOpened) {
                            maxY = alert.getMeasuredHeight();
                        } else if (cameraAnimationInProgress) {
                            maxY = AndroidUtilities.lerp(maxY, alert.getMeasuredHeight(), cameraOpenProgress);
                        }
                        int w = cameraDrawable.getIntrinsicWidth();
                        int h = cameraDrawable.getIntrinsicHeight();
                        int itemSize = getItemSize();
                        int x = (itemSize - w) / 2;
                        int y = (itemSize - h) / 2;
                        if (cameraViewOffsetY != 0) {
                            y -= cameraViewOffsetY;
                        }
                        boolean clip = maxY < alert.getMeasuredHeight();
                        if (clip) {
                            canvas.save();
                            canvas.clipRect(0, 0, alert.getMeasuredWidth(), maxY);
                        }
                        cameraDrawable.setBounds(x, y, x + w, y + h);
                        cameraDrawable.draw(canvas);
                        if (clip) {
                            canvas.restore();
                        }
                    }
                };
                cameraIcon.setWillNotDraw(false);
                cameraIcon.setClipChildren(true);
            }
            int itemSize = getItemSize();
            parentAlert.getContainer().addView(cameraIcon, 2, new FrameLayout.LayoutParams(itemSize, itemSize));

            cameraView.setAlpha(mediaEnabled ? 1.0f : 0.2f);
            cameraView.setEnabled(mediaEnabled);
            cameraIcon.setAlpha(mediaEnabled ? 1.0f : 0.2f);
            cameraIcon.setEnabled(mediaEnabled);
            if (isHidden) {
                cameraView.setVisibility(GONE);
                cameraIcon.setVisibility(GONE);
            }
            if (cameraOpened) {
                cameraIcon.setAlpha(0f);
            } else {
                checkCameraViewPosition();
            }
            invalidate();
        }
        if (zoomControlView != null) {
            zoomControlView.setZoom(0.0f, false);
            cameraZoom = 0.0f;
        }
        if (!cameraOpened) {
            cameraView.setTranslationX(cameraViewLocation[0]);
            cameraView.setTranslationY(cameraViewLocation[1] + currentPanTranslationY);
            cameraIcon.setTranslationX(cameraViewLocation[0]);
            cameraIcon.setTranslationY(cameraViewLocation[1] + cameraViewOffsetY + currentPanTranslationY);
        }
    }

    public void closeCamera(boolean animated) {
        if (takingPhoto || cameraView == null) {
            return;
        }

        ChatAttachAlert parentAlert = parentAlert();
        if (zoomControlHideRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(zoomControlHideRunnable);
            zoomControlHideRunnable = null;
        }
        setLightNavigationBar();
        if (animated) {
            additionCloseCameraY = cameraView.getTranslationY();

            cameraAnimationInProgress = true;
            ArrayList<Animator> animators = new ArrayList<>();
            animators.add(ObjectAnimator.ofFloat(this, OPEN_PROGRESS, 0.0f));
            animators.add(ObjectAnimator.ofFloat(cameraPanel, View.ALPHA, 0.0f));
            animators.add(ObjectAnimator.ofFloat(zoomControlView, View.ALPHA, 0.0f));
            animators.add(ObjectAnimator.ofFloat(counterTextView, View.ALPHA, 0.0f));
            animators.add(ObjectAnimator.ofFloat(cameraPhotoRecyclerView, View.ALPHA, 0.0f));
            for (int a = 0; a < 2; a++) {
                if (flashModeButton[a].getVisibility() == View.VISIBLE) {
                    animators.add(ObjectAnimator.ofFloat(flashModeButton[a], View.ALPHA, 0.0f));
                    break;
                }
            }

            notificationsLocker.lock();
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animators);
            animatorSet.setDuration(220);
            animatorSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    notificationsLocker.unlock();
                    cameraExpanded = false;
                    parentAlert.getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
                    setCameraOpenProgress(0f);
                    cameraAnimationInProgress = false;
                    if (cameraView != null) {
                        if (Build.VERSION.SDK_INT >= 21) {
                            cameraView.invalidateOutline();
                        } else {
                            cameraView.invalidate();
                        }
                    }
                    cameraOpened = false;

                    if (cameraPanel != null) {
                        cameraPanel.setVisibility(View.GONE);
                    }
                    if (zoomControlView != null) {
                        zoomControlView.setVisibility(View.GONE);
                        zoomControlView.setTag(null);
                    }
                    if (cameraPhotoRecyclerView != null) {
                        cameraPhotoRecyclerView.setVisibility(View.GONE);
                    }
                    if (cameraView != null) {
                        cameraView.setFpsLimit(30);
                        if (Build.VERSION.SDK_INT >= 21) {
                            cameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                        }
                    }
                }
            });
            animatorSet.start();
        } else {
            cameraExpanded = false;
            parentAlert.getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
            setCameraOpenProgress(0f);
            cameraPanel.setAlpha(0);
            cameraPanel.setVisibility(View.GONE);
            zoomControlView.setAlpha(0);
            zoomControlView.setTag(null);
            zoomControlView.setVisibility(View.GONE);
            cameraPhotoRecyclerView.setAlpha(0);
            counterTextView.setAlpha(0);
            cameraPhotoRecyclerView.setVisibility(View.GONE);
            for (int a = 0; a < 2; a++) {
                if (flashModeButton[a].getVisibility() == View.VISIBLE) {
                    flashModeButton[a].setAlpha(0.0f);
                    break;
                }
            }
            cameraOpened = false;
            if (cameraView != null) {
                cameraView.setFpsLimit(30);
                if (Build.VERSION.SDK_INT >= 21) {
                    cameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                }
            }
        }
        if (cameraView != null) {
            cameraView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        }
        if (Build.VERSION.SDK_INT >= 19) {
            RecyclerListView gridView = alert.gridView;
            gridView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        }

        if (!LiteMode.isEnabled(LiteMode.FLAGS_CHAT) && cameraView != null) {
            cameraView.showTexture(false, animated);
        }
    }

    public void openCamera(boolean animated) {
        if (cameraView == null || cameraInitAnimation != null || parentAlert.isDismissed()) {
            return;
        }
        cameraView.initTexture();
        if (alert.shouldLoadAllMedia()) {
            tooltipTextView.setVisibility(VISIBLE);
        } else {
            tooltipTextView.setVisibility(GONE);
        }
        if (ChatAttachAlertPhotoLayout.cameraPhotos.isEmpty()) {
            counterTextView.setVisibility(View.INVISIBLE);
            cameraPhotoRecyclerView.setVisibility(View.GONE);
        } else {
            counterTextView.setVisibility(View.VISIBLE);
            cameraPhotoRecyclerView.setVisibility(View.VISIBLE);
        }
        if (parentAlert.getCommentView().isKeyboardVisible() && isFocusable()) {
            parentAlert.getCommentView().closeKeyboard();
        }
        zoomControlView.setVisibility(View.VISIBLE);
        zoomControlView.setAlpha(0.0f);
        cameraPanel.setVisibility(View.VISIBLE);
        cameraPanel.setTag(null);
        additionCloseCameraY = 0;
        cameraExpanded = true;
        if (cameraView != null) {
            cameraView.setFpsLimit(-1);
        }
        AndroidUtilities.hideKeyboard(this);
        AndroidUtilities.setLightNavigationBar(parentAlert.getWindow(), false);
        parentAlert.getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        if (animated) {
            setCameraOpenProgress(0);
            cameraAnimationInProgress = true;
            notificationsLocker.lock();
            ArrayList<Animator> animators = new ArrayList<>();
            animators.add(ObjectAnimator.ofFloat(this, OPEN_PROGRESS, 0.0f, 1.0f));
            animators.add(ObjectAnimator.ofFloat(cameraPanel, View.ALPHA, 1.0f));
            animators.add(ObjectAnimator.ofFloat(counterTextView, View.ALPHA, 1.0f));
            animators.add(ObjectAnimator.ofFloat(cameraPhotoRecyclerView, View.ALPHA, 1.0f));
            for (int a = 0; a < 2; a++) {
                if (flashModeButton[a].getVisibility() == View.VISIBLE) {
                    animators.add(ObjectAnimator.ofFloat(flashModeButton[a], View.ALPHA, 1.0f));
                    break;
                }
            }
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animators);
            animatorSet.setDuration(350);
            animatorSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    notificationsLocker.unlock();
                    cameraAnimationInProgress = false;
                    if (cameraView != null) {
                        if (Build.VERSION.SDK_INT >= 21) {
                            cameraView.invalidateOutline();
                        } else {
                            cameraView.invalidate();
                        }
                    }
                    if (cameraOpened) {
                        parentAlert.delegate.onCameraOpened();
                    }
                    if (Build.VERSION.SDK_INT >= 21 && cameraView != null) {
                        cameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
                    }
                }
            });
            animatorSet.start();
        } else {
            setCameraOpenProgress(1.0f);
            cameraPanel.setAlpha(1.0f);
            counterTextView.setAlpha(1.0f);
            cameraPhotoRecyclerView.setAlpha(1.0f);
            for (int a = 0; a < 2; a++) {
                if (flashModeButton[a].getVisibility() == View.VISIBLE) {
                    flashModeButton[a].setAlpha(1.0f);
                    break;
                }
            }
            parentAlert.delegate.onCameraOpened();
            if (cameraView != null && Build.VERSION.SDK_INT >= 21) {
                cameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        }
        cameraOpened = true;
        if (cameraView != null) {
            cameraView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        if (Build.VERSION.SDK_INT >= 19) {
            alert.gridView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }

        if (!LiteMode.isEnabled(LiteMode.FLAGS_CHAT) && cameraView != null && cameraView.isInited()) {
            cameraView.showTexture(true, animated);
        }
    }

    public void setLightNavigationBar() {
        ChatAttachAlert parentAlert = parentAlert();
        AndroidUtilities.setLightNavigationBar(parentAlert.getWindow(), AndroidUtilities.computePerceivedBrightness(alert.getThemedColor(Theme.key_windowBackgroundGray)) > 0.721);
    }

    public void setCameraFlashModeIcon(ImageView imageView, String mode) {
        switch (mode) {
            case Camera.Parameters.FLASH_MODE_OFF:
                imageView.setImageResource(R.drawable.flash_off);
                imageView.setContentDescription(LocaleController.getString(R.string.AccDescrCameraFlashOff));
                break;
            case Camera.Parameters.FLASH_MODE_ON:
                imageView.setImageResource(R.drawable.flash_on);
                imageView.setContentDescription(LocaleController.getString(R.string.AccDescrCameraFlashOn));
                break;
            case Camera.Parameters.FLASH_MODE_AUTO:
                imageView.setImageResource(R.drawable.flash_auto);
                imageView.setContentDescription(LocaleController.getString(R.string.AccDescrCameraFlashAuto));
                break;
        }
    }

    public void onResume() {
        ChatAttachAlert parentAlert = parentAlert();
        if (parentAlert.isShowing() && !parentAlert.isDismissed() && !PhotoViewer.getInstance().isVisible()) {
            checkCamera(false);
        }
    }

    public boolean isCameraOpened() {
        return cameraOpened;
    }

    public boolean onDismiss() {
        if (cameraAnimationInProgress) {
            return true;
        }
        if (cameraOpened) {
            closeCamera(true);
            return true;
        }
        hideCamera(true);
        return false;
    }

    public void onPause() {
        if (shutterButton == null) {
            return;
        }
        if (!requestingPermissions) {
            if (cameraView != null && shutterButton.getState() == ShutterButton.State.RECORDING) {
                resetRecordState();
                CameraController.getInstance().stopVideoRecording(cameraView.getCameraSession(), false);
                shutterButton.setState(ShutterButton.State.DEFAULT, true);
            }
            if (cameraOpened) {
                closeCamera(false);
            }
            hideCamera(true);
        } else {
            if (cameraView != null && shutterButton.getState() == ShutterButton.State.RECORDING) {
                shutterButton.setState(ShutterButton.State.DEFAULT, true);
            }
            requestingPermissions = false;
        }
    }

    public void onAttachInit(boolean mediaEnabled) {
        this.mediaEnabled = mediaEnabled;
        if (cameraView != null) {
            cameraView.setAlpha(mediaEnabled ? 1.0f : 0.2f);
            cameraView.setEnabled(mediaEnabled);
        }
        if (cameraIcon != null) {
            cameraIcon.setAlpha(mediaEnabled ? 1.0f : 0.2f);
            cameraIcon.setEnabled(mediaEnabled);
        }
    }

    private void showZoomControls(boolean show) {
        if (zoomControlView.getTag() != null && show || zoomControlView.getTag() == null && !show) {
            if (show) {
                if (zoomControlHideRunnable != null) {
                    AndroidUtilities.cancelRunOnUIThread(zoomControlHideRunnable);
                }
                AndroidUtilities.runOnUIThread(zoomControlHideRunnable = () -> {
                    showZoomControls(false);
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
        zoomControlAnimation.playTogether(ObjectAnimator.ofFloat(zoomControlView, View.ALPHA, show ? 1.0f : 0.0f));
        zoomControlAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                zoomControlAnimation = null;
            }
        });
        zoomControlAnimation.start();
        if (show) {
            AndroidUtilities.runOnUIThread(zoomControlHideRunnable = () -> {
                showZoomControls(false);
                zoomControlHideRunnable = null;
            }, 2000);
        }
    }

    public void resumeCameraPreview() {
        try {
            checkCamera(false);
            if (cameraView != null) {
                startPreview();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void setCheckCameraWhenShown(boolean checkCameraWhenShown) {
        this.checkCameraWhenShown = checkCameraWhenShown;
    }

    public void onShown() {
        isHidden = false;
        if (cameraView != null) {
            cameraView.setVisibility(VISIBLE);
        }
        if (cameraIcon != null) {
            cameraIcon.setVisibility(VISIBLE);
        }
        if (cameraView != null) {
            RecyclerListView gridView = alert.gridView;
            int count = gridView.getChildCount();
            for (int a = 0; a < count; a++) {
                View child = gridView.getChildAt(a);
                if (child instanceof PhotoAttachCameraCell) {
                    child.setVisibility(View.INVISIBLE);
                    break;
                }
            }
        }
        if (checkCameraWhenShown) {
            checkCameraWhenShown = false;
            checkCamera(true);
        }
    }

    public void hideCamera(boolean async) {
        if (!deviceHasGoodCamera || cameraView == null) {
            return;
        }
        saveLastCameraBitmap();
        RecyclerListView gridView = alert.gridView;
        int count = gridView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = gridView.getChildAt(a);
            if (child instanceof PhotoAttachCameraCell) {
                child.setVisibility(View.VISIBLE);
                ((PhotoAttachCameraCell) child).updateBitmap();
                break;
            }
        }
        cameraView.destroy(async, null);
        if (cameraInitAnimation != null) {
            cameraInitAnimation.cancel();
            cameraInitAnimation = null;
        }
        AndroidUtilities.runOnUIThread(() -> {
            ChatAttachAlert parentAlert = parentAlert();
            parentAlert.getContainer().removeView(cameraView);
            parentAlert.getContainer().removeView(cameraIcon);
            cameraView = null;
            cameraIcon = null;
        }, 300);
        canSaveCameraPreview = false;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public boolean canShowCameraCell() {
        return cameraView != null && cameraView.isInited() && !isHidden;
    }

    public void saveLastCameraBitmap() {
        if (!canSaveCameraPreview) {
            return;
        }
        CameraUtils.saveThumb(cameraView);
    }

    public void openPhotoViewer() {
        if (cameraView != null) {
            zoomControlView.setZoom(0.0f, false);
            cameraZoom = 0.0f;
            cameraView.setZoom(0.0f);
            startPreview();
        }
    }

    public void cancelButtonPressedFromPhotoViewer() {
        ChatAttachAlert parentAlert = parentAlert();
        if (cameraOpened && cameraView != null) {
            AndroidUtilities.runOnUIThread(() -> {
                if (cameraView != null && !parentAlert.isDismissed() && Build.VERSION.SDK_INT >= 21) {
                    cameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
                }
            }, 1000);
            zoomControlView.setZoom(0.0f, false);
            cameraZoom = 0.0f;
            cameraView.setZoom(0.0f);
            startPreview();
        }
    }

    public void cancelButtonPressedFromPhotoViewerAndTakingPhotos() {
        counterTextView.setVisibility(View.INVISIBLE);
        cameraPhotoRecyclerView.setVisibility(View.GONE);
    }

    public void resetRecordState() {
        ChatAttachAlert parentAlert = parentAlert();
        if (parentAlert.destroyed) {
            return;
        }

        for (int a = 0; a < 2; a++) {
            flashModeButton[a].animate().alpha(1f).translationX(0).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
        }
        switchCameraButton.animate().alpha(1f).translationX(0).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
        tooltipTextView.animate().alpha(1f).setDuration(150).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
        AndroidUtilities.updateViewVisibilityAnimated(recordTime, false);

        AndroidUtilities.cancelRunOnUIThread(videoRecordRunnable);
        videoRecordRunnable = null;
        AndroidUtilities.unlockOrientation(AndroidUtilities.findActivity(getContext()));
    }

    public void checkColors() {
        if (cameraIcon != null) {
            cameraIcon.invalidate();
        }
        Theme.setDrawableColor(cameraDrawable, alert.getThemedColor(Theme.key_dialogCameraIcon));
    }

    public void needMorePhotos() {
        if (!cameraOpened) {
            openCamera(false);
        }
        counterTextView.setVisibility(View.VISIBLE);
        cameraPhotoRecyclerView.setVisibility(View.VISIBLE);
        counterTextView.setAlpha(1.0f);
    }

    public TextView getCounterTextView() {
        return counterTextView;
    }

    public boolean onCustomMeasure(View view, int width, int height) {
        boolean isPortrait = width < height;
        if (view == cameraIcon) {
            int itemSize = getItemSize();
            cameraIcon.measure(View.MeasureSpec.makeMeasureSpec(itemSize, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec((int) (itemSize - cameraViewOffsetBottomY - cameraViewOffsetY), View.MeasureSpec.EXACTLY));
            return true;
        } else if (view == cameraView) {
            if (cameraOpened && !cameraAnimationInProgress) {
                cameraView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height + parentAlert.getBottomInset(), View.MeasureSpec.EXACTLY));
                return true;
            }
        } else if (view == cameraPanel) {
            if (isPortrait) {
                cameraPanel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(126), View.MeasureSpec.EXACTLY));
            } else {
                cameraPanel.measure(View.MeasureSpec.makeMeasureSpec(dp(126), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            }
            return true;
        } else if (view == zoomControlView) {
            if (isPortrait) {
                zoomControlView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(50), View.MeasureSpec.EXACTLY));
            } else {
                zoomControlView.measure(View.MeasureSpec.makeMeasureSpec(dp(50), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            }
            return true;
        } else if (view == cameraPhotoRecyclerView) {
            cameraPhotoRecyclerViewIgnoreLayout = true;
            if (isPortrait) {
                cameraPhotoRecyclerView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(80), View.MeasureSpec.EXACTLY));
                if (cameraPhotoLayoutManager.getOrientation() != LinearLayoutManager.HORIZONTAL) {
                    cameraPhotoRecyclerView.setPadding(dp(8), 0, dp(8), 0);
                    cameraPhotoLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
                    cameraAttachAdapter.notifyDataSetChanged();
                }
            } else {
                cameraPhotoRecyclerView.measure(View.MeasureSpec.makeMeasureSpec(dp(80), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                if (cameraPhotoLayoutManager.getOrientation() != LinearLayoutManager.VERTICAL) {
                    cameraPhotoRecyclerView.setPadding(0, dp(8), 0, dp(8));
                    cameraPhotoLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                    cameraAttachAdapter.notifyDataSetChanged();
                }
            }
            cameraPhotoRecyclerViewIgnoreLayout = false;
            return true;
        }
        return false;
    }

    public boolean onCustomLayout(View view, int left, int top, int right, int bottom) {
        int width = (right - left);
        int height = (bottom - top);
        boolean isPortrait = width < height;
        if (view == cameraPanel) {
            if (isPortrait) {
                if (cameraPhotoRecyclerView.getVisibility() == View.VISIBLE) {
                    cameraPanel.layout(0, bottom - dp(126 + 96), width, bottom - dp(96));
                } else {
                    cameraPanel.layout(0, bottom - dp(126), width, bottom);
                }
            } else {
                if (cameraPhotoRecyclerView.getVisibility() == View.VISIBLE) {
                    cameraPanel.layout(right - dp(126 + 96), 0, right - dp(96), height);
                } else {
                    cameraPanel.layout(right - dp(126), 0, right, height);
                }
            }
            return true;
        } else if (view == zoomControlView) {
            if (isPortrait) {
                if (cameraPhotoRecyclerView.getVisibility() == View.VISIBLE) {
                    zoomControlView.layout(0, bottom - dp(126 + 96 + 38 + 50), width, bottom - dp(126 + 96 + 38));
                } else {
                    zoomControlView.layout(0, bottom - dp(126 + 50), width, bottom - dp(126));
                }
            } else {
                if (cameraPhotoRecyclerView.getVisibility() == View.VISIBLE) {
                    zoomControlView.layout(right - dp(126 + 96 + 38 + 50), 0, right - dp(126 + 96 + 38), height);
                } else {
                    zoomControlView.layout(right - dp(126 + 50), 0, right - dp(126), height);
                }
            }
            return true;
        } else if (view == counterTextView) {
            int cx;
            int cy;
            if (isPortrait) {
                cx = (width - counterTextView.getMeasuredWidth()) / 2;
                cy = bottom - dp(113 + 16 + 38);
                counterTextView.setRotation(0);
                if (cameraPhotoRecyclerView.getVisibility() == View.VISIBLE) {
                    cy -= dp(96);
                }
            } else {
                cx = right - dp(113 + 16 + 38);
                cy = height / 2 + counterTextView.getMeasuredWidth() / 2;
                counterTextView.setRotation(-90);
                if (cameraPhotoRecyclerView.getVisibility() == View.VISIBLE) {
                    cx -= dp(96);
                }
            }
            counterTextView.layout(cx, cy, cx + counterTextView.getMeasuredWidth(), cy + counterTextView.getMeasuredHeight());
            return true;
        } else if (view == cameraPhotoRecyclerView) {
            if (isPortrait) {
                int cy = height - dp(88);
                view.layout(0, cy, view.getMeasuredWidth(), cy + view.getMeasuredHeight());
            } else {
                int cx = left + width - dp(88);
                view.layout(cx, 0, cx + view.getMeasuredWidth(), view.getMeasuredHeight());
            }
            return true;
        }
        return false;
    }

    public boolean onContainerViewTouchEvent(MotionEvent event) {
        if (cameraAnimationInProgress) {
            return true;
        } else if (cameraOpened) {
            return processTouchEvent(event);
        }
        return false;
    }

    public boolean processTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        if (!pressed && event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            zoomControlView.getHitRect(hitRect);
            if (zoomControlView.getTag() != null && hitRect.contains((int) event.getX(), (int) event.getY())) {
                return false;
            }
            if (!takingPhoto && !dragging) {
                if (event.getPointerCount() == 2) {
                    pinchStartDistance = (float) Math.hypot(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0));
                    zooming = true;
                } else {
                    maybeStartDraging = true;
                    lastY = event.getY();
                    zooming = false;
                }
                zoomWas = false;
                pressed = true;
            }
        } else if (pressed) {
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                if (zooming && event.getPointerCount() == 2 && !dragging) {
                    float newDistance = (float) Math.hypot(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0));
                    if (!zoomWas) {
                        if (Math.abs(newDistance - pinchStartDistance) >= AndroidUtilities.getPixelsInCM(0.4f, false)) {
                            pinchStartDistance = newDistance;
                            zoomWas = true;
                        }
                    } else {
                        if (cameraView != null) {
                            float diff = (newDistance - pinchStartDistance) / dp(100);
                            pinchStartDistance = newDistance;
                            cameraZoom += diff;
                            if (cameraZoom < 0.0f) {
                                cameraZoom = 0.0f;
                            } else if (cameraZoom > 1.0f) {
                                cameraZoom = 1.0f;
                            }
                            zoomControlView.setZoom(cameraZoom, false);
                            parentAlert.getSheetContainer().invalidate();
                            cameraView.setZoom(cameraZoom);
                            showZoomControls(true);
                        }
                    }
                } else {
                    float newY = event.getY();
                    float dy = (newY - lastY);
                    if (maybeStartDraging) {
                        if (Math.abs(dy) > AndroidUtilities.getPixelsInCM(0.4f, false)) {
                            maybeStartDraging = false;
                            dragging = true;
                        }
                    } else if (dragging) {
                        if (cameraView != null) {
                            cameraView.setTranslationY(cameraView.getTranslationY() + dy);
                            lastY = newY;
                            zoomControlView.setTag(null);
                            if (zoomControlHideRunnable != null) {
                                AndroidUtilities.cancelRunOnUIThread(zoomControlHideRunnable);
                                zoomControlHideRunnable = null;
                            }
                            if (cameraPanel.getTag() == null) {
                                cameraPanel.setTag(1);
                                AnimatorSet animatorSet = new AnimatorSet();
                                animatorSet.playTogether(
                                        ObjectAnimator.ofFloat(cameraPanel, View.ALPHA, 0.0f),
                                        ObjectAnimator.ofFloat(zoomControlView, View.ALPHA, 0.0f),
                                        ObjectAnimator.ofFloat(counterTextView, View.ALPHA, 0.0f),
                                        ObjectAnimator.ofFloat(flashModeButton[0], View.ALPHA, 0.0f),
                                        ObjectAnimator.ofFloat(flashModeButton[1], View.ALPHA, 0.0f),
                                        ObjectAnimator.ofFloat(cameraPhotoRecyclerView, View.ALPHA, 0.0f));
                                animatorSet.setDuration(220);
                                animatorSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
                                animatorSet.start();
                            }
                        }
                    }
                }
            } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
                pressed = false;
                zooming = false;
                if (dragging) {
                    dragging = false;
                    if (cameraView != null) {
                        if (Math.abs(cameraView.getTranslationY()) > cameraView.getMeasuredHeight() / 6.0f) {
                            closeCamera(true);
                        } else {
                            AnimatorSet animatorSet = new AnimatorSet();
                            animatorSet.playTogether(
                                    ObjectAnimator.ofFloat(cameraView, View.TRANSLATION_Y, 0.0f),
                                    ObjectAnimator.ofFloat(cameraPanel, View.ALPHA, 1.0f),
                                    ObjectAnimator.ofFloat(counterTextView, View.ALPHA, 1.0f),
                                    ObjectAnimator.ofFloat(flashModeButton[0], View.ALPHA, 1.0f),
                                    ObjectAnimator.ofFloat(flashModeButton[1], View.ALPHA, 1.0f),
                                    ObjectAnimator.ofFloat(cameraPhotoRecyclerView, View.ALPHA, 1.0f));
                            animatorSet.setDuration(250);
                            animatorSet.setInterpolator(interpolator);
                            animatorSet.start();
                            cameraPanel.setTag(null);
                        }
                    }
                } else if (cameraView != null && !zoomWas) {
                    cameraView.getLocationOnScreen(viewPosition);
                    float viewX = event.getRawX() - viewPosition[0];
                    float viewY = event.getRawY() - viewPosition[1];
                    cameraView.focusToPoint((int) viewX, (int) viewY);
                }
            }
        }
        return true;
    }

    @Keep
    public void setCameraOpenProgress(float value) {
        if (cameraView == null) {
            return;
        }
        cameraOpenProgress = value;
        float startWidth = getItemSize();
        float startHeight = getItemSize();
        float endWidth = parentAlert.getContainer().getWidth() - parentAlert.getLeftInset() - parentAlert.getRightInset();
        float endHeight = parentAlert.getContainer().getHeight();

        float fromX = cameraViewLocation[0];
        float fromY = cameraViewLocation[1];
        float toX = 0;
        float toY = additionCloseCameraY;

        if (value == 0) {
            cameraIcon.setTranslationX(cameraViewLocation[0]);
            cameraIcon.setTranslationY(cameraViewLocation[1] + cameraViewOffsetY);
        }


        int cameraViewW, cameraViewH;
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) cameraView.getLayoutParams();

        float textureStartHeight = cameraView.getTextureHeight(startWidth, startHeight);
        float textureEndHeight = cameraView.getTextureHeight(endWidth, endHeight);

        float fromScale = textureStartHeight / textureEndHeight;
        float fromScaleY = startHeight / endHeight;
        float fromScaleX = startWidth / endWidth;

        if (cameraExpanded) {
            cameraViewW = (int) endWidth;
            cameraViewH = (int) endHeight;
            final float s = fromScale * (1f - value) + value;
            cameraView.getTextureView().setScaleX(s);
            cameraView.getTextureView().setScaleY(s);

            final float sX = fromScaleX * (1f - value) + value;
            final float sY = fromScaleY * (1f - value) + value;

            final float scaleOffsetY = (1 - sY) * endHeight / 2;
            final float scaleOffsetX = (1 - sX) * endWidth / 2;

            cameraView.setTranslationX(fromX * (1f - value) + toX * value - scaleOffsetX);
            cameraView.setTranslationY(fromY * (1f - value) + toY * value - scaleOffsetY);
            animationClipTop = fromY * (1f - value) - cameraView.getTranslationY();
            animationClipBottom = ((fromY + startHeight) * (1f - value) - cameraView.getTranslationY()) + endHeight * value;

            animationClipLeft = fromX * (1f - value) - cameraView.getTranslationX();
            animationClipRight = ((fromX + startWidth) * (1f - value) - cameraView.getTranslationX()) + endWidth * value;
        } else {
            cameraViewW = (int) startWidth;
            cameraViewH = (int) startHeight;
            cameraView.getTextureView().setScaleX(1f);
            cameraView.getTextureView().setScaleY(1f);
            animationClipTop = 0;
            animationClipBottom = endHeight;
            animationClipLeft = 0;
            animationClipRight = endWidth;

            cameraView.setTranslationX(fromX);
            cameraView.setTranslationY(fromY);
        }

        if (value <= 0.5f) {
            cameraIcon.setAlpha(1.0f - value / 0.5f);
        } else {
            cameraIcon.setAlpha(0.0f);
        }

        if (layoutParams.width != cameraViewW || layoutParams.height != cameraViewH) {
            layoutParams.width = cameraViewW;
            layoutParams.height = cameraViewH;
            cameraView.requestLayout();
        }
        if (Build.VERSION.SDK_INT >= 21) {
            cameraView.invalidateOutline();
        } else {
            cameraView.invalidate();
        }
    }

    public void checkCameraViewPosition() {
        if (PhotoViewer.hasInstance() && PhotoViewer.getInstance().stickerMakerView != null && PhotoViewer.getInstance().stickerMakerView.isThanosInProgress) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 21) {
            if (cameraView != null) {
                cameraView.invalidateOutline();
            }
        }
        alert.invalidatePhotoCell();
        if (cameraView != null) {
            cameraView.invalidate();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && recordTime != null) {
            MarginLayoutParams params = (MarginLayoutParams) recordTime.getLayoutParams();
            params.topMargin = (alert.getRootWindowInsets() == null ? dp(16) : alert.getRootWindowInsets().getSystemWindowInsetTop() + dp(2));
        }

        if (!deviceHasGoodCamera) {
            return;
        }
        RecyclerListView gridView = alert.gridView;
        int count = gridView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = gridView.getChildAt(a);
            if (child instanceof PhotoAttachCameraCell) {
                if (Build.VERSION.SDK_INT >= 19) {
                    if (!child.isAttachedToWindow()) {
                        break;
                    }
                }

                float topLocal = child.getY() + gridView.getY() + alert.getY();
                float top = topLocal + parentAlert.getSheetContainer().getY();
                float left = child.getX() + gridView.getX() + alert.getX() + parentAlert.getSheetContainer().getX();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    left -= alert.getRootWindowInsets().getSystemWindowInsetLeft();
                }

                float maxY = (Build.VERSION.SDK_INT >= 21 && !parentAlert.inBubbleMode ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() + parentAlert.topCommentContainer.getMeasuredHeight() * parentAlert.topCommentContainer.getAlpha();
                if (parentAlert.mentionContainer != null && parentAlert.mentionContainer.isReversed()) {
                    maxY = Math.max(maxY, parentAlert.mentionContainer.getY() + parentAlert.mentionContainer.clipTop() - parentAlert.currentPanTranslationY);
                }
                float newCameraViewOffsetY;
                if (topLocal < maxY) {
                    newCameraViewOffsetY = maxY - topLocal;
                } else {
                    newCameraViewOffsetY = 0;
                }

                if (newCameraViewOffsetY != cameraViewOffsetY) {
                    cameraViewOffsetY = newCameraViewOffsetY;
                    if (cameraView != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            cameraView.invalidateOutline();
                        } else {
                            cameraView.invalidate();
                        }
                    }
                    if (cameraIcon != null) {
                        cameraIcon.invalidate();
                    }
                }

                int containerHeight = parentAlert.getSheetContainer().getMeasuredHeight();
                maxY = (int) (containerHeight - parentAlert.buttonsRecyclerView.getMeasuredHeight() + parentAlert.buttonsRecyclerView.getTranslationY());
                if (parentAlert.mentionContainer != null) {
                    maxY -= parentAlert.mentionContainer.clipBottom() - dp(6);
                }

                if (topLocal + child.getMeasuredHeight() > maxY) {
                    cameraViewOffsetBottomY = Math.min(-dp(5), topLocal - maxY) + child.getMeasuredHeight();
                } else {
                    cameraViewOffsetBottomY = 0;
                }

                cameraViewLocation[0] = left;
                cameraViewLocation[1] = top;
                applyCameraViewPosition();
                return;
            }
        }


        if (cameraViewOffsetY != 0 || cameraViewOffsetX != 0) {
            cameraViewOffsetX = 0;
            cameraViewOffsetY = 0;
            if (cameraView != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cameraView.invalidateOutline();
                } else {
                    cameraView.invalidate();
                }
            }
            if (cameraIcon != null) {
                cameraIcon.invalidate();
            }
        }

        cameraViewLocation[0] = dp(-400);
        cameraViewLocation[1] = 0;

        applyCameraViewPosition();
    }

    private void applyCameraViewPosition() {
        if (cameraView != null) {
            int itemSize = getItemSize();
            if (!cameraOpened) {
                cameraView.setTranslationX(cameraViewLocation[0]);
                cameraView.setTranslationY(cameraViewLocation[1] + currentPanTranslationY);
            }
            cameraIcon.setTranslationX(cameraViewLocation[0]);
            cameraIcon.setTranslationY(cameraViewLocation[1] + cameraViewOffsetY + currentPanTranslationY);
            int finalWidth = itemSize;
            int finalHeight = itemSize;

            LayoutParams layoutParams;
            if (!cameraOpened) {
                layoutParams = (LayoutParams) cameraView.getLayoutParams();
                if (layoutParams.height != finalHeight || layoutParams.width != finalWidth) {
                    layoutParams.width = finalWidth;
                    layoutParams.height = finalHeight;
                    cameraView.setLayoutParams(layoutParams);
                    final LayoutParams layoutParamsFinal = layoutParams;
                    AndroidUtilities.runOnUIThread(() -> {
                        if (cameraView != null) {
                            cameraView.setLayoutParams(layoutParamsFinal);
                        }
                    });
                }
            }

            finalWidth = (int) (itemSize - cameraViewOffsetX);
            finalHeight = (int) (itemSize - cameraViewOffsetY - cameraViewOffsetBottomY);

            layoutParams = (LayoutParams) cameraIcon.getLayoutParams();
            if (layoutParams.height != finalHeight || layoutParams.width != finalWidth) {
                layoutParams.width = finalWidth;
                layoutParams.height = finalHeight;
                cameraIcon.setLayoutParams(layoutParams);
                final LayoutParams layoutParamsFinal = layoutParams;
                AndroidUtilities.runOnUIThread(() -> {
                    if (cameraIcon != null) {
                        cameraIcon.setLayoutParams(layoutParamsFinal);
                    }
                });
            }
        }
    }

    public void onContainerTranslationUpdated(float currentPanTranslationY) {
        this.currentPanTranslationY = currentPanTranslationY;
        invalidateCameraViewTransition();
    }

    public void invalidateCameraViewTransition() {
        checkCameraViewPosition();
        if (cameraView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraView.invalidateOutline();
            } else {
                cameraView.invalidate();
            }
        }
        if (cameraIcon != null) {
            cameraIcon.invalidate();
        }
    }

    public boolean onSheetKeyDown(int keyCode, KeyEvent event) {
        if (cameraOpened && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)) {
            shutterButton.getDelegate().shutterReleased();
            return true;
        }
        return false;
    }

    public boolean isCameraExpanded() {
        return cameraExpanded;
    }

    public boolean isNoCameraPermissions() {
        return noCameraPermissions;
    }

    public boolean isDeviceHasGoodCamera() {
        return deviceHasGoodCamera;
    }

    public int getItemSize() {
        return alert.getItemSize();
    }

    public float getCameraOpenProgress() {
        return cameraOpenProgress;
    }

    private static final Property<AttachCameraView, Float> OPEN_PROGRESS = new Property<AttachCameraView, Float>(Float.class, "cameraOpenProgress") {
        @Override
        public Float get(AttachCameraView view) {
            return view.getCameraOpenProgress();
        }

        @Override
        public void set(AttachCameraView view, Float value) {
            view.setCameraOpenProgress(value);
        }

    };
}

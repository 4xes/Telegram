package org.telegram.ui.Components.Attach;

import static android.view.View.VISIBLE;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static org.telegram.messenger.AndroidUtilities.dp;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Outline;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.util.Property;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.camera.CameraController;
import org.telegram.messenger.camera.CameraView;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.PhotoAttachCameraCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ChatAttachAlert;
import org.telegram.ui.Components.ChatAttachAlertPhotoLayout;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ZoomControlView;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.Stories.recorder.AttachRecorder;
import org.telegram.ui.Stories.recorder.DualCameraView;

import java.io.File;
import java.util.ArrayList;

public class AttachCameraManager implements AttachCameraDelegate {

    protected final Theme.ResourcesProvider resourcesProvider;
    private final boolean needCamera;
    private final ChatAttachAlertPhotoLayout alert;
    private final float[] cameraViewLocation = new float[2];
    private final ChatAttachAlert parentAlert;
    protected CameraIconView cameraIcon;
    float additionCloseCameraY;
    float animationClipTop;
    float animationClipBottom;
    float animationClipRight;
    float animationClipLeft;
    private boolean isHidden;
    private boolean canSaveCameraPreview;
    private boolean cameraExpanded;
    private boolean deviceHasGoodCamera;
    private boolean noCameraPermissions;
    private boolean checkCameraWhenShown;
    private AnimatorSet cameraInitAnimation;
    private DualCameraView cameraView;
    private View cameraContainerView;
    private boolean mediaEnabled;
    private boolean requestingPermissions;
    private float currentPanTranslationY;
    private float cameraViewOffsetX;
    private float cameraViewOffsetY;
    private float cameraViewOffsetBottomY;
    private boolean cameraAnimationInProgress;
    @Keep
    private float cameraOpenProgress;
    private Runnable zoomControlHideRunnable;
    private boolean cameraPhotoRecyclerViewIgnoreLayout;

    private AttachRecorder attachRecorder;
    private final Context context;

    public AttachCameraManager(@NonNull Context context, ChatAttachAlertPhotoLayout alert, Theme.ResourcesProvider resourcesProvider, boolean needCamera) {
        this.context = context;
        this.resourcesProvider = resourcesProvider;
        this.needCamera = needCamera;
        this.alert = alert;
        this.parentAlert = alert.parentAlert;
        initRecorder();
    }

    public Context getContext() {
        return context;
    }

    public void initRecorder() {
        attachRecorder = new AttachRecorder((Activity) getContext(), UserConfig.selectedAccount, this);
        attachRecorder.setGoneControls();
    }

    @Override
    public int getItemSize() {
        return alert.getItemSize();
    }

    @Override
    public boolean isCameraExpanded() {
        return cameraExpanded;
    }

    @Override
    public boolean isCollapsing() {
        return cameraExpanded && cameraAnimationInProgress;
    }

    @Override
    public void getClipZone(RectF clipRect) {
        int maxY = (int) Math.min(parentAlert.getCommentTextViewTop() + currentPanTranslationY + parentAlert.getContainerView().getTranslationY() - cameraView.getTranslationY() - (parentAlert.mentionContainer != null ? parentAlert.mentionContainer.clipBottom() + dp(8) : 0), alert.getMeasuredHeight());
        if (cameraAnimationInProgress) {
            clipRect.set(animationClipLeft + cameraViewOffsetX * (1f - cameraOpenProgress), animationClipTop + cameraViewOffsetY * (1f - cameraOpenProgress), animationClipRight, Math.min(maxY, animationClipBottom));
        } else if (!cameraExpanded) {
            clipRect.set(cameraViewOffsetX, cameraViewOffsetY, alert.getMeasuredWidth(), Math.min(maxY, alert.getMeasuredHeight()));
        } else {
            clipRect.set(0, 0, alert.getMeasuredWidth(), Math.min(maxY, alert.getMeasuredHeight()));
        }
    }

    @Override
    public int getAlertWidth() {
        return alert.getMeasuredWidth();
    }

    @Override
    public int getAlertHeight() {
        return alert.getMeasuredHeight();
    }

    @Override
    public void onInitCamera() {
        if (!cameraExpanded) {
            cameraInitAnimation = new AnimatorSet();
            cameraInitAnimation.playTogether(
                    ObjectAnimator.ofFloat(cameraView, View.ALPHA, 1.0f),
                    ObjectAnimator.ofFloat(cameraIcon, View.ALPHA, 1.0f));
            cameraInitAnimation.setDuration(180);
            cameraInitAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animation.equals(cameraInitAnimation)) {
                        canSaveCameraPreview = true;
                        cameraInitAnimation = null;
                        if (!isHidden) {
                            alert.hidePhotoCell();
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
    }

    @Override
    public void setCameraView(DualCameraView cameraView) {
        this.cameraView = cameraView;
    }

    @Override
    public Drawable getThumbCamera() {
        final boolean lazy = !LiteMode.isEnabled(LiteMode.FLAGS_CHAT);
        PhotoAttachCameraCell cameraCell = alert.cameraCell;
        if (cameraCell != null && lazy) {
            return cameraCell.getDrawable();
        }
        return null;
    }

    @Override
    public void onClose() {
        closeCamera(true);
    }

    @Override
    public File generateVideoOutput() {
        return AndroidUtilities.generateVideoPath(parentAlert.baseFragment instanceof ChatActivity && ((ChatActivity) parentAlert.baseFragment).isSecretChat());
    }

    @Override
    public File generatePhotoOutput() {
        return AndroidUtilities.generatePicturePath(parentAlert.baseFragment instanceof ChatActivity && ((ChatActivity) parentAlert.baseFragment).isSecretChat(), null);
    }

    @Override
    public ChatAttachAlertPhotoLayout getAlert() {
        return alert;
    }


    public FrameLayout getContainer() {
        return alert.parentAlert.container;
    }

    public boolean isCameraCreated() {
        return cameraView != null;
    }

    @Override
    public void stopCameraPreview() {
        if (isCameraCreated()) {
            CameraController.getInstance().stopPreview(cameraView.getCameraSessionObject());
        }
    }

    public void startPreview() {
        if (isCameraCreated()) {
            CameraController.getInstance().startPreview(cameraView.getCameraSessionObject());
        }
    }

    public ChatAttachAlert getParentAlert() {
        return alert.parentAlert;
    }

    public void pauseCamera(boolean pause) {
        if (needCamera && !noCameraPermissions) {
            if (pause) {
                if (cameraView != null) {
                    //isCameraFrontfaceBeforeEnteringEditMode = collageLayoutView2.isFrontface();
                    hideCamera(true);
                }
            } else {
                showCamera();
            }
        }
    }

    public void onOpenAnimationEnd() {
        ChatAttachAlert parentAlert = getParentAlert();
        checkCameraPermissionAndShow(parentAlert != null && parentAlert.baseFragment instanceof ChatActivity);
    }

    public void checkCameraPermissionAndShow(boolean request) {
        ChatAttachAlert parentAlert = getParentAlert();
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
                            getParentAlert().baseFragment.getParentActivity().requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 17);
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
        if (!parentAlert.destroyed && parentAlert.isShowing() && deviceHasGoodCamera && parentAlert.getBackDrawable().getAlpha() != 0 && !cameraExpanded) {
            showCamera();
        }
    }

    public void showCamera() {
        ChatAttachAlert parentAlert = getParentAlert();
        if (parentAlert.paused || !mediaEnabled) {
            return;
        }
        if (cameraView == null) {
            initRecorder();
            attachRecorder.createCameraView();
            cameraContainerView = attachRecorder.getCameraContainer();

            if (Build.VERSION.SDK_INT >= 21) {
                cameraContainerView.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        int maxY = (int) Math.min(parentAlert.getCommentTextViewTop() - (parentAlert.mentionContainer != null ? parentAlert.mentionContainer.clipBottom() + dp(8) : 0) + currentPanTranslationY + parentAlert.getContainerView().getTranslationY() - cameraView.getTranslationY(), view.getMeasuredHeight());
                        if (cameraExpanded) {
                            maxY = view.getMeasuredHeight();
                        } else if (cameraAnimationInProgress) {
                            maxY = AndroidUtilities.lerp(maxY, view.getMeasuredHeight(), cameraOpenProgress);
                        }
                        if (cameraAnimationInProgress) {
                            AndroidUtilities.rectTmp.set(animationClipLeft + cameraViewOffsetX * (1f - cameraOpenProgress), animationClipTop + cameraViewOffsetY * (1f - cameraOpenProgress), animationClipRight, animationClipBottom);
                            outline.setRect((int) AndroidUtilities.rectTmp.left, (int) AndroidUtilities.rectTmp.top, (int) AndroidUtilities.rectTmp.right, Math.min(maxY, (int) AndroidUtilities.rectTmp.bottom));
                        } else if (!cameraExpanded) {
                            int rad = dp(8 * parentAlert.cornerRadius);
                            outline.setRoundRect((int) cameraViewOffsetX, (int) cameraViewOffsetY, view.getMeasuredWidth() + rad, Math.min(maxY, view.getMeasuredHeight()) + rad, rad);
                        } else {
                            outline.setRect(0, 0, view.getMeasuredWidth(), Math.min(maxY, view.getMeasuredHeight()));
                        }
                    }
                });
                cameraContainerView.setClipToOutline(true);
            }

            int itemSize = getItemSize();
            setSize(cameraContainerView, getItemSize(), getItemSize());

            float endWidth = parentAlert.getContainer().getWidth() - parentAlert.getLeftInset() - parentAlert.getRightInset();
            float endHeight = parentAlert.getContainer().getHeight();
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams((int) endWidth, (int) endHeight);
            getContainer().addView(attachRecorder.getWindowView(),1, layoutParams);

            createCameraIcon(parentAlert);
            getContainer().addView(cameraIcon, 2, new FrameLayout.LayoutParams(itemSize, itemSize));

            updateMedia();

            if (isHidden) {
                cameraContainerView.setVisibility(View.GONE);
                cameraIcon.setVisibility(View.GONE);
            }
            if (cameraExpanded) {
                cameraIcon.setAlpha(0f);
            } else {
                checkCameraViewPosition();
            }
        }
        setZoom(0f);
        if (!cameraExpanded) {
            cameraContainerView.setTranslationX(cameraViewLocation[0]);
            cameraContainerView.setTranslationY(cameraViewLocation[1] + currentPanTranslationY);
            cameraIcon.setTranslationX(cameraViewLocation[0]);
            cameraIcon.setTranslationY(cameraViewLocation[1] + cameraViewOffsetY + currentPanTranslationY);
        }
    }

    private void createCameraIcon(ChatAttachAlert parentAlert) {
        if (cameraIcon == null) {
            cameraIcon = new CameraIconView(getContext(), this, resourcesProvider) {
                @Override
                int maxY() {
                    int maxY = (int) Math.min(parentAlert.getCommentTextViewTop() + currentPanTranslationY + parentAlert.getContainerView().getTranslationY() - cameraView.getTranslationY(), alert.getMeasuredHeight());
                    if (cameraExpanded) {
                        maxY = alert.getMeasuredHeight();
                    } else if (cameraAnimationInProgress) {
                        maxY = AndroidUtilities.lerp(maxY, alert.getMeasuredHeight(), cameraOpenProgress);
                    }
                    return maxY;
                }

                @Override
                int offsetY() {
                    return (int) cameraViewOffsetY;
                }
            };
        }
    }

    public void closeCamera(boolean animated) {
        if (cameraView == null) {
            return;
        }

        ChatAttachAlert parentAlert = getParentAlert();
        if (zoomControlHideRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(zoomControlHideRunnable);
            zoomControlHideRunnable = null;
        }
        setLightNavigationBar(true);
        attachRecorder.onClose(animated);
        if (animated) {
            additionCloseCameraY = cameraView.getTranslationY();

            cameraAnimationInProgress = true;
            ArrayList<Animator> animators = new ArrayList<>();
            animators.add(ObjectAnimator.ofFloat(this, OPEN_PROGRESS, 0.0f));
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animators);
            animatorSet.setDuration(220);
            animatorSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    cameraExpanded = false;
                    parentAlert.getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
                    setCameraOpenProgress(0f);
                    cameraAnimationInProgress = false;
                    cameraInvalidate();
                    disableFullscreen();
                }
            });
            animatorSet.start();
        } else {
            cameraExpanded = false;
            parentAlert.getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
            setCameraOpenProgress(0f);
            if (cameraView != null) {
                cameraView.setFpsLimit(30);
                disableFullscreen();
            }
        }
        if (cameraView != null) {
            cameraView.setFpsLimit(30);
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
        attachRecorder.checkVisibleViewer();
        if (parentAlert.getCommentView().isKeyboardVisible() && cameraView.isFocusable()) {
            parentAlert.getCommentView().closeKeyboard();
        }
        additionCloseCameraY = 0;
        cameraExpanded = true;
        if (cameraView != null) {
            cameraView.setFpsLimit(-1);
        }
        AndroidUtilities.hideKeyboard(cameraView);
        setLightNavigationBar(false);
        AndroidUtilities.setLightNavigationBar(parentAlert.getWindow(), false);
        parentAlert.getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        attachRecorder.onOpen(animated);
        if (animated) {
            setCameraOpenProgress(0);
            cameraAnimationInProgress = true;
            ArrayList<Animator> animators = new ArrayList<>();
            animators.add(ObjectAnimator.ofFloat(this, OPEN_PROGRESS, 0.0f, 1.0f));
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animators);
            animatorSet.setDuration(350);
            animatorSet.setInterpolator(CubicBezierInterpolator.DEFAULT);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    cameraAnimationInProgress = false;
                    cameraInvalidate();
                    if (cameraExpanded) {
                        attachRecorder.showHints();
                        parentAlert.delegate.onCameraOpened();
                    }
                    enableFullscreen();
                }
            });
            animatorSet.start();
        } else {
            setCameraOpenProgress(1.0f);
            parentAlert.delegate.onCameraOpened();
            enableFullscreen();
        }
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


    public void setLightNavigationBar(boolean isEnable) {
        if (isEnable) {
            ChatAttachAlert parentAlert = getParentAlert();
            boolean isLight = AndroidUtilities.computePerceivedBrightness(alert.getThemedColor(Theme.key_windowBackgroundGray)) > 0.721;
            AndroidUtilities.setLightNavigationBar(parentAlert.getWindow(), isLight);
        } else {
            AndroidUtilities.setLightNavigationBar(parentAlert.getWindow(), false);
        }
    }

    public void onResume() {
        ChatAttachAlert parentAlert = getParentAlert();
        if (parentAlert.isShowing() && !parentAlert.isDismissed() && !PhotoViewer.getInstance().isVisible()) {
            checkCameraPermissionAndShow(false);
        }
        attachRecorder.updateGallery();
    }

    private void enableFullscreen() {
        if (Build.VERSION.SDK_INT >= 21 && cameraView != null) {
            attachRecorder.getWindowView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    private void disableFullscreen() {
        if (Build.VERSION.SDK_INT >= 21 && cameraView != null) {
            attachRecorder.getWindowView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    public boolean onDismiss() {
        if (cameraAnimationInProgress) {
            return true;
        }
        if (cameraExpanded) {
            closeCamera(true);
            return true;
        }
        hideCamera(true);
        return false;
    }

    public void onPause() {
        if (!requestingPermissions) {
//            if (cameraContainerView != null && shutterButton.getState() == ShutterButton.State.RECORDING) {
//                resetRecordState();
//                CameraController.getInstance().stopVideoRecording(cameraContainerView.getCameraSession(), false);
//                shutterButton.setState(ShutterButton.State.DEFAULT, true);
//            }
            if (cameraExpanded) {
                closeCamera(false);
            }
            hideCamera(true);
        } else {
//            if (cameraContainerView != null && shutterButton.getState() == ShutterButton.State.RECORDING) {
//                shutterButton.setState(ShutterButton.State.DEFAULT, true);
//            }
            requestingPermissions = false;
        }
    }

    public void onAttachInit(boolean mediaEnabled) {
        this.mediaEnabled = mediaEnabled;
        updateMedia();
    }

    private void updateMedia() {
        if (cameraView != null) {
            cameraContainerView.setAlpha(mediaEnabled ? 1.0f : 0.2f);
            cameraContainerView.setEnabled(mediaEnabled);
        }
        if (cameraIcon != null) {
            cameraIcon.setAlpha(mediaEnabled ? 1.0f : 0.2f);
            cameraIcon.setEnabled(mediaEnabled);
        }
    }


    public void resumeCameraPreview() {
        try {
            checkCameraPermissionAndShow(false);
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
            cameraContainerView.setVisibility(VISIBLE);
        }
        if (cameraIcon != null) {
            cameraIcon.setVisibility(VISIBLE);
        }
        if (cameraView != null) {
            alert.hidePhotoCell();
        }
        if (checkCameraWhenShown) {
            checkCameraWhenShown = false;
            checkCameraPermissionAndShow(true);
        }
    }

    public void hideCamera(boolean async) {
        if (!deviceHasGoodCamera || cameraView == null) {
            return;
        }
        saveLastCameraBitmap();
        alert.showAndUpdatePhotoCell();
        if (cameraView != null) {
            cameraView.destroy(async, null);
        }
        if (cameraInitAnimation != null) {
            cameraInitAnimation.cancel();
            cameraInitAnimation = null;
        }
        AndroidUtilities.runOnUIThread(() -> {
            getContainer().removeView(attachRecorder.getWindowView());
            getContainer().removeView(cameraIcon);
            cameraView = null;
            cameraIcon = null;
        }, 300);
        canSaveCameraPreview = false;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
        saveLastCameraBitmap();
        alert.showAndUpdatePhotoCell();
        stopCameraPreview();
    }

    public boolean cameraIsNotShowed() {
        return cameraView != null && cameraView.isInited() && !isHidden;
    }

    public void saveLastCameraBitmap() {
        if (!canSaveCameraPreview) {
            return;
        }
        if (isCameraCreated()) {
            CameraThumbUtils.saveThumb(cameraView);
        }
    }

    public void openPhotoViewer() {
        if (cameraView != null) {
            setZoom(0f);
            startPreview();
        }
    }

    private void setZoom(float zoom) {
        if (cameraView != null && getZoomControlView() != null) {
            getZoomControlView().setZoom(zoom, false);
        }
    }

    private ZoomControlView getZoomControlView() {
        if (attachRecorder != null) {
            return attachRecorder.getZoomControlView();
        }
        return null;
    }

    //When back on PhotoViewer
    public void restoreCamera() {
        ChatAttachAlert parentAlert = getParentAlert();
        if (cameraExpanded && cameraView != null) {
            AndroidUtilities.runOnUIThread(() -> {
                if (!parentAlert.isDismissed()) {
                    enableFullscreen();
                }
            }, 1000);
            setZoom(0f);
            if (cameraView.isDual()) {
                cameraView.resetCamera();
            }
            startPreview();
        }
    }

    public void checkColors() {
        if (cameraIcon != null) {
            cameraIcon.updateThemeColors();
        }
    }

    public void needMorePhotos() {
        if (!cameraExpanded) {
            openCamera(false);
        }
        attachRecorder.setVisibleViewer(true);
    }


    public AttachRecorder getAttachRecorder() {
        return attachRecorder;
    }

    public CameraView getCameraView() {
        return cameraView;
    }

    public View getCameraViewContainer() {
        return cameraContainerView;
    }

    public CameraIconView getCameraIcon() {
        return cameraIcon;
    }

    public boolean onCustomMeasure(View view, int width, int height) {
        boolean isPortrait = width < height;
        if (view == cameraIcon) {
            int itemSize = getItemSize();
            cameraIcon.measure(View.MeasureSpec.makeMeasureSpec(itemSize, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec((int) (itemSize - cameraViewOffsetBottomY - cameraViewOffsetY), View.MeasureSpec.EXACTLY));
            return true;
        } else if (view == attachRecorder.getWindowView()) {
            if (cameraExpanded && !cameraAnimationInProgress) {
                attachRecorder.getWindowView().measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height + parentAlert.getBottomInset(), View.MeasureSpec.EXACTLY));
                cameraContainerView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height + parentAlert.getBottomInset(), View.MeasureSpec.EXACTLY));
                return true;
            }
        }
//        } else if (view == attachRecorder.getWindowView()) {
//            if (cameraExpanded && !cameraAnimationInProgress) {
//                attachRecorder.getWindowView().measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height + parentAlert.getBottomInset(), View.MeasureSpec.EXACTLY));
//                return true;
//            }
//        } else if (view == cameraPhotoRecyclerView) {
//            cameraPhotoRecyclerViewIgnoreLayout = true;
//            if (isPortrait) {
//                cameraPhotoRecyclerView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(80), View.MeasureSpec.EXACTLY));
//                if (cameraPhotoLayoutManager.getOrientation() != LinearLayoutManager.HORIZONTAL) {
//                    cameraPhotoRecyclerView.setPadding(dp(8), 0, dp(8), 0);
//                    cameraPhotoLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
//                    cameraAttachAdapter.notifyDataSetChanged();
//                }
//            } else {
//                cameraPhotoRecyclerView.measure(View.MeasureSpec.makeMeasureSpec(dp(80), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
//                if (cameraPhotoLayoutManager.getOrientation() != LinearLayoutManager.VERTICAL) {
//                    cameraPhotoRecyclerView.setPadding(0, dp(8), 0, dp(8));
//                    cameraPhotoLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
//                    cameraAttachAdapter.notifyDataSetChanged();
//                }
//            }
//            cameraPhotoRecyclerViewIgnoreLayout = false;
//            return true;
//        }
        return false;
    }

    public boolean onCustomLayout(View view, int left, int top, int right, int bottom) {
        int width = (right - left);
        int height = (bottom - top);
        boolean isPortrait = width < height;

        if (view == attachRecorder.getWindowView()) {
            attachRecorder.getWindowView().layout(left, top, right, bottom + parentAlert.getBottomInset());
            if (cameraExpanded && !cameraAnimationInProgress) {
                cameraContainerView.layout(left - parentAlert.getLeftInset(), top, right + parentAlert.getRightInset(), bottom + parentAlert.getBottomInset());
            }
            return true;
        }
//        if (view == counterTextView) {
//            int cx;
//            int cy;
//            if (isPortrait) {
//                cx = (width - counterTextView.getMeasuredWidth()) / 2;
//                cy = bottom - dp(113 + 16 + 38);
//                counterTextView.setRotation(0);
//                if (cameraPhotoRecyclerView.getVisibility() == VISIBLE) {
//                    cy -= dp(96);
//                }
//            } else {
//                cx = right - dp(113 + 16 + 38);
//                cy = height / 2 + counterTextView.getMeasuredWidth() / 2;
//                counterTextView.setRotation(-90);
//                if (cameraPhotoRecyclerView.getVisibility() == VISIBLE) {
//                    cx -= dp(96);
//                }
//            }
//            counterTextView.layout(cx, cy, cx + counterTextView.getMeasuredWidth(), cy + counterTextView.getMeasuredHeight());
//            return true;
//        } else if (view == cameraPhotoRecyclerView) {
//            if (isPortrait) {
//                int cy = height - dp(88);
//                view.layout(0, cy, view.getMeasuredWidth(), cy + view.getMeasuredHeight());
//            } else {
//                int cx = left + width - dp(88);
//                view.layout(cx, 0, cx + view.getMeasuredWidth(), view.getMeasuredHeight());
//            }
//            return true;
//        }
        return false;
    }

    public boolean onContainerViewTouchEvent(MotionEvent event) {
        if (cameraAnimationInProgress) {
            return true;
        } else if (cameraExpanded) {
            return false;
        }
        return false;
    }

    @Keep
    public void setCameraOpenProgress(float value) {
        if (cameraView == null) {
            return;
        }
        cameraOpenProgress = value;
        //attachRecorder.setProgress(cameraOpenProgress);
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

            cameraContainerView.setTranslationX(fromX * (1f - value) + toX * value - scaleOffsetX);
            cameraContainerView.setTranslationY(fromY * (1f - value) + toY * value - scaleOffsetY);
            animationClipTop = fromY * (1f - value) - cameraContainerView.getTranslationY();
            animationClipBottom = ((fromY + startHeight) * (1f - value) - cameraContainerView.getTranslationY()) + endHeight * value;
            animationClipLeft = fromX * (1f - value) - cameraContainerView.getTranslationX();
            animationClipRight = ((fromX + startWidth) * (1f - value) - cameraContainerView.getTranslationX()) + endWidth * value;
        } else {
            cameraViewW = (int) startWidth;
            cameraViewH = (int) startHeight;
            cameraView.getTextureView().setScaleX(1f);
            cameraView.getTextureView().setScaleY(1f);
            animationClipTop = 0;
            animationClipBottom = endHeight;
            animationClipLeft = 0;
            animationClipRight = endWidth;

            cameraContainerView.setTranslationX(fromX);
            cameraContainerView.setTranslationY(fromY);
        }

        if (value <= 0.5f) {
            cameraIcon.setAlpha(1.0f - value / 0.5f);
        } else {
            cameraIcon.setAlpha(0.0f);
        }

        setSize(cameraContainerView, cameraViewW, cameraViewH);
        cameraInvalidate();
    }

    public void checkCameraViewPosition() {
        if (PhotoViewer.hasInstance() && PhotoViewer.getInstance().stickerMakerView != null && PhotoViewer.getInstance().stickerMakerView.isThanosInProgress) {
            return;
        }

        alert.invalidatePhotoCell();

        if (!deviceHasGoodCamera) {
            return;
        }
        RecyclerListView gridView = alert.gridView;
        int count = gridView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = gridView.getChildAt(a);
            if (child instanceof PhotoAttachCameraCell) {
                if (!child.isAttachedToWindow()) {
                    break;
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
                    cameraInvalidate();
                    cameraIconInvalidate();
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
        }

        cameraViewLocation[0] = dp(-400);
        cameraViewLocation[1] = 0;

        applyCameraViewPosition();
    }

    private void applyCameraViewPosition() {
        if (cameraView != null) {
            if (!cameraExpanded) {
                cameraContainerView.setTranslationX(cameraViewLocation[0]);
                cameraContainerView.setTranslationY(cameraViewLocation[1] + currentPanTranslationY);
            }
            cameraIcon.setTranslationX(cameraViewLocation[0]);
            cameraIcon.setTranslationY(cameraViewLocation[1] + cameraViewOffsetY + currentPanTranslationY);

            int itemSize = getItemSize();
            if (!cameraExpanded) {
                setSize(cameraContainerView, itemSize, itemSize);
            }
            final int iconWidth = (int) (itemSize - cameraViewOffsetX);
            final int iconHeight = (int) (itemSize - cameraViewOffsetY - cameraViewOffsetBottomY);
            setSize(cameraIcon, iconWidth, iconHeight);
        }
    }

    private void setSize(final View view, final int width, final int height) {
        FrameLayout.LayoutParams layoutParams; layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        if (layoutParams.height != height || layoutParams.width != width) {
            layoutParams.width = width;
            layoutParams.height = height;
            if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                view.setLayoutParams(layoutParams);
            } else {
                AndroidUtilities.runOnUIThread(() -> view.setLayoutParams(layoutParams));
            }
        }
    }

    public void onContainerTranslationUpdated(float currentPanTranslationY) {
        this.currentPanTranslationY = currentPanTranslationY;
        invalidateCameraViewTransition();
    }

    public void invalidateCameraViewTransition() {
        checkCameraViewPosition();
        cameraInvalidate();
        cameraIconInvalidate();
    }

    private void cameraInvalidate() {
        if (cameraView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraContainerView.invalidateOutline();
            }
            cameraContainerView.invalidate();
        }
        if (cameraView != null) {
            cameraView.invalidate();
        }
    }

    private void cameraIconInvalidate() {
        if (cameraIcon != null) {
            cameraIcon.invalidate();
        }
    }

    public boolean onSheetKeyDown(int keyCode, KeyEvent event) {
        if (attachRecorder.getWindowView().dispatchKeyEventPreIme(event)) {
            return true;
        }
        return cameraExpanded && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
    }

    public boolean isNoCameraPermissions() {
        return noCameraPermissions;
    }

    public boolean isDeviceHasGoodCamera() {
        return deviceHasGoodCamera;
    }

    public float getCameraOpenProgress() {
        return cameraOpenProgress;
    }

    private static final Property<AttachCameraManager, Float> OPEN_PROGRESS = new Property<AttachCameraManager, Float>(Float.class, "cameraOpenProgress") {
        @Override
        public Float get(AttachCameraManager view) {
            return view.getCameraOpenProgress();
        }

        @Override
        public void set(AttachCameraManager view, Float value) {
            view.setCameraOpenProgress(value);
        }

    };
}

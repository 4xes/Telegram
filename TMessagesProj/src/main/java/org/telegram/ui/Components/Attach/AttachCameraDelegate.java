package org.telegram.ui.Components.Attach;

import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.camera.CameraView;
import org.telegram.ui.Components.ChatAttachAlertPhotoLayout;
import org.telegram.ui.Stories.recorder.DualCameraView;

import java.io.File;

public interface AttachCameraDelegate {
    int getItemSize();

    boolean isCameraExpanded();

    void getClipZone(RectF clipRect);

    int getAlertWidth();

    int getAlertHeight();

    void onInitCamera();

    void setCameraView(DualCameraView cameraView);

    Drawable getThumbCamera();

    boolean isCollapsing();

    void onClose();

    File generateVideoOutput();

    File generatePhotoOutput();

    ChatAttachAlertPhotoLayout getAlert();

    void stopCameraPreview();
}
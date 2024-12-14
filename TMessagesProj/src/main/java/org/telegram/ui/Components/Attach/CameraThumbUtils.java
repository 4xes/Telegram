package org.telegram.ui.Components.Attach;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.TextureView;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.camera.CameraView;

import java.io.File;
import java.io.FileOutputStream;

public class CameraThumbUtils {

    public static void saveThumb(CameraView cameraView) {
        saveThumb(cameraView, null);
    }

    public static void saveThumb(CameraView cameraView, Runnable whenDone) {
        if (cameraView == null || cameraView.getTextureView() == null) {
            return;
        }
        try {
            TextureView textureView = cameraView.getTextureView();
            Bitmap bitmap = textureView.getBitmap();
            if (bitmap != null) {
                Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), cameraView.getMatrix(), true);
                bitmap.recycle();
                bitmap = newBitmap;
                Bitmap lastBitmap = Bitmap.createScaledBitmap(bitmap, 80, (int) (bitmap.getHeight() / (bitmap.getWidth() / 80.0f)), true);
                if (lastBitmap != bitmap) {
                    bitmap.recycle();
                }
                Utilities.blurBitmap(lastBitmap, 7, 1, lastBitmap.getWidth(), lastBitmap.getHeight(), lastBitmap.getRowBytes());
                File file = new File(ApplicationLoader.getFilesDirFixed(), SAVE_THUMB);
                FileOutputStream stream = new FileOutputStream(file);
                lastBitmap.compress(Bitmap.CompressFormat.JPEG, 87, stream);
                lastBitmap.recycle();
                stream.close();
            }
        } catch (Throwable ignore) {

        } finally {
            if (whenDone != null) {
                AndroidUtilities.runOnUIThread(whenDone);
            }
        }
    }

    public static void saveThumbInQueue(CameraView cameraView, Runnable whenDone) {
        Utilities.themeQueue.postRunnable(() -> {
            saveThumb(cameraView, whenDone);
        });
    }

    @Nullable
    public static Bitmap loadThumb() {
        try {
            File file = new File(ApplicationLoader.getFilesDirFixed(), SAVE_THUMB);
            if (!file.exists()) {
                return null;
            }
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static final String SAVE_THUMB = "cthumb.jpg";
}

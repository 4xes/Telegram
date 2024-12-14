package org.telegram.ui.Components.Attach;

import android.graphics.Bitmap;
import android.view.TextureView;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.camera.CameraView;

import java.io.File;
import java.io.FileOutputStream;

public class CameraUtils {

    public static void saveThumb(CameraView cameraView) {
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

        }
    }

    private static final String SAVE_THUMB = "cthumb.jpg";
}

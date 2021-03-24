package org.telegram.ui.Animations.Background;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.RawRes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ShaderReader {

    public static String readShader(Context context, @RawRes int shader) {
        Resources resources = context.getApplicationContext().getResources();
        String result = null;
        try (InputStream is = resources.openRawResource(shader); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            result = os.toString("UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}

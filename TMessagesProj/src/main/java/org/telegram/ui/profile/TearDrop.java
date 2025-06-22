package org.telegram.ui.profile;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;

public final class TearDrop {

    private TearDrop() {}

    private static final int DEFAULT_COLOR = 0xFF000000;
    private static final float MIN_RADIUS_DP = 28f;
    private static final float WAVE_OFFSET_DP = 40f;
    private static final float SCALE_SHOW_WAVE = 1.1f;
    private static final float SCALE_SHOW_CIRCLE = 1.2f;

    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH = new Path();

    @ColorInt
    private static int color = DEFAULT_COLOR;

    static {
        configurePaint();
    }

    public static void setColor(@ColorInt int newColor) {
        if (color != newColor) {
            color = newColor;
            configurePaint();
        }
    }

    public static void draw(@NonNull Canvas canvas, @NonNull View target, @NonNull View container) {
        float width = container.getMeasuredWidth();
        float targetBottom = target.getY() + target.getMeasuredHeight() * target.getScaleY();

        canvas.save();
        canvas.clipRect(0, 0, width, targetBottom);

        float scaleY = target.getScaleY();
        if (scaleY >= SCALE_SHOW_CIRCLE) {
            canvas.restore();
            return;
        }

        float radius = Math.min(
                AndroidUtilities.dp(MIN_RADIUS_DP),
                target.getMeasuredWidth() * target.getScaleX() / 2f);

        float cx = width / 2f;
        float cy = target.getY() + radius;

        canvas.drawCircle(cx, cy, radius, PAINT);

        if (scaleY < SCALE_SHOW_WAVE) {
            PATH.reset();
            float step = (SCALE_SHOW_WAVE - scaleY) / SCALE_SHOW_WAVE;

            if (target.getY() > 0) {
                float y = AndroidUtilities.dp(WAVE_OFFSET_DP) - target.getY();
                y = Math.min(y, targetBottom); // ensure within bounds
                PATH.moveTo(cx - radius, 0);
                PATH.cubicTo(cx - radius / 2f, 0, cx - radius / 2f, y, cx, y);
                PATH.cubicTo(cx + radius / 2f, y, cx + radius / 2f, 0, cx + radius, 0);
            } else {
                float prevY = Math.max(0, AndroidUtilities.dp(WAVE_OFFSET_DP) - target.getY());
                float newY = targetBottom;
                float factor = Math.min((step - 0.2f) / 0.3f, 1f);

                float y = AndroidUtilities.lerp(prevY, newY, factor);

                // cap r so it never exceeds half of container width
                float r = Math.min(AndroidUtilities.lerp(radius, radius * 2f, factor), width / 2f);

                float leftX = AndroidUtilities.lerp(cx - radius / 2f, cx - r / 2f, factor);
                float rightX = AndroidUtilities.lerp(cx + radius / 2f, cx + r / 2f, factor);

                PATH.moveTo(cx - r, 0);
                PATH.cubicTo(leftX, 0, leftX, y, cx, y);
                PATH.cubicTo(rightX, y, rightX, 0, cx + r, 0);
            }
            canvas.drawPath(PATH, PAINT);
        }
        canvas.restore();
    }

    private static void configurePaint() {
        PAINT.setColor(color);
        PAINT.setStyle(Paint.Style.FILL);
    }
}

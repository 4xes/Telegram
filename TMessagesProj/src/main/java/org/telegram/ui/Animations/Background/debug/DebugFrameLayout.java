package org.telegram.ui.Animations.Background.debug;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

public class DebugFrameLayout extends FrameLayout {

    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Rect rect = new Rect();

    public DebugFrameLayout(@NonNull Context context, int sizeStroke, int color) {
        super(context);
        paint.setStrokeWidth(sizeStroke);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        setWillNotDraw(false);
    }

    @Override
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
        canvas.drawRect(rect, paint);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        rect.set(0, 0, getWidth(), getHeight());
    }
}
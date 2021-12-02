package org.telegram.ui.Reactions;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

public class ReactionBubbleDrawable extends Drawable {

    final Drawable backgroundStart;
    final Drawable backgroundEnd;
    final Drawable drawableCircle;
    final Drawable drawableCircleSmall;

    public static final int heightBubbleDp = 72;
    private final Rect contentPadding = new Rect();

    private final int circleSize = AndroidUtilities.dp(24);
    private final int circlePaddingRight = AndroidUtilities.dp(22);
    private final int circlePaddingBottom = AndroidUtilities.dp(11);

    private final int circleSmallSize = AndroidUtilities.dp(16);
    private final int circleSmallPaddingRight = AndroidUtilities.dp(22);
    private final int circleSmallPaddingBottom = 0;

    private static final int circleOverSize = AndroidUtilities.dp(14);
    private final RectF circleOverRect = new RectF(0f, 0f, circleOverSize, circleOverSize);

    private final Rect bounds = new Rect();

    private float progress = 0.99f;

    private final Paint overPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int backgroundColor = Color.WHITE;

    private final int contentHeight;

    public ReactionBubbleDrawable(Context context) {
        backgroundStart = context.getResources().getDrawable(R.drawable.popup_reactions_animation);
        backgroundEnd = context.getResources().getDrawable(R.drawable.popup_reactions);
        drawableCircle = context.getResources().getDrawable(R.drawable.popup_reactions_circle);
        drawableCircleSmall = context.getResources().getDrawable(R.drawable.popup_reactions_circle_small);
        backgroundEnd.getPadding(contentPadding);

        contentHeight = AndroidUtilities.dp(heightBubbleDp) - contentPadding.top - contentPadding.bottom;

        overPaint.setColor(Color.WHITE);
        overPaint.setStyle(Paint.Style.FILL);

        drawableCircle.setBounds(0, 0, circleSize, circleSize);
        drawableCircleSmall.setBounds(0, 0, circleSmallSize, circleSmallSize);
    }

    public void setBackgroundColor(int color) {
        if (backgroundColor != color) {
            backgroundColor = color;
            PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
            backgroundStart.setColorFilter(colorFilter);
            backgroundEnd.setColorFilter(colorFilter);
            drawableCircle.setColorFilter(colorFilter);
            drawableCircleSmall.setColorFilter(colorFilter);
            overPaint.setColor(color);
        }
    }

    public int getContentHeight() {
        return contentHeight;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (progress == 1f) {
            backgroundEnd.draw(canvas);
        } else {
            if (progress == 0f) {
                backgroundStart.draw(canvas);
            } else {
                drawSmallCircle(canvas);
                drawCircle(canvas);
                backgroundStart.draw(canvas);
                drawOverCircle(canvas);
            }
        }
    }

    private void drawCircle(Canvas canvas) {
        final int saveTranslate = canvas.save();
        final float x = bounds.width() - circlePaddingRight - circleSize;
        final float y = bounds.height() - circlePaddingBottom - circleSize;
        canvas.translate(x, y);
        final float centerCircle = circleSize / 2f;
        canvas.scale(progress, progress, centerCircle, centerCircle);
        drawableCircle.draw(canvas);
        canvas.restoreToCount(saveTranslate);
    }

    private void drawOverCircle(Canvas canvas) {
        final int saveTranslate = canvas.save();
        final float offset = (circleSize - circleOverSize) / 2f;
        final float x = bounds.width() - circlePaddingRight - circleSize + offset;
        final float y = bounds.height() - circlePaddingBottom - circleSize + offset;
        canvas.translate(x, y);
        final float centerCircle = circleOverSize / 2f;
        canvas.scale(progress, progress, centerCircle, centerCircle);
        canvas.drawOval(circleOverRect, overPaint);
        canvas.restoreToCount(saveTranslate);
    }

    private void drawSmallCircle(Canvas canvas) {
        final int saveTranslate = canvas.save();
        final float x = bounds.width() - circleSmallPaddingRight - circleSmallSize;
        final float y = bounds.height() - circleSmallPaddingBottom - circleSmallSize;
        canvas.translate(x, y);
        final float centerCircle = circleSmallSize / 2f;
        canvas.scale(progress, progress, centerCircle, centerCircle);
        drawableCircleSmall.draw(canvas);
        canvas.restoreToCount(saveTranslate);
    }

    @Override
    public void setAlpha(int alpha) {

    }
    public Rect getContentPadding() {
        return contentPadding;
    }

    @Override
    public boolean getPadding(@NonNull Rect padding) {
        return backgroundEnd.getPadding(padding);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        this.bounds.set(bounds);
        backgroundEnd.setBounds(bounds);
        backgroundStart.setBounds(bounds);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

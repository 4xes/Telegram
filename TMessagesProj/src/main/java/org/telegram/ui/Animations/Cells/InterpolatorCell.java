package org.telegram.ui.Animations.Cells;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Animations.AnimationPreferences;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Animations.InterpolatorData;
import org.telegram.ui.Animations.Parameter;

public class InterpolatorCell extends View {

    private final Paint paintThumb = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLabel = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean isSupportShadowLayer() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }
    private Bitmap progressionShadowBitmap;
    private Bitmap timeShadowBitmap;

    private final Paint debug = null;

    private String key = null;
    private final InterpolatorData d = InterpolatorData.defaultData();
    private final AnimationPreferences preferences;

    public InterpolatorCell(Context context, AnimationPreferences preferences) {
        super(context);
        init();
        this.preferences = preferences;
    }

    public void setInterpolationData(String key, InterpolatorData data) {
        boolean invalidate = false;
        if (setProgressionTop(data.progressionTop)) {
            invalidate = true;
        }
        if (setProgressionBottom(data.progressionBottom)) {
            invalidate = true;
        }
        if (setTimeStart(data.timeStart)) {
            invalidate = true;
        }
        if (setTimeEnd(data.timeEnd)) {
            invalidate = true;
        }
        if (invalidate) {
            invalidate();
        }
        this.key = key;
    }

    private void init() {
        initPaints();
        if (!isSupportShadowLayer()) {
            initShadows();
        }
    }

    private int zone = NONE;
    private final RectF bound = new RectF();
    private final RectF rangeBound = new RectF();

    private final RectF progressionTopBound = new RectF();
    private final RectF progressionTopBoundTouch = new RectF();
    private final RectF progressionBottomBound = new RectF();
    private final RectF progressionBottomBoundTouch = new RectF();
    private final RectF timeStartBound = new RectF();
    private final RectF timeStartBoundTouch = new RectF();
    private final RectF timeEndBound = new RectF();
    private final RectF timeEndBoundTouch = new RectF();
    private final RectF progressionTopTrackTouch = new RectF();
    private final RectF progressionBottomTrackTouch = new RectF();
    private final RectF timeBound = new RectF();

    private final Path interpolationPath = new Path();
    private float timeStartX;
    private float timeEndX;
    private float progressionTopX;
    private float progressionBottomX;

    private final float touchOutset = AndroidUtilities.dp(12);
    private final float labelHeight = AndroidUtilities.dp(36);
    private final float thumbProgressionRadius = AndroidUtilities.dp(9);
    private final float thumbTimeWidth = AndroidUtilities.dp(12);
    private final float thumbTimeHeight = AndroidUtilities.dp(26);
    private final float thumbShadowRadius = AndroidUtilities.dp(4);
    private final float labelMinDistance = AndroidUtilities.dp(7);
    private final float labelStartPadding = AndroidUtilities.dp(3);

    private final float lineWidth = AndroidUtilities.dp(2);
    private final float dotWidth = AndroidUtilities.dp(2);
    private final float dotEdgeWidth = AndroidUtilities.dp(6);
    private final float dotSpacing = AndroidUtilities.dp(8);
    private final float dotClip = AndroidUtilities.dp(2) + dotEdgeWidth / 2f;

    private final int timeColor = 0xffefd256;
    private final int progressionFillLineColor = 0xff89bce2;
    private final int progressionTrackLineColor = 0xffe3e4e6;
    private final int thumbShadow = 0x3b000000;


    private final float slopSize = getScaledHoverSlop(ViewConfiguration.get(getContext()));
    private long lastTime;
    private final float tapTime = ViewConfiguration.getPressedStateDuration();

    private float startX;
    private float startY;

    private boolean isSlopZone(float dx, float dy) {
        return dx > -slopSize && dx < slopSize && dy > -slopSize && dy < slopSize;
    }
    private final float timeRangeMin = 0.14f;
    private float timeRangeMinX;

    private long duration = 1000L;

    private float[] dots;

    public void setDuration(long duration) {
        this.duration = duration;
        invalidate();
    }

    private boolean setProgressionTop(float value) {
        if (d.progressionTop != value) {
            d.progressionTop = value;
            if (key != null) {
                preferences.putProgressionTop(key, value);
            }
            return true;
        }
        return false;
    }

    private boolean setProgressionBottom(float value) {
        if (d.progressionBottom != value) {
            d.progressionBottom = value;
            if (key != null) {
                preferences.putProgressionBottom(key, value);
            }
            return true;
        }
        return false;
    }

    private boolean setTimeStart(float value) {
        if (d.timeStart != value) {
            d.timeStart = value;
            if (key != null) {
                preferences.putTimeStart(key, value);
            }
            return true;
        }
        return false;
    }

    private boolean setTimeEnd(float value) {
        if (d.timeEnd != value) {
            d.timeEnd = value;
            if (key != null) {
                preferences.putTimeEnd(key, value);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        bound.left = 0f;
        bound.top = 0f;
        bound.right = getWidth();
        bound.bottom = getHeight();

        rangeBound.set(bound);
        rangeBound.inset(AndroidUtilities.dp(18), 0);
        final float insetX = Math.max(thumbProgressionRadius, thumbTimeWidth / 2);
        final float insetY = labelHeight + thumbProgressionRadius;
        rangeBound.inset(insetX, insetY);
        timeRangeMinX = rangeBound.width() * timeRangeMin;

        int dotsCount = (int) (rangeBound.height() / dotSpacing) / 2 + 1;
        if (dots == null || dots.length != dotsCount * 4) {
            dots = new float[dotsCount * 4];
        }
        int i = 0;
        for (int d = 0; d < dotsCount; d++) {
            float dotY = rangeBound.top + d * dotSpacing;
            dots[i++] = 0;
            dots[i++] = dotY;
        }
        i = dots.length-1;
        for (int d = 0; d < dotsCount; d++) {
            float dotY = rangeBound.bottom - d * dotSpacing;
            dots[i--] = dotY;
            dots[i--] = 0;
        }
        calculateZones();
    }

    private void calculateZones() {
        timeStartX = rangeBound.left + rangeBound.width() * d.timeStart;
        timeEndX = rangeBound.left + rangeBound.width() * d.timeEnd;
        timeBound.set(rangeBound);
        timeBound.left = timeStartX;
        timeBound.right = timeEndX;

        float timeWidth = timeEndX - timeStartX;

        progressionTopX = timeStartX + timeWidth * d.progressionTop;
        progressionBottomX = timeStartX + timeWidth * d.progressionBottom;

        final float insetTrackTouch = thumbProgressionRadius + touchOutset;
        progressionTopTrackTouch.set(
                timeBound.left,
                timeBound.top - insetTrackTouch,
                timeBound.right,
                timeBound.top + insetTrackTouch
        );

        progressionBottomTrackTouch.set(
                timeBound.left,
                timeBound.bottom - insetTrackTouch,
                timeBound.right,
                timeBound.bottom + insetTrackTouch
        );

        progressionTopBound.set(
                progressionTopX - thumbProgressionRadius,
                rangeBound.top - thumbProgressionRadius,
                progressionTopX + thumbProgressionRadius,
                rangeBound.top + thumbProgressionRadius
        );
        progressionTopBoundTouch.set(progressionTopBound);
        progressionTopBoundTouch.inset(-touchOutset, -touchOutset);

        progressionBottomBound.set(
                progressionBottomX - thumbProgressionRadius,
                rangeBound.bottom - thumbProgressionRadius,
                progressionBottomX + thumbProgressionRadius,
                rangeBound.bottom + thumbProgressionRadius
        );
        progressionBottomBoundTouch.set(progressionBottomBound);
        progressionBottomBoundTouch.inset(-touchOutset, -touchOutset);

        final float thumbTimeHalfWidth = thumbTimeWidth / 2f;
        final float thumbTimeHalfHeight = thumbTimeHeight / 2f;
        final float thumbTimeCenterY = rangeBound.centerY();

        timeStartBound.set(
                timeStartX - thumbTimeHalfWidth,
                thumbTimeCenterY - thumbTimeHalfHeight,
                timeStartX + thumbTimeHalfWidth,
                thumbTimeCenterY + thumbTimeHalfHeight
        );
        timeStartBoundTouch.set(timeStartBound);
        timeStartBoundTouch.inset(-touchOutset, -touchOutset);

        timeEndBound.set(
                timeEndX - thumbTimeHalfWidth,
                thumbTimeCenterY - thumbTimeHalfHeight,
                timeEndX + thumbTimeHalfWidth,
                thumbTimeCenterY + thumbTimeHalfHeight
        );
        timeEndBoundTouch.set(timeEndBound);
        timeEndBoundTouch.inset(-touchOutset, -touchOutset);
    }

    private float paintLabelHeight;
    private Rect labelBound = new Rect();

    private void initPaints() {
        paintThumb.setStyle(Paint.Style.FILL_AND_STROKE);
        paintThumb.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        if (isSupportShadowLayer()) {
            paintThumb.setShadowLayer(thumbShadowRadius, 0f,  0f, thumbShadow);
        }

        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(lineWidth);

        paintDot.setStyle(Paint.Style.STROKE);
        paintDot.setStrokeCap(Paint.Cap.ROUND);
        paintDot.setColor(timeColor);

        if (debug != null) {
            debug.setStyle(Paint.Style.FILL);
        }
        paintLabel.setTextSize(AndroidUtilities.dp(12f));
        Paint.FontMetrics fm = paintLabel.getFontMetrics();
        paintLabelHeight = -(fm.ascent + fm.descent);
    }

    private void initShadows() {
        final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.TRANSPARENT);
        shadowPaint.setShadowLayer(thumbShadowRadius, 0f,  0f, thumbShadow);

        int progressShadowSize = Math.round((thumbProgressionRadius + thumbShadowRadius) * 2f);
        progressionShadowBitmap = Bitmap.createBitmap(
                progressShadowSize,
                progressShadowSize,
                Bitmap.Config.ARGB_8888);
        Canvas progressionShadowCanvas = new Canvas(progressionShadowBitmap);

        RectF shadowRect = new RectF(0, 0, progressShadowSize, progressShadowSize);
        shadowRect.inset(thumbShadowRadius, thumbShadowRadius);

        progressionShadowCanvas.drawOval(shadowRect, shadowPaint);


        int timeShadowWidth = Math.round(thumbTimeWidth + thumbShadowRadius * 2f);
        int timeShadowHeight = Math.round(thumbTimeHeight + thumbShadowRadius * 2f);
        timeShadowBitmap = Bitmap.createBitmap(
                timeShadowWidth,
                timeShadowHeight,
                Bitmap.Config.ARGB_8888);
        Canvas timeShadowCanvas = new Canvas(timeShadowBitmap);

        shadowRect.set(0, 0, timeShadowWidth, timeShadowHeight);
        shadowRect.inset(thumbShadowRadius, thumbShadowRadius);
        final float cornerRadius = shadowRect.width() / 2f;
        timeShadowCanvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();
        final int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTime = System.currentTimeMillis();
                startX = x;
                startY = y;
                zone = getZone(x, y);
                if (zone != NONE) {
                    previousX = x;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (zone != NONE) {
                    handleMove(x, y);
                    return true;
                }
            case MotionEvent.ACTION_UP:
                if (zone != NONE) {
                    handleTap(x, y);
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;
        }

        return false;
    }

    private boolean isTap() {
        return System.currentTimeMillis() - lastTime < tapTime;
    }

    private int getZone(final float x, final float y) {
        if (progressionTopBoundTouch.contains(x, y)) {
            return PROGRESSION_TOP;
        }
        if (progressionBottomBoundTouch.contains(x, y)) {
            return PROGRESSION_BOTTOM;
        }
        boolean isTimeStart = timeStartBoundTouch.contains(x, y);
        boolean isTimeEnd = timeEndBoundTouch.contains(x, y);

        if (isTimeStart && isTimeEnd) {
            float distanceStart = (float) Math.hypot(timeStartBoundTouch.centerX() - x, timeStartBoundTouch.centerY() - y);
            float distanceEnd = (float) Math.hypot(timeEndBoundTouch.centerX() - x, timeEndBoundTouch.centerY() - y);
            if (distanceStart < distanceEnd) {
                return TIME_START;
            } else {
                return TIME_END;
            }
        }
        if (isTimeStart) {
            return TIME_START;
        }
        if (isTimeEnd) {
            return TIME_END;
        }

        if (progressionTopTrackTouch.contains(x, y)) {
            return PROGRESSION_TOP_TRACK;
        }
        if (progressionBottomTrackTouch.contains(x, y)) {
            return PROGRESSION_BOTTOM_TRACK;
        }

        if (timeBound.contains(x, y)) {
            return TIME_MOVE;
        }

        return NONE;
    }

    private float previousX;

    private void handleMove(final float x, final float y) {
        boolean needInvalidate = false;
        float difX = previousX - x;
        if (zone == TIME_START) {
            float edgeLeft = rangeBound.left - timeStartBoundTouch.width() / 2f;
            if (x < edgeLeft) {
                previousX = edgeLeft;
                return;
            }
            float edgeRight = timeEndBoundTouch.right - timeRangeMinX;
            if (x > edgeRight) {
                previousX = edgeRight;
                return;
            }
        }
        if (zone == TIME_END) {
            float edgeLeft = timeStartBoundTouch.left + timeRangeMinX;
            if (x < edgeLeft) {
                previousX = edgeLeft;
                return;
            }
            float edgeRight = rangeBound.right + timeEndBoundTouch.width() / 2f;
            if (x > edgeRight) {
                previousX = edgeRight;
                return;
            }
        }
        float dif = difX / rangeBound.width();
        if (zone == PROGRESSION_TOP || zone == PROGRESSION_BOTTOM) {
            float halfTouch = progressionTopBoundTouch.width() / 2f;
            float edgeLeft = timeBound.left - halfTouch;
            if (x < edgeLeft) {
                previousX = edgeLeft;
            }
            float edgeRight = timeBound.right + halfTouch;
            if (x > edgeRight) {
                previousX = edgeRight;
            }
            dif = difX / timeBound.width();
        }
        float value;
        switch (zone) {
            case TIME_START:
                value = Math.max(0f, Math.min(d.timeStart - dif, d.timeEnd - timeRangeMin));
                needInvalidate = setTimeStart(value);
                break;
            case TIME_END:
                value = Math.max(d.timeStart + timeRangeMin, Math.min(d.timeEnd - dif, 1f));
                needInvalidate = setTimeEnd(value);
                break;
            case TIME_MOVE:
                final float range = d.timeEnd - d.timeStart;
                value = Math.max(0f, Math.min(d.timeStart - dif, 1f - range));
                if (value != d.timeStart) {
                    setTimeStart(value);
                    setTimeEnd(value + range);
                    needInvalidate = true;
                }
                break;
            case PROGRESSION_TOP:
                value = Math.max(0f, Math.min(d.progressionTop - dif, 1f));
                needInvalidate = setProgressionTop(value);
                break;
            case PROGRESSION_BOTTOM:
                value = Math.max(0f, Math.min(d.progressionBottom - dif, 1f));
                needInvalidate = setProgressionBottom(value);
                break;
            case PROGRESSION_TOP_TRACK:
                if (!progressionTopTrackTouch.contains(x, y)) {
                    zone = NONE;
                    return;
                }
                if (isSlopZone(startX - x, 0)) {
                    return;
                }
                zone = PROGRESSION_TOP;
                needInvalidate = handleProgression(x);
                break;
            case PROGRESSION_BOTTOM_TRACK:
                if (!progressionBottomTrackTouch.contains(x, y)) {
                    zone = NONE;
                    return;
                }
                if (isSlopZone(startX - x, 0)) {
                    return;
                }
                zone = PROGRESSION_BOTTOM;
                needInvalidate = handleProgression(x);
                break;
        }
        if (needInvalidate) {
            calculateZones();
            invalidate();
        }
        previousX = x;
    }

    private void handleTap(float x, float y) {
        boolean needInvalidate = false;
        if (progressionTopTrackTouch.contains(x, y)) {
            needInvalidate = handleProgression(x);
        }
        if (progressionBottomTrackTouch.contains(x, y)) {
            needInvalidate = handleProgression(x);
        }
        if (needInvalidate) {
            calculateZones();
            invalidate();
        }
    }

    private boolean handleProgression(float x) {
        float progression = (x - timeBound.left) / timeBound.width();
        progression = Math.max(0f, Math.min(progression, 1f));
        if (zone == PROGRESSION_TOP || zone == PROGRESSION_TOP_TRACK) {
            return setProgressionTop(progression);
        } else if (zone == PROGRESSION_BOTTOM || zone == PROGRESSION_BOTTOM_TRACK) {
            return setProgressionBottom(progression);
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawDots(canvas, timeStartX);
        drawDots(canvas, timeEndX);
        int clipSave = canvas.save();
        canvas.clipRect(
                timeStartX - dotClip,
                rangeBound.top - dotClip,
                timeStartX + dotClip,
                rangeBound.bottom + dotClip,
                Region.Op.DIFFERENCE
        );
        canvas.clipRect(
                timeEndX - dotClip,
                rangeBound.top - dotClip,
                timeEndX + dotClip,
                rangeBound.bottom + dotClip,
                Region.Op.DIFFERENCE
        );
        drawBezier(canvas);
        paintLine.setColor(progressionTrackLineColor);
        canvas.drawLine(rangeBound.left, rangeBound.top, rangeBound.right, rangeBound.top, paintLine);
        canvas.drawLine(rangeBound.left, rangeBound.bottom, rangeBound.right, rangeBound.bottom, paintLine);
        paintLine.setColor(progressionFillLineColor);
        canvas.drawLine(progressionTopX, rangeBound.top, timeEndX, rangeBound.top, paintLine);
        canvas.drawLine(timeStartX, rangeBound.bottom, progressionBottomX, rangeBound.bottom, paintLine);

        canvas.restoreToCount(clipSave);

        drawThumbProgression(canvas, progressionTopBound);
        drawThumbProgression(canvas, progressionBottomBound);

        drawThumbTime(canvas, timeStartBound);
        drawThumbTime(canvas, timeEndBound);

        drawLabels(canvas);

        drawDebug(canvas);
    }

    private void drawDots(Canvas canvas, float x) {
        if (dots != null) {
            int save = canvas.save();
            canvas.translate(x, 0f);
            paintDot.setStrokeWidth(dotEdgeWidth);
            canvas.drawPoints(dots,0, 2, paintDot);
            canvas.drawPoints(dots,dots.length - 2, 2, paintDot);
            paintDot.setStrokeWidth(dotWidth);
            canvas.drawPoints(dots,2, dots.length - 4, paintDot);
            canvas.restoreToCount(save);
        }
    }

    private void drawDebug(Canvas canvas) {
        if (debug != null) {
            debug.setColor(0x2200ff00);
            canvas.drawRect(progressionTopTrackTouch, debug);
            debug.setColor(0x2200ff00);
            canvas.drawRect(progressionBottomTrackTouch, debug);

            debug.setColor(0x22ff00ff);
            canvas.drawRect(progressionTopBoundTouch, debug);
            debug.setColor(0x22ff00ff);
            canvas.drawRect(progressionBottomBoundTouch, debug);

            debug.setColor(0x2200ffff);
            canvas.drawRect(timeStartBoundTouch, debug);
            debug.setColor(0x2200ffff);
            canvas.drawRect(timeEndBoundTouch, debug);
        }
    }

    private void drawThumbProgression(Canvas canvas, RectF bound) {
        if (progressionShadowBitmap != null) {
            drawShadowBitmap(canvas, bound, progressionShadowBitmap);
        }
        canvas.drawOval(bound, paintThumb);
    }

    private void drawThumbTime(Canvas canvas, RectF bound) {
        if (timeShadowBitmap != null) {
            drawShadowBitmap(canvas, bound, timeShadowBitmap);
        }
        final float cornerRadius = bound.width() / 2f;
        canvas.drawRoundRect(bound, cornerRadius, cornerRadius, paintThumb);
    }

    private void drawShadowBitmap(Canvas canvas, RectF bound, Bitmap bitmap) {
        final float offsetX = bitmap.getWidth() / 2f;
        final float offsetY = bitmap.getHeight() / 2f;
        canvas.drawBitmap(bitmap, bound.centerX() - offsetX, bound.centerY() - offsetY, paintThumb);
    }

    private void drawBezier(Canvas canvas) {
        interpolationPath.reset();
        final float anc1X = timeStartX;
        final float anc1Y = rangeBound.bottom;
        final float anc2X = progressionBottomX;
        final float anc2Y = rangeBound.bottom;
        final float anc3X = progressionTopX;
        final float anc3Y = rangeBound.top;
        final float anc4X = timeEndX;
        final float anc4Y = rangeBound.top;
        interpolationPath.moveTo(anc1X, anc1Y);
        interpolationPath.cubicTo(anc2X, anc2Y, anc3X, anc3Y, anc4X, anc4Y);
        paintLine.setColor(progressionTrackLineColor);
        canvas.drawPath(interpolationPath, paintLine);
    }

    private void drawLabels(Canvas canvas) {
        paintLabel.setColor(progressionFillLineColor);
        paintLabel.setTextAlign(Paint.Align.LEFT);

        canvas.drawText(formatPercent(1f - d.progressionTop), progressionTopBound.left + labelStartPadding, progressionTopBound.top - labelMinDistance, paintLabel);
        paintLabel.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(formatPercent(d.progressionBottom), progressionBottomBound.right, progressionBottomBound.bottom + paintLabelHeight + labelMinDistance, paintLabel);

        paintLabel.setColor(timeColor);
        final float timeOffsetHeight = paintLabelHeight / 2f;
        String timeStartLabel = formatDuration(d.timeStart);
        String timeEndLabel = formatDuration(d.timeEnd);
        paintLabel.getTextBounds(timeStartLabel, 0, timeStartLabel.length(), labelBound);
        float timeStartLabelWidth = labelBound.width();
        paintLabel.getTextBounds(timeEndLabel, 0, timeEndLabel.length(), labelBound);
        float timeEndLabelWidth = labelBound.width();

        float timeStartLabelX = timeStartBound.right + labelMinDistance;
        float timeEndLabelX = timeEndBound.left - labelMinDistance;

        float dif = (timeEndLabelX - timeEndLabelWidth - labelMinDistance) - (timeStartLabelX + timeStartLabelWidth + labelMinDistance);
        float offsetX = 0f;
        if (dif < 0f) {
            offsetX = dif / 2f;
        }

        paintLabel.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(timeStartLabel, timeStartLabelX + offsetX, timeStartBound.centerY() + timeOffsetHeight, paintLabel);
        paintLabel.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(timeEndLabel, timeEndLabelX - offsetX, timeEndBound.centerY() + timeOffsetHeight, paintLabel);
    }

    private String formatPercent(float value) {
        int percent = Math.round(value * 100f);
        return percent + "%";
    }

    private String formatDuration(float value) {
        int duration = Math.round(value * this.duration);
        return duration + "mc";
    }

    private static final int NONE = 0;
    private static final int TIME_START = 1;
    private static final int TIME_END = 2;
    private static final int TIME_MOVE = 3;
    private static final int PROGRESSION_TOP = 4;
    private static final int PROGRESSION_BOTTOM = 5;
    private static final int PROGRESSION_TOP_TRACK = 6;
    private static final int PROGRESSION_BOTTOM_TRACK = 7;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(238), MeasureSpec.EXACTLY));
    }

    public static int getScaledHoverSlop(ViewConfiguration config) {
        if (Build.VERSION.SDK_INT >= 28) {
            return config.getScaledHoverSlop();
        }
        return config.getScaledTouchSlop() / 2;
    }
}
package com.blackfox.surface.renderer.particle;


import android.graphics.Bitmap;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.Px;

import com.blackfox.surface.renderer.DustEffectDelegate;

import org.telegram.messenger.AndroidUtilities;

import java.lang.ref.WeakReference;

public class DustRequest {

    private static final float multiTime = 3.0f;
    private final Bitmap bitmap;

    @Nullable
    public final WeakReference<View> view;

    @Nullable
    private final DustEffectDelegate delegate;
    private final int offsetX;
    private final int offsetY;
    private final int width;
    private final int height;

    private final float durationUs;

    private final float radius;
    private final int countVertical;
    private final int countHorizontal;
    private final int count;
    public float timeUs = 0;

    public DustRenderer renderer;

    Runnable startedEffect;

    Runnable finishedEffect;

    public DustRenderer getRendererAndInitIfNeeded(int width, int height) {
        if (renderer == null) {
            renderer = new DustRenderer(this);
            renderer.setSize(width, height);
            renderer.init();
        }
        return renderer;
    }

    public boolean isFinish() {
        return timeUs > durationUs * multiTime;
    }

    public void notifyStart() {
        if (startedEffect != null) {
            AndroidUtilities.runOnUIThread(startedEffect);
            startedEffect = null;
        }
    }

    public boolean needNotify() {
        return startedEffect != null;
    }
    public void finishAndCleanup() {
        AndroidUtilities.runOnUIThread(finishedEffect);
        renderer.cleanup();
    }

    public DustRequest(@Nullable View view, Bitmap bitmap, @Nullable DustEffectDelegate delegate, @Px int width, @Px int height, @Px int offsetX, @Px int offsetY, float radius, int durationMs) {
        this.view = view != null ? new WeakReference<>(view): null;
        this.bitmap = bitmap;
        this.delegate = delegate;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;
        this.durationUs = (float) durationMs / 1000.0f / multiTime;
        this.radius = radius;
        //to do radius
        this.countHorizontal = (int) Math.floor(width / radius);
        this.countVertical = (int) Math.floor(height / radius);
        this.count = countHorizontal * countVertical;
        if (this.delegate != null) {
            startedEffect = () -> delegate.onStartEffect(DustRequest.this);
        }
        if (delegate != null) {
            finishedEffect = () -> delegate.onFinishedEffect(DustRequest.this);
        }
    }

    @Nullable
    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getCountVertical() {
        return countVertical;
    }

    public int getCountHorizontal() {
        return countHorizontal;
    }

    public int getCount() {
        return count;
    }

    public float getDurationUs() {
        return durationUs;
    }

    public float getRadius() {
        return radius;
    }
}

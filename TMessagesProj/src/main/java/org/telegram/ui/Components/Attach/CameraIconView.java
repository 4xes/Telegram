package org.telegram.ui.Components.Attach;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

import java.util.Objects;

@SuppressLint("ViewConstructor")
public abstract class CameraIconView extends FrameLayout {

    private final AttachCameraDelegate delegate;
    private final Theme.ResourcesProvider resourcesProvider;
    private final Drawable iconDrawable;

    public CameraIconView(@NonNull Context context, AttachCameraDelegate delegate, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.delegate = delegate;
        this.resourcesProvider = resourcesProvider;
        iconDrawable = Objects.requireNonNull(ContextCompat.getDrawable(context, R.drawable.instant_camera)).mutate();
        updateThemeColors();
        setWillNotDraw(false);
        setClipChildren(true);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int w = iconDrawable.getIntrinsicWidth();
        int h = iconDrawable.getIntrinsicHeight();
        int itemSize = delegate.getItemSize();
        int x = (itemSize - w) / 2;
        int y = (itemSize - h) / 2 - offsetY();
        boolean clip = maxY() < delegate.getAlertHeight();
        if (clip) {
            canvas.save();
            canvas.clipRect(0, 0, delegate.getAlertWidth(), maxY());
        }
        iconDrawable.setBounds(x, y, x + w, y + h);
        iconDrawable.draw(canvas);
        if (clip) {
            canvas.restore();
        }
    }

    public void updateThemeColors() {
        Theme.setDrawableColor(iconDrawable, getThemedColor(Theme.key_dialogCameraIcon));
        invalidate();
    }

    public int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }

    abstract int maxY();
    abstract int offsetY();
}

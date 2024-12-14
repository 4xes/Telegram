package org.telegram.ui.Components.Attach;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Build;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BlurringShader;
import org.telegram.ui.Stories.recorder.CollageLayoutView2;

@SuppressLint("ViewConstructor")
public class CollageAttachLayoutView extends CollageLayoutView2 {
    public CollageAttachLayoutView(Context context, BlurringShader.BlurManager blurManager, FrameLayout containerView, Theme.ResourcesProvider resourcesProvider, AttachCameraDelegate delegate) {
        super(context, blurManager, containerView, resourcesProvider, delegate);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (isCollapsed()) {
            return false;
        } else {
            return super.dispatchTouchEvent(event);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (AndroidUtilities.makingGlobalBlurBitmap) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 21) {
            super.dispatchDraw(canvas);
        } else {
            AttachCameraDelegate delegate = attachCameraDelegate;
            if (delegate == null) {
                super.dispatchDraw(canvas);
                return;
            }
            canvas.save();
            RectF clipRect = AndroidUtilities.rectTmp;
            delegate.getClipZone(clipRect);
            canvas.clipRect(clipRect);
            super.dispatchDraw(canvas);
            canvas.restore();
        }
    }

}

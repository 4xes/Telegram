package org.telegram.ui.Popup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;

@SuppressLint("ViewConstructor")
public class ChatMenuClipLayout extends FrameLayout {

    private final RectF bounds = new RectF();
    private final Path mask = new Path();
    private final int cornerRadius = AndroidUtilities.dp(4);

    public ChatMenuClipLayout(Context context) {
        super(context);
        if (isSupportOutline()) {
            setClipToOutline(true);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), cornerRadius);
                }
            });
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bounds.set(0f, 0f, getMeasuredWidth(), getMeasuredHeight());

        rebuildMask();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(220), View.MeasureSpec.EXACTLY), heightMeasureSpec);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (isSupportOutline()) {
            super.dispatchDraw(canvas);
        } else {
            int save = canvas.save();
            canvas.clipPath(mask);
            super.dispatchDraw(canvas);
            canvas.restoreToCount(save);
        }
    }

    private boolean isSupportOutline() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    private void rebuildMask() {
        if (!isSupportOutline()) {
            mask.reset();
            mask.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW);
            mask.close();
        }
    }
}

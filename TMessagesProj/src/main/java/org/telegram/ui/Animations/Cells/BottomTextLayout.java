/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Animations.Cells;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class BottomTextLayout extends FrameLayout {

    private TextView textView;

    public BottomTextLayout(Context context) {
        super(context);
        setWillNotDraw(false);

        textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setTextColor(Theme.getColor(Theme.key_chat_fieldOverlayText));
        textView.setBackgroundDrawable(Theme.getSelectorDrawable(false));
        textView.setGravity(Gravity.CENTER);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        setPadding(0, AndroidUtilities.dp(2), 0, 0);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        textView.setOnClickListener(l);
    }

    public TextView getTextView() {
        return textView;
    }


    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    public void setText(String text) {
        textView.setText(text);
    }


    Animator animator;

    public void setEnabled(boolean value, boolean animate) {
        setEnabled(value);
        if (animate) {
            if (animator != null) {
                animator.cancel();
            }
            animator = ObjectAnimator.ofFloat(textView, "alpha", value ? 1.0f : 0.5f);
            animator.setDuration(500L);
            animator.start();
        }
    }

    @Override
    public void setEnabled(boolean value) {
        textView.setEnabled(value);
        textView.setAlpha(value ? 1.0f : 0.5f);
    }

    @Override
    public boolean isEnabled() {
        return textView.isEnabled();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int bottom = Theme.chat_composeShadowDrawable.getIntrinsicHeight();
        Theme.chat_composeShadowDrawable.setBounds(0, 0, getMeasuredWidth(), bottom);
        Theme.chat_composeShadowDrawable.draw(canvas);
        canvas.drawRect(0, bottom, getMeasuredWidth(), getMeasuredHeight(), Theme.chat_composeBackgroundPaint);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setEnabled(isEnabled());
    }
}

package org.telegram.ui.Animations.Cells;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class ColorSettingsCell extends FrameLayout {

    private TextView textView;
    private TextView colorView;
    private boolean needDivider;
    private boolean canDisable;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int color = Color.TRANSPARENT;

    private int roundInner = AndroidUtilities.dp(2);
    private int roundOuter = AndroidUtilities.dp(2);

    private boolean isSelect = false;

    public ColorSettingsCell(Context context) {
        this(context, 21);
    }

    public ColorSettingsCell(Context context, int padding) {
        super(context);

        textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, padding, 0, padding, 0));

        colorView = new TextView(context);
        colorView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        colorView.setGravity(Gravity.CENTER);
        addView(colorView, LayoutHelper.createFrame(76, 28, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER, 0, 0, padding, 0));
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(50) + (needDivider ? 1 : 0));

        int availableWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - AndroidUtilities.dp(34);
        int width;
        if (colorView.getVisibility() == VISIBLE) {
            colorView.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(76), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(28), MeasureSpec.EXACTLY));
            width = availableWidth - colorView.getMeasuredWidth() - AndroidUtilities.dp(8);
        } else {
            width = availableWidth;
        }
        textView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    }

    public TextView getTextView() {
        return textView;
    }

    public void setCanDisable(boolean value) {
        canDisable = value;
    }

    public void setColor(int color) {
        if (this.color != color) {
            boolean isDark = AndroidUtilities.computePerceivedBrightness(color) < 0.60f;
            String hexColor = "#" + Integer.toHexString(color).substring(2).toUpperCase();
            colorView.setText(hexColor);
            if (isDark) {
                colorView.setTextColor(Color.WHITE);
            } else {
                colorView.setTextColor(Color.BLACK);
            }
            this.color = color;
            invalidate();
        }
    }

    public int getColor() {
        return color;
    }

    public void setSelect(boolean select) {
        isSelect = select;
        invalidate();
    }

    public TextView getValueTextView() {
        return colorView;
    }

    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    public void setText(String text, boolean divider) {
        textView.setText(text);
        colorView.setVisibility(INVISIBLE);
        needDivider = divider;
    }

    public void setTextAndColor(String text,@ColorInt int color, boolean divider) {
        textView.setText(text);
        setColor(color);
        needDivider = divider;
        requestLayout();
    }

    public void setEnabled(boolean value, ArrayList<Animator> animators) {
        setEnabled(value);
        if (animators != null) {
            animators.add(ObjectAnimator.ofFloat(textView, "alpha", value ? 1.0f : 0.5f));
            if (colorView.getVisibility() == VISIBLE) {
                animators.add(ObjectAnimator.ofFloat(colorView, "alpha", value ? 1.0f : 0.5f));
            }
        } else {
            textView.setAlpha(value ? 1.0f : 0.5f);
            if (colorView.getVisibility() == VISIBLE) {
                colorView.setAlpha(value ? 1.0f : 0.5f);
            }
        }
    }

    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        textView.setAlpha(value || !canDisable ? 1.0f : 0.5f);
        if (colorView.getVisibility() == VISIBLE) {
            colorView.setAlpha(value || !canDisable ? 1.0f : 0.5f);
        }
    }

    int verticalInnerInset = AndroidUtilities.dp(5);
    int startInnerInset = AndroidUtilities.dp(15);
    int endInnerInset = AndroidUtilities.dp(4);

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        rect.set(colorView.getLeft(), colorView.getTop(), colorView.getRight(), colorView.getBottom());
        paint.setColor(color);
        canvas.drawRoundRect(rect, roundOuter, roundOuter, paint);
        rect.set(rect.left + startInnerInset, rect.top + verticalInnerInset, rect.right - endInnerInset, rect.bottom - verticalInnerInset);
        if (isSelect) {
            paint.setColor(brightnessColor(color));
            canvas.drawRoundRect(rect, roundInner, roundInner, paint);
        }
        if (needDivider) {
            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(20), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(20) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }

    float[] hsv = new float[3];

    @ColorInt int brightnessColor(@ColorInt int color) {
        Color.colorToHSV(color, hsv);
        if (hsv[2] > 0.5) {
            hsv[2] = 0.75f * hsv[2];
        } else {
            hsv[2] = 1.25f * hsv[2];
        }
        return Color.HSVToColor(hsv);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setEnabled(isEnabled());
    }
}

package org.telegram.ui.Cells;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.util.Property;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SvgHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.Switch;

import java.util.ArrayList;

public class ReactionCheckCell extends FrameLayout {

    public SimpleTextView textView;
    private BackupImageView imageView;
    private Switch checkBox;
    private boolean needDivider;

    private int animatedColorBackground;
    private float animationProgress;
    private Paint animationPaint;
    private float lastTouchX;
    private ObjectAnimator animator;
    private boolean drawCheckRipple;

    public static final Property<ReactionCheckCell, Float> ANIMATION_PROGRESS = new AnimationProperties.FloatProperty<ReactionCheckCell>("animationProgress") {
        @Override
        public void setValue(ReactionCheckCell object, float value) {
            object.setAnimationProgress(value);
            object.invalidate();
        }

        @Override
        public Float get(ReactionCheckCell object) {
            return object.animationProgress;
        }
    };

    public ReactionCheckCell(Context context) {
        super(context);

        textView = new SimpleTextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(16);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(textView);

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        addView(imageView);

        checkBox = new Switch(context);
        checkBox.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        addView(checkBox, LayoutHelper.createFrame(37, 20, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 22, 0, 22, 0));
        setClipChildren(false);
        setFocusable(true);
    }

    public SimpleTextView getTextView() {
        return textView;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        checkBox.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(37), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.AT_MOST));
        textView.measure(MeasureSpec.makeMeasureSpec(width - AndroidUtilities.dp(72) - checkBox.getMeasuredWidth(), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.EXACTLY));
        imageView.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(28), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(28), MeasureSpec.AT_MOST));
        setMeasuredDimension(width, AndroidUtilities.dp(50) + (needDivider ? 1 : 0));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        lastTouchX = event.getX();
        return super.onTouchEvent(event);
    }

    public void setColors(String switchKey, String switchKeyChecked, String switchThumb, String switchThumbChecked) {
        checkBox.setColors(switchKey, switchKeyChecked, switchThumb, switchThumbChecked);
    }

    public void setDrawCheckRipple(boolean value) {
        drawCheckRipple = value;
    }

    @Override
    public void setPressed(boolean pressed) {
        if (drawCheckRipple) {
            checkBox.setDrawRipple(pressed);
        }
        super.setPressed(pressed);
    }

    public void setEnabled(boolean value, ArrayList<Animator> animators) {
        super.setEnabled(value);
        if (animators != null) {
            animators.add(ObjectAnimator.ofFloat(textView, "alpha", value ? 1.0f : 0.5f));
            animators.add(ObjectAnimator.ofFloat(checkBox, "alpha", value ? 1.0f : 0.5f));
        } else {
            textView.setAlpha(value ? 1.0f : 0.5f);
            checkBox.setAlpha(value ? 1.0f : 0.5f);
        }
    }

    public void setChecked(boolean checked) {
        checkBox.setChecked(checked, true);
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }

    @Override
    public void setBackgroundColor(int color) {
        clearAnimation();
        animatedColorBackground = 0;
        super.setBackgroundColor(color);
    }

    public void setBackgroundColorAnimated(boolean checked, int color) {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (animatedColorBackground != 0) {
            setBackgroundColor(animatedColorBackground);
        }
        if (animationPaint == null) {
            animationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
        checkBox.setOverrideColor(checked ? 1 : 2);
        animatedColorBackground = color;
        animationPaint.setColor(animatedColorBackground);
        animationProgress = 0.0f;
        animator = ObjectAnimator.ofFloat(this, ANIMATION_PROGRESS, 0.0f, 1.0f);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setBackgroundColor(animatedColorBackground);
                animatedColorBackground = 0;
                invalidate();
            }
        });
        animator.setInterpolator(CubicBezierInterpolator.EASE_OUT);
        animator.setDuration(240).start();
    }

    private void setAnimationProgress(float value) {
        animationProgress = value;
        float rad = Math.max(lastTouchX, getMeasuredWidth() - lastTouchX) + AndroidUtilities.dp(40);
        float cx = lastTouchX;
        int cy = getMeasuredHeight() / 2;
        float animatedRad = rad * animationProgress;
        checkBox.setOverrideColorProgress(cx, cy, animatedRad);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (animatedColorBackground != 0) {
            float rad = Math.max(lastTouchX, getMeasuredWidth() - lastTouchX) + AndroidUtilities.dp(40);
            float cx = lastTouchX;
            int cy = getMeasuredHeight() / 2;
            float animatedRad = rad * animationProgress;
            canvas.drawCircle(cx, cy, animatedRad, animationPaint);
        }
        if (needDivider) {
            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(70), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(70) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int height = bottom - top;
        int width = right - left;

        int viewTop;
        int viewLeft;

        viewTop = (height - textView.getTextHeight()) / 2;
        if (LocaleController.isRTL) {
            viewLeft = getMeasuredWidth() - textView.getMeasuredWidth() - AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? 72 : 23);
        } else {
            viewLeft = AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? 72 : 23);
        }
        textView.layout(viewLeft, viewTop, viewLeft + textView.getMeasuredWidth(), viewTop + textView.getMeasuredHeight());

        if (imageView.getVisibility() == VISIBLE) {
            viewTop = (height - imageView.getMeasuredHeight()) / 2;
            viewLeft = !LocaleController.isRTL ? AndroidUtilities.dp(23) : width - imageView.getMeasuredWidth() - AndroidUtilities.dp(23);
            imageView.layout(viewLeft, viewTop, viewLeft + imageView.getMeasuredWidth(), viewTop + imageView.getMeasuredHeight());
        }

        if (checkBox.getVisibility() == VISIBLE) {
            viewTop = (height - checkBox.getMeasuredHeight()) / 2;
            viewLeft = LocaleController.isRTL ? AndroidUtilities.dp(23) : width - checkBox.getMeasuredWidth() - AndroidUtilities.dp(23);
            checkBox.layout(viewLeft, viewTop, viewLeft + checkBox.getMeasuredWidth(), viewTop + checkBox.getMeasuredHeight());
        }
    }

    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    public void setTextAndValueDrawable(String text, boolean checked, TLRPC.Document document, boolean divider) {
        textView.setText(text);
        checkBox.setChecked(checked, false);
        needDivider = divider;

        String parentObject = "react";
        if (document != null) {
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
            SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document, Theme.key_windowBackgroundGray, 1.0f);
            if (MessageObject.canAutoplayAnimatedSticker(document)) {
                if (svgThumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(document), "80_80", null, svgThumb, parentObject);
                } else if (thumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(document), "80_80", ImageLocation.getForDocument(thumb, document), null, 0, parentObject);
                } else {
                    imageView.setImage(ImageLocation.getForDocument(document), "80_80", null, null, parentObject);
                }
            } else {
                if (svgThumb != null) {
                    if (thumb != null) {
                        imageView.setImage(ImageLocation.getForDocument(thumb, document), null, "webp", svgThumb, parentObject);
                    } else {
                        imageView.setImage(ImageLocation.getForDocument(document), null, "webp", svgThumb, parentObject);
                    }
                } else {
                    imageView.setImage(ImageLocation.getForDocument(thumb, document), null, "webp", null, parentObject);
                }
            }
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        final CharSequence text = textView.getText();
        if (!TextUtils.isEmpty(text)) {
            info.setText(text);
        }
    }

    public void setNeedDivider(boolean needDivider) {
        if (this.needDivider != needDivider) {
            this.needDivider = needDivider;
            setWillNotDraw(!needDivider);
            invalidate();
        }
    }
}
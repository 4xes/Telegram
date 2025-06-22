package org.telegram.ui.profile.section;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.math.MathUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;

@SuppressLint("ViewConstructor")
public class SectionButton extends FrameLayout {

    private final View backgroundView;
    private final ImageView imageView;
    private final TextView textView;
    private final FrameLayout contentView;
    private final RectF rect = new RectF();
    private final int ROUND_CORNERS = AndroidUtilities.dp(10);
    private final int MIN_HEIGHT_HIDE_BACKGROUND = ROUND_CORNERS * 2;
    private final int MIN_HEIGHT_HIDE_AVATAR = MIN_HEIGHT_HIDE_BACKGROUND + AndroidUtilities.dp(8);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Theme.ResourcesProvider resourceProvider;
    private RLottieImageView imageAnimationView;

    @Nullable
    private RLottieDrawable rLottieIcon;

    public SectionButton(@NonNull Context context) {
        super(context);
        setWillNotDraw(false);

        backgroundView = new View(context) {
            @Override
            protected void onDraw(@NonNull Canvas canvas) {
                rect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                canvas.drawRoundRect(rect, ROUND_CORNERS, ROUND_CORNERS, overlayPaint);
                super.onDraw(canvas);
            }
        };
        imageView = new ImageView(context);
        textView = new TextView(context);
        textView.setTextColor(Color.WHITE);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);

        contentView = new FrameLayout(context);
        addView(backgroundView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        addView(contentView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        contentView.addView(imageView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL,0, 8, 0, 0));
        contentView.addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 33, 0, 0));
        setMinimumWidth(AndroidUtilities.dp(100));

        backgroundView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(ROUND_CORNERS, Color.TRANSPARENT, ColorUtils.setAlphaComponent(Color.BLACK, 80)));
        setOnClickListener(v -> {

        });
        overlayPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
        updateStyle();
    }

    public void setResourceProvider(Theme.ResourcesProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    public void setTextAndIcon(CharSequence text, int icon) {
        textView.setText(text);
        imageView.setImageDrawable(ContextCompat.getDrawable(getContext(), icon));
    }

    public void setSection(SectionType section) {
        imageView.setImageDrawable(ContextCompat.getDrawable(getContext(), section.iconRes));
        textView.setText(LocaleController.getString(section.text));

        if (section.rawAnimation != 0) {
//            rLottieIcon = new RLottieDrawable(
//                    section.rawAnimation,
//                    String.valueOf(section.rawAnimation),
//                    AndroidUtilities.dp(56),
//                    AndroidUtilities.dp(56),
//                    false,
//                    null
//            );
        }
        if (section.contentDescription != 0) {
            setContentDescription(LocaleController.getString(section.contentDescription));
        }
    }

    public void setCurrentFrame(int frame, boolean async) {
        if (rLottieIcon != null) {
            rLottieIcon.setCurrentFrame(frame, async);
        }
    }

    public void setCurrentFrame(int frame) {
        if (rLottieIcon != null) {
            rLottieIcon.setCurrentFrame(frame);
        }
    }

    public void setCustomEndFrame(int frame) {
        if (rLottieIcon != null) {
            rLottieIcon.setCustomEndFrame(frame);
        }
    }

    public void updateStyle() {
        overlayPaint.setColor(0xFF6A6969);
    }

    public void playAnimation() {
        if (imageAnimationView != null) {
            imageAnimationView.playAnimation();
        }
    }

    /** @noinspection SuspiciousNameCombination*/
    public void updateState(final boolean isEnabled, final int height) {
        float hideAvatarHeight = MathUtils.clamp(height, MIN_HEIGHT_HIDE_AVATAR, getMeasuredHeight());
        float hideBackgroundHeight = MathUtils.clamp(height, MIN_HEIGHT_HIDE_BACKGROUND, getMeasuredHeight());
        float contentAlpha = AndroidUtilities.ilerp(hideAvatarHeight, MIN_HEIGHT_HIDE_AVATAR, getMeasuredHeight());
        float backgroundAlpha = AndroidUtilities.ilerp(hideBackgroundHeight, MIN_HEIGHT_HIDE_BACKGROUND, getMeasuredHeight());

        if (getMeasuredHeight() == 0) {
            return;
        }
        float scale = hideBackgroundHeight / getMeasuredHeight();

        contentView.setAlpha(contentAlpha);
        contentView.setScaleX(scale);
        contentView.setScaleY(scale);
        backgroundView.setScaleY(scale);
        overlayPaint.setAlpha((int) (backgroundAlpha * 255f));
        setEnabled(isEnabled);
        backgroundView.invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        contentView.setPivotX(getMeasuredWidth() / 2f);
        contentView.setPivotY(getMeasuredHeight() / 2f);
    }

    public int getThemedColor(int key) {
        return Theme.getColor(key, resourceProvider);
    }

}
package org.telegram.ui.Transitions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Animations.AnimationManager;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.RecyclerListView;

import static org.telegram.ui.ActionBar.Theme.key_chat_outBubble;

public class BaseMessageTransition {

    float progress;
    float yProgress;
    float xProgress;
    float colorProgress;
    float alphaProgress;

    float messageX;
    float messageY;

    private final ValueAnimator animator;

    final View view;
    final FrameLayout containerView;
    final ChatMessageCell messageView;
    final ChatActivityEnterView enterView;
    final RecyclerListView listView;
    final int messageId;

    Paint messagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BaseMessageTransition(FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView chatActivityEnterView, RecyclerListView listView) {
        this.containerView = containerView;
        this.messageView = messageView;
        this.enterView = chatActivityEnterView;
        this.listView = listView;
        messageId = messageView.getMessageObject().stableId;
        messageView.setEnterTransition(this);
        messageView.setVisibility(View.INVISIBLE);


        view = new View(containerView.getContext()) {
            @Override
            protected void onDraw(Canvas canvas) {
                int translateSave = canvas.save();
                canvas.translate(-getX(), -getY());
                yProgress = CubicBezierInterpolator.DEFAULT.getInterpolation(progress);
                colorProgress = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(progress);
                alphaProgress = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(progress);
                xProgress = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(progress);

                if (messageView.getMessageObject().stableId == messageId) {
                    messageX = messageView.getX() + listView.getX();
                    messageY = messageView.getY() + listView.getY();
                }
                animationDraw(canvas);
                canvas.restoreToCount(translateSave);
            }
        };
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        containerView.addView(view);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(valueAnimator -> {
            if (messageView.getMessageObject().stableId != messageId) {
                stop();
            } else {
                progress = (float) valueAnimator.getAnimatedValue();
                view.invalidate();
            }
        });

        long duration = AnimationManager.getInstance().getDuration(AnimationType.Voice, null);

        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(duration);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (view.getParent() != null) {
                    release();
                }
            }
        });
    }

    public void animationDraw(Canvas canvas) {

    }

    public void stop() {
        if (animator.isRunning()) {
            animator.cancel();
        }
        release();
    }

    public void release() {
        if (messageView.getEnterTransition() == this) {
            messageView.setEnterTransition(null);
            messageView.setVisibility(View.VISIBLE);
        }
        containerView.removeView(view);
    }

    public void start() {
        animator.start();
    }

    RectF backgroundRectStart = new RectF();
    RectF backgroundRectEnd = new RectF();
    Rect backgroundRect = new Rect();

    float backgroundScaleX;
    float backgroundScaleY;
    float shiftY;
    float shiftX;
    protected void animateBackground(Canvas canvas) {
        messageView.updateBackgroundState();

        backgroundRectStart.set(
                enterView.getLeft(),
                enterView.getTop(),
                enterView.getRight(),
                enterView.getBottom());

        if (messageView.getMessageObject().stableId == messageId) {
            backgroundRectEnd.set(
                    messageView.getBackgroundDrawableLeft() + messageX,
                    messageView.getBackgroundDrawableTop() + messageY,
                    messageView.getBackgroundDrawableRight() + messageX,
                    messageView.getBackgroundDrawableBottom() + messageY
            );
            evaluate(backgroundRect, yProgress, backgroundRectStart, backgroundRectEnd);
        }


        backgroundScaleX = backgroundRectEnd.width() / backgroundRect.width();
        backgroundScaleY = backgroundRectEnd.height() / backgroundRect.height();

        Theme.MessageDrawable messageDrawable = messageView.getCurrentBackgroundDrawable();
        messageDrawable.setBounds(backgroundRect);

        int startColor = Theme.getColor(Theme.key_chat_messagePanelBackground);
        int endColor = Theme.getColor(key_chat_outBubble);

        float hideShadowProgress = 0.4f;
        if (yProgress > hideShadowProgress) {
            messageDrawable.draw(canvas);
        }
        messagePaint.setColor(evaluateColor(yProgress, startColor, endColor));
        messageDrawable.draw(canvas, messagePaint);

//        messagePaint.setColor(0x50ff00ff);
//        canvas.drawRect(
//                messageView.getLeft() + messageX - messageView.getX(),
//                messageView.getTop() + messageY - messageView.getY(),
//                messageView.getRight() + messageX - messageView.getX(),
//                messageView.getBottom()+ messageY - messageView.getY(),
//                messagePaint
//        );
//
//        canvas.drawRect(
//                backgroundRect,
//                messagePaint
//        );

        canvas.save();
        shiftY = (backgroundRect.height() - backgroundRectEnd.height());
        shiftX = (backgroundRect.width() - backgroundRectEnd.width());
        canvas.translate(messageX, backgroundRect.top + shiftY);
        messageView.drawTime(canvas, 1f * alphaProgress, false);
        canvas.restore();
    }

    protected final int[] location = new int[2];
    protected final int[] locationTemp = new int[2];

    protected void location(View parent, View child) {
        child.getLocationOnScreen(location);
        parent.getLocationOnScreen(locationTemp);
        location[0]-= locationTemp[0];
        location[1]-= locationTemp[1];
    }

    protected Rect evaluate(Rect rect, float fraction, Rect startValue, Rect endValue) {
        int left = startValue.left + (int) ((endValue.left - startValue.left) * fraction);
        int top = startValue.top + (int) ((endValue.top - startValue.top) * fraction);
        int right = startValue.right + (int) ((endValue.right - startValue.right) * fraction);
        int bottom = startValue.bottom + (int) ((endValue.bottom - startValue.bottom) * fraction);
        if (rect == null) {
            return new Rect(left, top, right, bottom);
        } else {
            rect.set(left, top, right, bottom);
            return rect;
        }
    }

    protected Rect evaluate(Rect rect, float fraction, RectF startValue, RectF endValue) {
        int left = (int) (startValue.left +  ((endValue.left - startValue.left) * fraction));
        int top = (int) (startValue.top + (int) ((endValue.top - startValue.top) * fraction));
        int right = (int)(startValue.right + (int) ((endValue.right - startValue.right) * fraction));
        int bottom = (int)(startValue.bottom + (int) ((endValue.bottom - startValue.bottom) * fraction));
        if (rect == null) {
            return new Rect(left, top, right, bottom);
        } else {
            rect.set(left, top, right, bottom);
            return rect;
        }
    }

    protected RectF evaluate(RectF rect, float fraction, RectF startValue, RectF endValue) {
        float left = startValue.left + ((endValue.left - startValue.left) * fraction);
        float top =  startValue.top + ((endValue.top - startValue.top) * fraction);
        float right = startValue.right + ((endValue.right - startValue.right) * fraction);
        float bottom = startValue.bottom +  ((endValue.bottom - startValue.bottom) * fraction);
        if (rect == null) {
            return new RectF(left, top, right, bottom);
        } else {
            rect.set(left, top, right, bottom);
            return rect;
        }
    }

    protected Float evaluate(float fraction, float startValue, float endValue) {
        return startValue + fraction * (endValue - startValue);
    }

    protected int evaluateColor(float fraction, int startValue, int endValue) {
        return ColorUtils.blendARGB(startValue, endValue, fraction);
    }
}

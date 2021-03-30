package org.telegram.ui.Transitions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.RecyclerListView;

public class BaseMessageTransition {

    float animatorProgress;
    float progress;
    float xProgress;

    float offsetX;
    float offsetY;

    private final ValueAnimator animator;

    final View view;
    final FrameLayout containerView;
    final ChatMessageCell messageView;
    final ChatActivityEnterView enterView;
    final RecyclerListView listView;
    final int messageId;

    public BaseMessageTransition(FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView chatActivityEnterView, RecyclerListView listView) {
        this.containerView = containerView;
        this.messageView = messageView;
        this.enterView = chatActivityEnterView;
        this.listView = listView;

        messageView.setTransitionInProgress(true);
        messageView.setVisibility(View.INVISIBLE);
        messageId = messageView.getMessageObject().stableId;

        view = new View(containerView.getContext()) {
            @Override
            protected void onDraw(Canvas canvas) {
                int translateSave = canvas.save();
                canvas.translate(-getX(), -getY());
                progress = CubicBezierInterpolator.DEFAULT.getInterpolation(animatorProgress);
                xProgress = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(animatorProgress);

                if (messageView.getMessageObject().stableId == messageId) {
                    offsetX = messageView.getX() + listView.getX();
                    offsetY = messageView.getY() + listView.getY();
                }
                animationDraw(canvas);
                canvas.restoreToCount(translateSave);
            }
        };
        containerView.addView(view);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(valueAnimator -> {
            animatorProgress = (float) valueAnimator.getAnimatedValue();
            view.invalidate();
        });

        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(10000);
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

    public void release() {
        messageView.setTransitionInProgress(false);
        messageView.setVisibility(View.VISIBLE);
        containerView.removeView(view);
    }

    public void start() {
        animator.start();
    }

    Rect enterRect = new Rect();
    Rect messageRect = new Rect();
    Rect drawRect = new Rect();

    protected void animateBackground(Canvas canvas) {
        enterRect.set(
                (int) (enterView.getLeft()),
                (int) (enterView.getTop()),
                (int) (enterView.getRight()),
                (int) (enterView.getBottom()));

        if (messageView.getMessageObject().stableId == messageId) {
            messageRect.set(
                    (int) (messageView.getBackgroundDrawableLeft() + offsetX),
                    (int) (messageView.getBackgroundDrawableTop() + offsetY),
                    (int) (messageView.getBackgroundDrawableRight() + offsetX),
                    (int) (messageView.getBackgroundDrawableBottom() + offsetY)
            );
        }

        evaluate(drawRect, progress, enterRect, messageRect);

        Theme.MessageDrawable messageDrawable = Theme.chat_msgOutDrawable;
        messageDrawable.setBounds(drawRect);
        messageDrawable.draw(canvas);
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

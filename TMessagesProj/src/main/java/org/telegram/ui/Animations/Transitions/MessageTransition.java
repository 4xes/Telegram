package org.telegram.ui.Animations.Transitions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.collection.SparseArrayCompat;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Animations.AnimationManager;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Animations.Parameter;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

import static org.telegram.ui.ActionBar.Theme.key_chat_outBubble;

public abstract class MessageTransition {

    float progress;

    float yProgress;
    float xProgress;
    float bubbleProgress;
    float scaleProgress;
    float colorProgress;
    float timeProgress;

    float messageX;
    float messageY;

    float editX;
    float editY;

    protected Animator animator;
    protected View view;

    final FrameLayout containerView;
    final ChatMessageCell messageView;
    final ChatActivityEnterView enterView;
    final RecyclerListView listView;
    final ActionBar actionBar;
    final int messageId;

    Paint messagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    SparseArrayCompat<Interpolator> interpolators = new SparseArrayCompat<>(10);

    protected float interpolate(Parameter parameter, float progress) {
        int key = parameter.ordinal();
        Interpolator interpolator = interpolators.get(key);
        if (interpolator == null) {
            interpolator = AnimationManager.getInstance().getInterpolator(getAnimationType(), parameter);
            interpolators.put(key, interpolator);
        }
        return interpolator.getInterpolation(progress);
    }

    public void setProgresses() {
        xProgress = interpolate(Parameter.X, progress);
        yProgress = interpolate(Parameter.Y, progress);
        bubbleProgress = interpolate(Parameter.Bubble, progress);
        scaleProgress = interpolate(Parameter.Scale, progress);
        colorProgress = interpolate(Parameter.Color, progress);
        timeProgress = interpolate(Parameter.TimeAppears, progress);
    }

    public MessageTransition(ActionBar actionBar, FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView chatActivityEnterView, RecyclerListView listView) {
        this.actionBar = actionBar;
        this.containerView = containerView;
        this.messageView = messageView;
        this.enterView = chatActivityEnterView;
        this.listView = listView;

        location(enterView, enterView.getEditField());
        editX = location[0];
        editY = location[1];

        messageId = messageView.getMessageObject().stableId;
        messageView.setEnterTransition(this);
        messageView.setVisibility(View.INVISIBLE);

        view = new View(containerView.getContext()) {
            @Override
            protected void onDraw(Canvas canvas) {
                int translateSave = canvas.save();
                canvas.translate(-getX(), -getY());
                canvas.clipRect(0, actionBar.getY() + actionBar.getMeasuredHeight(), containerView.getMeasuredWidth(), containerView.getMeasuredHeight());
                if (messageView.getMessageObject().stableId == messageId) {
                    messageX = messageView.getX() + listView.getX();
                    messageY = messageView.getY() + listView.getY();
                    if (enterView.isStickersExpanded()) {
                        return;
                    }
                    animationDraw(canvas);
                }
                canvas.restoreToCount(translateSave);
            }
        };
        //view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        containerView.addView(view);
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

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(valueAnimator -> {
            if (messageView.getMessageObject().stableId != messageId) {
                stop();
            } else {
                progress = (float) valueAnimator.getAnimatedValue();
                setProgresses();
                view.invalidate();
            }
        });

        long duration = AnimationManager.getInstance().getDuration(getAnimationType(), null);

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
        this.animator = animator;
        animator.start();
    }

    RectF startRect = new RectF();
    RectF endRect = new RectF();
    RectF currentRect = new RectF();

    float backgroundScaleX;
    float backgroundScaleY;
    float shiftY;
    float shiftX;

    public void setBubbleRectEnd() {
        messageView.updateBackgroundState();


        if (messageView.getMessageObject().stableId == messageId) {
            endRect.set(
                    messageView.getBackgroundDrawableLeft() + messageX,
                    messageView.getBackgroundDrawableTop() + messageY,
                    messageView.getBackgroundDrawableRight() + messageX,
                    messageView.getBackgroundDrawableBottom() + messageY
            );
        }

    }

    public void setStartEnterEnter() {
        startRect.set(
                enterView.getLeft(),
                enterView.getTop(),
                enterView.getRight(),
                enterView.getBottom());
    }


    RectF editBounds = new RectF();
    RectF enterViewBounds = new RectF();
    float editPaddingVertical = AndroidUtilities.dp(11);

    public void setEditRect() {
        float editTextY = enterView.getY() + editY;
        float editTextX = enterView.getX() + editX;
        editBounds.set(
                editTextX,
                editTextY,
                editTextX + enterView.getEditField().getMeasuredWidth(),
                editTextY + enterView.getEditField().getMeasuredHeight());

    }

    public void setEnterViewRect() {
        float editTextY = enterView.getY();
        float editTextX = enterView.getX();
        enterViewBounds.set(
                editTextX,
                editTextY,
                enterView.getMeasuredHeight(),
                enterView.getMeasuredWidth());
    }

    Path path;
    Rect temp = new Rect();

    protected void animateBackground(Canvas canvas, float showShadowProgress) {
        evaluate(currentRect, bubbleProgress, bubbleProgress, startRect, endRect);
        backgroundScaleX = endRect.width() / currentRect.width();
        backgroundScaleY = endRect.height() / currentRect.height();

        Theme.MessageDrawable messageDrawable = messageView.getCurrentBackgroundDrawable();
        messageDrawable.setBounds( (int) currentRect.left, (int) currentRect.top, (int) currentRect.right, (int)  currentRect.bottom);

        int startColor = Theme.getColor(Theme.key_chat_messagePanelBackground);
        int endColor = Theme.getColor(key_chat_outBubble);

        if (yProgress > showShadowProgress) {
            messageDrawable.draw(canvas);
        }
        messagePaint.setColor(evaluateColor(colorProgress, startColor, endColor));
        temp.set((int) currentRect.left, (int) currentRect.top, (int) currentRect.right, (int) currentRect.bottom);

        path = messageDrawable.getPath(temp, messagePaint);
        canvas.drawPath(path, messagePaint);


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
    }

    protected void drawTime(Canvas canvas, float x, float y) {
        int timeSave = canvas.save();
        canvas.translate(x, y);
        messageView.drawTime(canvas, 1f * timeProgress, false);
        canvas.restoreToCount(timeSave);
    }

    protected void drawTime(Canvas canvas) {
        drawTime(canvas, messageX, messageY);
    }

    protected final int[] location = new int[2];
    protected final int[] locationTemp = new int[2];

    protected void location(View parent, View child) {
        child.getLocationOnScreen(location);
        parent.getLocationOnScreen(locationTemp);
        location[0]-= locationTemp[0];
        location[1]-= locationTemp[1];
    }

    protected RectF evaluate(RectF rect, float fractionX, float fractionY, RectF startValue, RectF endValue) {
        float left = (startValue.left +  ((endValue.left - startValue.left) * fractionX));
        float top = (startValue.top + ((endValue.top - startValue.top) * fractionY));
        float right = (startValue.right +  ((endValue.right - startValue.right) * fractionX));
        float bottom = (startValue.bottom + ((endValue.bottom - startValue.bottom) * fractionY));
        if (rect == null) {
            return new RectF(left, top, right, bottom);
        } else {
            rect.set(left, top, right, bottom);
            return rect;
        }
    }

    protected RectF evaluate(RectF rect, float fractionX, float fractionY, float fractionScale, RectF startValue, RectF endValue) {
        float left = evaluate(fractionX, startValue.left, endValue.left);
        float top = evaluate(fractionY, startValue.top, endValue.top);
        float width = evaluate(fractionScale, startValue.width(), endValue.width());
        float height = evaluate(fractionScale, startValue.height(), endValue.height());
        float right = left + width;
        float bottom = top + height;
        if (rect == null) {
            return new RectF(left, top, right, bottom);
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

    public int evaluateColor(float fraction, int startValue, int endValue) {
        return ColorUtils.blendARGB(startValue, endValue, fraction);
    }

    protected abstract AnimationType getAnimationType();

    public interface OnPreDrawCallback {
        void onPreDraw();
    }
}

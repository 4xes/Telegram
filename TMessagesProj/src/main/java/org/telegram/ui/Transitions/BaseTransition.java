package org.telegram.ui.Transitions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

public class BaseTransition {

    float progress;

    private final ValueAnimator animator;

    final FrameLayout containerView;
    final ChatMessageCell messageView;
    final ChatActivityEnterView chatActivityEnterView;
    final RecyclerListView listView;
    final int messageId;

    public BaseTransition(FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView chatActivityEnterView, RecyclerListView listView) {
        this.containerView = containerView;
        this.messageView = messageView;
        this.chatActivityEnterView = chatActivityEnterView;
        this.listView = listView;

        messageView.setVisibility(View.INVISIBLE);
        messageId = messageView.getMessageObject().stableId;

        View view = new View(containerView.getContext()) {
            @Override
            protected void onDraw(Canvas canvas) {
                int translateSave = canvas.save();
                canvas.translate(-getX(), -getY());
                animationDraw(canvas);
                canvas.restoreToCount(translateSave);
            }
        };

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(valueAnimator -> {
            progress = (float) valueAnimator.getAnimatedValue();
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
        messageView.setVisibility(View.VISIBLE);
    }

    public void start() {

    }

    private final int[] location = new int[2];
    private final int[] locationTemp = new int[2];

    private void relativeRect(View parent, View child) {
        child.getLocationOnScreen(location);
        parent.getLocationOnScreen(locationTemp);
        location[0]-= locationTemp[0];
        location[1]-= locationTemp[1];
    }
}

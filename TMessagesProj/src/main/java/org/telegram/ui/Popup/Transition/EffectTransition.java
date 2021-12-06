package org.telegram.ui.Popup.Transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.recyclerview.widget.ChatListItemAnimator;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.MessageEnterTransitionContainer;

public abstract class EffectTransition implements MessageEnterTransitionContainer.Transition {

    float progress;

    public final ValueAnimator animator;
    public final MessageEnterTransitionContainer container;
    public final RecyclerListView listView;
    ChatActivity chatActivity;
    ChatActivityEnterView enterView;
    public final ChatMessageCell messageView;
    MessageObject currentMessageObject;
    private final int messageId;

    float lastMessageX;
    float lastMessageY;

    Paint debugPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public EffectTransition(MessageEnterTransitionContainer container, ChatActivity chatActivity, RecyclerListView listView, ChatMessageCell cell) {
        this.container = container;
        this.messageView = cell;
        this.currentMessageObject = cell.getMessageObject();
        this.listView = listView;
        this.chatActivity = chatActivity;
        this.enterView = chatActivity.getChatActivityEnterView();
        this.messageId = messageView.getMessageObject().stableId;

        debugPaint.setStyle(Paint.Style.FILL);
        debugPaint.setColor(0x44ff00ff);

        animator = ValueAnimator.ofFloat(0f, 1.0f);

        this.container.addTransition(this);
        animator.addUpdateListener(valueAnimator -> {
            progress = (float) valueAnimator.getAnimatedValue();
            container.invalidate();
        });

        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(duration());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                release();
                container.removeTransition(EffectTransition.this);
            }
        });
    }

    public long delay() {
        return 0L;
    }

    public void start() {
        if (animator != null) {
            animator.start();
        }
    }

    public long duration() {
        return 250;
    }

    abstract void release();

    @Override
    public void onDraw(Canvas canvas) {
        float messageViewX;
        float messageViewY;

        if (messageView.getMessageObject().stableId != messageId) {
            return;
        } else {
            messageViewX = messageView.getX() + listView.getX() - container.getX();
            messageViewY = messageView.getTop() + listView.getTop() - container.getY();
            messageViewY += enterView.getTopViewHeight();

            lastMessageX = messageViewX;
            lastMessageY = messageViewY;
        }

        float progress = ChatListItemAnimator.DEFAULT_INTERPOLATOR.getInterpolation(this.progress);

        onDraw(canvas, progress);
    }

    abstract void onDraw(Canvas canvas, float progress);

    protected final int[] location = new int[2];
    protected final int[] locationTemp = new int[2];

    protected void location(View parent, View child) {
        child.getLocationOnScreen(location);
        parent.getLocationOnScreen(locationTemp);
        location[0]-= locationTemp[0];
        location[1]-= locationTemp[1];
    }

    public float evaluate(float start, float end, float progress) {
        return start + ((end - start) * progress);
    }
}
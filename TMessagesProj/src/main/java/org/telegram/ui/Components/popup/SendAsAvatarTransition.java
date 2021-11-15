package org.telegram.ui.Components.popup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageReceiver;
import org.telegram.ui.MessageEnterTransitionContainer;

public class SendAsAvatarTransition implements MessageEnterTransitionContainer.Transition {
    private final SendAsPeerButton button;
    private final SendAsPeerCell cell;
    float fromRadius;
    float toRadius;
    float progress;

    private final ValueAnimator animator;
    MessageEnterTransitionContainer container;

    private final float cellAvatarCenterX;
    private final float cellAvatarCenterY;

    int overshootDp = AndroidUtilities.dp(6);

    public SendAsAvatarTransition(SendAsPeerCell cell, SendAsPeerButton button, MessageEnterTransitionContainer container) {
        this.container = container;
        this.button = button;
        this.cell = cell;

        fromRadius = cell.getAvatarRadius();
        toRadius = button.getAvatarRadius();

        button.setDialogId(cell.getCurrentDialog());
        cell.getAvatarImageView().setVisibility(View.INVISIBLE);
        button.setVisibility(View.INVISIBLE);
        button.setEnterTransitionInProgress(true);

        container.addTransition(this);

        location(container, cell.getAvatarImageView());

        float avatarLocationX = location[0];
        float avatarLocationY = location[1];


        cellAvatarCenterX = avatarLocationX + cell.getAvatarRect().centerX();
        cellAvatarCenterY = avatarLocationY + cell.getAvatarRect().centerY();


        animator = ValueAnimator.ofFloat(0f, 1.1f);
        animator.addUpdateListener(valueAnimator -> {
            progress = (float) valueAnimator.getAnimatedValue();
            container.invalidate();
        });

        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(250);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                button.resetDrawRect();
                button.setEnterTransitionInProgress(false);
                button.toAvatarAnimation(false);
                button.setVisibility(View.VISIBLE);
                button.invalidate();
                container.removeTransition(SendAsAvatarTransition.this);
            }
        });
    }

    public void start() {
        animator.start();
    }

    @Override
    public void onDraw(Canvas canvas) {
        location(container, button);


        float toCenterX = location[0] + button.getRectDrawing().centerX();
        float toCenterY = location[1] + button.getRectDrawing().centerY();

        float toOvershootY = toCenterY + overshootDp;

        float progress = Math.min(this.progress, 1f);
        if (this.progress > 1f) {
            button.resetDrawRect();
            button.setEnterTransitionInProgress(false);
            button.toAvatarAnimation(false);
            button.setVisibility(View.VISIBLE);
            return;
        }

        float avatarProgress = progress;
        float avatarProgressX;
        float y;
        if (avatarProgress <= 0.7) {
            avatarProgress = avatarProgress / 0.7f;
            y = cellAvatarCenterY + ((toOvershootY - cellAvatarCenterY) * avatarProgress);
            avatarProgressX = avatarProgress;
        } else {
            avatarProgress = ((avatarProgress - 0.7f) / 0.3f);
            y = toOvershootY + ((toCenterY - toOvershootY) * avatarProgress);
            avatarProgressX = 1f;
        }

        float scaleAvatarProgress = avatarProgressX;

        float x = cellAvatarCenterX + ((toCenterX - cellAvatarCenterX) * avatarProgressX);

        float avatarRadius = cell.getAvatarRadius() + ((button.getAvatarRadius() - cell.getAvatarRadius()) * scaleAvatarProgress);


        int restoreButtonFade = canvas.save();
        canvas.translate(location[0], location[1]);
        float fadeProgress = 1f - progress;
        button.setTransitionProgress(fadeProgress);
        button.draw(canvas);
        canvas.restoreToCount(restoreButtonFade);

        ImageReceiver imageReceiver = button.getImageReceiver();
        imageReceiver.setImageCoords((int) (x - avatarRadius), (int) (y - avatarRadius), (int) (avatarRadius * 2f), (int) (avatarRadius * 2f));
        imageReceiver.draw(canvas);
    }

    protected final int[] location = new int[2];
    protected final int[] locationTemp = new int[2];

    protected void location(View parent, View child) {
        child.getLocationOnScreen(location);
        parent.getLocationOnScreen(locationTemp);
        location[0]-= locationTemp[0];
        location[1]-= locationTemp[1];
    }
}

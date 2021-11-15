package org.telegram.ui.Components.popup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;

import android.graphics.Paint;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageReceiver;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.MessageEnterTransitionContainer;

public class SendAsAvatarTransition implements MessageEnterTransitionContainer.Transition {
    private final SendAsPeerButton button;
    private final SendAsPeerCell cell;
    float fromRadius;
    float toRadius;
    float progress;

    private final ValueAnimator animator;
    MessageEnterTransitionContainer container;
    private final Theme.ResourcesProvider resourcesProvider;

    private final Paint paint = new Paint();

    private float cellAvatarCenterX;
    private float cellAvatarCenterY;

    private Interpolator interpolator = new DecelerateInterpolator();

    int overshootDp = AndroidUtilities.dp(6);

    public SendAsAvatarTransition(SendAsPeerCell cell, SendAsPeerButton button, MessageEnterTransitionContainer container, Theme.ResourcesProvider resourcesProvider) {
        this.resourcesProvider = resourcesProvider;
        this.container = container;
        this.button = button;
        this.cell = cell;

        fromRadius = cell.getAvatarRadius();
        toRadius = button.getAvatarRadius();

        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);

        button.setEnterTransitionInProgress(true);

        container.addTransition(this);

        location(container, cell.getAvatarImageView());

        float avatarLocationX = location[0];
        float avatarLocationY = location[1];


        cellAvatarCenterX = avatarLocationX + cell.getAvatarRect().centerX();
        cellAvatarCenterY = avatarLocationY + cell.getAvatarRect().centerY();

        ImageReceiver imageReceiver = cell.getAvatarImageView().getImageReceiver();
        float saveX = imageReceiver.getImageX();
        float saveY = imageReceiver.getImageY();
        float saveWidth = imageReceiver.getImageWidth();
        float saveHeight = imageReceiver.getImageHeight();

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(valueAnimator -> {
            progress = (float) valueAnimator.getAnimatedValue();
            container.invalidate();
        });
        button.setDialogId(cell.getCurrentDialog());

        cell.getAvatarImageView().setVisibility(View.INVISIBLE);
        button.setVisibility(View.INVISIBLE);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(220);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                container.removeTransition(SendAsAvatarTransition.this);
                button.setEnterTransitionInProgress(false);
                button.toAvatarAnimation(false);
                button.setVisibility(View.VISIBLE);
                imageReceiver.setImageCoords(saveX, saveY, saveWidth, saveHeight);
                cell.getAvatarImageView().setVisibility(View.VISIBLE);
                button.invalidate();
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

        float avatarProgress = interpolator.getInterpolation(progress);
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

        int avatarSave = canvas.save();
        float avatarRadius = cell.getAvatarRadius() + ((button.getAvatarRadius() - cell.getAvatarRadius()) * scaleAvatarProgress);

        ImageReceiver imageReceiver = cell.getAvatarImageView().getImageReceiver();

        imageReceiver.setImageCoords(x - avatarRadius, y - avatarRadius, avatarRadius * 2f, avatarRadius * 2f);
        imageReceiver.draw(canvas);

        canvas.restoreToCount(avatarSave);

        int restoreButtonFade = canvas.save();
        canvas.translate(location[0], location[1]);
        float fadeProgress = 1f - progress;
        button.setTransitionProgress(fadeProgress);
        button.draw(canvas);
        canvas.restoreToCount(restoreButtonFade);

    }

    protected final int[] location = new int[2];
    protected final int[] locationTemp = new int[2];

    protected void location(View parent, View child) {
        child.getLocationOnScreen(location);
        parent.getLocationOnScreen(locationTemp);
        location[0]-= locationTemp[0];
        location[1]-= locationTemp[1];
    }

    private int getThemedColor(String key) {
        Integer color = resourcesProvider != null ? resourcesProvider.getColor(key) : null;
        return color != null ? color : Theme.getColor(key);
    }
}

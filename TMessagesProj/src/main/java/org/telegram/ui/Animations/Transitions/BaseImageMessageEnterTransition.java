package org.telegram.ui.Animations.Transitions;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageReceiver;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ChatActivityEnterTopView;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RecyclerListView;

public abstract class BaseImageMessageEnterTransition extends MessageTransition {

    final ImageView replyIcon;
    final ChatActivityEnterTopView topView;

    boolean hasReply = false;

    public BaseImageMessageEnterTransition(ActionBar actionBar, FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView enterView, RecyclerListView listView, ChatActivity chatActivity) {
        super(actionBar, containerView, messageView, enterView, listView);
        replyIcon = chatActivity.replyIconImageView;
        topView = chatActivity.chatActivityEnterTopView;
        hasReply = messageView.getMessageObject().replyMessageObject != null;
        location(enterView, enterView.getEditField());
        ImageLoader.getInstance().loadImageForImageReceiver(messageView.getPhotoImage());
    }

    boolean restarted = false;
    boolean stopped = false;

    abstract void setStartImage();

    boolean positionByCenter = false;

    @Override
    public void animationDraw(Canvas canvas) {
        ImageReceiver image = messageView.getPhotoImage();
        if (image == null) {
            return;
        }
        Drawable drawable = image.getDrawable();
        if (drawable instanceof RLottieDrawable) {
            RLottieDrawable lottieDrawable = (RLottieDrawable) image.getDrawable();

            if (!stopped && lottieDrawable.isRunning()) {
                lottieDrawable.stop();
                stopped = true;
            }
            if (!restarted && scaleProgress == 1f ) {
                lottieDrawable.restart();
                restarted = true;
            }
        }

        float tempX = image.getImageX();
        float tempY = image.getImageY();
        float tempWidth = image.getImageWidth();
        float tempHeight = image.getImageHeight();

        setStartImage();

        endRect.set(
                messageX + tempX,
                messageY + tempY,
                messageX + tempX + tempWidth,
                messageY + tempY + tempHeight
        );

        float currentWidth = evaluate(scaleProgress, startRect.width(), endRect.width());
        float currentHeight = evaluate(scaleProgress, startRect.height(), endRect.height());

        if (positionByCenter) {
            float centerX = evaluate(xProgress, startRect.centerX(), endRect.centerX());
            float centerY = evaluate(yProgress, startRect.centerY(), endRect.centerY());

            currentRect.set(
                    centerX - currentWidth / 2f,
                    centerY - currentHeight / 2f,
                    centerX + currentWidth / 2f,
                    centerY + currentHeight / 2f);
        } else {
            float leftX = evaluate(xProgress, startRect.left, endRect.left);
            float topY = evaluate(yProgress, startRect.top, endRect.top);

            currentRect.set(
                    leftX,
                    topY,
                    leftX + currentWidth,
                    topY + currentHeight);
        }

        image.setImageCoords(currentRect);
        image.setCurrentAlpha(1f);
        image.draw(canvas);

        // reset image
        image.setImageCoords(tempX, tempY, tempWidth, tempHeight);
        if (drawable instanceof RLottieDrawable) {
            image.setCurrentAlpha(1f);
        }
    }

    RectF replyRect = new RectF();
    RectF startIcon = new RectF();

    protected void drawReplySticker(Canvas canvas) {
        if (hasReply) {
            messageView.updateReplayPositions();
            float endX = messageX;
            float endY = messageY;

            float replayStartX = AndroidUtilities.dp(23);
            float replayStartY = AndroidUtilities.dp(12);
            float startX = topView.getX() + replayStartX - AndroidUtilities.dp(5);
            location(containerView, topView);
            float clipTop = location[1];
            float startY = listView.getY() + listView.getMeasuredHeight() - topView.getTranslationY() - AndroidUtilities.dp(6);

            float currentX = evaluate(xProgress, startX, endX);
            float currentY = evaluate(yProgress, startY, endY);
            replyIcon.draw(canvas);

            int backWidth = Math.max(messageView.replyNameWidth, messageView.replyTextWidth) + AndroidUtilities.dp(14);
            replyRect.set( currentX + replayStartX - AndroidUtilities.dp(7),
                    currentY + replayStartY - AndroidUtilities.dp(6),
                    currentX + replayStartX - AndroidUtilities.dp(7) + backWidth,
                    currentY + replayStartY + AndroidUtilities.dp(41)
            );

            startIcon.left = replayStartX - AndroidUtilities.dp(7);
            startIcon.top = replayStartY - AndroidUtilities.dp(6);
            startIcon.right = startIcon.left + replyIcon.getMeasuredWidth();
            startIcon.bottom = startIcon.top + replyIcon.getMeasuredHeight();

            Theme.chat_systemDrawable.setColorFilter(Theme.colorFilter);
            Theme.chat_systemDrawable.setBounds((int) replyRect.left,(int) replyRect.top, (int) replyRect.right, (int) replyRect.bottom);
            int saveClip = Integer.MIN_VALUE;
            if (replyRect.bottom > clipTop) {
                saveClip = canvas.save();
                canvas.clipRect(replyRect.left, clipTop - replyRect.height(), replyRect.right, clipTop);
            }
            Theme.chat_systemDrawable.draw(canvas);

            if (saveClip != Integer.MIN_VALUE) {
                canvas.restoreToCount(saveClip);
            }
            canvas.save();
            canvas.translate(currentX, currentY);
//            messagePaint.setColor(Color.RED);
//            canvas.drawRect(startIcon, messagePaint);
            messageView.getTransitionParams().ignoreBackground = true;
            messageView.getTransitionParams().myTransition = true;
            messageView.getTransitionParams().myReplyProgress = colorProgress;
            messageView.drawNamesLayout(canvas, 1f);

            messageView.getTransitionParams().ignoreBackground = false;
            messageView.getTransitionParams().myTransition = false;
            canvas.restore();
        }
    }

    @Override
    public void release() {
        super.release();
        if (!restarted) {
            ImageReceiver image = messageView.getPhotoImage();
            if (image == null) {
                return;
            }
            Drawable drawable = image.getDrawable();
            if (drawable instanceof RLottieDrawable) {
                RLottieDrawable lottieDrawable = (RLottieDrawable) image.getDrawable();
                lottieDrawable.restart();
            }
        }
    }
}
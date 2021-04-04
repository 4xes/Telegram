package org.telegram.ui.Animations.Transitions;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;

import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageReceiver;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RecyclerListView;

public abstract class BaseImageMessageEnterTransition extends MessageTransition {


    public BaseImageMessageEnterTransition(FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView enterView, RecyclerListView listView) {
        super(containerView, messageView, enterView, listView);
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
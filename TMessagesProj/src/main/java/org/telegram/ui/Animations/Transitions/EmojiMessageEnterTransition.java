package org.telegram.ui.Animations.Transitions;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageReceiver;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RecyclerListView;

public class EmojiMessageEnterTransition extends BaseMessageTransition {

    float editX;
    float editY;

    public EmojiMessageEnterTransition(FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView enterView, RecyclerListView listView) {
        super(containerView, messageView, enterView, listView);
        location(enterView, enterView.getEditField());
        ImageLoader.getInstance().loadImageForImageReceiver(messageView.getPhotoImage());
        editX = location[0];
        editY = location[1];
        drawable = (RLottieDrawable) messageView.getPhotoImage().getLottieAnimation();
        Log.e("drawable", "drawable: " + drawable);
    }

    final float emojiTextSize = AndroidUtilities.dp(20);
    final float emojiSize = emojiTextSize * 1.2f;

    boolean restarted = false;
    boolean stopped = false;

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

        setEditRect();

        editBounds.right = editBounds.left + emojiSize;
        final float inset = emojiSize / 2f;
        startRect.set(
                editBounds.centerX() - inset,
                editBounds.centerY() - inset,
                editBounds.centerX() + inset,
                editBounds.centerY() + inset
        );
        endRect.set(
                messageX + tempX,
                messageY + tempY,
                messageX + tempX + tempWidth,
                messageY + tempY + tempHeight
        );

        float centerX = evaluate(xProgress, startRect.centerX(), endRect.centerX());
        float centerY = evaluate(yProgress, startRect.centerY(), endRect.centerY());
        float currentWidthEmoji = evaluate(scaleProgress, startRect.width(), endRect.width());
        float currentHeightEmoji = evaluate(scaleProgress, startRect.height(), endRect.height());

        currentRect.set(
                centerX - currentWidthEmoji / 2f,
                centerY - currentHeightEmoji / 2f,
                centerX + currentWidthEmoji / 2f,
                centerY + currentHeightEmoji / 2f);

        image.setImageCoords(currentRect);
        image.setCurrentAlpha(1f);
        image.draw(canvas);

        // reset image
        image.setImageCoords(tempX, tempY, tempWidth, tempHeight);
        if (drawable instanceof RLottieDrawable) {
            image.setCurrentAlpha(1f);
        }

        int timeSave = canvas.save();
        canvas.translate(messageX, messageY);
        drawTime(canvas);
        canvas.restoreToCount(timeSave);
    }

    @Override
    protected AnimationType getAnimationType() {
        return AnimationType.Emoji;
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
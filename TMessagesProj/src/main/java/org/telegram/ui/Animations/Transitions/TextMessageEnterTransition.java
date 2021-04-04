package org.telegram.ui.Animations.Transitions;

import android.graphics.Canvas;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

public class TextMessageEnterTransition extends MessageTransition {

    float editX;
    float editY;

    float startSize = AndroidUtilities.dp(18);
    float endSize = AndroidUtilities.dp(SharedConfig.fontSize);
    
    float enterHeight;

    public TextMessageEnterTransition(ActionBar actionBar, FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView enterView, RecyclerListView listView) {
        super(actionBar, containerView, messageView, enterView, listView);
        location(enterView, enterView.getEditField());
        editX = location[0];
        editY = location[1];
        enterHeight = enterView.getMeasuredHeight();
    }

    @Override
    public void animationDraw(Canvas canvas) {
        float editTextX = enterView.getX() + editX;
        float editTextY = enterView.getY() + enterView.getMeasuredHeight() - enterHeight + editY + editPaddingVertical;

        setBubbleRectEnd();
        messageView.onLayoutUpdateText();

        float textX = messageView.textX - endRect.left;
        float textY = messageView.textY;

        startRect.set(
                editTextX - textX,
                editTextY - textY,
                enterView.getRight(),
                editTextY + endRect.height());

        animateBackground(canvas, 0.08f);

        canvas.save();
        canvas.clipPath(path);
        float textScale = evaluate(scaleProgress, startSize, endSize) / endSize;
        float offsetXDraw = endRect.left - currentRect.left;
        float offsetYDraw = endRect.top - currentRect.top;
        float deltaTimeX = currentRect.width() - endRect.width();
        float deltaTimeY = currentRect.height() - endRect.height();
        canvas.translate(messageX - offsetXDraw, messageY - offsetYDraw);
        messageView.getTransitionParams().animateDrawingTimeAlpha = true;
        messageView.getTransitionParams().animateChangeProgress = 0f;
        messageView.getTransitionParams().myTransition = true;
        messageView.getTransitionParams().textScale = textScale;
        messageView.getTransitionParams().myReplyProgress = colorProgress;
        messageView.getTransitionParams().ignoreBackground = true;
        messageView.draw(canvas);
        messageView.getTransitionParams().ignoreBackground = false;
        messageView.getTransitionParams().deltaTimeX = (int) deltaTimeX;
        messageView.getTransitionParams().deltaTimeY = (int) deltaTimeY;
        messageView.getTransitionParams().animateChangeProgress = timeProgress;
        messageView.drawTime(canvas, 1f, false);
        messageView.getTransitionParams().animateDrawingTimeAlpha = false;
        messageView.getTransitionParams().myTransition = false;
        canvas.restore();
    }

    @Override
    public void release() {
        super.release();
        messageView.getTransitionParams().ignoreBackground = false;
        messageView.getTransitionParams().myTransition = false;
    }

    @Override
    protected AnimationType getAnimationType() {
        if (messageView.getMessageObject().linesCount > 1) {
            return AnimationType.LongText;
        } else {
            return AnimationType.ShortText;
        }
    }
}
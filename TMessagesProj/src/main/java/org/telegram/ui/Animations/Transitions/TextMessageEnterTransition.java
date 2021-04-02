package org.telegram.ui.Animations.Transitions;

import android.graphics.Canvas;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

public class TextMessageEnterTransition extends BaseMessageTransition {

    float editX;
    float editY;

    float startSize = AndroidUtilities.dp(18);
    float endSize = AndroidUtilities.dp(SharedConfig.fontSize);

    public TextMessageEnterTransition(FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView enterView, RecyclerListView listView) {
        super(containerView, messageView, enterView, listView);
        location(enterView, enterView.getEditField());
        editX = location[0];
        editY = location[1];
    }

    @Override
    public void animationDraw(Canvas canvas) {
        float editTextX = enterView.getX() + editX;
        float editTextY = enterView.getY() + editY + editPaddingVertical;

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
        float scale = evaluate(scaleProgress, startSize, endSize) / endSize;
        canvas.translate(currentRect.left + textX, currentRect.top + textY);
        canvas.scale(scale, scale);
        messageView.textX = 0;
        messageView.textY = 0;
        messageView.drawMessageText(canvas,  messageView.getMessageObject().textLayoutBlocks, true, 1.0f);
        canvas.restore();
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
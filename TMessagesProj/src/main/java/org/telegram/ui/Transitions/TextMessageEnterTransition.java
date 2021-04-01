package org.telegram.ui.Transitions;

import android.graphics.Canvas;
import android.graphics.Color;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.Theme;
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
        float editTextY = enterView.getY() + editY + AndroidUtilities.dp(11);

        setBackgroundRectEnd();
        messageView.onLayoutUpdateText();

        float textX = messageView.textX - backgroundRectEnd.left;
        float textY = messageView.textY;

        backgroundRectStart.set(
                editTextX - textX,
                editTextY - textY,
                enterView.getRight(),
                editTextY + backgroundRectEnd.height());

        animateBackground(canvas, 0.08f);
        Theme.dialogs_onlinePaint.setColor(Color.MAGENTA);
        canvas.drawCircle(editTextX, editTextY, AndroidUtilities.dp(5), Theme.dialogs_onlinePaint);

        canvas.save();
        float scale = evaluate(scaleProgress, startSize, endSize) / endSize;
        canvas.translate(backgroundRect.left + textX, backgroundRect.top + textY);
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
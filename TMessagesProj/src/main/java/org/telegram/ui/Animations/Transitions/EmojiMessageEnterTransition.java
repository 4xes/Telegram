package org.telegram.ui.Animations.Transitions;

import android.graphics.Canvas;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

public class EmojiMessageEnterTransition extends BaseImageMessageEnterTransition {

    final static float emojiTextSize = AndroidUtilities.dp(20);
    final static float emojiSize = emojiTextSize * 1.2f;

    public EmojiMessageEnterTransition(ActionBar actionBar, FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView enterView, RecyclerListView listView, ChatActivity chatActivity) {
        super(actionBar, containerView, messageView, enterView, listView, chatActivity);
        location(enterView, enterView.getEditField());
    }

    @Override
    void setStartImage() {
        setEditRect();

        editBounds.right = editBounds.left + emojiSize;
        final float inset = emojiSize / 2f;
        startRect.set(
                editBounds.centerX() - inset,
                editBounds.centerY() - inset,
                editBounds.centerX() + inset,
                editBounds.centerY() + inset
        );
    }

    @Override
    public void animationDraw(Canvas canvas) {
        super.animationDraw(canvas);
        drawReplySticker(canvas);
        drawTime(canvas);
    }

    @Override
    protected AnimationType getAnimationType() {
        return AnimationType.Emoji;
    }
}
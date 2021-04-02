package org.telegram.ui.Animations.Transitions;

import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

public class EmojiMessageEnterTransition extends BaseStickerMessageEnterTransition {

    final static float emojiTextSize = AndroidUtilities.dp(20);
    final static float emojiSize = emojiTextSize * 1.2f;

    public EmojiMessageEnterTransition(FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView enterView, RecyclerListView listView) {
        super(containerView, messageView, enterView, listView);
        location(enterView, enterView.getEditField());
    }

    @Override
    void setStartSticker() {
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
    protected AnimationType getAnimationType() {
        return AnimationType.Emoji;
    }
}
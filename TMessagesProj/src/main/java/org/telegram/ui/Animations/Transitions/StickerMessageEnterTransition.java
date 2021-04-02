package org.telegram.ui.Animations.Transitions;

import android.graphics.RectF;
import android.view.View;
import android.widget.FrameLayout;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

public class StickerMessageEnterTransition extends BaseStickerMessageEnterTransition {

    final float stickerCellX;
    final float stickerCellY;
    final float stickerCellHeight;
    final float stickerCellWidth;

    public StickerMessageEnterTransition(FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView enterView, RecyclerListView listView, View stickerView) {
        super(containerView, messageView, enterView, listView);
        location(enterView, stickerView);
        stickerCellX = location[0];
        stickerCellY = location[1];
        stickerCellWidth = stickerView.getMeasuredWidth();
        stickerCellHeight = stickerView.getMeasuredHeight();
    }

    final static float stickerSize = AndroidUtilities.dp(66);

    private final RectF stickerCellRect = new RectF();

    public void setStartSticker() {
        setEnterViewRect();

        float stickerLeft = enterViewBounds.left + stickerCellX;
        float stickerTop = enterViewBounds.top + stickerCellY;
        stickerCellRect.set(
                stickerLeft,
                stickerTop,
                stickerLeft + stickerCellWidth,
                stickerTop + stickerCellHeight
        );
        final float inset = stickerSize / 2f;
        startRect.set(
                stickerCellRect.centerX() - inset,
                stickerCellRect.centerY() - inset,
                stickerCellRect.centerX() + inset,
                stickerCellRect.centerY() + inset
        );
    }
}
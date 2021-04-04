package org.telegram.ui.Animations.Transitions;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

public class StickerMessageEnterTransition extends BaseImageMessageEnterTransition {

    final float stickerCellX;
    final float stickerCellY;
    final float stickerCellHeight;
    final float stickerCellWidth;

    public StickerMessageEnterTransition(ActionBar actionBar, FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView enterView, RecyclerListView listView, View stickerView, ChatActivity chatActivity) {
        super(actionBar, containerView, messageView, enterView, listView ,chatActivity);
        stickerCellWidth = stickerView.getMeasuredWidth();
        stickerCellHeight = stickerView.getMeasuredHeight();

        location(enterView, stickerView);
        stickerCellX = location[0];
        stickerCellY = location[1];
    }

    final static float stickerSize = AndroidUtilities.dp(66);

    private final RectF stickerCellRect = new RectF();

    @Override
    public void setStartImage() {
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


    @Override
    public void animationDraw(Canvas canvas) {
        super.animationDraw(canvas);
        drawReplySticker(canvas);
        drawTime(canvas);
    }

    @Override
    protected AnimationType getAnimationType() {
        return AnimationType.Sticker;
    }
}
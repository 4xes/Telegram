package org.telegram.ui.Animations.Transitions;

import android.graphics.RectF;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

public class PhotoMessageEnterTransition extends BaseImageMessageEnterTransition {

    final float photoCellX;
    final float photoCellY;
    final float photoCellHeight;
    final float photoCellWidth;

    public PhotoMessageEnterTransition(ActionBar actionBar, FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView enterView, RecyclerListView listView, View photoView, ChatActivity chatActivity) {
        super(actionBar, containerView, messageView, enterView, listView, chatActivity);

        photoCellX = 0f;
        photoCellY = 0f;
        photoCellWidth = AndroidUtilities.dp(photoWidth);
        photoCellHeight = AndroidUtilities.dp(photoHeight);
    }

    final static float photoWidth = AndroidUtilities.dp(40);
    final static float photoHeight = AndroidUtilities.dp(40);

    private final RectF photoCellRect = new RectF();

    @Override
    public void setStartImage() {
        setEnterViewRect();

        float photoLeft = enterViewBounds.left + photoCellX + AndroidUtilities.dp(70);
        float photoTop = enterViewBounds.top + photoCellY - AndroidUtilities.dp(100);
        photoCellRect.set(
                photoLeft,
                photoTop,
                photoLeft + photoCellWidth,
                photoTop + photoCellHeight
        );
        final float insetWidth = photoCellWidth / 2f;
        final float insetHeight = photoCellHeight /2f;
        startRect.set(
                photoCellRect.centerX() - insetWidth,
                photoCellRect.centerY() - insetHeight,
                photoCellRect.centerX() + insetWidth,
                photoCellRect.centerY() + insetHeight
        );
    }

    @Override
    protected AnimationType getAnimationType() {
        return AnimationType.Photo;
    }
}
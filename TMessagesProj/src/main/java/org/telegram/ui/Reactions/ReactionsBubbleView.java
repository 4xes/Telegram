package org.telegram.ui.Reactions;

import android.content.Context;
import android.graphics.Canvas;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class ReactionsBubbleView extends FrameLayout {

    final ReactionBubbleDrawable bubbleDrawable;
    final ReactionsBubbleListView bubbleListView;

    public ReactionsBubbleView(@NonNull Context context, ArrayList<TLRPC.TL_availableReaction> reactions) {
        super(context);
        bubbleDrawable = new ReactionBubbleDrawable(context);
        bubbleListView = new ReactionsBubbleListView(context, bubbleDrawable.getContentHeight() / 2, reactions);
        addView(bubbleListView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setBackground(bubbleDrawable);
    }

    @Override
    public void setBackgroundColor(int color) {
        bubbleDrawable.setBackgroundColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}

package org.telegram.ui.Reactions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.tgnet.TLRPC;

import java.util.List;

public class ReactionsBubbleView extends FrameLayout {

    final ReactionBubbleDrawable bubbleDrawable;
    final ReactionsBubbleListView bubbleListView;

    @SuppressLint("ViewConstructor")
    public ReactionsBubbleView(@NonNull Context context, List<TLRPC.TL_availableReaction> reactions, @Nullable ReactionsBubbleListView.ReactionSelectedListener selectedListener) {
        super(context);
        bubbleDrawable = new ReactionBubbleDrawable(context);
        bubbleListView = new ReactionsBubbleListView(context, bubbleDrawable.getContentHeight() / 2, reactions, selectedListener);
        addView(bubbleListView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setBackground(bubbleDrawable);
    }

    public Rect getPaddingDrawable() {
        Rect padding = new Rect();
        bubbleDrawable.getPadding(padding);
        return padding;
    }

    public int getContentHeight() {
        return bubbleDrawable.getContentHeight();
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

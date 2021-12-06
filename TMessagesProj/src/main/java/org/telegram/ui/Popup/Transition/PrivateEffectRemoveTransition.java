package org.telegram.ui.Popup.Transition;

import android.graphics.Canvas;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.MessageEnterTransitionContainer;

import java.util.ArrayList;

public class PrivateEffectRemoveTransition extends EffectTransition {


    final ArrayList<TLRPC.TL_reactionCount> counts = new ArrayList<>();

    public PrivateEffectRemoveTransition(MessageEnterTransitionContainer container, ChatActivity chatActivity, RecyclerListView listView, ChatMessageCell cell) {
        super(container,chatActivity, listView, cell);

        ArrayList<TLRPC.TL_reactionCount> reactionCounts = currentMessageObject.getReactionCounts();
        if(reactionCounts != null) {
            counts.addAll(reactionCounts);
        }

        messageView.reactionsHelper.hideChoose = true;
        messageView.invalidate();
    }

    @Override
    void release() {
        messageView.reactionsHelper.hideChoose = false;
        messageView.invalidate();
    }

    @Override
    public long duration() {
        return 1500;
    }

    @Override
    void onDraw(Canvas canvas, float progress) {
        canvas.save();

        float angleEnd = currentMessageObject.isOutOwner() ? 180 : -180;
        float offsetEnd = currentMessageObject.isOutOwner() ? -AndroidUtilities.dp(150): AndroidUtilities.dp(150);

        canvas.rotate(0 - angleEnd * progress,  lastMessageX + messageView.getDrawTimeX() + offsetEnd / 2f,lastMessageY + messageView.getDrawTimeY());
        canvas.translate(lastMessageX, lastMessageY);
        messageView.reactionsHelper.drawPrivateReactions(canvas, counts, messageView.getDrawTimeX(), messageView.getDrawTimeY(), 1f - progress, 1f - progress);
        canvas.restore();
    }

}
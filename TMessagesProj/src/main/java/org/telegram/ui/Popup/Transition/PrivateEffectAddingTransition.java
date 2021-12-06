package org.telegram.ui.Popup.Transition;

import android.graphics.Canvas;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.MessageEnterTransitionContainer;
import org.telegram.ui.Reactions.ReactionCell;
import org.telegram.ui.Reactions.ReactionsPreferences;
import org.telegram.ui.Reactions.ReactionsRequestController;

public class PrivateEffectAddingTransition extends EffectTransition {
    private final ReactionCell button;
    TLRPC.TL_messages_stickerSet set;

    ImageReceiver effectImage;
    ImageReceiver activateImage;

    private final String INTERACTIONS_STICKER_PACK = "EmojiAnimations";

    private void setImage(ImageReceiver image, TLRPC.Document document) {
        image.setImage(ImageLocation.getForDocument(document), "256_256_pcache", null, "tgs", null, 1);
    }

    public PrivateEffectAddingTransition(MessageEnterTransitionContainer container, ChatActivity chatActivity, RecyclerListView listView, ChatMessageCell cell, @Nullable ReactionCell button, @Nullable String reaction) {
        super(container,chatActivity, listView, cell);
        this.button = button;

        set = MediaDataController.getInstance(UserConfig.selectedAccount).getStickerSetByName(INTERACTIONS_STICKER_PACK);
        if (set == null) {
            set = MediaDataController.getInstance(UserConfig.selectedAccount).getStickerSetByEmojiOrName(INTERACTIONS_STICKER_PACK);
        }
        if (set == null) {
            MediaDataController.getInstance(UserConfig.selectedAccount).loadStickersByEmojiOrName(INTERACTIONS_STICKER_PACK, false, true);
        }
        this.effectImage = new ImageReceiver();
        this.activateImage = new ImageReceiver();
        TLRPC.Document effectDocument = null;
        TLRPC.Document activateDocument = null;
        if (button != null) {
            TLRPC.TL_availableReaction availableReaction = button.getReaction();
            if (availableReaction != null) {
                effectDocument = availableReaction.effect_animation;
                activateDocument = availableReaction.activate_animation;
            }
        }
        if (reaction != null) {
            effectDocument = chatActivity.getReactionsController().getEffectAnimation(reaction);
            activateDocument = chatActivity.getReactionsController().getActivateAnimation(reaction);
        }

        if (effectDocument == null && activateDocument == null) {
            return;
        }

        setImage(effectImage, effectDocument);
        setImage(activateImage, activateDocument);

        if (button != null) {
            location(chatActivity.contentView, button);
        }
    }

    @Override
    public long duration() {
        return 4000;
    }

    final int stickerSize = AndroidUtilities.dp(150);

    @Override
    void onDraw(Canvas canvas, float progress) {


        int widthScreen = chatActivity.contentView.getMeasuredWidth();
        int heightScreen = chatActivity.contentView.getMeasuredHeight();


        if (button == null) {
            location[0] = (int) (widthScreen / 2f) - stickerSize / 2;
            location[1] = (int) (heightScreen / 2f) - stickerSize / 2;
        }

        float part1 = duration() * 0.4f / duration();


        int startSize = button != null? button.getMeasuredWidth(): 0;

        float startPositionX = location[0];
        float startPositionY = location[1];

        int size = 0;
        if (progress < part1) {
            size = startSize + (int) ((stickerSize - startSize) * (progress / part1));
        } else {
            size = stickerSize;
        }

        float endPositionMiddleX = (int) (widthScreen / 2f) - stickerSize / 2;
        float endPositionMiddleY = (int) (heightScreen / 2f) - stickerSize / 2;

        if (button != null) {
            endPositionMiddleY -= stickerSize - AndroidUtilities.dp(10);
        }

        float targetPositionX = 0f;
        float targetPositionY = 0f;
        if (progress < part1) {
            targetPositionX = evaluate(startPositionX, endPositionMiddleX, (progress / part1));
            targetPositionY = evaluate(startPositionY, endPositionMiddleY, (progress / part1));
        } else {
           targetPositionX = endPositionMiddleX;
           targetPositionY = endPositionMiddleY;
        }

        float effectSize = Math.min(widthScreen, heightScreen) / 2f;

        float effectPositionY = location[1] - effectSize / 2f;
        if (button != null) {
            effectPositionY -= effectSize /2f;
        }

        effectImage.draw(canvas);
        if (progress > part1) {
            effectImage.setImageCoords(widthScreen / 2f - effectSize / 2f, effectPositionY, effectSize, effectSize);
            if (this.effectImage.getLottieAnimation() != null  && !this.effectImage.getLottieAnimation().isRunning()) {
                this.effectImage.getLottieAnimation().start();
            }
        }

        if (this.activateImage.getLottieAnimation() != null && !this.activateImage.getLottieAnimation().isRunning()) {
            this.activateImage.getLottieAnimation().start();
        }
        activateImage.setImageCoords(targetPositionX,targetPositionY, size, size);
        activateImage.draw(canvas);

    }

    @Override
    void release() {

    }

}
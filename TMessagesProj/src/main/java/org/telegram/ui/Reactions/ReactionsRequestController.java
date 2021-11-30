package org.telegram.ui.Reactions;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLRPC;

import java.util.List;
import java.util.stream.Collectors;

public class ReactionsRequestController extends BaseController {

    @Nullable
    private TLRPC.TL_messages_availableReactions reactions;

    private static final ReactionsRequestController[] instances = new ReactionsRequestController[UserConfig.MAX_ACCOUNT_COUNT];

    private final ReactionsPreferences preferences = new ReactionsPreferences();

    public static ReactionsRequestController getInstance(int accountNum) {
        ReactionsRequestController local = instances[accountNum];
        if (local == null) {
            synchronized (ReactionsRequestController.class) {
                local = instances[accountNum];
                if (local == null) {
                    local = new ReactionsRequestController(accountNum);
                    instances[accountNum] = local;
                }
            }
        }
        return local;
    }

    public ReactionsRequestController(int accountNum) {
        super(accountNum);
    }

    @Nullable
    public TLRPC.TL_messages_availableReactions cachedReactions() {
        if (reactions == null) {
            reactions = preferences.getAvailableReactions();
        }
        return reactions;
    }

    @Nullable
    public List<TLRPC.TL_availableReaction> getReactions(@Nullable TLRPC.ChatFull info) {
        final TLRPC.TL_messages_availableReactions reactions = cachedReactions();
        if (info == null || reactions == null) {
            return null;
        }
        return reactions.reactions.stream().filter(reaction -> info.available_reactions.contains(reaction.reaction)).collect(Collectors.toList());
    }

    public boolean canSendReaction(MessageObject messageObject) {
        //scheduled todo check
        return messageObject != null && !messageObject.isSponsored() && !messageObject.scheduled && !(messageObject.messageOwner instanceof TLRPC.TL_messageService);
    }

    public void sendReaction(MessageObject messageObject, @Nullable String reaction) {
        if (messageObject == null) {
            return;
        }
        TLRPC.TL_messages_sendReaction req = new TLRPC.TL_messages_sendReaction();
        req.peer = getMessagesController().getInputPeer(messageObject.getDialogId());
        req.msg_id = messageObject.getId();
        if (reaction != null) {
            req.reaction = reaction;
            req.flags |= 1;
        }

        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response != null) {
                getMessagesController().processUpdates((TLRPC.Updates) response, false);
            }
            if (error != null) {
                if (error.text != null && error.text.equals("400 REACTION_INVALID")) {
                    AndroidUtilities.runOnUIThread(() -> {
                        getAccountInstance().getMessagesController().loadFullChat(messageObject.getDialogId(), 0, true);
                    });
                }
            }
        });
    }

    public int requestReactions(RequestDelegate onComplete) {
        TLRPC.TL_messages_getAvailableReactions req = new TLRPC.TL_messages_getAvailableReactions();
        final TLRPC.TL_messages_availableReactions reactions = cachedReactions();
        if (reactions != null) {
            boolean isExpired = preferences.isCacheExpired();
            if (!isExpired) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (onComplete != null) {
                        onComplete.run(reactions, null);
                    }
                });
                return -1;
            } else {
                req.hash = reactions.hash;
            }
        }
        return getConnectionsManager().sendRequest(req, (response, error) -> {
            AndroidUtilities.runOnUIThread(() -> {
                if (error == null) {
                    if (response instanceof TLRPC.TL_messages_availableReactions) {
                        this.reactions = (TLRPC.TL_messages_availableReactions) response;
                        preferences.save(this.reactions);
                    }
                }
                if (onComplete != null) {
                    onComplete.run(this.reactions, error);
                }
            });
        });
    }
}

package org.telegram.ui.Reactions;

import androidx.annotation.Nullable;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactionsPagingController {

    final ReactionsRequestController requestController;
    final List<TLRPC.User> reactedUsers;
    final Map<Long, String> userReactions;
    final List<TLRPC.User> readUsers;
    final String reaction;
    final int currentAccount;
    final MessageObject messageObject;
    String offset;
    int reactionCount;

    boolean isEnded = false;
    boolean isLoading = false;

    public ReactionsPagingController(ReactionsRequestController requestController, MessageObject messageObject, int currentAccount, @Nullable List<TLRPC.User> readUsers, @Nullable List<TLRPC.User> reactedUsers, @Nullable Map<Long, String> userReactions, String reaction, int reactionCount, @Nullable String offset) {
        this.requestController = requestController;
        this.readUsers = readUsers == null ? new ArrayList<>(): readUsers;
        this.reactedUsers = reactedUsers == null ? new ArrayList<>(): reactedUsers;
        this.userReactions = userReactions == null? new HashMap<>(): userReactions;
        this.reaction = reaction;
        this.offset = reaction == null ? offset: null;
        this.messageObject = messageObject;
        this.currentAccount = currentAccount;
        this.reactionCount = reactionCount;
        if(this.reactedUsers.size() >= reactionCount) {
            isEnded = true;
        }
        if (reactionCount == 0) {
            isEnded = true;
            isLoading = false;
        }
    }

    public boolean isEnded() {
        return isEnded;
    }

    public void load(UserUpdate userUpdate) {
        isLoading = true;
        requestController.getMessageReactionsList(messageObject, 100, offset, currentAccount, reaction, (recentReactedUsers, mapReactions, nextOffset, count) -> {
            this.offset = nextOffset;
            isLoading = false;
            reactionCount = count;
            int oldSize = getItemSize();
            for (TLRPC.User user: recentReactedUsers) {
                if (!userReactions.containsKey(user.id)) {
                    reactedUsers.add(user);
                }
            }
            userReactions.putAll(mapReactions);
            if (nextOffset == null) {
                isEnded = true;
            }
            userUpdate.onLoaded(oldSize, recentReactedUsers.size());
        });
    }

    public TLRPC.User getUser(int position) {
        if (position < reactedUsers.size()) {
            return reactedUsers.get(position);
        } else {
            return readUsers.get(position - reactedUsers.size());
        }
    }

    @Nullable
    public TLRPC.Document getReaction(long userId) {
        String reaction = userReactions.get(userId);
        return requestController.getStaticIcon(reaction);
    }

    public boolean isLoading() {
        return isLoading;
    }

    public int getItemSize() {
        return reactedUsers.size() + readUsers.size();
    }

    public interface UserUpdate {
        void onLoaded(int oldSize, int added);
    }
}

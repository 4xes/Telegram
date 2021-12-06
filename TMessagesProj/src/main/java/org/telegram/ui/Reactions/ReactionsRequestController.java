package org.telegram.ui.Reactions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ReactionsRequestController extends BaseController {

    @Nullable
    private TLRPC.TL_messages_availableReactions reactions;

    private HashMap<String, TLRPC.Document> staticIcon = new HashMap<>(11);
    private HashMap<String, TLRPC.Document> selectAnimation = new HashMap<>(11);
    private HashMap<String, TLRPC.Document> activateAnimation = new HashMap<>(11);
    private HashMap<String, TLRPC.Document> effectAnimation = new HashMap<>(11);

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

    String heart = "❤";
    String like = "\uD83D\uDC4D";

    public boolean hasFastReactions(@Nullable TLRPC.ChatFull chatInfo, @Nullable TLRPC.UserFull userInfo) {
        return getFastReaction(chatInfo, userInfo) != null;
    }

    public @Nullable String getFastReaction(@Nullable TLRPC.ChatFull chatInfo, @Nullable TLRPC.UserFull userInfo) {
        if (userInfo != null) {
            return heart;
        }
        if (chatInfo != null && chatInfo.available_reactions != null ) {
            if (chatInfo.available_reactions.contains(heart)) {
                return heart;
            }
            if (chatInfo.available_reactions.contains(like)) {
                return like;
            }
        }
        return null;
    }

    public boolean isCurrentReaction(MessageObject messageObject, String reaction) {
        if (messageObject.hasReactions()) {
            if (!messageObject.messageOwner.reactions.min) {
                for (TLRPC.TL_reactionCount reactionCount: messageObject.messageOwner.reactions.results) {
                    if (reactionCount.chosen && reactionCount.reaction.equals(reaction)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public ReactionsRequestController(int accountNum) {
        super(accountNum);
    }

    @Nullable
    public TLRPC.TL_messages_availableReactions cachedReactions() {
        if (reactions == null) {
            reactions = preferences.getAvailableReactions();
            if (reactions != null) {
                fillMaps(reactions);
            }
        }
        return reactions;
    }

    private void fillMaps(TLRPC.TL_messages_availableReactions reactions) {
        for (TLRPC.TL_availableReaction reaction: reactions.reactions) {
            String emoji = reaction.reaction;
            staticIcon.put(emoji, reaction.static_icon);
            selectAnimation.put(emoji, reaction.select_animation);
            activateAnimation.put(emoji, reaction.activate_animation);
            effectAnimation.put(emoji, reaction.effect_animation);

            ChatMessageReactionsHelper.updateDocument(emoji, reaction.static_icon);
        }
    }

    @Nullable
    public List<TLRPC.TL_availableReaction> getReactions(@Nullable TLRPC.ChatFull chatInfo, @Nullable TLRPC.UserFull userInfo) {
        final TLRPC.TL_messages_availableReactions reactions = cachedReactions();
        if ((chatInfo == null && userInfo == null) || reactions == null) {
            return null;
        }
        if (chatInfo != null) {
            List<TLRPC.TL_availableReaction> filtered = new ArrayList<>(chatInfo.available_reactions.size());
            for (TLRPC.TL_availableReaction reaction: reactions.reactions) {
                if (chatInfo.available_reactions.contains(reaction.reaction)) {
                    filtered.add(reaction);
                }
            }

            if (filtered.isEmpty()) {
                return null;
            } else {
                return filtered;
            }
        }
        return reactions.reactions;
    }

    public boolean canSendReaction(MessageObject messageObject) {
        return messageObject != null && !messageObject.isSponsored() && !messageObject.scheduled && !(messageObject.messageOwner instanceof TLRPC.TL_messageService);
    }

    public void getUsers(ArrayList<Long> userIds, @NonNull UsersDelegate onComplete) {
        ArrayList<TLRPC.User> users = new ArrayList<>();
        ArrayList<TLRPC.InputUser> unknown = new ArrayList<>();
        for (Long userId:  userIds) {
            TLRPC.User user = getMessagesStorage().getUser(userId);
            if (user == null) {
                unknown.add(getMessagesController().getInputUser(userId));
            } else {
                users.add(user);
            }
        }
        if (unknown.isEmpty()) {
            onComplete.run(users);
        } else {
            TLRPC.TL_users_getUsers req = new TLRPC.TL_users_getUsers();
            req.id = unknown;
            getConnectionsManager().sendRequest(req, (response, error) -> {
                AndroidUtilities.runOnUIThread(() -> {
                    if (response != null) {
                        TLRPC.Vector vector = (TLRPC.Vector) response;
                        for (Object obj : vector.objects) {
                            users.add((TLRPC.User) obj);
                        }
                        onComplete.run(users);
                    }
                });
            });
        }
    }

    @Nullable
    public TLRPC.Document getStaticIcon(String emoji) {
        return staticIcon.get(emoji);
    }

    @Nullable
    public TLRPC.Document getEffectAnimation(String emoji) {
        return effectAnimation.get(emoji);
    }

    @Nullable
    public TLRPC.Document getActivateAnimation(String emoji) {
        return activateAnimation.get(emoji);
    }

    public void getReadParticipants(MessageObject messageObject, int currentAccount, TLRPC.Chat chat, @NonNull ParticipantsReadDelegate onComplete) {
        TLRPC.TL_messages_getMessageReadParticipants req = new TLRPC.TL_messages_getMessageReadParticipants();
        req.msg_id = messageObject.getId();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(messageObject.getDialogId());

        long fromId = 0;
        if (messageObject.messageOwner.from_id != null) {
            fromId = messageObject.messageOwner.from_id.user_id;
        }
        long finalFromId = fromId;

        final ArrayList<Long> peerIds = new ArrayList<>();
        final ArrayList<TLRPC.User> users = new ArrayList<>();

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            FileLog.e("MessageSeenView request completed");
            if (error == null) {
                TLRPC.Vector vector = (TLRPC.Vector) response;
                ArrayList<Long> unknownUsers = new ArrayList<>();
                HashMap<Long, TLRPC.User> usersLocal = new HashMap<>();
                ArrayList<Long> allPeers = new ArrayList<>();
                for (int i = 0, n = vector.objects.size(); i < n; i++) {
                    Object object = vector.objects.get(i);
                    if (object instanceof Long) {
                        Long peerId = (Long) object;
                        if (finalFromId == peerId) {
                            continue;
                        }
                        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(peerId);
                        allPeers.add(peerId);
                        if (user == null) {
                            unknownUsers.add(peerId);
                        } else {
                            usersLocal.put(peerId, user);
                        }
                    }
                }

                if (unknownUsers.isEmpty()) {
                    for (int i = 0; i < allPeers.size(); i++) {
                        peerIds.add(allPeers.get(i));
                        users.add(usersLocal.get(allPeers.get(i)));
                    }
                    onComplete.run(users, peerIds);
                } else {
                    if (ChatObject.isChannel(chat)) {
                        TLRPC.TL_channels_getParticipants usersReq = new TLRPC.TL_channels_getParticipants();
                        usersReq.limit = 50;
                        usersReq.offset = 0;
                        usersReq.filter = new TLRPC.TL_channelParticipantsRecent();
                        usersReq.channel = MessagesController.getInstance(currentAccount).getInputChannel(chat.id);
                        ConnectionsManager.getInstance(currentAccount).sendRequest(usersReq, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                            if (response1 != null) {
                                TLRPC.TL_channels_channelParticipants participants = (TLRPC.TL_channels_channelParticipants) response1;
                                for (int i = 0; i < participants.users.size(); i++) {
                                    TLRPC.User user = participants.users.get(i);
                                    MessagesController.getInstance(currentAccount).putUser(user, false);
                                    usersLocal.put(user.id, user);
                                }
                                for (int i = 0; i < allPeers.size(); i++) {
                                    peerIds.add(allPeers.get(i));
                                    users.add(usersLocal.get(allPeers.get(i)));
                                }
                            }
                            onComplete.run(users, peerIds);
                        }));
                    } else {
                        TLRPC.TL_messages_getFullChat usersReq = new TLRPC.TL_messages_getFullChat();
                        usersReq.chat_id = chat.id;
                        ConnectionsManager.getInstance(currentAccount).sendRequest(usersReq, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                            if (response1 != null) {
                                TLRPC.TL_messages_chatFull chatFull = (TLRPC.TL_messages_chatFull) response1;
                                for (int i = 0; i < chatFull.users.size(); i++) {
                                    TLRPC.User user = chatFull.users.get(i);
                                    MessagesController.getInstance(currentAccount).putUser(user, false);
                                    usersLocal.put(user.id, user);
                                }
                                for (int i = 0; i < allPeers.size(); i++) {
                                    peerIds.add(allPeers.get(i));
                                    users.add(usersLocal.get(allPeers.get(i)));
                                }
                            }
                            onComplete.run(users, peerIds);
                        }));
                    }
                }
            } else {
                onComplete.run(users, peerIds);
            }
        }));

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

    public int getMessageReactionsList(MessageObject messageObject, int limit, String offset, int currentAccount, String reactionFilter, @NonNull RecentReactionsDelegate recentReactionsDelegate) {
        TLRPC.TL_messages_getMessageReactionsList req = new TLRPC.TL_messages_getMessageReactionsList();
        req.id = messageObject.getId();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(messageObject.getDialogId());
        req.limit = limit;
        if (reactionFilter != null) {
            req.reaction = reactionFilter;
            req.flags |= 1;
        }
        if (offset != null) {
            req.offset = offset;
            req.flags |= 2;
        }
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                if (error != null) {
                    FileLog.e(error.text);
                }
                AndroidUtilities.runOnUIThread(() -> {
                    if (response != null) {
                        TLRPC.TL_messages_messageReactionsList result = (TLRPC.TL_messages_messageReactionsList) response;
                        HashMap<Long, String> mapReactions = new HashMap<>();
                        for (TLRPC.TL_messageUserReaction reaction: result.reactions) {
                            mapReactions.put(reaction.user_id, reaction.reaction);
                        }
                        recentReactionsDelegate.run(result.users, mapReactions, result.next_offset, result.count);
                    }
            });
        });
        return 0;
    }

    public void prefetchReactions() {
        getAvailableReactions(null);
    }

    /**
     * Загружает список доступных реакций, если реакции закешированы и срок актуальный, то просто берет из кеш, если кеш неактуальный пытается обновить
     *
     * @param onComplete каллбек (TLRPC.TL_messages_availableReactions, TLRPC.TL_error error)
     * @return requestToken
     */
    public int getAvailableReactions(@Nullable RequestDelegate onComplete) {
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
                        fillMaps(this.reactions);
                    }
                }
                if (onComplete != null) {
                    onComplete.run(this.reactions, error);
                }
            });
        });
    }

    public interface RecentReactionsDelegate {
        void run(List<TLRPC.User> recentReactedUsers, HashMap<Long, String> mapReactions, String offset, int count);
    }

    public interface ParticipantsReadDelegate {
        void run(List<TLRPC.User> users, ArrayList<Long> peerIds);
    }

    public interface UsersDelegate {
        void run(List<TLRPC.User> users);
    }
}

package org.telegram.ui.Popup;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.HideViewAfterAnimation;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ShowViewAfterAnimation;
import org.telegram.ui.Reactions.ReactionsBubbleListView;
import org.telegram.ui.Reactions.ReactionsBubbleView;
import org.telegram.ui.Reactions.ReactionsPagingController;
import org.telegram.ui.Reactions.ReactionsRequestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ChatPopupWindowSimple extends FrameLayout {

    public static final int POPUP_WIDTH = 220;
    private static final int REACTION_TOP_MARGIN = 60;
    private static final int REACTION_RIGHT_MARGIN_TWO = 40;
    private static final int REACTION_MAX_WIDTH = POPUP_WIDTH + REACTION_RIGHT_MARGIN_TWO;
    private static final int REACTION_START_WIDTH = 70;
    private static final int POPUP_ITEMS_MAX_HEIGHT = 340;
    public static final int POPUP_BOTTOM_FILL = 70;

    public static final int MENU_ACTION_HEIGHT = 44;
    public static final int MENU_ITEM_TABS_HEIGHT = 40;
    private static final int MENU_ITEM_DIVIDER_HEIGHT = 8;
    private static final int SPONSORED_HEIGHT = 56;
    private static final int SPONSORED_PADDING = 6;

    private ChatMenuClipLayout container;
    private FrameLayout childContainer;

    private ReactionsBubbleView reactionsView;
    private ActionBarMenuSubItem sponsoredCell;
    private ChatPopupItemsListView itemsListView;
    private MenuItemReactionView menuItemReactionView;
    private ShadowSectionCell divider;

    private ChatPopupWindow.OnUserSelectedListener userSelectedListener;
    private ActionBarPopupWindow.OnDispatchKeyEventListener dispatchKeyEventListener;

    private final Theme.ResourcesProvider resourcesProvider;

    private final MessageObject messageObject;

    private int backgroundColor = Color.WHITE;
    private Drawable shadowDrawable = getContext().getResources().getDrawable(R.drawable.popup_fixed_alert).mutate();

    private Rect shadowPadding = new Rect();

    private final int currentAccount;
    private final TLRPC.Chat currentChat;

    public ArrayList<TLRPC.User> users = new ArrayList<>();

    private final ReactionsRequestController requestController;

    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondContentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int roundCorners = AndroidUtilities.dp(4);
    private final RectF roundRect = new RectF();

    public ChatPopupWindowSimple(Context context, Theme.ResourcesProvider resourcesProvider, MessageObject messageObject, int currentAccount, TLRPC.Chat chat, ReactionsRequestController requestController, TLRPC.TL_reactionCount counter, ChatPopupWindow.OnUserSelectedListener userSelectedListener) {
        super(context);
        this.currentAccount = currentAccount;
        this.currentChat = chat;
        this.requestController = requestController;
        this.resourcesProvider = resourcesProvider;

        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setStyle(Paint.Style.FILL);
        secondContentPaint.setColor(Color.WHITE);
        secondContentPaint.setStyle(Paint.Style.FILL);
        setBackgroundColor(getThemedColor(Theme.key_actionBarDefaultSubmenuBackground));
        this.messageObject = messageObject;
        shadowDrawable.getPadding(shadowPadding);

        container = new ChatMenuClipLayout(getContext());
        container.setWillNotDraw(false);
        container.setMinimumWidth(AndroidUtilities.dp(POPUP_WIDTH));
        addView(container);

        MarginLayoutParams layoutParams = ((MarginLayoutParams) container.getLayoutParams());
        layoutParams.setMargins(shadowPadding.left, shadowPadding.top, shadowPadding.right, shadowPadding.bottom);

        addBackgroundView();
        ReactionsPagingController controller = new ReactionsPagingController(requestController, messageObject, currentAccount, new ArrayList<>(), new ArrayList<>(), new HashMap<>(), counter.reaction, counter.count, null);
        childContainer = new FrameLayout(getContext());
        childContainer.setLayoutParams(new LayoutParams(AndroidUtilities.dp(POPUP_WIDTH), container.getMeasuredHeight()));

        backAction = createMenuItem(v -> animateHideMenu());
        childContainer.addView(backAction);
        childContainer.addView(createDivider());

        ChatReactionsList chatReactionsList = new ChatReactionsList(getContext(), resourcesProvider, controller, userSelectedListener);
        childContainer.addView(chatReactionsList);
        MarginLayoutParams itemsParams = ((MarginLayoutParams) chatReactionsList.getLayoutParams());
        itemsParams.topMargin = AndroidUtilities.dp(MENU_ACTION_HEIGHT + MENU_ITEM_DIVIDER_HEIGHT);
        chatReactionsList.setLayoutParams(itemsParams);
        childContainer.setTranslationX(container.getMeasuredWidth());

        container.addView(childContainer);
        dimLayout.invalidate();

        setWillNotDraw(false);
    }

    public void addSponsored(OnClickListener onClickListener) {
        sponsoredCell = new ActionBarMenuSubItem(getContext(), true, true, resourcesProvider);
        sponsoredCell.setTextAndIcon(LocaleController.getString("SponsoredMessageInfo", R.string.SponsoredMessageInfo), R.drawable.menu_info);
        sponsoredCell.setOnClickListener(onClickListener);
        sponsoredCell.setItemHeight(SPONSORED_HEIGHT);
        sponsoredCell.setMultiline();
        LayoutParams layoutParams = LayoutHelper.createFrame(POPUP_WIDTH, SPONSORED_HEIGHT, LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT);
        layoutParams.setMargins(shadowPadding.left, shadowPadding.top, shadowPadding.right, shadowPadding.bottom);
        addView(sponsoredCell, layoutParams);
    }

    public ActionBarMenuSubItem createMenuItem(OnClickListener onClickListener) {
        ActionBarMenuSubItem backButton = new ActionBarMenuSubItem(getContext(), true, true, resourcesProvider);
        backButton.setTextAndIcon(LocaleController.getString("Back", R.string.Back), R.drawable.msg_arrow_back);
        backButton.setOnClickListener(onClickListener);
        backButton.setItemHeight(MENU_ACTION_HEIGHT);
        backButton.setColors(
                getThemedColor(Theme.key_actionBarDefaultSubmenuItem),
                getThemedColor(Theme.key_actionBarDefaultSubmenuItem));
        LayoutParams layoutParams = LayoutHelper.createFrame(POPUP_WIDTH, MENU_ACTION_HEIGHT, LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT);
        backButton.setLayoutParams(layoutParams);
        return backButton;
    }

    private ShadowSectionCell createDivider() {
        ShadowSectionCell divider = new ShadowSectionCell(getContext(), MENU_ITEM_DIVIDER_HEIGHT);
        Drawable shadowDrawable = Theme.getThemedDrawable(getContext(), R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow);
        Drawable background = new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray));
        CombinedDrawable combinedDrawable = new CombinedDrawable(background, shadowDrawable, 0, 0);
        combinedDrawable.setFullsize(true);
        divider.setBackgroundDrawable(combinedDrawable);
        divider.setLayoutParams(LayoutHelper.createFrame(POPUP_WIDTH, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 0, MENU_ACTION_HEIGHT, 0, 0));
        return divider;
    }

    FrameLayout dimLayout;

    private void addBackgroundView() {
        dimLayout = new FrameLayout(getContext()) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                if (childContainer != null) {
                    float shadow = childContainer.getTranslationX() / container.getMeasuredWidth();
                    float shadowProgress = 1f - shadow;
                    int alphaShadow = Math.round(255f * 0.2f * shadowProgress);
                    if (alphaShadow != 0) {
                        roundRect.left = 0;
                        roundRect.top = 0;
                        roundRect.right = childContainer.getTranslationX() + roundCorners;
                        roundRect.bottom = container.getMeasuredHeight();

                        shadowPaint.setAlpha(alphaShadow);
                        int clipSave = canvas.save();
                        canvas.clipRect(roundRect.left, roundRect.top, roundRect.right - roundCorners, roundRect.bottom);
                        shadowPaint.setAlpha(alphaShadow);
                        canvas.drawRoundRect(roundRect, roundCorners, roundCorners, shadowPaint);
                        canvas.restoreToCount(clipSave);
                    }

                    if (childContainer.getVisibility() == View.VISIBLE) {
                        roundRect.left = childContainer.getTranslationX() - roundCorners;
                        roundRect.top = 0;
                        roundRect.right = container.getMeasuredWidth();
                        roundRect.bottom = container.getMeasuredHeight();

                        int clipSave = canvas.save();
                        canvas.clipRect(roundRect.left + roundCorners, roundRect.top, roundRect.right, roundRect.bottom);
                        canvas.drawRoundRect(roundRect, roundCorners, roundCorners, secondContentPaint);
                        canvas.restoreToCount(clipSave);
                    }
                }
            }
        };
        dimLayout.setWillNotDraw(false);
        container.addView(dimLayout, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public int getReactionEndWidth() {
        Rect paddingContent = reactionsView.getPaddingDrawable();
        int contentWidth = reactionsView.calculateContent() + paddingContent.left + paddingContent.right;
        int maxWidth = AndroidUtilities.dp(REACTION_MAX_WIDTH) + paddingContent.left + paddingContent.right;

        return Math.min(contentWidth, maxWidth);
    }

    public int getReactionStartWidth() {
        int width = getReactionEndWidth();
        int startWidth = AndroidUtilities.dp(REACTION_START_WIDTH);
        return Math.min(width, startWidth);
    }

    public void addReactionsAndSeen(List<TLRPC.TL_availableReaction> messageReactions, boolean isAddSeen,
                                    @Nullable ReactionsBubbleListView.ReactionSelectedListener selectedListener, @Nullable ChatPopupWindow.OnUserSelectedListener onUserSelectedListener) {
        this.userSelectedListener = onUserSelectedListener;
        if (messageReactions != null && !messageReactions.isEmpty()) {
            reactionsView = new ReactionsBubbleView(getContext(), messageReactions, selectedListener);
            reactionsView.setBackgroundColor(getThemedColor(Theme.key_actionBarDefaultSubmenuBackground));

            Rect paddingContent = reactionsView.getPaddingDrawable();
            int height = reactionsView.getContentHeight() + paddingContent.top + paddingContent.bottom;

            LayoutParams layoutParams = new LayoutParams(getReactionStartWidth(), height);
            layoutParams.setMargins(0, paddingContent.top, 0, 0);
            layoutParams.gravity = Gravity.RIGHT;

            addView(reactionsView, layoutParams);
            animateShowReaction();
        }

        boolean isChannel = ChatObject.isChannel(currentChat) && !currentChat.megagroup;
        if ((messageObject.hasReactions() && !isChannel) || isAddSeen) {
            int countReactions = 0;
            if (messageObject.hasReactions()) {
                for (TLRPC.TL_reactionCount reactionCount : messageObject.messageOwner.reactions.results) {
                    countReactions += reactionCount.count;
                }
            }
            menuItemReactionView = new MenuItemReactionView(getContext(), resourcesProvider, currentAccount, messageObject, countReactions);
            menuItemReactionView.setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), AndroidUtilities.dp(4), 0));
            menuItemReactionView.setMinimumWidth(AndroidUtilities.dp(POPUP_WIDTH));

            LayoutParams layoutParams = LayoutHelper.createFrame(POPUP_WIDTH, MENU_ACTION_HEIGHT, Gravity.LEFT);
            container.addView(menuItemReactionView, layoutParams);

            if ((countReactions > 0 && !isAddSeen) || countReactions > 10) {
                requestController.getMessageReactionsList(messageObject, 100, null, currentAccount, null, (reactedUsers, mapReactions, offset, count) ->
                        reactionLoaded(count, reactedUsers, null, mapReactions, offset)
                );
            } else if (countReactions > 0) {
                requestController.getMessageReactionsList(messageObject, 100, null, currentAccount, null, (recentReacted, mapReactions, filter, count) -> {
                    requestController.getReadParticipants(messageObject, currentAccount, currentChat, (recentRead, readPeerIds) -> {
                        Iterator<TLRPC.User> iterator = recentRead.listIterator();
                        while (iterator.hasNext()) {
                            TLRPC.User user = iterator.next();
                            if (mapReactions.containsKey(user.id)) {
                                iterator.remove();
                            }
                        }
                        reactionLoaded(count, recentReacted, recentRead, mapReactions, filter);
                    });
                });
            } else {
                requestController.getReadParticipants(messageObject, currentAccount, currentChat, (recentRead, readPeerIds) -> {
                    reactionLoaded(0, null, recentRead, null, null);
                });
            }

            divider = createDivider();
            container.addView(divider);
        }
    }

    View backAction;

    private void createSimpleSecondMenu(ReactionsRequestController requestController, MessageObject messageObject, int currentAccount, @Nullable List<TLRPC.User> readUsers, @Nullable List<TLRPC.User> reactedUsers, @Nullable Map<Long, String> userReactions, int reactionCount, @Nullable String offset) {
        if (childContainer == null) {
            addBackgroundView();
            ReactionsPagingController controller = new ReactionsPagingController(requestController, messageObject, currentAccount, readUsers, reactedUsers, userReactions, null, reactionCount, offset);
            childContainer = new FrameLayout(getContext());
            childContainer.setLayoutParams(new LayoutParams(AndroidUtilities.dp(POPUP_WIDTH), container.getMeasuredHeight()));

            backAction = createMenuItem(v -> animateHideMenu());
            childContainer.addView(backAction);
            childContainer.addView(createDivider());

            ChatReactionsList chatReactionsList = new ChatReactionsList(getContext(), resourcesProvider, controller, userSelectedListener);
            childContainer.addView(chatReactionsList);
            MarginLayoutParams itemsParams = ((MarginLayoutParams) chatReactionsList.getLayoutParams());
            itemsParams.topMargin = AndroidUtilities.dp(MENU_ACTION_HEIGHT + MENU_ITEM_DIVIDER_HEIGHT);
            chatReactionsList.setLayoutParams(itemsParams);
            childContainer.setTranslationX(container.getMeasuredWidth());

            container.addView(childContainer);
            dimLayout.invalidate();
        }
        animateShowMenu();
    }

    private ValueAnimator animator;

    public void animateShowReaction() {
        int startWidth = getReactionStartWidth();
        int endWidth = getReactionEndWidth();
        ValueAnimator animator = ValueAnimator.ofInt(startWidth, endWidth);

        animator.setInterpolator(AndroidUtilities.decelerateInterpolator);
        animator.addUpdateListener(animation -> {
            float progress = animation.getAnimatedFraction();
            reactionsView.setProgressAnimation(Math.min(progress, 1f));
            int value = (int) animation.getAnimatedValue();
            LayoutParams layoutParams = (LayoutParams) reactionsView.getLayoutParams();
            layoutParams.width = value;
            reactionsView.measure(MeasureSpec.makeMeasureSpec(value, MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(layoutParams.height, MeasureSpec.EXACTLY));
            reactionsView.layout(getMeasuredWidth() - value, reactionsView.getTop(), getMeasuredWidth(), reactionsView.getTop() + layoutParams.height);
        });
        animator.setDuration(220);
        animator.start();
    }

    public void animateInnerShowReaction() {

    }

    private final int translationSize = AndroidUtilities.dp(40);

    public void animateMenu(float progress) {
        childContainer.setTranslationX(container.getMeasuredWidth() * progress);
        if (reactionsView != null) {
            reactionsView.setAlpha(progress);
        }
        float translationX = -translationSize * (1f - progress);
        float translationY = translationX * 0.5f;
        if (menuItemReactionView != null) {
            menuItemReactionView.setTranslationX(translationX);
            menuItemReactionView.setTranslationY(translationY);
        }
        if (divider != null) {
            divider.setTranslationX(translationX);
            divider.setTranslationY(translationY);
        }
        if (itemsListView != null) {
            itemsListView.setTranslationX(translationX);
            itemsListView.setTranslationY(translationY);
        }
        dimLayout.invalidate();
    }

    public void animateShowMenu() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        animator = ValueAnimator.ofFloat(childContainerProgress(), 0f);
        animator.setInterpolator(AndroidUtilities.decelerateInterpolator);
        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            animateMenu(progress);
        });
        animator.addListener(new HideViewAfterAnimation(reactionsView));
        animator.start();
    }

    public float childContainerProgress() {
        if (childContainer == null) {
            return 1f;
        }
        return childContainer.getTranslationX() / container.getMeasuredWidth();
    }

    public void animateHideMenu() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        animator = ValueAnimator.ofFloat(childContainerProgress(), 1f);
        animator.setInterpolator(AndroidUtilities.decelerateInterpolator);
        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            animateMenu(progress);
        });
        animator.addListener(new ShowViewAfterAnimation(reactionsView));
        animator.start();
    }

    private void createTabsSecondMenu(ReactionsRequestController requestController, MessageObject messageObject, int currentAccount, @Nullable List<TLRPC.User> reactedUsers, Map<Long, String> userReactions, int allCount, @Nullable String offset) {
        if (childContainer == null) {
            addBackgroundView();
            childContainer = new FrameLayout(getContext());
            childContainer.setLayoutParams(new LayoutParams(AndroidUtilities.dp(POPUP_WIDTH), container.getMeasuredHeight()));

            backAction = createMenuItem(v -> animateHideMenu());
            childContainer.addView(backAction);

            ReactionTabPagerView pager = new ReactionTabPagerView(getContext());

            final ArrayList<String> filters = new ArrayList<>();
            final ArrayList<Integer> counts = new ArrayList<>();
            final ArrayList<String> titles = new ArrayList<>();
            final ArrayList<Object> icons = new ArrayList<>();
            if (messageObject.hasReactions()) {
                filters.add(null);
                counts.add(allCount);
                titles.add(LocaleController.formatShortNumber(allCount));
                icons.add(getContext().getResources().getDrawable(R.drawable.ic_all_reaction_tab).mutate());

                ArrayList<TLRPC.TL_reactionCount> sortedResults = new ArrayList<>(messageObject.messageOwner.reactions.results);
                Collections.sort(sortedResults, (o1, o2) -> Integer.compare(o2.count, o1.count));
                for (TLRPC.TL_reactionCount count: sortedResults) {
                    filters.add(count.reaction);
                    counts.add(count.count);
                    titles.add(LocaleController.formatShortNumber(count.count));
                    icons.add(requestController.getStaticIcon(count.reaction));
                }
            }

            pager.setAdapter(new ReactionTabPagerView.Adapter() {

                @Override
                public int getItemCount() {
                    return filters.size();
                }

                @Override
                public View createView(int position) {
                    ArrayList<TLRPC.User> filtered;
                    final Map<Long, String> reactions;
                    String filter = filters.get(position);
                    String offsetNext;
                    if (filter == null) {
                        reactions = userReactions;
                        filtered = new ArrayList<>(reactedUsers);
                        offsetNext = offset;
                    } else {
                        filtered = new ArrayList<>();
                        reactions = new HashMap<>();
                        offsetNext = null;
                        for (TLRPC.User user: users) {
                            String reaction = userReactions.get(user.id);
                            if (filter.equals(reaction)) {
                                filtered.add(user);
                                reactions.put(user.id, filter);
                            }
                        }
                    }
                    ReactionsPagingController controller = new ReactionsPagingController(requestController, messageObject, currentAccount, null, filtered, reactions, filter, counts.get(position), offsetNext);
                    FrameLayout parent = new FrameLayout(getContext());
                    ChatReactionsList list = new ChatReactionsList(getContext(), resourcesProvider, controller, userSelectedListener);
                    parent.addView(list);
                    return parent;
                }

                @Override
                public void bindView(View view, int position, int viewType) {

                }

                @Override
                public String getItemTitle(int position) {
                    return titles.get(position);
                }

                @Override
                public Object getIcon(int position) {
                    return icons.get(position);
                }
            });
            ReactionTabPagerView.TabsView tabsView = pager.createTabsView();

            childContainer.addView(tabsView, LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT, MENU_ITEM_TABS_HEIGHT, Gravity.TOP | Gravity.LEFT, 0, MENU_ACTION_HEIGHT, 0, 0));
            childContainer.addView(pager, LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, MENU_ACTION_HEIGHT + MENU_ITEM_TABS_HEIGHT, 0, 0));

            childContainer.setTranslationX(container.getMeasuredWidth());

            container.addView(childContainer);
            dimLayout.invalidate();
        }
        animateShowMenu();
    }

    private void reactionLoaded(int count, @Nullable List<TLRPC.User> reactedUsers, @Nullable List<TLRPC.User> readUsers, HashMap<Long, String> userReactions, String offset) {

        int reactedCount = reactedUsers != null ? reactedUsers.size(): 0;
        int readCount = (readUsers != null ? readUsers.size(): 0);
        int sumCount = reactedCount + readCount;

        if (sumCount == 1) {
            final TLRPC.User user = reactedUsers != null? reactedUsers.get(0): readUsers.get(0);
            menuItemReactionView.setTitle(ContactsController.formatName(user.first_name, user.last_name));
            if (userReactions != null && !userReactions.isEmpty()) {
                TLRPC.Document sticker = requestController.getStaticIcon(userReactions.get(user.id));
                menuItemReactionView.setReaction(sticker);
            }
            menuItemReactionView.setOnClickListener(v -> {
                if (userSelectedListener != null) {
                    userSelectedListener.onSelected(user);
                }
            });
        } else {
            boolean isVoice = (messageObject.isRoundVideo() || messageObject.isVoice());
            if (reactedCount == 0 && readCount > 0) {
                menuItemReactionView.setTitle(LocaleController.formatPluralString(isVoice ? "MessagePlayed" : "MessageSeen", readCount));
                menuItemReactionView.setOnClickListener(v -> createSimpleSecondMenu(requestController, messageObject, currentAccount, readUsers, reactedUsers, userReactions, count, offset));
            } else if (reactedCount > 10 && readCount == 0) {
                menuItemReactionView.setTitle(LocaleController.formatString("Reacted", R.string.Reacted, String.valueOf(count)));
                menuItemReactionView.setOnClickListener(v -> createTabsSecondMenu(requestController, messageObject, currentAccount, reactedUsers, userReactions, count, offset));
            } else if (reactedCount > 0) {
                menuItemReactionView.setTitle(LocaleController.formatString("Reacted", R.string.Reacted, count + "/" + sumCount));
                menuItemReactionView.setOnClickListener(v -> createSimpleSecondMenu(requestController, messageObject, currentAccount, readUsers, reactedUsers, userReactions, count, offset));
            } else {
                menuItemReactionView.setTitle(LocaleController.formatPluralString(isVoice ? "MessagePlayed" : "MessageSeen", 0));
            }
        }

        List<TLRPC.User> avatarUsers = new ArrayList<>(3);
        if (reactedUsers != null) {
            for (int i = 0; i < Math.min(3, reactedUsers.size()); i++) {
                avatarUsers.add(reactedUsers.get(i));
            }
        }
        if (readUsers != null) {
            for (int i = 0; i < Math.min(3 - avatarUsers.size(), readUsers.size()); i++) {
                avatarUsers.add(readUsers.get(i));
            }
        }
        if (!avatarUsers.isEmpty()) {
            menuItemReactionView.updateRecentAvatars(avatarUsers);
        }
        menuItemReactionView.animateLoaded();
    }

    public void addItems(@Nullable List<ChatMenuItem> items, @Nullable ChatPopupItemsListView.ChatMenuItemClickListener selectedListener) {
        if (items != null && !items.isEmpty()) {
            boolean isTop = menuItemReactionView == null;
            itemsListView = new ChatPopupItemsListView(getContext(), resourcesProvider, messageObject, items, isTop, selectedListener);
            container.addView(itemsListView, LayoutHelper.createFrame(POPUP_WIDTH, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        }
    }

    public void buildMargins() {
        MarginLayoutParams containerParams = ((MarginLayoutParams) container.getLayoutParams());
        if (sponsoredCell != null) {
            containerParams.topMargin = AndroidUtilities.dp(SPONSORED_HEIGHT + SPONSORED_PADDING) + shadowPadding.top;
        }
        if (reactionsView != null) {
            containerParams.leftMargin = AndroidUtilities.dp(AndroidUtilities.dp(5));
            containerParams.topMargin = AndroidUtilities.dp(REACTION_TOP_MARGIN);
            containerParams.rightMargin = AndroidUtilities.dp(REACTION_RIGHT_MARGIN_TWO);
        }
        container.setLayoutParams(containerParams);
        if (menuItemReactionView != null && itemsListView != null) {
            MarginLayoutParams itemsParams = ((MarginLayoutParams) itemsListView.getLayoutParams());
            itemsParams.topMargin = AndroidUtilities.dp(MENU_ACTION_HEIGHT + MENU_ITEM_DIVIDER_HEIGHT);
            itemsListView.setLayoutParams(itemsParams);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int left = -shadowPadding.left;
        int top = -shadowPadding.top;
        int right = shadowPadding.right;
        int bottom = shadowPadding.bottom;

        if (container.getChildCount() > 0 && container.getMeasuredHeight() > 0) {
            left += container.getLeft();
            top += container.getTop();
            right += container.getLeft() + container.getMeasuredWidth();
            bottom += container.getTop() + container.getMeasuredHeight();
            shadowDrawable.setBounds(left, top, right, bottom);
            shadowDrawable.draw(canvas);
        }
        if (sponsoredCell != null) {
            left = -shadowPadding.left;
            top = -shadowPadding.top;
            right = shadowPadding.right;
            bottom = shadowPadding.bottom;

            left += sponsoredCell.getLeft();
            top += sponsoredCell.getTop();
            right += sponsoredCell.getLeft() + sponsoredCell.getMeasuredWidth();
            bottom += sponsoredCell.getTop() + sponsoredCell.getMeasuredHeight();
            shadowDrawable.setBounds(left, top, right, bottom);
            shadowDrawable.draw(canvas);
        }
    }

    public Rect getPaddingPopup() {
        return new Rect();
    }

    public void setDispatchKeyEventListener(ActionBarPopupWindow.OnDispatchKeyEventListener listener) {
        dispatchKeyEventListener = listener;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (dispatchKeyEventListener != null) {
            dispatchKeyEventListener.onDispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }

    public void setBackgroundColor(int color) {
        if (backgroundColor != color) {
            shadowDrawable.setColorFilter(new PorterDuffColorFilter(backgroundColor = color, PorterDuff.Mode.MULTIPLY));
            secondContentPaint.setColor(color);
        }
    }

    private int getThemedColor(String key) {
        Integer color = resourcesProvider != null ? resourcesProvider.getColor(key) : null;
        return color != null ? color : Theme.getColor(key);
    }
}
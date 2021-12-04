package org.telegram.ui.Reactions;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ManageChatUserCell;
import org.telegram.ui.Cells.ReactionCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerItemsEnterAnimator;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ReactionsActivity extends BaseFragment {

    private ListAdapter listViewAdapter;
    private RecyclerListView listView;

    private long chatId;
    private TLRPC.Chat currentChat;
    private TLRPC.ChatFull info;

    private int enableRow;
    private int enableHintRow;
    private int headerListRow;
    private int loadingListRow;
    private int lastDivider;
    private int reactionsStartRow;
    private int reactionsEndRow;
    boolean reactionsLoading;

    private int rowCount;

    private boolean isReactionsEnable = false;

    private boolean isOpened;

    private RecyclerItemsEnterAnimator recyclerItemsEnterAnimator;
    private ArrayList<TLRPC.TL_availableReaction> reactions = new ArrayList<>();
    private Set<String> availableSet = new HashSet<>(11);

    public ReactionsActivity(Bundle args) {
        super(args);
        chatId = arguments.getLong("chat_id");
        currentChat = getMessagesController().getChat(chatId);
    }

    public void updateChatFullInfo(@NonNull TLRPC.ChatFull info) {
        isReactionsEnable = info.available_reactions != null && !info.available_reactions.isEmpty();
        ArrayList<String> available_reactions = info.available_reactions;
        if (available_reactions != null && !available_reactions.isEmpty()) {
            availableSet.clear();
            availableSet.addAll(available_reactions);
        }
    }

    public void makeEnableSetAvailable() {
        availableSet.clear();
        if (reactions != null) {
            for (TLRPC.TL_availableReaction reaction: reactions) {
                availableSet.add(reaction.reaction);
            }
        }
    }

    private void loadReactions(boolean notify) {
        reactionsLoading = true;
        int reqId = getAccountInstance().getReactionsController().getAvailableReactions((response, error) -> {
            AndroidUtilities.runOnUIThread(() -> getNotificationCenter().doOnIdle(() -> {
                DiffCallback callback = saveListState();
                reactionsLoading = false;

                boolean updateByDiffUtils = false;

                if (error == null) {
                    TLRPC.TL_messages_availableReactions availableReactions = (TLRPC.TL_messages_availableReactions) response;

                    this.reactions.clear();
                    this.reactions.addAll(availableReactions.reactions);

                    int oldRowsCount = rowCount;
                    if (this.reactions.size() > 0 && isOpened) {
                        if (recyclerItemsEnterAnimator != null && !isPaused) {
                            recyclerItemsEnterAnimator.showItemsAnimated(oldRowsCount + 1);
                        }
                    } else {
                        updateByDiffUtils = true;
                    }
                }

                if (reactions.size() > 5) {
                    resumeDelayedFragmentAnimation();
                }

                if (updateByDiffUtils && listViewAdapter != null && listView.getChildCount() > 0) {
                    updateRecyclerViewAnimated(callback);
                } else {
                    updateRows(true);
                }
            }));
        });
        if (reqId != -1) {
            getConnectionsManager().bindRequestToGuid(reqId, getClassGuid());
        }
        if (notify) {
            updateRows(true);
        }
    }

    private void setReactionsEnable(boolean isEnabled) {
        isReactionsEnable = isEnabled;
        if (!isReactionsEnable) {
            availableSet.clear();
        } else {
            makeEnableSetAvailable();
        }
        updateRows(true);
    }

    private void saveReactions() {
        TLRPC.TL_messages_setChatAvailableReactions req = new TLRPC.TL_messages_setChatAvailableReactions();
        req.peer = getMessagesController().getInputPeer(-chatId);

        req.available_reactions = new ArrayList<>(availableSet);

        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response != null) {
                AndroidUtilities.runOnUIThread(() -> {
                    TLRPC.ChatFull chatFull = getMessagesController().getChatFull(chatId);
                    if (chatFull != null) {
                        chatFull.available_reactions = new ArrayList<>(availableSet);
                        getMessagesStorage().updateChatInfo(chatFull, false);
                        getNotificationCenter().postNotificationName(NotificationCenter.chatInfoDidLoad, chatFull, 0, false, false);
                    }
                });
            }
        }, ConnectionsManager.RequestFlagInvokeAfter);
    }

    @Override
    public boolean onBackPressed() {
        saveReactions();
        return super.onBackPressed();
    }

    private void updateRows(boolean notify) {
        currentChat = MessagesController.getInstance(currentAccount).getChat(chatId);
        if (currentChat == null) {
            return;
        }

        enableRow = -1;
        enableHintRow = -1;
        headerListRow = -1;
        loadingListRow = -1;
        reactionsStartRow = -1;
        reactionsEndRow = -1;
        lastDivider = -1;

        rowCount = 0;

        enableRow = rowCount++;
        enableHintRow = rowCount++;
        if (isReactionsEnable) {

            if (!reactions.isEmpty()) {
                headerListRow = rowCount++;
                reactionsStartRow = rowCount;
                rowCount += reactions.size();
                reactionsEndRow = rowCount;
            }

            if (reactionsLoading) {
                loadingListRow = rowCount++;
            }

            if (!reactions.isEmpty()) {
                lastDivider = rowCount++;
            }
        }

        if (listViewAdapter != null && notify) {
            listViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("Reactions", R.string.Reactions));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    saveReactions();
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView.setTag(Theme.key_windowBackgroundGray);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                recyclerItemsEnterAnimator.dispatchDraw();
                super.dispatchDraw(canvas);
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                recyclerItemsEnterAnimator.onDetached();
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        listView.setLayoutManager(layoutManager);
        listView.setAdapter(listViewAdapter = new ListAdapter(context));
        listView.setHasFixedSize(true);
        recyclerItemsEnterAnimator = new RecyclerItemsEnterAnimator(listView, false);
        DefaultItemAnimator defaultItemAnimator = new DefaultItemAnimator();
        defaultItemAnimator.setDelayAnimations(false);
        defaultItemAnimator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(defaultItemAnimator);
        listView.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (view instanceof ReactionCheckCell) {
                ReactionCheckCell checkCell = (ReactionCheckCell) view;
                boolean newChecked = !checkCell.isChecked();
                TLRPC.TL_availableReaction reaction = (TLRPC.TL_availableReaction) checkCell.getTag();
                if (newChecked) {
                    availableSet.add(reaction.reaction);
                } else {
                    availableSet.remove(reaction.reaction);
                }
                checkCell.setChecked(newChecked);
            }
        });

        updateRows(true);
        return fragmentView;
    }

    public void setInfo(TLRPC.ChatFull chatFull) {
        info = chatFull;
        if (info != null) {
            updateChatFullInfo(info);
        }
         loadReactions(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listViewAdapter != null) {
            listViewAdapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        private static final int TYPE_REACTIONS_ENABLE = 0;
        private static final int TYPE_REACTIONS_HINT = 1;
        private static final int TYPE_REACTIONS_LIST_HEADER = 2;
        private static final int TYPE_REACTIONS_ITEM = 3;
        private static final int TYPE_FLICKER = 4;
        private static final int TYPE_LAST_DIVIDER = 5;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (enableRow == position) {
                return true;
            } else if (position >= reactionsStartRow && position < reactionsEndRow) {
                return true;
            }
            return false;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        public TLRPC.TL_availableReaction getItem(int position) {
            if (position >= reactionsStartRow && position < reactionsEndRow) {
                return reactions.get(position - reactionsStartRow);
            }
            return null;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case TYPE_REACTIONS_ENABLE:
                    TextCheckCell enableReactions = new TextCheckCell(mContext) {
                        @Override
                        protected void onDraw(Canvas canvas) {
                            canvas.save();
                            canvas.clipRect(0, 0, getWidth(), getHeight());
                            super.onDraw(canvas);
                            canvas.restore();
                        }
                    };
                    enableReactions.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundUnchecked));
                    enableReactions.setColors(Theme.key_windowBackgroundCheckText, Theme.key_switchTrackBlue, Theme.key_switchTrackBlueChecked, Theme.key_switchTrackBlueThumb, Theme.key_switchTrackBlueThumbChecked);
                    enableReactions.setDrawCheckRipple(true);
                    enableReactions.setEnabled(false);
                    enableReactions.setHeight(56);
                    enableReactions.setTag(Theme.key_windowBackgroundUnchecked);
                    enableReactions.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    enableReactions.setOnClickListener(v -> {
                        TextCheckCell cell = (TextCheckCell) v;
                        boolean newIsChecked = !cell.isChecked();
                        cell.setBackgroundColorAnimated(newIsChecked, Theme.getColor(newIsChecked ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked));
                        cell.setChecked(newIsChecked);
                        setReactionsEnable(newIsChecked);
                    });
                    enableReactions.setBackgroundColor(Theme.getColor(isReactionsEnable ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked));
                    enableReactions.setChecked(isReactionsEnable);
                    view = enableReactions;
                    break;
                case TYPE_REACTIONS_HINT:
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    break;
                case TYPE_REACTIONS_LIST_HEADER:
                    HeaderCell headerCell = new HeaderCell(mContext, Theme.key_windowBackgroundWhiteBlueHeader, 21, 11, false);
                    headerCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    headerCell.setHeight(43);
                    view = headerCell;
                    break;
                case TYPE_REACTIONS_ITEM:
                    ReactionCheckCell reactionCheckCell = new ReactionCheckCell(mContext);
                    reactionCheckCell.setDrawCheckRipple(true);
                    reactionCheckCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    view = reactionCheckCell;
                    break;
                case TYPE_FLICKER:
                    FlickerLoadingView flickerLoadingView = new FlickerLoadingView(mContext);
                    flickerLoadingView.setIsSingleCell(true);
                    flickerLoadingView.setViewType(FlickerLoadingView.USERS_TYPE);
                    flickerLoadingView.showDate(false);
                    flickerLoadingView.setPaddingLeft(AndroidUtilities.dp(5));
                    flickerLoadingView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    flickerLoadingView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    view = flickerLoadingView;
                    break;
                case TYPE_LAST_DIVIDER:
                    view = new ShadowSectionCell(mContext);
                    view.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    break;
                default:
                    throw new IllegalArgumentException("Not supported viewType " + viewType);
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (getItemViewType(position)) {
                case TYPE_REACTIONS_ENABLE:
                    TextCheckCell enableReactions = (TextCheckCell) holder.itemView;
                    enableReactions.setTextAndCheck(LocaleController.getString("ReactionsEnable", R.string.ReactionsEnable), isReactionsEnable, false);
                    enableReactions.setEnabled(!reactions.isEmpty());
                    break;
                case TYPE_REACTIONS_HINT:
                    TextInfoPrivacyCell privacyCell = (TextInfoPrivacyCell) holder.itemView;
                    privacyCell.setText(LocaleController.getString("ReactionsEnableHint", R.string.ReactionsEnableHint));
                    break;
                case TYPE_REACTIONS_LIST_HEADER:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(LocaleController.getString("ReactionsAvailable", R.string.ReactionsAvailable));
                    break;
                case TYPE_REACTIONS_ITEM:
                    ReactionCheckCell checkCell = (ReactionCheckCell) holder.itemView;
                    TLRPC.TL_availableReaction reaction = getItem(position);
                    checkCell.setTag(reaction);
                    checkCell.setTextAndValueDrawable(reaction.title, availableSet.contains(reaction.reaction), reaction.static_icon, true);
                    break;
                case TYPE_FLICKER:
                    FlickerLoadingView flickerLoadingView = (FlickerLoadingView) holder.itemView;
                    flickerLoadingView.setItemsCount(10);
                    break;
            }
        }

        @Override
        public void onViewRecycled(RecyclerView.ViewHolder holder) {
            if (holder.itemView instanceof ManageChatUserCell) {
                ((ManageChatUserCell) holder.itemView).recycle();
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == enableRow) {
                return TYPE_REACTIONS_ENABLE;
            } else if (position == enableHintRow) {
                return TYPE_REACTIONS_HINT;
            } else if (position == headerListRow) {
                return TYPE_REACTIONS_LIST_HEADER;
            } else if ((position >= reactionsStartRow && position < reactionsEndRow)) {
                return TYPE_REACTIONS_ITEM;
            } else if (position == loadingListRow) {
                return TYPE_FLICKER;
            } else if (position == lastDivider) {
                return TYPE_LAST_DIVIDER;
            }
            return 1;
        }
    }

    private void updateRecyclerViewAnimated(DiffCallback callback) {
        if (isPaused || listViewAdapter == null || listView == null) {
            updateRows(true);
            return;
        }
        updateRows(false);
        callback.fillPositions(callback.newPositionToItem);
        DiffUtil.calculateDiff(callback).dispatchUpdatesTo(listViewAdapter);
        AndroidUtilities.updateVisibleRows(listView);
    }


    private class DiffCallback extends DiffUtil.Callback {

        int oldRowCount;
        int oldReactionsStartRow;
        int oldReactionsEndRow;

        SparseIntArray oldPositionToItem = new SparseIntArray();
        SparseIntArray newPositionToItem = new SparseIntArray();
        ArrayList<TLRPC.TL_availableReaction> oldReactions = new ArrayList<>();

        @Override
        public int getOldListSize() {
            return oldRowCount;
        }

        @Override
        public int getNewListSize() {
            return rowCount;
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            if (oldItemPosition >= oldReactionsStartRow && oldItemPosition < oldReactionsEndRow && newItemPosition >= reactionsStartRow && newItemPosition < reactionsEndRow) {
                return oldReactions.get(oldItemPosition - oldReactionsStartRow).equals(reactions.get(newItemPosition - reactionsStartRow));
            }
            int oldItem = oldPositionToItem.get(oldItemPosition, -1);
            int newItem = newPositionToItem.get(newItemPosition, -1);
            return oldItem >= 0 && oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return areItemsTheSame(oldItemPosition, newItemPosition);
        }

        public void fillPositions(SparseIntArray sparseIntArray) {
            sparseIntArray.clear();
            int pointer = 0;
            put(++pointer, enableRow, sparseIntArray);
            put(++pointer, enableHintRow, sparseIntArray);
            put(++pointer, headerListRow, sparseIntArray);
            put(++pointer, loadingListRow, sparseIntArray);
            put(++pointer, lastDivider, sparseIntArray);
        }

        private void put(int id, int position, SparseIntArray sparseIntArray) {
            if (position >= 0) {
                sparseIntArray.put(position, id);
            }
        }
    }

    private DiffCallback saveListState() {
        DiffCallback callback = new DiffCallback();
        callback.fillPositions(callback.oldPositionToItem);
        callback.oldReactionsStartRow = reactionsStartRow;
        callback.oldReactionsEndRow = reactionsEndRow;
        callback.oldRowCount = rowCount;
        callback.oldReactions.clear();
        callback.oldReactions.addAll(reactions);

        return callback;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, null, null, null, null, Theme.key_windowBackgroundGray));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, null, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{HeaderCell.class, ReactionCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));


        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));
        return themeDescriptions;
    }

    @Override
    public boolean needDelayOpenAnimation() {
        return true;
    }

    int animationIndex = -1;

    @Override
    protected void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        super.onTransitionAnimationEnd(isOpen, backward);
        if (isOpen) {
            isOpened = true;
        }
        NotificationCenter.getInstance(currentAccount).onAnimationFinish(animationIndex);
    }

    @Override
    protected void onTransitionAnimationStart(boolean isOpen, boolean backward) {
        super.onTransitionAnimationStart(isOpen, backward);
        animationIndex = NotificationCenter.getInstance(currentAccount).setAnimationInProgress(animationIndex, null);
    }
}

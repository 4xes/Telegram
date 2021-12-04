package org.telegram.ui.Popup;

import static org.telegram.ui.Popup.ChatPopupWindow.POPUP_WIDTH;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.List;

@SuppressLint("ViewConstructor")
public class ChatPopupItemsListView extends RecyclerListView {

    private final List<ChatMenuItem> menuItems;
    private final boolean isFirstView;
    private final MessageObject selectedObject;

    private static final int TYPE_OPTION_DELETE = 1;

    private ActionBarMenuSubItem deleteItem;
    private Runnable updateDeleteItemRunnable;

    private static final int MENU_ITEM_HEIGHT = 48;

    public ChatPopupItemsListView(Context context, Theme.ResourcesProvider resourcesProvider, MessageObject selectObject, List<ChatMenuItem> menuItems, boolean isFirstView, @Nullable ChatMenuItemClickListener selectedListener) {
        super(context, resourcesProvider);
        this.menuItems = menuItems;
        this.isFirstView = isFirstView;
        this.selectedObject = selectObject;
        setSelectorDrawableColor(Color.TRANSPARENT);
        setLayoutManager(new LinearLayoutManager(getContext()));
        setAdapter(new ReactionsAdapter());
        setOnItemClickListener((view, position) -> {
                if (selectedListener != null) {
                    ChatMenuItem item = menuItems.get(position);
                    selectedListener.onMenuClick(item.option);
                }
            }
        );
    }

    private class ReactionsAdapter extends SelectionAdapter {

        private static final int TYPE_ITEM = 0;
        private static final int TYPE_TIMER_DELETE = 1;

        @Override
        public boolean isEnabled(ViewHolder holder) {
            return true;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ActionBarMenuSubItem cell = new ActionBarMenuSubItem(parent.getContext(), true, true, resourcesProvider);
            cell.setItemHeight(MENU_ITEM_HEIGHT);
            cell.setLayoutParams(LayoutHelper.createFrame(POPUP_WIDTH, MENU_ITEM_HEIGHT));
            return new Holder(cell);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ActionBarMenuSubItem cell = (ActionBarMenuSubItem) holder.itemView;
            ChatMenuItem item = menuItems.get(position);
            cell.setTextAndIcon(item.title, item.icon);
            boolean isTop = isFirstView && position == 0;
            boolean isBottom = position == getItemCount() - 1;
            cell.updateSelectorBackground(isTop, isBottom);

            if (getItemViewType(position) == TYPE_TIMER_DELETE) {
                deleteItem = cell;
                startUpdateDeleteRunnable();
            }
        }

        @Override
        public int getItemCount() {
           return menuItems.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (isDeleteTimer(position)) {
                return TYPE_TIMER_DELETE;
            }
            return TYPE_ITEM;
        }

        public boolean isDeleteTimer(int position) {
            ChatMenuItem item = getItem(position);
            return item.option == TYPE_OPTION_DELETE && selectedObject.messageOwner.ttl_period != 0;
        }

        public ChatMenuItem getItem(int position) {
            return menuItems.get(position);
        }
    }

    public interface ChatMenuItemClickListener {
        void onMenuClick(int option);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        startUpdateDeleteRunnable();
    }

    public void startUpdateDeleteRunnable() {
        if (updateDeleteItemRunnable == null && deleteItem != null) {
            updateDeleteItemRunnable = () -> {
                if (deleteItem == null) {
                    updateDeleteItemRunnable = null;
                    return;
                }
                int remaining = Math.max(0, selectedObject.messageOwner.ttl_period - (AccountInstance.getInstance(UserConfig.selectedAccount).getConnectionsManager().getCurrentTime() - selectedObject.messageOwner.date));
                String remainingText;
                if (remaining < 24 * 60 * 60) {
                    remainingText = AndroidUtilities.formatDuration(remaining, false);
                } else {
                    remainingText = LocaleController.formatPluralString("Days", Math.round(remaining / (24 * 60 * 60.0f)));
                }
                deleteItem.setSubtext(LocaleController.formatString("AutoDeleteIn", R.string.AutoDeleteIn, remainingText));
                deleteItem.setSubtextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText6));
                AndroidUtilities.runOnUIThread(updateDeleteItemRunnable, 1000);
            };
            AndroidUtilities.runOnUIThread(updateDeleteItemRunnable);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        updateDeleteItemRunnable = null;
    }
}

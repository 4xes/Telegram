package org.telegram.ui.Popup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Reactions.ReactionsPagingController;
import org.telegram.ui.Reactions.UserReactionCell;

@SuppressLint("NotifyDataSetChanged")
public class ChatReactionsList extends RecyclerListView {

    ReactionsPagingController controller;
    final LinearLayoutManager layoutManager;
    final ReactionsAdapter adapter;

    public ChatReactionsList(Context context, Theme.ResourcesProvider resourcesProvider, @Nullable ReactionsPagingController controller) {
        super(context, resourcesProvider);
        setSelectorDrawableColor(Color.TRANSPARENT);
        setHasFixedSize(true);
        this.controller = controller;

        setLayoutManager(layoutManager = new LinearLayoutManager(getContext()));
        addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkLoading();
            }
        });
        adapter = new ReactionsAdapter();
        adapter.updateIndexes();
        if (this.controller != null) {
            if (!controller.isEnded()) {
                controller.load(this::update);
            }
        }
        setAdapter(adapter);
    }

    public void setController(ReactionsPagingController controller) {
        this.controller = controller;
        if (!controller.isEnded()) {
            controller.load(this::update);
        }
        adapter.updateIndexes();
        adapter.notifyDataSetChanged();
    }

    public void update(int oldSize, int added) {
        adapter.updateIndexes();
        adapter.notifyDataSetChanged();
    }

    private void checkLoading() {
        if (controller == null) {
            return;
        }
        if (controller.isEnded() || controller.isLoading()) {
            return;
        }
        int position = layoutManager.findLastVisibleItemPosition();
        if (position == controller.getItemSize()) {
            controller.load(this::update);
        }
    }


    private class ReactionsAdapter extends SelectionAdapter {

        final static int TYPE_USER_CELL = 1;
        final static int TYPE_FLICKER = 2;

        int usersRowStart = -1;
        int usersRowEnd = -1;
        int flickerRow = -1;

        public void updateIndexes() {

            usersRowStart = -1;
            usersRowEnd = -1;
            flickerRow = -1;

            int rowCount = 0;
            if (controller == null) {
                usersRowStart = 0;
                return;
            }
            if (controller.getItemSize() > 0) {
                usersRowStart = rowCount;
                rowCount += controller.getItemSize();
                usersRowEnd = rowCount;
            }
            if (controller.isLoading() || !controller.isEnded()) {
                flickerRow = rowCount;
            }
        }

        @Override
        public boolean isEnabled(ViewHolder holder) {
            return holder.itemView instanceof UserReactionCell;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_USER_CELL) {
                UserReactionCell cell = new UserReactionCell(parent.getContext(), true, true, resourcesProvider);
                cell.setMinimumWidth(AndroidUtilities.dp(ChatPopupWindow.POPUP_WIDTH));
                view = cell;
            } else {
                FlickerLoadingView flickerLoadingView = new FlickerLoadingView(getContext());
                flickerLoadingView.setIsSingleCell(true);
                flickerLoadingView.setViewType(FlickerLoadingView.MESSAGE_SEEN_TYPE);
                flickerLoadingView.setItemsCount(5);
                flickerLoadingView.showDate(false);
                flickerLoadingView.setPaddingLeft(AndroidUtilities.dp(5));
                flickerLoadingView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                flickerLoadingView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                view = flickerLoadingView;
            }
            return new Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == flickerRow) {
                return TYPE_FLICKER;
            } else if ((position >= usersRowStart && position < usersRowEnd)) {
                return TYPE_USER_CELL;
            }
            return 1;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_USER_CELL) {
                UserReactionCell cell = (UserReactionCell) holder.itemView;
                TLRPC.User user = controller.getUser(position);
                TLRPC.Document reaction = controller.getReaction(user.id);
                cell.setUser(user, reaction);
            }
        }

        @Override
        public int getItemCount() {
            if (controller == null) {
                return 1;
            }
            if (!controller.isEnded()) {
                return controller.getItemSize() + 1;
            } else {
                return controller.getItemSize();
            }
        }

        public TLRPC.User getItem(int position) {
            return controller.getUser(position);
        }
    }

    public interface ChatMenuItemClickListener {
        void onMenuClick(int option);
    }
}

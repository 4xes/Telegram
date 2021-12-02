package org.telegram.ui.Popup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Reactions.ReactionCell;
import org.telegram.ui.Reactions.ReactionsBubbleListView;
import org.telegram.ui.Reactions.ReactionsBubbleView;

import java.util.List;

public class ChatPopupWindow extends FrameLayout {

    private static final int MIN_WIDTH = 262;
    public static final int POPUP_WIDTH = 220;
    private static final int REACTION_TOP_MARGIN = 60;
    private static final int REACTION_MAX_WIDTH = 266;
    private static final int POPUP_ITEMS_MAX_HEIGHT = 340;
    private static final int POPUP_BOTTOM_FILL = 70;

    private static final int MENU_ITEM_HEADER = 44;
    private static final int MENU_ITEM_DIVIDER = 8;
    private static final int SPONSORED_HEIGHT = 56;
    private static final int SPONSORED_PADDING = 6;

    private FrameLayout menuContainer;
    private ReactionsBubbleView reactionsView;
    private ActionBarMenuSubItem sponsoredCell;

    private ActionBarPopupWindow.OnDispatchKeyEventListener dispatchKeyEventListener;
    protected LinearLayout linearLayout;

    private final Theme.ResourcesProvider resourcesProvider;

    private final MessageObject messageObject;

    private int backgroundColor = Color.WHITE;
    private Drawable shadowDrawable = getContext().getResources().getDrawable(R.drawable.popup_fixed_alert).mutate();

    private Rect shadowPadding = new Rect();

    public ChatPopupWindow(Context context, Theme.ResourcesProvider resourcesProvider, MessageObject messageObject) {
        super(context);
        setBackgroundColor(getThemedColor(Theme.key_actionBarDefaultSubmenuBackground));
        this.resourcesProvider = resourcesProvider;
        this.messageObject = messageObject;
        setMinimumWidth(AndroidUtilities.dp(POPUP_WIDTH));
        shadowDrawable.getPadding(shadowPadding);

        menuContainer = new FrameLayout(getContext());
        addView(menuContainer);

        MarginLayoutParams layoutParams = ((MarginLayoutParams) menuContainer.getLayoutParams());
        layoutParams.setMargins(shadowPadding.left, shadowPadding.top, shadowPadding.right, shadowPadding.bottom);

        setWillNotDraw(false);
    }

    public void addSponsored(OnClickListener onClickListener) {
        sponsoredCell = new ActionBarMenuSubItem(getContext(), true, true, resourcesProvider);
        sponsoredCell.setTextAndIcon(LocaleController.getString("SponsoredMessageInfo", R.string.SponsoredMessageInfo), R.drawable.menu_info);
        sponsoredCell.setOnClickListener(onClickListener);
        sponsoredCell.setItemHeight(SPONSORED_HEIGHT);
        sponsoredCell.setMultiline();
        sponsoredCell.setMinimumWidth(AndroidUtilities.dp(POPUP_WIDTH));
        FrameLayout.LayoutParams layoutParams = LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL? Gravity.LEFT : Gravity.RIGHT);
        layoutParams.setMargins(shadowPadding.left, shadowPadding.top, shadowPadding.right, shadowPadding.bottom);
        addView(sponsoredCell, layoutParams);
    }

    public void addReactions(List<TLRPC.TL_availableReaction> reactions,
                             @Nullable ReactionsBubbleListView.ReactionSelectedListener selectedListener) {
        if (reactions != null && !reactions.isEmpty()) {
            reactionsView = new ReactionsBubbleView(getContext(), reactions, selectedListener);
            reactionsView.setBackgroundColor(getThemedColor(Theme.key_actionBarDefaultSubmenuBackground));

            int listContentWidth = AndroidUtilities.dp(ReactionCell.SIZE_CELL) * reactions.size() +
                    (AndroidUtilities.dp(ReactionsBubbleListView.SPACING) * (reactions.size() - 1)) +
                    AndroidUtilities.dp(ReactionsBubbleListView.HORIZONTAL_PADDING) * 2;

            int maxWidth = Math.min(AndroidUtilities.dp(REACTION_MAX_WIDTH), listContentWidth);

            Rect paddingRect = reactionsView.getPaddingDrawable();

            int width = paddingRect.left + maxWidth + paddingRect.right;
            int height = reactionsView.getContentHeight() + paddingRect.top + paddingRect.bottom;

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
            layoutParams.setMargins(0, paddingRect.top, 0, 0);
            addView(reactionsView, layoutParams);
        }
    }

    public void addItems(@Nullable List<ChatMenuItem> items, @Nullable ChatPopupItemsListView.ChatMenuItemClickListener selectedListener) {
        if (items != null && !items.isEmpty()) {
            //Убрать округление сверху, если есть вьюхи сверху
            ChatPopupItemsListView itemsListView = new ChatPopupItemsListView(getContext(), resourcesProvider, messageObject, items, true, selectedListener);
            menuContainer.addView(itemsListView);
        }
    }

    public void buildMargins() {
        MarginLayoutParams layoutParams = ((MarginLayoutParams) menuContainer.getLayoutParams());
        if (sponsoredCell != null) {
            layoutParams.topMargin = AndroidUtilities.dp(SPONSORED_HEIGHT + SPONSORED_PADDING) + shadowPadding.top;
        }
        if (reactionsView != null) {
            layoutParams.topMargin = AndroidUtilities.dp(REACTION_TOP_MARGIN);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int left = -shadowPadding.left;
        int top = -shadowPadding.top;
        int right = shadowPadding.right;
        int bottom = shadowPadding.bottom;

        if (menuContainer.getChildCount() > 0 && menuContainer.getMeasuredHeight() > 0) {
            left += menuContainer.getLeft();
            top += menuContainer.getTop();
            right += menuContainer.getLeft() + menuContainer.getMeasuredWidth();
            bottom += menuContainer.getTop() + menuContainer.getMeasuredHeight();
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
        }
    }

    private int getThemedColor(String key) {
        Integer color = resourcesProvider != null ? resourcesProvider.getColor(key) : null;
        return color != null ? color : Theme.getColor(key);
    }
}
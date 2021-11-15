/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components.popup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CheckBox2;
import org.telegram.ui.Components.LayoutHelper;

public class SendAsPeerCell extends FrameLayout {

    private BackupImageView avatarImageView;
    private SimpleTextView nameTextView;
    private SimpleTextView subtitleTextView;
    private CheckBox2 checkBox;
    private AvatarDrawable avatarDrawable = new AvatarDrawable();

    private long currentDialog;
    private int currentAccount = UserConfig.selectedAccount;

    private final Theme.ResourcesProvider resourcesProvider;

    private static final int avatarSizeDp = 38;
    private static final int avatarSize = AndroidUtilities.dp(avatarSizeDp);
    private static final int avatarRadius = avatarSize / 2;

    RectF rect = new RectF(0, 0, avatarSize, avatarSize);

    public RectF getAvatarRect() {
        return rect;
    }

    public BackupImageView getAvatarImageView() {
        return avatarImageView;
    }

    public float getAvatarRadius() {
        return avatarRadius;
    }

    public SendAsPeerCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        setWillNotDraw(false);
        avatarImageView = new BackupImageView(context);
        addView(avatarImageView, LayoutHelper.createFrame(avatarSizeDp, avatarSizeDp, Gravity.LEFT | Gravity.CENTER_VERTICAL, 16, 0, 0, 0));
        avatarImageView.setRoundRadius(avatarRadius);

        nameTextView = new SimpleTextView(context);
        nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        nameTextView.setTextSize(16);
        nameTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 64, 8, 13, 0));

        subtitleTextView = new SimpleTextView(context);
        subtitleTextView.setTextSize(13);
        subtitleTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        addView(subtitleTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 64, 30, 13, 0));

        checkBox = new CheckBox2(context, 21, resourcesProvider);
        checkBox.setVisibility(View.INVISIBLE);
        checkBox.setProgressDelegate(progress -> {
            float scale = 1.0f - (1.0f - 0.857f) * checkBox.getProgress();
            avatarImageView.setScaleX(scale);
            avatarImageView.setScaleY(scale);
            invalidate();
        });
        addView(checkBox, LayoutHelper.createFrame(24, 24,Gravity.LEFT | Gravity.CENTER_VERTICAL, 32 - 13, 8, 0, 0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(56), MeasureSpec.EXACTLY));
    }

    public void setDialog(TLObject object, boolean checked) {
        ImageLocation imageLocation;
        Object imageParent;

        String subtitle = "";
        if (object instanceof TLRPC.User) {
            TLRPC.User user = (TLRPC.User) object;
            avatarDrawable.setInfo(user);
            nameTextView.setText(ContactsController.formatName(user.first_name, user.last_name));
            if (user.id == UserConfig.getInstance(currentAccount).getClientUserId()) {
                subtitle = LocaleController.getString("SendAsPeerPersonalTitle", R.string.SendAsPeerPersonalTitle);
            }

            imageLocation = ImageLocation.getForUser(user, ImageLocation.TYPE_SMALL);
            imageParent = user;
            currentDialog = user.id;
        } else if (object instanceof TLRPC.Chat) {
            TLRPC.Chat chat = (TLRPC.Chat) object;
            avatarDrawable.setInfo(chat);
            nameTextView.setText(chat.title);

            if (chat.megagroup) {
                subtitle = LocaleController.formatPluralString("Members", chat.participants_count);
            } else {
                int[] result = new int[1];
                String shortNumber = LocaleController.formatShortNumber(chat.participants_count, result);
                subtitle = LocaleController.formatPluralString("Subscribers", result[0]).replace(String.format("%d", result[0]), shortNumber);
            }

            imageLocation = ImageLocation.getForChat(chat, ImageLocation.TYPE_SMALL);
            imageParent = chat;
            currentDialog = -chat.id;
        } else {
            imageLocation = null;
            imageParent = null;
            currentDialog = 0;
        }

        subtitleTextView.setText(subtitle);

        avatarDrawable.setInfo(object);
        avatarImageView.setImage(imageLocation, "50_50", avatarDrawable, imageParent);
        checkBox.setChecked(checked, false);
    }


    public long getCurrentDialog() {
        return currentDialog;
    }

    public void setChecked(boolean checked, boolean animated) {
        checkBox.setChecked(checked, animated);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int cx = avatarImageView.getLeft() + avatarImageView.getMeasuredWidth() / 2;
        int cy = avatarImageView.getTop() + avatarImageView.getMeasuredHeight() / 2;
        Theme.checkboxSquare_checkPaint.setColor(getThemedColor(Theme.key_dialogRoundCheckBox));
        Theme.checkboxSquare_checkPaint.setAlpha((int) (checkBox.getProgress() * 255));
        canvas.drawCircle(cx, cy, AndroidUtilities.dp(20), Theme.checkboxSquare_checkPaint);
    }

    private int getThemedColor(String key) {
        Integer color = resourcesProvider != null ? resourcesProvider.getColor(key) : null;
        return color != null ? color : Theme.getColor(key);
    }
}

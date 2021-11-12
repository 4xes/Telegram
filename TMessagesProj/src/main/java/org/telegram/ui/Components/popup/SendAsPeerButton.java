/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components.popup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;

@SuppressLint("ViewConstructor")
public class SendAsPeerButton extends View {
    private long currentDialog;
    private final int currentAccount = UserConfig.selectedAccount;
    private static final Paint backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable closeDrawable;
    private final RectF rect = new RectF();
    private final ImageReceiver imageReceiver;
    private final AvatarDrawable avatarDrawable;
    private float progress;
    private boolean deleting;
    private long lastUpdateTime;
    private final int[] colors = new int[8];
    private static final int closeColor = 0xff50A7EA;

    private final int buttonSize = AndroidUtilities.dp(48);
    private final int avatarSize = AndroidUtilities.dp(32);

    public SendAsPeerButton(Context context) {
        super(context);

        int offset = (buttonSize - avatarSize) / 2;
        closeDrawable = getResources().getDrawable(R.drawable.ic_send_as_peer);
        closeDrawable.setBounds(offset, offset, offset + avatarSize, offset + avatarSize);

        avatarDrawable = new AvatarDrawable();


        imageReceiver = new ImageReceiver();
        imageReceiver.setRoundRadius(avatarSize / 2);
        imageReceiver.setParentView(this);
        imageReceiver.setImageCoords(offset, offset, avatarSize, avatarSize);
        rect.set(offset, offset, offset + avatarSize, offset + avatarSize);
        updateColors();
    }

    public void setDialog(long uid) {
        ImageLocation imageLocation;
        Object imageParent;

        if (DialogObject.isUserDialog(uid)) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(uid);
            avatarDrawable.setInfo(user);
            avatarDrawable.setInfo(user);

            imageLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_SMALL);
            imageParent = user;
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-uid);
            avatarDrawable.setInfo(chat);
            imageLocation = ImageLocation.getForUserOrChat(chat, ImageLocation.TYPE_SMALL);
            imageParent = chat;
        }
        imageReceiver.setImage(imageLocation, "50_50", avatarDrawable, 0, null, imageParent, 1);
        updateColors();
    }

    public long getCurrentDialog() {
        return currentDialog;
    }

    public void updateColors() {
        int color = avatarDrawable.getColor();
        int back = Theme.getColor(Theme.key_groupcreate_spanBackground);
        int delete = Theme.getColor(Theme.key_groupcreate_spanDelete);
        colors[0] = Color.red(back);
        colors[1] = Color.red(color);
        colors[2] = Color.green(back);
        colors[3] = Color.green(color);
        colors[4] = Color.blue(back);
        colors[5] = Color.blue(color);
        colors[6] = Color.alpha(back);
        colors[7] = Color.alpha(color);
        closeDrawable.setColorFilter(new PorterDuffColorFilter(delete, PorterDuff.Mode.MULTIPLY));
        backPaint.setColor(back);
    }

    public boolean isDeleting() {
        return deleting;
    }

    public void startDeleteAnimation() {
        if (deleting) {
            return;
        }
        deleting = true;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public void cancelDeleteAnimation() {
        if (!deleting) {
            return;
        }
        deleting = false;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int imageSize = AndroidUtilities.dp(48);
        setMeasuredDimension(imageSize, imageSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float ft = 400.0f;
        if (deleting && progress != 1.0f || !deleting && progress != 0.0f) {
            long newTime = System.currentTimeMillis();
            long dt = newTime - lastUpdateTime;
            if (dt < 0 || dt > 17) {
                dt = 17;
            }
            if (deleting) {
                progress += dt / ft;
                if (progress >= 1.0f) {
                    progress = 1.0f;
                }
            } else {
                progress -= dt / ft;
                if (progress < 0.0f) {
                    progress = 0.0f;
                }
            }
            invalidate();
        }
        float scaleAvatar = 1f;
        float scaleClose = 0.75f;
        scaleAvatar = scaleAvatar - 0.25f * Math.min(progress * 2f, 1f);
        if (progress > 0.5) {
            scaleClose = scaleClose + 0.25f * (progress - 0.5f) * 2f;
        }
        canvas.save();
        canvas.scale(scaleAvatar, scaleAvatar, rect.centerX(), rect.centerY());
        backPaint.setColor(Color.argb(colors[6] + (int) ((colors[7] - colors[6]) * progress), colors[0] + (int) ((colors[1] - colors[0]) * progress), colors[2] + (int) ((colors[3] - colors[2]) * progress), colors[4] + (int) ((colors[5] - colors[4]) * progress)));
        canvas.drawOval(rect, backPaint);
        imageReceiver.draw(canvas);
        canvas.restore();
        if (progress != 0) {
            backPaint.setColor(closeColor);
            backPaint.setAlpha((int) (255 * progress));
            canvas.save();
            canvas.scale(scaleClose, scaleClose, rect.centerX(), rect.centerY());
            canvas.drawOval(rect, backPaint);
            closeDrawable.setAlpha((int) (255 * progress));
            closeDrawable.draw(canvas);
            canvas.restore();
        }
    }
}

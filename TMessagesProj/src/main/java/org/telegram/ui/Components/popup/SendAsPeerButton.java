/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components.popup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("ViewConstructor")
public class SendAsPeerButton extends View {
    private TLRPC.Peer currentPeer;
    private final int currentAccount = UserConfig.selectedAccount;
    private static final Paint backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable closeDrawable;
    private final RectF rect = new RectF();
    private final ImageReceiver imageReceiver;
    private final AvatarDrawable avatarDrawable;
    private float progress;
    private boolean isClose;
    private long lastUpdateTime;
    private final int[] colors = new int[8];
    private static final int closeColor = 0xff50A7EA;
    final int maxEmojiOffset = AndroidUtilities.dp(48);

    boolean inTransitionProgress = false;

    public void setEnterTransitionInProgress(boolean inTransitionProgress) {
        this.inTransitionProgress = inTransitionProgress;
    }

    public boolean isInTransitionProgress() {
        return inTransitionProgress;
    }

    public float transitionProgress = 0f;

    public void setTransitionProgress(float transitionProgress) {
        this.transitionProgress = transitionProgress;
    }

    private static final int avatarSize = AndroidUtilities.dp(32);
    private static final int avatarRadius = avatarSize / 2;

    public float getAvatarRadius() {
        return avatarRadius;
    }

    public RectF getRectDrawing() {
        return rect;
    }

    public SendAsPeerButton(Context context) {
        super(context);

        setAlpha(0f);
        setScaleX(0f);
        setScaleY(0f);

        int buttonSize = AndroidUtilities.dp(48);
        int avatarSize = AndroidUtilities.dp(32);
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

    private long uidAvatar = 0;

    public void setDialogId(long uid) {
        if (uidAvatar == uid) {
            return;
        } else {
            uidAvatar = uid;
        }
        ImageLocation imageLocation;
        Object imageParent;

        if (DialogObject.isUserDialog(uid)) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(uid);
            avatarDrawable.setInfo(user);

            imageLocation = ImageLocation.getForUser(user, ImageLocation.TYPE_SMALL);
            imageParent = user;
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-uid);
            avatarDrawable.setInfo(chat);
            imageLocation = ImageLocation.getForChat(chat, ImageLocation.TYPE_SMALL);
            imageParent = chat;
        }
        imageReceiver.setImage(imageLocation, "50_50", avatarDrawable, 0, null, imageParent, 1);
        updateColors();
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

    public boolean isClose() {
        return isClose;
    }

    public boolean isAvatar() {
        return !isClose;
    }

    public void onPeersLoaded(OnPeersOpenListener listener) {
        if (requestToken != 0) {
            prefetchCallback = () -> {
                listener.onLoaded(getData());
            };
        } else {
            if (inputPeer != null) {
                SendAsPeerView.SendAsPeerData data = getData();
                if (data != null) {
                    listener.onLoaded(data);
                } else {
                    prefetchCallback = () -> {
                        listener.onLoaded(getData());
                    };
                    getSendAs(inputPeer);
                }
            }
        }
    }

    private SendAsPeerView.SendAsPeerData getData() {
        if (currentPeer != null && inputPeer != null && objects != null && peersMap != null) {
            return new SendAsPeerView.SendAsPeerData(
                    currentPeer,
                    inputPeer,
                    objects,
                    peersMap
            );
        }
        return null;
    }

    private AnimatorSet currentAnimation;
    private final ArrayList<Animator> animators = new ArrayList<>();

    private TLRPC.InputPeer inputPeer;

    public void setChatInfo(TLRPC.Chat chat, TLRPC.ChatFull chatInfo, boolean animate) {
        //todo move to utils checking
        if (ChatObject.isSendAsPeer(chat, chatInfo)) {
            this.currentPeer = chatInfo.default_send_as;
            setCurrentPeer(this.currentPeer);
            this.inputPeer = MessagesController.getInputPeer(chat);
            getSendAs(inputPeer);
        } else {
            this.currentPeer = null;
        }
        boolean isShow = this.currentPeer != null;
        if (currentAnimation != null) {
            currentAnimation.cancel();
            currentAnimation = null;
        }
        if (animate) {
            currentAnimation = new AnimatorSet();
            setEnabled(false);
            currentAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    currentAnimation = null;
                    setEnabled(isShow);
                }
            });

            currentAnimation.setDuration(200);
            animators.clear();
            animators.add(ObjectAnimator.ofFloat(this, View.SCALE_X,  isShow ? 1.0f: 0.0f));
            animators.add(ObjectAnimator.ofFloat(this, View.SCALE_Y, isShow ? 1.0f: 0f));
            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(this, View.ALPHA, isShow ? 1.0f: 0f);
            alphaAnimator.addUpdateListener(animation -> {
                updateLayout();
            });
            animators.add(alphaAnimator);
            currentAnimation.playTogether(animators);
            currentAnimation.start();
        } else {
            setAlpha(isShow ? 1.0f: 0.0f);
            setScaleX(isShow ? 1.0f: 0.0f);
            setScaleY(isShow ? 1.0f: 0.0f);
            setEnabled(isShow);
            if (!isShow) {
                isClose = false;
                progress = 0f;
                invalidate();
            }
        }
    }

    private int requestToken;
    private Map<Long, TLRPC.Peer> peersMap = new HashMap<>(10);
    private List<TLObject> objects = new ArrayList<>();

    Runnable prefetchCallback;

    public void getSendAs(TLRPC.InputPeer inputPeer) {
        if (requestToken != 0) {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(requestToken, false);
        }

        requestToken = MessagesController.getInstance(currentAccount).getSendAs(inputPeer, (objects, peersMap, error) -> {
            if (objects != null && peersMap != null) {
                this.objects = objects;
                this.peersMap = peersMap;

                if (prefetchCallback != null) {
                    prefetchCallback.run();
                    prefetchCallback = null;
                }
            } else {
                prefetchCallback = null;
                toAvatarAnimation(true);
            }
            requestToken = 0;
        });
    }

    public void setAndSaveCurrentPeer(long chatId, TLRPC.Peer currentPeer, TLRPC.ChatFull info) {
        if (currentPeer != null) {
            MessagesController.getInstance(currentAccount).setSendAs(chatId, currentPeer, info);
            setCurrentPeer(currentPeer);
        }
    }

    private void setCurrentPeer(TLRPC.Peer currentPeer) {
        this.currentPeer = currentPeer;
        setDialogId(DialogObject.getPeerDialogId(currentPeer));
    }

    public TLRPC.Peer getCurrentPeer() {
        return currentPeer;
    }

    public int getPeerWidth() {
        return (int) (maxEmojiOffset * getAlpha());
    }

    public void updateLayout() {
        this.setLayoutParams(getLayoutParams());
    }

    public void toCloseAnimation(boolean animation) {
        if (!animation) {
            isClose = true;
            progress = 1.0f;
        }
        if (isClose) {
            return;
        }
        isClose = true;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public void toAvatarAnimation(boolean animation) {
        if (!animation) {
            isClose = false;
            progress = 0.0f;
        }
        if (!isClose) {
            return;
        }
        isClose = false;
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
        if (isClose && progress != 1.0f || !isClose && progress != 0.0f) {
            long newTime = System.currentTimeMillis();
            long dt = newTime - lastUpdateTime;
            if (dt < 0 || dt > 17) {
                dt = 17;
            }
            if (isClose) {
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
        float drawProgress = progress;
        if (isInTransitionProgress()) {
            drawProgress = this.transitionProgress;
            scaleClose = this.transitionProgress;
        }

        if (!isInTransitionProgress()) {
            canvas.save();
            canvas.scale(scaleAvatar, scaleAvatar, rect.centerX(), rect.centerY());
            backPaint.setColor(Color.argb(colors[6] + (int) ((colors[7] - colors[6]) * drawProgress), colors[0] + (int) ((colors[1] - colors[0]) * drawProgress), colors[2] + (int) ((colors[3] - colors[2]) * drawProgress), colors[4] + (int) ((colors[5] - colors[4]) * drawProgress)));
            canvas.drawOval(rect, backPaint);
            imageReceiver.draw(canvas);
            canvas.restore();
        }
        if (drawProgress != 0.0) {
            backPaint.setColor(closeColor);
            backPaint.setAlpha((int) (255 * drawProgress));
            canvas.save();
            canvas.scale(scaleClose, scaleClose, rect.centerX(), rect.centerY());
            canvas.drawOval(rect, backPaint);
            closeDrawable.setAlpha((int) (255 * drawProgress));
            closeDrawable.draw(canvas);
            canvas.restore();
        }
    }

    public interface OnPeersOpenListener {
        void onLoaded(SendAsPeerView.SendAsPeerData sendAsPeerData);
    }


    public interface RequestDelegate {
        void run(List<TLObject> objects, Map<Long, TLRPC.Peer> peersMap, TLRPC.TL_error error);
    }
}

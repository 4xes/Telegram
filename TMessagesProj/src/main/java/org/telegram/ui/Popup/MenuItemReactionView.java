package org.telegram.ui.Popup;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SvgHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarsImageView;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.HideViewAfterAnimation;
import org.telegram.ui.Components.LayoutHelper;
import java.util.List;

public class MenuItemReactionView extends FrameLayout {

    final AvatarsImageView avatarsImageView;
    final TextView titleView;
    final ImageView imageView;
    final BackupImageView reactionView;
    final int currentAccount;
    boolean isVoice;

    final FlickerLoadingView flickerLoadingView;
    final Theme.ResourcesProvider resourcesProvider;

    public MenuItemReactionView(@NonNull Context context, Theme.ResourcesProvider resourcesProvider, int currentAccount, MessageObject messageObject, int countReactions) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.currentAccount = currentAccount;
        isVoice = (messageObject.isRoundVideo() || messageObject.isVoice());
        flickerLoadingView = new FlickerLoadingView(context);
        flickerLoadingView.setColors(Theme.key_actionBarDefaultSubmenuBackground, Theme.key_listSelector, null);
        flickerLoadingView.setViewType(FlickerLoadingView.MESSAGE_SEEN_TYPE);
        flickerLoadingView.setIsSingleCell(true);
        addView(flickerLoadingView, LayoutHelper.createFrame(220, LayoutHelper.MATCH_PARENT));

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        titleView.setLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setMaxWidth(220 - 40 - 62);
        titleView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));

        addView(titleView, LayoutHelper.createFrame(220 - 40 - 62, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 40, 0, 62, 0));

        avatarsImageView = new AvatarsImageView(context, false);
        avatarsImageView.setStyle(AvatarsImageView.STYLE_MESSAGE_SEEN);
        addView(avatarsImageView, LayoutHelper.createFrame(24 + 12 + 12 + 8, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

        imageView = new ImageView(context);
        addView(imageView, LayoutHelper.createFrame(24, 24, Gravity.LEFT | Gravity.CENTER_VERTICAL, 11, 0, 0, 0));

        reactionView = new BackupImageView(context);
        reactionView.setVisibility(GONE);
        addView(reactionView, LayoutHelper.createFrame(24, 24, Gravity.LEFT | Gravity.CENTER_VERTICAL, 11, 0, 0, 0));


        Drawable drawable;
        if (countReactions == 0) {
            drawable = ContextCompat.getDrawable(context, isVoice ? R.drawable.msg_played : R.drawable.msg_seen).mutate();
        } else {
            drawable = ContextCompat.getDrawable(context, R.drawable.actions_reactions).mutate();
        }
        drawable.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_actionBarDefaultSubmenuItemIcon), PorterDuff.Mode.MULTIPLY));
        imageView.setImageDrawable(drawable);

        avatarsImageView.setAlpha(0);
        titleView.setAlpha(0);
        setEnabled(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(220), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(44), View.MeasureSpec.EXACTLY));
    }

    public void setReaction(TLRPC.Document document) {
        if (document != null) {
            String parentObject = "react";
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
            SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document, Theme.key_emptyListPlaceholder, 0.2f);
            imageView.setVisibility(View.GONE);
            reactionView.setVisibility(View.VISIBLE);
            if (svgThumb != null) {
                reactionView.setImage(ImageLocation.getForDocument(document), "66_66", null, svgThumb, parentObject);
            } else if (thumb != null) {
                reactionView.setImage(ImageLocation.getForDocument(document), "66_66", ImageLocation.getForDocument(thumb, document), null, 0, parentObject);
            } else {
                reactionView.setImage(ImageLocation.getForDocument(document), "66_66", null, null, parentObject);
            }
        }
    }

    public void updateRecentAvatars(List<TLRPC.User> users) {
        for (int i = 0; i < 3; i++) {
            if (i < users.size()) {
                avatarsImageView.setObject(i, currentAccount, users.get(i));
            } else {
                avatarsImageView.setObject(i, currentAccount, null);
            }
        }
        if (users.size() == 1) {
            avatarsImageView.setTranslationX(AndroidUtilities.dp(24));
        } else if (users.size() == 2) {
            avatarsImageView.setTranslationX(AndroidUtilities.dp(12));
        } else {
            avatarsImageView.setTranslationX(0);
        }
        avatarsImageView.commitTransition(false);
    }

    public void setTitle(String title) {
        titleView.setText(title);
    }

    public void animateLoaded() {
        setEnabled(true);
        titleView.animate().alpha(1f).setDuration(220).start();
        avatarsImageView.animate().alpha(1f).setDuration(220).start();
        flickerLoadingView.animate().alpha(0f).setDuration(220).setListener(new HideViewAfterAnimation(flickerLoadingView)).start();
    }

    private int getThemedColor(String key) {
        Integer color = resourcesProvider != null ? resourcesProvider.getColor(key) : null;
        return color != null ? color : Theme.getColor(key);
    }

}

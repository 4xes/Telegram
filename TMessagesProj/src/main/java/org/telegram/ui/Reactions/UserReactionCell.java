package org.telegram.ui.Reactions;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SvgHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

public class UserReactionCell extends FrameLayout {

    BackupImageView avatarImageView;
    TextView nameView;
    AvatarDrawable avatarDrawable = new AvatarDrawable();
    BackupImageView imageView;

    int selectorColor;

    boolean top;
    boolean bottom;

    private final int itemHeight = 48;
    private final Theme.ResourcesProvider resourcesProvider;

    public UserReactionCell(Context context, boolean top, boolean bottom, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        this.top = top;
        this.bottom = bottom;
        selectorColor = getThemedColor(Theme.key_dialogButtonSelector);

        avatarImageView = new BackupImageView(context);
        addView(avatarImageView, LayoutHelper.createFrame(34, 34, Gravity.CENTER_VERTICAL, 10, 0, 0, 0));
        avatarImageView.setRoundRadius(AndroidUtilities.dp(16));
        nameView = new TextView(context);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        nameView.setLines(1);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        addView(nameView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 57, 0, 59, 0));

        nameView.setTextColor(getThemedColor(Theme.key_actionBarDefaultSubmenuItem));

        imageView = new BackupImageView(context);
        addView(imageView, LayoutHelper.createFrame(22, 22, Gravity.CENTER_VERTICAL| Gravity.END, 0, 0, 13, 0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(itemHeight), View.MeasureSpec.EXACTLY));
    }

    public void setUser(TLRPC.User user, TLRPC.Document reaction) {
        if (user != null) {
            avatarDrawable.setInfo(user);
            ImageLocation imageLocation = ImageLocation.getForUser(user, ImageLocation.TYPE_SMALL);
            avatarImageView.setImage(imageLocation, "50_50", avatarDrawable, user);
            nameView.setText(ContactsController.formatName(user.first_name, user.last_name));
        }
        ((MarginLayoutParams) nameView.getLayoutParams()).rightMargin = reaction == null ? AndroidUtilities.dp(13): AndroidUtilities.dp(59);
        setReaction(reaction);
    }

    public void updateSelectorBackground(boolean top, boolean bottom) {
        if (this.top == top && this.bottom == bottom) {
            return;
        }
        this.top = top;
        this.bottom = bottom;
        updateBackground();
    }

    void updateBackground() {
        int topBackgroundRadius = top ? 6 : 0;
        int bottomBackgroundRadius = bottom ? 6 : 0;
        setBackground(Theme.createRadSelectorDrawable(selectorColor, topBackgroundRadius, bottomBackgroundRadius));
    }

    private void setReaction(TLRPC.Document document) {
        String parentObject = null;
        if (document != null) {
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 80);
            SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document, Theme.key_windowBackgroundGray, 1.0f);
            if (MessageObject.canAutoplayAnimatedSticker(document)) {
                if (svgThumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(document), "80_80", null, svgThumb, parentObject);
                } else if (thumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(document), "80_80", ImageLocation.getForDocument(thumb, document), null, 0, parentObject);
                } else {
                    imageView.setImage(ImageLocation.getForDocument(document), "80_80", null, null, parentObject);
                }
            } else {
                if (svgThumb != null) {
                    if (thumb != null) {
                        imageView.setImage(ImageLocation.getForDocument(thumb, document), null, "webp", svgThumb, parentObject);
                    } else {
                        imageView.setImage(ImageLocation.getForDocument(document), null, "webp", svgThumb, parentObject);
                    }
                } else {
                    imageView.setImage(ImageLocation.getForDocument(thumb, document), null, "webp", null, parentObject);
                }
            }
        }
    }

    private int getThemedColor(String key) {
        Integer color = resourcesProvider != null ? resourcesProvider.getColor(key) : null;
        return color != null ? color : Theme.getColor(key);
    }
}

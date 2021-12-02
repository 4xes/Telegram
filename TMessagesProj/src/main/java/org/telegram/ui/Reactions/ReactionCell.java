package org.telegram.ui.Reactions;

import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SvgHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

public class ReactionCell extends FrameLayout {

    private BackupImageView imageView;
    private TLRPC.Document sticker;
    private Object parentObject;

    public static int SIZE_CELL = 34;

    public ReactionCell(Context context) {
        super(context);

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        addView(imageView, LayoutHelper.createFrame(SIZE_CELL, SIZE_CELL));
        setFocusable(true);
    }

    public TLRPC.Document getSticker() {
        return sticker;
    }

    public Object getParentObject() {
        return parentObject;
    }


    public MessageObject.SendAnimationData getSendAnimationData() {
        ImageReceiver imageReceiver = imageView.getImageReceiver();
        if (!imageReceiver.hasNotThumb()) {
            return null;
        }
        MessageObject.SendAnimationData data = new MessageObject.SendAnimationData();
        int[] position = new int[2];
        imageView.getLocationInWindow(position);
        data.x = imageReceiver.getCenterX() + position[0];
        data.y = imageReceiver.getCenterY() + position[1];
        data.width = imageReceiver.getImageWidth();
        data.height = imageReceiver.getImageHeight();
        return data;
    }

    public void setSticker(TLRPC.Document document, Object parent) {
        if (document != null) {
            sticker = document;
            parentObject = parent;
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
            SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document, Theme.key_emptyListPlaceholder, 0.2f);
            if (svgThumb != null) {
                imageView.setImage(ImageLocation.getForDocument(document), "66_66", null, svgThumb, parentObject);
            } else if (thumb != null) {
                imageView.setImage(ImageLocation.getForDocument(document), "66_66", ImageLocation.getForDocument(thumb, document), null, 0, parentObject);
            } else {
                imageView.setImage(ImageLocation.getForDocument(document), "66_66", null, null, parentObject);
            }
        }
    }

    public BackupImageView getImageView() {
        return imageView;
    }


    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        String description = LocaleController.getString("AttachSticker", R.string.AttachSticker);
        if (sticker != null) {
            for (int a = 0; a < sticker.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = sticker.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeSticker) {
                    if (attribute.alt != null && attribute.alt.length() > 0) {
                        description = attribute.alt + " " + description;
                    }
                    break;
                }
            }
        }
        info.setContentDescription(description);
        info.setEnabled(true);
    }
}
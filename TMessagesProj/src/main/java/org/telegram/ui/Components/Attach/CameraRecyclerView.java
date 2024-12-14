package org.telegram.ui.Components.Attach;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import org.telegram.messenger.MediaController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.PhotoAttachPhotoCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ChatAttachAlert;
import org.telegram.ui.Components.ChatAttachAlertPhotoLayout;
import org.telegram.ui.Components.RecyclerListView;

@SuppressLint("ViewConstructor")
public class CameraRecyclerView extends RecyclerListView {

    public final ChatAttachAlertPhotoLayout alert;
    public final ChatAttachAlert parentAlert;

    public CameraRecyclerView(Context context, Theme.ResourcesProvider resourcesProvider, ChatAttachAlertPhotoLayout alert, ChatAttachAlert parentAlert) {
        super(context, resourcesProvider);
        this.alert = alert;
        this.parentAlert = parentAlert;

        setVerticalScrollBarEnabled(true);
        setClipToPadding(false);
        setPadding(dp(8), 0, dp(8), 0);
        setItemAnimator(null);
        setLayoutAnimation(null);
        setOverScrollMode(RecyclerListView.OVER_SCROLL_NEVER);
        setAlpha(0.0f);
    }

    public void setPhotoChecked(int index, int num, boolean add) {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof PhotoAttachPhotoCell) {
                int tag = (Integer) view.getTag();
                if (tag == index) {
                    if (parentAlert.baseFragment instanceof ChatActivity && parentAlert.allowOrder) {
                        ((PhotoAttachPhotoCell) view).setChecked(num, add, false);
                    } else {
                        ((PhotoAttachPhotoCell) view).setChecked(-1, add, false);
                    }
                    break;
                }
            }
        }
    }

    public void updateCheckedPhotoIndices() {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof PhotoAttachPhotoCell) {
                PhotoAttachPhotoCell cell = (PhotoAttachPhotoCell) view;
                MediaController.PhotoEntry photoEntry = alert.getPhotoEntryAtPosition((Integer) cell.getTag());
                if (photoEntry != null) {
                    cell.setNum(ChatAttachAlertPhotoLayout.selectedPhotosOrder.indexOf(photoEntry.imageId));
                }
            }
        }
    }
}

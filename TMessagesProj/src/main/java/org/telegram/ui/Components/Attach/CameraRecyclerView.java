package org.telegram.ui.Components.Attach;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;

import androidx.core.view.MotionEventCompat;

import org.telegram.ui.ActionBar.Theme;
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
}

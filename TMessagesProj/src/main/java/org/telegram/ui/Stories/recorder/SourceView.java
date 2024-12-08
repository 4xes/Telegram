package org.telegram.ui.Stories.recorder;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ShareDialogCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.Stories.DialogStoriesCell;
import org.telegram.ui.Stories.PeerStoriesView;
import org.telegram.ui.Stories.StoryViewer;

import java.util.Objects;

public class SourceView {
    int type = 0;
    float rounding;
    RectF screenRect = new RectF();
    Drawable backgroundDrawable;
    ImageReceiver backgroundImageReceiver;
    boolean hasShadow;
    Paint backgroundPaint;
    Drawable iconDrawable;
    int iconSize;
    View view;

    protected void show(boolean sent) {
    }

    protected void hide() {
    }

    protected void drawAbove(Canvas canvas, float alpha) {
    }

    public static SourceView fromAvatarImage(ProfileActivity.AvatarImageView avatarImage, boolean isForum) {
        if (avatarImage == null || avatarImage.getRootView() == null) {
            return null;
        }
        float scale = ((View) avatarImage.getParent()).getScaleX();
        final float size = avatarImage.getImageReceiver().getImageWidth() * scale;
        final float rounding = isForum ? size * 0.32f : size;
        SourceView src = new SourceView() {
            @Override
            protected void show(boolean sent) {
                avatarImage.drawAvatar = true;
                avatarImage.invalidate();
            }

            @Override
            protected void hide() {
                avatarImage.drawAvatar = false;
                avatarImage.invalidate();
            }
        };
        final int[] loc = new int[2];
        final float[] locPosition = new float[2];
        avatarImage.getRootView().getLocationOnScreen(loc);
        AndroidUtilities.getViewPositionInParent(avatarImage, (ViewGroup) avatarImage.getRootView(), locPosition);
        final float x = loc[0] + locPosition[0] + avatarImage.getImageReceiver().getImageX() * scale;
        final float y = loc[1] + locPosition[1] + avatarImage.getImageReceiver().getImageY() * scale;

        src.screenRect.set(x, y, x + size, y + size);
        src.backgroundImageReceiver = avatarImage.getImageReceiver();
        src.rounding = rounding;
        return src;
    }

    public static SourceView fromStoryViewer(StoryViewer storyViewer) {
        if (storyViewer == null) {
            return null;
        }
        SourceView src = new SourceView() {
            @Override
            protected void show(boolean sent) {
                final PeerStoriesView peerView = storyViewer.getCurrentPeerView();
                if (peerView != null) {
                    peerView.animateOut(false);
                }
                if (view != null) {
                    view.setTranslationX(0);
                    view.setTranslationY(0);
                }
            }

            @Override
            protected void hide() {
                final PeerStoriesView peerView = storyViewer.getCurrentPeerView();
                if (peerView != null) {
                    peerView.animateOut(true);
                }
            }
        };
        if (!storyViewer.getStoryRect(src.screenRect)) {
            return null;
        }
        src.type = FROM_STORY;
        src.rounding = dp(8);
        final PeerStoriesView peerView = storyViewer.getCurrentPeerView();
        if (peerView != null) {
            src.view = peerView.storyContainer;
        }
        return src;
    }

    public static SourceView fromFloatingButton(FrameLayout floatingButton) {
        if (floatingButton == null) {
            return null;
        }
        SourceView src = new SourceView() {
            @Override
            protected void show(boolean sent) {
                floatingButton.setVisibility(View.VISIBLE);
            }

            @Override
            protected void hide() {
                floatingButton.post(() -> {
                    floatingButton.setVisibility(View.GONE);
                });
            }
        };
        int[] loc = new int[2];
        final View imageView = floatingButton.getChildAt(0);
        imageView.getLocationOnScreen(loc);
        src.screenRect.set(loc[0], loc[1], loc[0] + imageView.getWidth(), loc[1] + imageView.getHeight());
        src.hasShadow = true;
        src.backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        src.backgroundPaint.setColor(Theme.getColor(Theme.key_chats_actionBackground));
        src.iconDrawable = Objects.requireNonNull(ContextCompat.getDrawable(floatingButton.getContext(), R.drawable.story_camera)).mutate();
        src.iconSize = AndroidUtilities.dp(56);
        src.rounding = Math.max(src.screenRect.width(), src.screenRect.height()) / 2f;
        return src;
    }

    public static SourceView fromShareCell(ShareDialogCell shareDialogCell) {
        if (shareDialogCell == null) {
            return null;
        }
        BackupImageView imageView = shareDialogCell.getImageView();
        SourceView src = new SourceView() {
            @Override
            protected void show(boolean sent) {
                imageView.setVisibility(View.VISIBLE);
            }

            @Override
            protected void hide() {
                imageView.post(() -> {
                    imageView.setVisibility(View.GONE);
                });
            }
        };
        int[] loc = new int[2];
        imageView.getLocationOnScreen(loc);
        src.screenRect.set(loc[0], loc[1], loc[0] + imageView.getWidth(), loc[1] + imageView.getHeight());
        src.backgroundDrawable = new ShareDialogCell.RepostStoryDrawable(imageView.getContext(), null, false, shareDialogCell.resourcesProvider);
        src.rounding = Math.max(src.screenRect.width(), src.screenRect.height()) / 2f;
        return src;
    }

    public static SourceView fromStoryCell(DialogStoriesCell.StoryCell storyCell) {
        if (storyCell == null || storyCell.getRootView() == null) {
            return null;
        }
        final float size = storyCell.avatarImage.getImageWidth();
        SourceView src = getSourceView(storyCell, size);
        final int[] loc = new int[2];
        final float[] locPosition = new float[2];
        storyCell.getRootView().getLocationOnScreen(loc);
        AndroidUtilities.getViewPositionInParent(storyCell, (ViewGroup) storyCell.getRootView(), locPosition);
        final float x = loc[0] + locPosition[0] + storyCell.avatarImage.getImageX();
        final float y = loc[1] + locPosition[1] + storyCell.avatarImage.getImageY();

        src.screenRect.set(x, y, x + size, y + size);
        src.backgroundImageReceiver = storyCell.avatarImage;
        src.rounding = Math.max(src.screenRect.width(), src.screenRect.height()) / 2f;
        return src;
    }

    private static @NonNull SourceView getSourceView(DialogStoriesCell.StoryCell storyCell, float size) {
        final float radius = size / 2f;
        return new SourceView() {
            @Override
            protected void show(boolean sent) {
                storyCell.drawAvatar = true;
                storyCell.invalidate();
                if (sent) {
                    final int[] loc = new int[2];
                    storyCell.getLocationInWindow(loc);
                    LaunchActivity.makeRipple(loc[0] + storyCell.getWidth() / 2f, loc[1] + storyCell.getHeight() / 2f, 1f);
                }
            }

            @Override
            protected void hide() {
                storyCell.post(() -> {
                    storyCell.drawAvatar = false;
                    storyCell.invalidate();
                });
            }

            @Override
            protected void drawAbove(Canvas canvas, float alpha) {
                storyCell.drawPlus(canvas, radius, radius, (float) Math.pow(alpha, 16));
            }
        };
    }

    public static final int FROM_STORY = 1;
}

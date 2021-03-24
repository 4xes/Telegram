package org.telegram.ui.Animations;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Animations.Cells.InterpolatorCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.RecyclerListView;

public class AnimationPageAdapter extends RecyclerListView.Adapter {

    private Context mContext;
    public int pageType;

    public final static int PAGE_BACKGROUND = 0;
    public final static int PAGE_SHORT_TEXT = 1;
    public final static int PAGE_LONG_TEXT = 2;
    public final static int PAGE_LINKS = 3;
    public final static int PAGE_EMOJI = 4;
    public final static int PAGE_VOICE = 5;
    public final static int PAGE_VIDEO = 6;

    public AnimationPageAdapter(Context context, int pageType) {
        mContext = context;
        this.pageType = pageType;
    }

    private final static int duration_cell = 1;
    private final static int section_cell = 2;
    private final static int header_cell = 4;
    private final static int interpolator_cell = 5;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        switch (viewType) {
            case duration_cell:
                view = new TextSettingsCell(mContext);
                break;
            case header_cell:
                view = new HeaderCell(mContext);
                break;
            case section_cell:
                view = new ShadowSectionCell(mContext, 12);
                break;
            case interpolator_cell:
                view = new InterpolatorCell(mContext);
                break;
        }
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case duration_cell:
                TextSettingsCell textSettingsCell = (TextSettingsCell) holder.itemView;
                textSettingsCell.setTextAndValue(getTitle(), "23", false);
                break;
            case header_cell:
                HeaderCell headerCell = (HeaderCell) holder.itemView;
                headerCell.setText("Header");
                break;
            case section_cell:
                ShadowSectionCell shadowSectionCell = (ShadowSectionCell) holder.itemView;
                int backgroundRes = R.drawable.greydivider;
                if (pageType == PAGE_BACKGROUND) {

                } else {
                    if (position == 14) {
                        backgroundRes = R.drawable.greydivider_bottom;
                    }
                }
                Drawable shadowDrawable = Theme.getThemedDrawable(mContext, backgroundRes, Theme.key_windowBackgroundGrayShadow);
                Drawable background = new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray));
                CombinedDrawable combinedDrawable = new CombinedDrawable(background, shadowDrawable, 0, 0);
                combinedDrawable.setFullsize(true);
                shadowSectionCell.setBackgroundDrawable(combinedDrawable);
                break;
        }
    }

    @Override
    public int getItemCount() {
        if (pageType == PAGE_BACKGROUND) {
            return 4;
        } else {
            return 14;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (pageType == PAGE_BACKGROUND) {
            if (position == 0) {
                return duration_cell;
            }
            if (position == 1) {
                return section_cell;
            }
            if (position == 2) {
                return header_cell;
            }
            if (position == 3) {
                return interpolator_cell;
            }
        } else {
            if (position == 0) {
                return duration_cell;
            } else if (position == 1) {
                return section_cell;
            } else if (position == 2) { // X Position
                return header_cell;
            } else if (position == 3) {
                return interpolator_cell;
            } else if (position == 4) {
                return section_cell;
            } else if (position == 5) { // Y Position
                return header_cell;
            } else if (position == 6) {
                return interpolator_cell;
            } else if (position == 7) {
                return section_cell;
            } else if (position == 8) { // Title scale
                return header_cell;
            } else if (position == 9) {
                return interpolator_cell;
            } else if (position == 10) {
                return section_cell;
            } else if (position == 11) { // Time appears
                return header_cell;
            } else if (position == 12) {
                return interpolator_cell;
            } else if (position == 13) {
                return section_cell;
            }
        }
        return 0;
    }

    public String getTitle() {
        switch (pageType) {
            case PAGE_BACKGROUND:
                return "Background";
            case PAGE_SHORT_TEXT:
                return "Short Text";
            case PAGE_LONG_TEXT:
                return "Long Text";
            case PAGE_LINKS:
                return "Link";
            case PAGE_EMOJI:
                return "Emoji";
            case PAGE_VOICE:
                return "Voice";
            case PAGE_VIDEO:
                return "Video";

        }
        throw new IllegalArgumentException("PageType " + pageType + " is not implemented");
    }

}

package org.telegram.ui.Animations;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
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

    private final static int TYPE_SHADOW = 2;
    private final static int TYPE_TEXT_CELL = 3;
    private final static int TYPE_TEXT_SETTING_CELL = 4;
    private final static int TYPE_SECTION_VIEW = 5;


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        switch (viewType) {
            case TYPE_TEXT_CELL:
                view =  new TextCell(mContext);
                break;
            case TYPE_TEXT_SETTING_CELL:
                view = new TextSettingsCell(mContext);
                break;
            case TYPE_SHADOW:
                view = new ShadowSectionCell(mContext, 12, Theme.getColor(Theme.key_windowBackgroundGray));
                break;
        }
        return new RecyclerListView.Holder(view);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_TEXT_CELL:
                TextCell textCell = (TextCell) holder.itemView;
                textCell.setText(getTitle(), false);
                break;
            case TYPE_TEXT_SETTING_CELL:
                TextSettingsCell textSettingsCell = (TextSettingsCell) holder.itemView;
                textSettingsCell.setTextAndValue(getTitle(), "23", false);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_TEXT_CELL;
        }
        if (position == 1) {
            return TYPE_TEXT_SETTING_CELL;
        }
        if (position == 2) {
            return TYPE_SHADOW;
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

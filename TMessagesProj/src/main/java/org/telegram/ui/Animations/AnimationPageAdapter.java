package org.telegram.ui.Animations;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.qrcode.decoder.Mode;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Animations.Cells.GradientSurfaceCell;
import org.telegram.ui.Animations.Cells.InterpolatorCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class AnimationPageAdapter extends RecyclerListView.SelectionAdapter implements RecyclerListView.OnItemClickListener {

    private final Context context;
    public final AnimationType pageType;
    private final AnimationSettingsActivity activity;

    private final ArrayList<Model> models;

    public AnimationPageAdapter(Context context, AnimationType type, AnimationSettingsActivity activity) {
        this.context = context;
        this.pageType = type;
        this.activity = activity;

        models = new ArrayList<>();
        if (type == AnimationType.Background) {
            models.add(new Model(header_cell, "Background Preview"));
            models.add(new Model(empty_cell, null));
            models.add(new Model(background_cell, "Background Preview"));
            models.add(new Model(action_cell, "Open Full Screen"));

            models.add(new Model(section_cell, null));

            models.add(new Model(header_cell, "Colors"));
            models.add(new Model(color_cell, 0));
            models.add(new Model(color_cell, 1));
            models.add(new Model(color_cell, 2));
            models.add(new Model(color_cell, 3));

        } else {
            models.add(new Model(duration_cell, type.name()));
        }
        models.add(new Model(section_cell, null));

        for (AnimationType.Interpolator i: pageType.params) {
            String key = type.name() + "_" + i.name();
            if (i == AnimationType.Interpolator.Scale) {
                if (type == AnimationType.ShortText || type == AnimationType.LongText) {
                    models.add(new Model(header_cell, "Text" + i.title));
                }
            } else {
                models.add(new Model(header_cell, i.title));
            }
            if (i.hasDuration) {
                models.add(new Model(duration_cell, key));
            }
            models.add(new Model(interpolator_cell, key));
            models.add(new Model(section_cell, null));
        }
    }

    private final static int duration_cell = 1;
    private final static int section_cell = 2;
    private final static int header_cell = 4;
    private final static int interpolator_cell = 5;
    private final static int background_cell = 6;
    private final static int color_cell = 7;
    private final static int action_cell = 8;
    private final static int empty_cell = 9;

    public static class Model {
        int type;
        Object value;

        public Model(int type, Object key) {
            this.type = type;
            this.value = key;
        }
    }

    Paint paintBackground;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        switch (viewType) {
            case background_cell:
                view = new GradientSurfaceCell(context);
                break;
            case duration_cell:
                view = new TextSettingsCell(context);
                break;
            case header_cell:
                view = new HeaderCell(context);
                break;
            case section_cell:
                view = new ShadowSectionCell(context, 12);
                break;
            case interpolator_cell:
                view = new InterpolatorCell(context);
                break;
            case color_cell:
                view = new TextSettingsCell(context) {
                    @Override
                    protected void onDraw(Canvas canvas) {
                        super.onDraw(canvas);
                    }
                };
                view.setWillNotDraw(false);
                break;
            case action_cell:
                view = new BottomSheet.BottomSheetCell(context, 0);
                view.setBackground(null);
                break;
            case empty_cell:
                view = new View(context) {
                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(10), MeasureSpec.EXACTLY));
                    }
                };
                break;
        }
        return new RecyclerListView.Holder(view);
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        int position = holder.getAdapterPosition();
        int type = getItemViewType(position);
        if (type == duration_cell || type == action_cell) {
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(View view, int position) {
        if (view instanceof TextSettingsCell) {
            TextSettingsCell textSettingsCell = (TextSettingsCell) view;
            activity.createMenu(view, durations, 0, 0, i -> {
                textSettingsCell.setTextAndValue("Duration", durations.get(i).toString(), pageType == AnimationType.Background);
                activity.preferences.putDuration((String) view.getTag(), DURATIONS[i]);
            });
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case duration_cell:
                String key = (String) models.get(position).value;
                TextSettingsCell textSettingsCell = (TextSettingsCell) holder.itemView;
                textSettingsCell.setTextAndValue("Duration", activity.preferences.getDuration(key) + "ms", pageType == AnimationType.Background);
                textSettingsCell.setTag(key);
                break;
            case header_cell:
                HeaderCell headerCell = (HeaderCell) holder.itemView;
                headerCell.setText((String) models.get(position).value);
                break;
            case section_cell:
                ShadowSectionCell shadowSectionCell = (ShadowSectionCell) holder.itemView;
                int backgroundRes = R.drawable.greydivider;
                if (position == models.size() - 1) {
                    backgroundRes = R.drawable.greydivider_bottom;
                }
                Drawable shadowDrawable = Theme.getThemedDrawable(context, backgroundRes, Theme.key_windowBackgroundGrayShadow);
                Drawable background = new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray));
                CombinedDrawable combinedDrawable = new CombinedDrawable(background, shadowDrawable, 0, 0);
                combinedDrawable.setFullsize(true);
                shadowSectionCell.setBackgroundDrawable(combinedDrawable);
                break;
            case action_cell:
                BottomSheet.BottomSheetCell cell = (BottomSheet.BottomSheetCell) holder.itemView;
                cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
                cell.setTextAndIcon((String) models.get(position).value, null);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    @Override
    public int getItemViewType(int position) {
        return models.get(position).type;
    }

    public String getTitle() {
        return pageType.title;
    }

    private static final long[] DURATIONS = new long[] {
            200L, 300L, 400L, 500L, 600L, 700, 800L, 900L, 1000L, 1500L, 2000L, 3000L
    };

    private static final ArrayList<CharSequence> durations = new ArrayList<>();
    static {
        durations.clear();
        for (long duration : DURATIONS) {
            durations.add(duration + "ms");
        }
    }

}

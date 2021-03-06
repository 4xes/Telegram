package org.telegram.ui.Animations;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.collection.SparseArrayCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Animations.Cells.ColorSettingsCell;
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
    public final AnimationType type;
    private final AnimationSettingsActivity activity;
    private final ArrayList<Model> models;
    int colorIndex;
    int backgroundIndex;

    public int currentPickerIndex = -1;

    public void restoreColors() {
        currentPickerIndex = -1;
        pickerColors = AnimationManager.getPreferences().getColors();
        for (int i = 0; i < 4; i++) {
            notifyItemChanged(colorIndex + i);
        }
        notifyItemChanged(backgroundIndex);
    }

    public void applyColors() {
        for (int i = 0; i < 4; i++) {
            AnimationManager.getPreferences().putColors(pickerColors);
            restoreColors();
        }
    }

    public GradientSurfaceCell surfaceView;
    private int[] pickerColors;

    SparseArrayCompat<ColorSettingsCell> colorCells = new SparseArrayCompat<>(4);

    public void setColor(int index, int color) {
        ColorSettingsCell cell = colorCells.get(index);
        this.pickerColors[index] = color;
        if (cell != null) {
            cell.setColor(color);
        }
        if (surfaceView != null) {
            surfaceView.setColors(pickerColors, false);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        pickerColors = AnimationManager.getPreferences().getColors();
        super.notifyDataSetChanged();
    }

    public AnimationPageAdapter(Context context, AnimationType type, AnimationSettingsActivity activity) {
        this.context = context;
        this.type = type;
        this.activity = activity;

        this.pickerColors = AnimationManager.getInstance().getColors();

        models = new ArrayList<>();
        if (type == AnimationType.Background) {
            models.add(new Model(header_cell, "Background Preview"));
            models.add(new Model(empty_cell, null));
            backgroundIndex = models.size();
            models.add(new Model(background_cell, "Background Preview"));
            models.add(new Model(action_cell, "Open Full Screen"));

            models.add(new Model(section_cell, null));

            models.add(new Model(header_cell, "Colors"));
            colorIndex = models.size();
            models.add(new Model(color_cell, 0));
            models.add(new Model(color_cell, 1));
            models.add(new Model(color_cell, 2));
            models.add(new Model(color_cell, 3));

        } else {
            models.add(new Model(duration_cell, null));
        }
        models.add(new Model(section_cell, null));

        for (Parameter i: this.type.params) {
            if (i == Parameter.Scale) {
                if (type == AnimationType.ShortText || type == AnimationType.LongText) {
                    models.add(new Model(header_cell, "Text " + i.title));
                } else {
                    models.add(new Model(header_cell, type.title + " " + i.title));
                }
            } else {
                models.add(new Model(header_cell, i.title));
            }
            if (i.hasDuration) {
                models.add(new Model(duration_cell, i));
            }
            models.add(new Model(interpolator_cell, i));
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
                view = new InterpolatorCell(context, AnimationManager.getInstance().preferences);
                break;
            case color_cell:
                view = new ColorSettingsCell(context);
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
        if (type == duration_cell || type == action_cell || type == color_cell) {
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(View view, int position) {
        if (view instanceof ColorSettingsCell) {
            int index = position - colorIndex;
            ColorSettingsCell colorCell = (ColorSettingsCell) view;
            int color = colorCell.getColor();
            activity.lastIndexColor = index;
            activity.lastPickedColor = color;
            activity.showColorPicker(true);
            colorCell.setSelect(true);
            currentPickerIndex = index;
            for (int i = 0; i < 4; i++) {
                if (currentPickerIndex != i) {
                    ColorSettingsCell cell = colorCells.get(i);
                    if (cell != null) {
                        cell.setSelect(false);
                    }
                }
            }
        } else if (view instanceof TextSettingsCell) {
            Object item = getItem(position);
            Parameter durationParameter;
            if (item instanceof Parameter) {
                durationParameter = (Parameter) item;
            } else {
                durationParameter = null;
            }
            activity.createMenu(view, durations, 0, 0, i -> {
                long newDuration = DURATIONS[i];
                long duration = AnimationManager.getInstance().getDuration(type, durationParameter);
                if (duration != newDuration) {
                    AnimationManager.getInstance().setDuration(type, durationParameter, DURATIONS[i]);
                    notifyItemChanged(position);
                }
                if (type == AnimationType.Background) {
                    notifyItemChanged(position + 1);
                } else {
                    for (int notifyPos = 0; notifyPos < models.size(); notifyPos++) {
                        if (models.get(notifyPos).type == interpolator_cell) {
                            notifyItemChanged(notifyPos);
                        }
                    }
                }
            });
        }
        if (view instanceof BottomSheet.BottomSheetCell) {
            activity.presentFragment(new AnimationBackgroundActivity());
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int type = holder.getItemViewType();
        switch (type) {
            case background_cell:
                surfaceView = (GradientSurfaceCell) holder.itemView;
                if (currentPickerIndex == -1) {
                    surfaceView.restoreColors(true);
                } else {
                    surfaceView.setColors(pickerColors, false);
                }
                break;
            case duration_cell:
            case interpolator_cell:
                Parameter parameter = getItem(position);
                long duration = AnimationManager.getInstance().getDuration(this.type, parameter);
                if (type == duration_cell) {
                    TextSettingsCell view = (TextSettingsCell) holder.itemView;
                    boolean hasDivider = this.type == AnimationType.Background;
                    view.setTextAndValue("Duration", duration + "ms", hasDivider);
                }
                if (type == interpolator_cell) {
                    InterpolatorCell view = (InterpolatorCell) holder.itemView;
                    view.setDuration(duration);
                    InterpolatorData data = AnimationManager.getInstance().getInterpolatorData(this.type, parameter);
                    String key = AnimationPreferences.key(this.type, parameter);
                    view.setInterpolationData(key, data);
                }
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
                String text = getItem(position);
                BottomSheet.BottomSheetCell cell = (BottomSheet.BottomSheetCell) holder.itemView;
                cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
                cell.setTextAndIcon(text, null);
                break;
            case color_cell:
                Integer index = getItem(position);
                ColorSettingsCell colorCell = (ColorSettingsCell) holder.itemView;
                int color = pickerColors[index];
                colorCell.setTextAndColor("Color " + index, color, true);
                if (index == currentPickerIndex) {
                    colorCell.setSelect(true);
                } else {
                    colorCell.setSelect(false);
                }
                colorCells.put(index, colorCell);
                colorCell.setColor(pickerColors[index]);
                colorCell.setWillNotDraw(false);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    public <T> T getItem(int position) {
        //noinspection unchecked
        return (T) models.get(position).value;
    }

    @Override
    public int getItemViewType(int position) {
        return models.get(position).type;
    }

    public String getTitle() {
        return type.title;
    }

    private static final long[] DURATIONS = new long[] {
            200L, 300L, 400L, 500L, 600L, 700, 800L, 900L, 1000L, 1500L, 2000L, 3000L, 10000L, 15000L
    };

    private static final ArrayList<CharSequence> durations = new ArrayList<>();
    static {
        durations.clear();
        for (long duration : DURATIONS) {
            durations.add(duration + "ms");
        }
    }

}

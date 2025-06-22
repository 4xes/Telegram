package org.telegram.ui.profile.section;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressLint("ViewConstructor")
public class SectionButtons extends ViewGroup {
    private final int SPACING = AndroidUtilities.dp(8);
    private final Theme.ResourcesProvider resourceProvider;
    public final static int HEIGHT = AndroidUtilities.dp(54);
    public final HashMap<SectionType, SectionButton> map = new HashMap<>();
    @Nullable
    public SectionButtonsDelegate sectionButtonsDelegate;

    public SectionButtons(Context context, Theme.ResourcesProvider resourceProvider) {
        super(context);
        this.resourceProvider = resourceProvider;
    }

    public void setSections(ArrayList<SectionType> sections) {
        LayoutParams layoutParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                HEIGHT
        );
        removeAllViews();
        map.clear();
        for (SectionType section: sections) {
            SectionButton sectionView = new SectionButton(getContext());
            sectionView.setResourceProvider(resourceProvider);
            sectionView.setSection(section);
            sectionView.updateStyle();
            map.put(section, sectionView);
            addView(sectionView, layoutParams);
            sectionView.setOnClickListener(v -> {
                if (sectionButtonsDelegate != null) {
                    sectionButtonsDelegate.onClickSection(section);
                }
            });
        }
    }

    public void setCurrentFrame(SectionType sectionType, int frame, boolean async) {
        SectionButton sectionButton = map.get(sectionType);
        if (sectionButton != null) {
            sectionButton.setCurrentFrame(frame, async);
        }
    }

    public void setCurrentFrame(SectionType sectionType, int frame) {
        SectionButton sectionButton = map.get(sectionType);
        if (sectionButton != null) {
            sectionButton.setCurrentFrame(frame);
        }
    }

    public void setCustomEndFrame(SectionType sectionType, int frame) {
        SectionButton sectionButton = map.get(sectionType);
        if (sectionButton != null) {
            sectionButton.setCustomEndFrame(frame);
        }
    }

    public void playAnimation(SectionType sectionType) {
        playAnimation(sectionType, true);
    }

    public void playAnimation(SectionType sectionType, boolean isAnimate) {
        SectionButton sectionButton = map.get(sectionType);
        if (sectionButton != null) {
            if (isAnimate) {
                sectionButton.playAnimation();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int childCount = getChildCount();
        if (childCount == 0) {
            setMeasuredDimension(width, 0);
            return;
        }

        int availableWidth = width - getPaddingLeft() - getPaddingRight();
        int childWidth = (availableWidth - (childCount - 1) * SPACING) / childCount;
        int childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(HEIGHT, MeasureSpec.EXACTLY);

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.measure(childWidthSpec, childHeightSpec);
        }

        setMeasuredDimension(width, HEIGHT);
    }

    public void setSectionButtonsDelegate(@Nullable SectionButtonsDelegate sectionButtonsDelegate) {
        this.sectionButtonsDelegate = sectionButtonsDelegate;
    }

    public void updateState(final boolean isEnabled, final int height) {
        for (int i = 0; i < getChildCount(); i++) {
            SectionButton child = (SectionButton) getChildAt(i);
            child.updateState(isEnabled, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        if (childCount == 0) {
            return;
        }
        int currentX = getPaddingLeft();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.layout(currentX, 0, currentX + child.getMeasuredWidth(), child.getMeasuredHeight());
            currentX += child.getMeasuredWidth() + SPACING;
        }
    }

    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        
        // Add theme descriptions for child views
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof SectionButton) {

                // Add theme descriptions for SectionButton children
//                themeDescriptions.add(new ThemeDescription(child, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
//                themeDescriptions.add(new ThemeDescription(child, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_featuredStickers_addButton));
//                themeDescriptions.add(new ThemeDescription(child, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_featuredStickers_addButtonPressed));
            }
        }
        
        return themeDescriptions;
    }

    public interface SectionButtonsDelegate {
        void onClickSection(SectionType sectionType);
    }
}
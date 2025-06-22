package org.telegram.ui.profile.section;

import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class SectionCustomButtons extends ViewGroup {

    private final ArrayList<ButtonState> buttons = new ArrayList<>();
    private final RectF bound = new RectF();
    private final int SPACING = AndroidUtilities.dp(8);
    boolean isInit = true;
    boolean isChanged = false;
    private final HashMap<Integer, Drawable> drawables = new HashMap<>();

    public SectionCustomButtons(Context context) {
        super(context);
    }

    public void setTypes(ArrayList<SectionType> sections) {
        if (buttons.isEmpty()) {
            for (SectionType section: sections) {
                ButtonState state = getButton(section);
                state.progress = 1f;
                buttons.add(state);
            }
        } else {
            ArrayList<ButtonState> newList = new ArrayList<>();
            boolean isResize = buttons.size() != sections.size();
            for (int i = 0; i < sections.size(); i++) {
                SectionType section = sections.get(i);
                ButtonState state = null;
                for (int j = 0; j < buttons.size(); j++) {
                    SectionType buttonType = buttons.get(j).type;
                    if (isSameOrMute(section, buttonType)) {
                        state = buttons.get(j);
                        break;
                    }
                }

                // A B C
                // F A B G

                if (state != null && state.type != section) {
                    state.type = section;
                    state.drawable = getDrawable(section);
                    state.text = LocaleController.getString(section.text);
                    state.isResizing = isResize;
                    state.progress = 0f;
                } else {
                    state = getButton(section);
                    state.isAdding = true;
                    state.progress = 0f;
                }
                newList.add(state);
            }
//            for (ButtonState buttonState: buttons) {
//                boolean isDeleting = true;
//                for (SectionType sectionType: sections) {
//                    if ()
//                }
//            }
        }
    }

    private boolean isSameOrMute(SectionType type1, SectionType type2) {
        return type1 == type2 ||
                type1 == SectionType.Mute && type2 == SectionType.Unmute ||
                type1 == SectionType.Unmute && type2 == SectionType.Mute;
    }

    private Drawable getDrawable(SectionType section) {
        Drawable drawable = drawables.get(section.iconRes);
        if (drawable == null) {
            drawable = ContextCompat.getDrawable(getContext(), section.iconRes);
            drawables.put(section.iconRes, drawable);
        }
        return drawable;
    }

    private ButtonState getButton(SectionType section) {
        Drawable drawable = drawables.get(section.iconRes);
        if (drawable == null) {
            drawable = ContextCompat.getDrawable(getContext(), section.iconRes);
            drawables.put(section.iconRes, drawable);
        }
        ButtonState state = new ButtonState(section, Objects.requireNonNull(drawable));
        state.text = LocaleController.getString(section.text);
        return state;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        float tempLeft = bound.left;
        float tempRight = bound.right;
        float tempTop = bound.top;
        float tempBottom = bound.bottom;

        bound.set(
                l + getPaddingLeft(),
                t + getPaddingTop(),
                r - getPaddingRight(),
                b - getPaddingBottom()
        );
        if (tempLeft != bound.left &&
                tempTop != bound.top &&
                tempRight != bound.right &&
                tempBottom != bound.bottom
        ) {
            isChanged = true;
        }
    }

    private void updateButtonsStates() {
        float spacingSize = (buttons.size() - 1) * SPACING;
        float widthButton = (bound.width() - spacingSize) / buttons.size();
        for(ButtonState button: buttons) {
            if (isInit) {

            }
        }
    }

    private static class ButtonState {
        @NonNull
        SectionType type;
        @NonNull
        Drawable drawable;
        String text;
        RectF from = new RectF();
        RectF current = new RectF();
        RectF to = new RectF();
        float progress;
        boolean isInit;
        boolean isAdding;
        boolean isResizing;
        boolean isDeleting;

        public ButtonState(@NonNull SectionType type, @NonNull Drawable drawable) {
            this.type = type;
            this.drawable = drawable;
        }

        public void setInit() {
            isAdding = false;
            isInit = true;
            isResizing = false;
            isDeleting = false;
        }

        public void setAdding() {
            isAdding = true;
            isInit = false;
            isResizing = false;
            isDeleting = false;
        }

        public void setResizing() {
            isAdding = false;
            isInit = false;
            isResizing = true;
            isDeleting = false;
        }

        public void setDeleting() {
            isAdding = false;
            isInit = false;
            isResizing = false;
            isDeleting = true;
        }
    }
}

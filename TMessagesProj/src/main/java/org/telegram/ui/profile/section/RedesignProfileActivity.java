package org.telegram.ui.profile.section;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class RedesignProfileActivity extends BaseFragment {


    @Override
    public View createView(Context context) {

        //Заблюрить аватарку, заблюрить ее повторно с фоном для кнопок и отрисовать на кнопке, сделать сдвиг цветов)
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        fragmentView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));

        ArrayList<SectionType> sectionTypes = new ArrayList<>();
        sectionTypes.add(SectionType.Message);
        sectionTypes.add(SectionType.Join);
        sectionTypes.add(SectionType.Leave);
        sectionTypes.add(SectionType.Mute);

        SectionButtons sectionButtons = new SectionButtons(context, getResourceProvider());
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                SectionButtons.HEIGHT
        );
        sectionButtons.setSections(sectionTypes);
        ((FrameLayout) fragmentView).addView(sectionButtons, layoutParams);

        return frameLayout;
    }
}

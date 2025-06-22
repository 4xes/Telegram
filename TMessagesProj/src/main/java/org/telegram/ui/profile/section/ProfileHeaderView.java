package org.telegram.ui.profile.section;

import android.content.Context;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import org.telegram.ui.ActionBar.Theme;

public class ProfileHeaderView extends LinearLayout {

    private final Theme.ResourcesProvider resourceProvider;

    public ProfileHeaderView(@NonNull Context context, Theme.ResourcesProvider resourceProvider) {
        super(context);
        this.resourceProvider = resourceProvider;


    }
}

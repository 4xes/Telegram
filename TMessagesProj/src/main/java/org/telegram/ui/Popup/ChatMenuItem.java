package org.telegram.ui.Popup;

import androidx.annotation.DrawableRes;

public class ChatMenuItem {
    CharSequence title;
    @DrawableRes
    int icon;
    int option;

    public ChatMenuItem(CharSequence title, int icon, int option) {
        this.title = title;
        this.icon = icon;
        this.option = option;
    }
}
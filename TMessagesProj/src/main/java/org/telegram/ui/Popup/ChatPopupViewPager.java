package org.telegram.ui.Popup;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import org.telegram.ui.Components.ViewPagerFixed;

public class ChatPopupViewPager extends ViewPagerFixed {

    public ChatPopupViewPager(@NonNull Context context) {
        super(context);

        setAdapter(new Adapter() {
            @Override
            public int getItemCount() {
                return 1;
            }

            @Override
            public View createView(int viewType) {
                return null;
            }

            @Override
            public void bindView(View view, int position, int viewType) {

            }
        });
    }
}

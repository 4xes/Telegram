package org.telegram.ui.Components.Attach;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

public class TooltipView extends AppCompatTextView {

    public TooltipView(@NonNull Context context) {
        super(context);
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        setTextColor(0xffffffff);
        setText(LocaleController.getString(R.string.TapForVideo));
        setShadowLayer(dp(3.33333f), 0, dp(0.666f), 0x4c000000);
        setPadding(dp(6), 0, dp(6), 0);
    }
}

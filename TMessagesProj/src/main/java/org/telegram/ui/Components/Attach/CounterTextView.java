package org.telegram.ui.Components.Attach;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

public class CounterTextView extends AppCompatTextView {
    public CounterTextView(@NonNull Context context) {
        super(context);
        setBackgroundResource(R.drawable.photos_rounded);
        setTextColor(0xffffffff);
        setGravity(Gravity.CENTER);
        setPivotX(0);
        setPivotY(0);
        setTypeface(AndroidUtilities.bold());
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.photos_arrow, 0);
        setCompoundDrawablePadding(dp(4));
        setPadding(dp(16), 0, dp(16), 0);
    }
}

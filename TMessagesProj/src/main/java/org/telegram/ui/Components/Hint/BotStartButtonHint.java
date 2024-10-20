package org.telegram.ui.Components.Hint;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.text.Layout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Stories.recorder.HintView2;

public class BotStartButtonHint extends HintView2 {
    public BotStartButtonHint(Context context) {
        super(context, HintView2.DIRECTION_BOTTOM);

        setIcon(ContextCompat.getDrawable(context, R.drawable.ic_double_arrow_down));
        //setBackgroundAlpha(200);
        setIconTranslate(dp(4), dp(1));
        setInnerPadding(16, 8, 14, 9);
        setBackgroundAlpha((int)(255 * 0.8f));
        setTextAlign(Layout.Alignment.ALIGN_CENTER);
        setRounding(12);
        setText(LocaleController.getString(R.string.BotStartHint));
        setMaxWidthPx(HintView2.cutInFancyHalf(getText(), getTextPaint()));
        setDuration(-1);
        setPadding(dp(6), 0, dp(6), 0);
    }

    public void show(ViewGroup contentView, View bottomView) {
        AndroidUtilities.runOnUIThread(() -> {
            FrameLayout.LayoutParams lp = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0, 0, 0);
            lp.bottomMargin = bottomView.getMeasuredHeight() + dp(6);
            contentView.addView(this, lp);
            setJointPx(0, bottomView.getMeasuredWidth() / 2f);
            show();
        }, 300);
    }

    public void fixLayout(View bottomView) {
        setJointPx(0, bottomView.getMeasuredWidth() / 2f);
        FrameLayout.LayoutParams lp = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0, 0, 0);
        lp.bottomMargin = bottomView.getMeasuredHeight() + dp(6);
        setLayoutParams(lp);
    }
}

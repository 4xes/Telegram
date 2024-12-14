package org.telegram.ui.Components.Attach;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatTextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

public class RecordTimeView extends AppCompatTextView {

    private final Paint recordPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float alpha = 0f;
    private boolean isInc;

    public RecordTimeView(Context context) {
        super(context);
        recordPaint.setColor(0xffda564d);

        setBackgroundResource(R.drawable.system);
        getBackground().setColorFilter(new PorterDuffColorFilter(0x66000000, PorterDuff.Mode.MULTIPLY));
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        setTypeface(AndroidUtilities.bold());
        setAlpha(0.0f);
        setTextColor(0xffffffff);
        setPadding(dp(24), dp(5), dp(10), dp(5));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        recordPaint.setAlpha((int) (125 + 130 * alpha));

        if (!isInc) {
            alpha -= 16 / 600.0f;
            if (alpha <= 0) {
                alpha = 0;
                isInc = true;
            }
        } else {
            alpha += 16 / 600.0f;
            if (alpha >= 1) {
                alpha = 1;
                isInc = false;
            }
        }
        super.onDraw(canvas);
        canvas.drawCircle(dp(14), getMeasuredHeight() / 2f, dp(4), recordPaint);
        invalidate();
    }
}

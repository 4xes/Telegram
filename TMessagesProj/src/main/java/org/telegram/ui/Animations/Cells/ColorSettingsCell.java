package org.telegram.ui.Animations.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.view.Gravity;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;

public class ColorSettingsCell extends TextSettingsCell {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int color = Color.TRANSPARENT;

    private int roundInner = AndroidUtilities.dp(2);
    private int roundOuter = AndroidUtilities.dp(2);

    public ColorSettingsCell(Context context) {
        super(context);
        init(21);
    }

    public ColorSettingsCell(Context context, int padding) {
        super(context, padding);
        init(padding);
    }

    public void setColor(int color) {
        if (this.color != color) {
            this.color = color;
            invalidate();
        }
    }

    private void init(int padding) {
        getValueTextView().setLayoutParams(LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.TOP, padding, 0, padding, 0));
        paint.setStyle(Paint.Style.FILL);

        setWillNotDraw(false);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Layout textColor = getValueTextView().getLayout();
        rect.set(textColor.getLineLeft(0), textColor.getLineTop(0), textColor.getLineRight(0), textColor.getLineBottom(0));
//        rect.inset(0, textColor.getLayout());
//        rect.inset(0, -textColor.getTextSize()/ 2);
        paint.setColor(color);
        canvas.drawRoundRect(rect, roundInner, roundInner, paint);
    }
}

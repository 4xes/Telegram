package org.telegram.ui.Animations.Transitions;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

abstract class BaseRecordMessageEnterTransition extends MessageTransition {

    float dotFromRadius;

    final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    float dotGetX;
    float dotGetY;

    float slideGetX;
    float slideGetY;

    float timerGetX;
    float timerGetY;

    final ChatActivityEnterView.RecordDot recordDot;
    final ChatActivityEnterView.SlideTextView slideText;
    final ChatActivityEnterView.TimerView timerView;

    final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    final float fromTimeSize = AndroidUtilities.dp(15);
    final float toTimeSize = AndroidUtilities.dp(12);
    final String timerEnd;
    final String durationTime;
    final float timeOffsetY;
    final StaticLayout msLayout;
    float measureSeconds;

    public BaseRecordMessageEnterTransition(FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView chatActivityEnterView, RecyclerListView listView) {
        super(containerView, messageView, chatActivityEnterView, listView);
        recordDot = chatActivityEnterView.getRecordDot();
        recordDot.skipDraw = true;
        dotFromRadius = recordDot.drawingDotRadius;

        slideText = chatActivityEnterView.getSlideText();
        slideText.transitionInProgress = true;
        slideText.skipDraw = true;
        slideText.transitionAlpha = 1f;

        timerView = chatActivityEnterView.getTimerView();
        textPaint.setTextSize(fromTimeSize);
        timerView.transitionInProgress = true;
        timerEnd = timerView.getTimeString();
        timerView.transitionInProgress = false;
        durationTime = messageView.durationLayout.getText().toString();
        String ms = timerEnd.substring(timerEnd.length() - 2);
        msLayout = new StaticLayout(ms, timerView.getTextPaint(), timerView.getMeasuredWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        timeOffsetY = (timerView.getHeight() - msLayout.getHeight()) / 2f;
        measureSeconds = timerView.getTextPaint().measureText(timerEnd, 0, 4);

        location(chatActivityEnterView, recordDot);
        dotGetX = location[0];
        dotGetY = location[1];

        location(chatActivityEnterView, slideText);
        slideGetX = location[0];
        slideGetY = location[1];

        location(chatActivityEnterView, timerView);
        timerGetX = location[0];
        timerGetY = location[1];
    }


    public void animateSlide(Canvas canvas) {
        int saveTranslate = canvas.save();
        float left = slideGetX + enterView.getX();
        float top = slideGetY + enterView.getY();
        canvas.translate(left, top);
        slideText.skipDraw = false;
        slideText.transitionAlpha = reverse2x(progress);
        slideText.draw(canvas);
        slideText.skipDraw = true;
        canvas.restoreToCount(saveTranslate);
    }

    protected float reverse2x(float progress) {
        return Math.max(0f, 1f - progress * 2f);
    }

    @Override
    public void release() {
        super.release();
        recordDot.skipDraw = false;
        slideText.skipDraw = false;
        slideText.transitionInProgress = false;
    }
}

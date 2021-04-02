package org.telegram.ui.Animations.Transitions;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

import static org.telegram.ui.Components.ChatActivityEnterView.RECORD_STATE_SENDING;

public class VoiceMessageEnterTransition extends MessageTransition {

    float fromRadius;

    float dotFromRadius;

    final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    float dotGetX;
    float dotGetY;

    float slideGetX;
    float slideGetY;

    float timerGetX;
    float timerGetY;

    final ChatActivityEnterView.RecordCircle recordCircle;
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

    public VoiceMessageEnterTransition(FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView chatActivityEnterView, RecyclerListView listView) {
        super(containerView, messageView, chatActivityEnterView, listView);

        recordCircle = chatActivityEnterView.getRecordCircle();
        chatActivityEnterView.startMessageTransition();
        chatActivityEnterView.updateRecordIntefrace(RECORD_STATE_SENDING);

        recordCircle.transitionInProgress = true;
        recordCircle.skipDraw = true;
        fromRadius = recordCircle.drawingCircleRadius;

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

    float lastToCx;
    float lastToCy;

    private void animateRecordCircle(Canvas canvas) {
        final float step1Time = 0.6f;
        float hideWavesProgress = progress > step1Time ? 1f : progress / step1Time;

        float fromCx = recordCircle.drawingCx + recordCircle.getX();
        float fromCy = recordCircle.drawingCy + recordCircle.getY();

        float toCy;
        float toCx;

        if (messageView.getMessageObject().stableId != messageId) {
            toCx = lastToCx;
            toCy = lastToCy;
        } else {
            toCx = messageView.getRadialProgress().getProgressRect().centerX() + messageX;
            toCy = messageView.getRadialProgress().getProgressRect().centerY() + messageY;
        }

        lastToCx = toCx;
        lastToCy = toCy;

        float cx = evaluate(xProgress, fromCx, toCx);
        float cy = evaluate(yProgress, fromCy, toCy);

        float toRadius = messageView.getRadialProgress().getProgressRect().height() / 2;
        float radius = evaluate(yProgress, fromRadius, toRadius);

        int fromColor = Theme.getColor(Theme.key_chat_messagePanelVoiceBackground);
        int toColor = Theme.getColor(messageView.getRadialProgress().getCircleColorKey());
        int color = evaluateColor(colorProgress, fromColor, toColor);

        circlePaint.setColor(color);
        recordCircle.drawWaves(canvas, cx, cy, 1f - hideWavesProgress);

        canvas.drawCircle(cx, cy, radius, circlePaint);

        canvas.save();

        float scale = radius / toRadius;
        canvas.scale(scale, scale, cx, cy);
        canvas.translate(cx - messageView.getRadialProgress().getProgressRect().centerX(), cy - messageView.getRadialProgress().getProgressRect().centerY());

        messageView.getRadialProgress().setOverrideAlpha(colorProgress);
        messageView.getRadialProgress().setDrawBackground(false);
        messageView.getRadialProgress().draw(canvas);
        messageView.getRadialProgress().setDrawBackground(true);
        messageView.getRadialProgress().setOverrideAlpha(1f);
        canvas.restore();

        recordCircle.drawIcon(canvas, (int) fromCx, (int) fromCy, 1f - colorProgress);

        recordCircle.skipDraw = false;
        canvas.save();
        canvas.translate(recordCircle.getX(), recordCircle.getY());
        recordCircle.draw(canvas);
        canvas.restore();
        recordCircle.skipDraw = true;
    }

    float dotLastToCx;
    float dotLastToCy;

    public void animateRecordDot(Canvas canvas) {
        float fromCx = recordDot.drawingCx + dotGetX + enterView.getX();
        float fromCy = recordDot.drawingCy + dotGetY + enterView.getY();

        float toCx;
        float toCy;

        if (messageView.getMessageObject().stableId != messageId) {
            toCx = dotLastToCx;
            toCy = dotLastToCy;
        } else {
            toCx = messageView.getRecordDotCenterX(messageView.timeAudioX) + messageX;
            toCy = messageView.getRecordDotCenterY() + messageY;
        }

        dotLastToCx = toCx;
        dotLastToCy = toCy;


        float cx = evaluate(xProgress, fromCx, toCx);
        float cy = evaluate(yProgress, fromCy, toCy);

        float toRadius = messageView.getRecordDotRadius();
        float radius = evaluate(scaleProgress, dotFromRadius, toRadius);


        canvas.drawCircle(cx, cy, radius, circlePaint);

        int fromColor = Theme.getColor(Theme.key_chat_recordedVoiceDot);
        int toColor = messageView.getRecordDotColor();
        int color = evaluateColor(colorProgress, fromColor, toColor);
        circlePaint.setColor(color);
        canvas.drawCircle(cx, cy, radius, circlePaint);
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

    private float reverse(float progress) {
        return Math.max(0f, 1f - progress);
    }

    private float reverse2x(float progress) {
        return Math.max(0f, 1f - progress * 2f);
    }

    public void animateWave(Canvas canvas) {
        messageView.updatePaintState();
        int waveFormX = messageView.seekBarX + messageView.getVoiceOffsetX() + AndroidUtilities.dp(13);
        int waveFormY = messageView.seekBarY;

        float offset = (messageView.getSeekBarWaveform().getHeight() / 2f) * reverse(progress);
        int scaleSave = canvas.save();
        canvas.translate(messageX, currentRect.top - offset );
        canvas.translate(waveFormX * backgroundScaleX, waveFormY * backgroundScaleX);
        canvas.scale(1f / backgroundScaleX, 1f /  backgroundScaleX);
        messageView.getSeekBarWaveform().setOverrideAlpha(colorProgress);
        messageView.getSeekBarWaveform().draw(canvas, null);
        messageView.getSeekBarWaveform().setOverrideAlpha(1f);
        canvas.restoreToCount(scaleSave);
    }

    float timeLastToX;
    float timeLastToY;

    public void animateTime(Canvas canvas) {
        float fromX = timerGetX + enterView.getX();
        float fromY = timerGetY + enterView.getY() + timeOffsetY;

        float toX;
        float toY;

        if (messageView.getMessageObject().stableId != messageId) {
            toX = timeLastToX;
            toY = timeLastToY;
        } else {
            toX = messageView.timeAudioX;
            toY = messageY + messageView.getVoiceDurationY();
        }

        timeLastToX = toX;
        timeLastToY = toY;

        TextPaint timerPaint = timerView.getTextPaint();


        float x = evaluate(xProgress, fromX, toX);
        float y = evaluate(yProgress, fromY, toY);

        float textSize = (int) Math.ceil(evaluate(yProgress, fromTimeSize, toTimeSize));

        timerView.transitionInProgress = true;

        int hideAlpha = (int) (255 * reverse2x(colorProgress));
        if (hideAlpha > 0) {
            timerPaint.setAlpha(hideAlpha);
            int msSave = canvas.save();
            canvas.translate(fromX + measureSeconds, fromY);
            msLayout.draw(canvas);
            canvas.restoreToCount(msSave);
            timerPaint.setAlpha(255);
        }
        int colorStart = timerPaint.getColor();
        int colorEnd = Theme.getColor(Theme.key_chat_outTimeText);
        int color = evaluateColor(colorProgress, colorStart, colorEnd);

        int secondsSave = canvas.save();
        canvas.translate(x, y);
        textPaint.setTextSize(textSize);
        textPaint.setColor(color);
        timerView.overrideY = 0f;
        timerView.isOverrideY = true;
        timerView.drawText(canvas, durationTime, textPaint);
        timerView.isOverrideY = false;
        timerView.transitionInProgress = false;
        canvas.restoreToCount(secondsSave);
    }

    @Override
    public void animationDraw(Canvas canvas) {
        super.animationDraw(canvas);
        setBubbleRectEnd();
        setStartEnterEnter();
        animateBackground(canvas, 0.3f);
        drawTime(canvas, messageX, currentRect.top + shiftY);
        animateWave(canvas);
        animateSlide(canvas);
        animateTime(canvas);
        animateRecordDot(canvas);
        animateRecordCircle(canvas);
    }

    @Override
    public void release() {
        super.release();
        recordCircle.skipDraw = false;
        recordDot.skipDraw = false;
        slideText.skipDraw = false;
    }

    @Override
    protected AnimationType getAnimationType() {
        return AnimationType.Voice;
    }
}

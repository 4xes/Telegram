package org.telegram.ui.Transitions;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.FrameLayout;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

public class VoiceMessageEnterTransition extends BaseMessageTransition {

    float fromRadius;

    float dotFromRadius;

    final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    float dotGetX;
    float dotGetY;

    final ChatActivityEnterView.RecordCircle recordCircle;
    final ChatActivityEnterView.RecordDot recordDot;

    public VoiceMessageEnterTransition(FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView chatActivityEnterView, RecyclerListView listView) {
        super(containerView, messageView, chatActivityEnterView, listView);

        recordCircle = chatActivityEnterView.getRecordCircle();
        chatActivityEnterView.startMessageTransition();

        recordCircle.voiceEnterTransitionInProgress = true;
        recordCircle.skipDraw = true;
        fromRadius = recordCircle.drawingCircleRadius;

        recordDot = chatActivityEnterView.getRecordDot();
        recordDot.skipDraw = true;
        dotFromRadius = recordDot.drawingDotRadius;

        location(chatActivityEnterView, recordDot);
        dotGetX = location[0];
        dotGetY = location[1];
    }

    float lastToCx;
    float lastToCy;

    private void animateRecordCircle(Canvas canvas) {
        final float step1Time = 0.6f;
        float hideWavesProgress = animatorProgress > step1Time ? 1f : animatorProgress / step1Time;

        float fromCx = recordCircle.drawingCx + recordCircle.getX();
        float fromCy = recordCircle.drawingCy + recordCircle.getY();

        float toCy;
        float toCx;

        if (messageView.getMessageObject().stableId != messageId) {
            toCx = lastToCx;
            toCy = lastToCy;
        } else {
            toCx = messageView.getRadialProgress().getProgressRect().centerX() + offsetX;
            toCy = messageView.getRadialProgress().getProgressRect().centerY() + offsetY;
        }

        lastToCx = toCx;
        lastToCy = toCy;


        float cx = evaluate(xProgress, fromCx, toCx);
        float cy = evaluate(progress, fromCy, toCy);

        float toRadius = messageView.getRadialProgress().getProgressRect().height() / 2;
        float radius = evaluate(progress, fromRadius, toRadius);

        int fromColor = Theme.getColor(Theme.key_chat_messagePanelVoiceBackground);
        int toColor = Theme.getColor(messageView.getRadialProgress().getCircleColorKey());
        int color = evaluateColor(progress, fromColor, toColor);

        circlePaint.setColor(color);
        recordCircle.drawWaves(canvas, cx, cy, 1f - hideWavesProgress);

        canvas.drawCircle(cx, cy, radius, circlePaint);

        canvas.save();

        float scale = radius / toRadius;
        canvas.scale(scale, scale, cx, cy);
        canvas.translate(cx - messageView.getRadialProgress().getProgressRect().centerX(), cy - messageView.getRadialProgress().getProgressRect().centerY());

        messageView.getRadialProgress().setOverrideAlpha(progress);
        messageView.getRadialProgress().setDrawBackground(false);
        messageView.getRadialProgress().draw(canvas);
        messageView.getRadialProgress().setDrawBackground(true);
        messageView.getRadialProgress().setOverrideAlpha(1f);
        canvas.restore();


        recordCircle.drawIcon(canvas, (int) fromCx, (int) fromCy, 1f - animatorProgress);

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
            toCx = messageView.getRecordDotCenterX(messageView.timeAudioX) + offsetX;
            toCy = messageView.getRecordDotCenterY() + offsetY;
        }

        dotLastToCx = toCx;
        dotLastToCy = toCy;


        float cx = evaluate(xProgress, fromCx, toCx);
        float cy = evaluate(progress, fromCy, toCy);

        float toRadius = messageView.getRecordDotRadius();
        float radius = evaluate(progress, dotFromRadius, toRadius);


        canvas.drawCircle(cx, cy, radius, circlePaint);

        int fromColor = Theme.getColor(Theme.key_chat_recordedVoiceDot);
        int toColor = messageView.getRecordDotColor();
        int color = evaluateColor(progress, fromColor, toColor);
        circlePaint.setColor(color);
        canvas.drawCircle(cx, cy, radius, circlePaint);

    }

    @Override
    public void animationDraw(Canvas canvas) {
        animateBackground(canvas);
        animateRecordDot(canvas);
        animateRecordCircle(canvas);
    }

    @Override
    public void release() {
        super.release();
        recordCircle.skipDraw = false;
        recordDot.skipDraw = false;
    }
}

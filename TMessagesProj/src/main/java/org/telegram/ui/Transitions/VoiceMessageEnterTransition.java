package org.telegram.ui.Transitions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Animations.Evaluator.RectEvaluator;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.RecyclerListView;

public class VoiceMessageEnterTransition {

    float fromRadius;

    float dotFromRadius;

    float progress;

    final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final ValueAnimator animator;

    private static RectEvaluator rectEvaluator = new RectEvaluator(new Rect());
    private static FloatEvaluator floatEvaluator = new FloatEvaluator();

    float dotGetX;
    float dotGetY;

    public VoiceMessageEnterTransition(FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView chatActivityEnterView, RecyclerListView listView) {
        messageView.setTransitionInProgress(true);

        ChatActivityEnterView.RecordCircle recordCircle = chatActivityEnterView.getRecordCircle();
        chatActivityEnterView.startMessageTransition();

        messageView.setVisibility(View.INVISIBLE);
        fromRadius = recordCircle.drawingCircleRadius;
        recordCircle.voiceEnterTransitionInProgress = true;
        recordCircle.skipDraw = true;

        ChatActivityEnterView.RecordDot recordDot = chatActivityEnterView.getRecordDot();
        dotFromRadius = recordDot.drawingDotRadius;
        recordDot.skipDraw = true;
        relativeRect(chatActivityEnterView, recordDot);

        dotGetX = location[0];
        dotGetY = location[1];


        int messageId = messageView.getMessageObject().stableId;

        View view = new View(containerView.getContext()) {

            float lastToCx;
            float lastToCy;

            Rect enterRect = new Rect();
            Rect messageRect = new Rect();
            Rect drawRect = new Rect();

            float dotLastToCx;
            float dotLastToCy;

            @Override
            protected void onDraw(Canvas canvas) {
                float step1Time = 0.6f;
                float moveProgress = progress;
                float hideWavesProgress = progress > step1Time ? 1f : progress / step1Time;

                float progress = CubicBezierInterpolator.DEFAULT.getInterpolation(moveProgress);
                float xProgress = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(moveProgress);

                float fromCx = recordCircle.drawingCx + recordCircle.getX() - getX();
                float fromCy = recordCircle.drawingCy + recordCircle.getY() - getY();

                float dotFromCx = recordDot.drawingCx + dotGetX + chatActivityEnterView.getX() - getX();
                float dotFromCy = recordDot.drawingCy + dotGetY + chatActivityEnterView.getY() - getY();

                float toCy;
                float toCx;

                float dotToCx;
                float dotToCy;

                enterRect.set(
                        (int) (chatActivityEnterView.getLeft() - getX()),
                        (int) (chatActivityEnterView.getTop() - getY()),
                        (int) (chatActivityEnterView.getRight() - getX()),
                        (int) (chatActivityEnterView.getBottom() - getY()));

                if (messageView.getMessageObject().stableId != messageId) {
                    toCx = lastToCx;
                    toCy = lastToCy;

                    dotToCx = dotLastToCx;
                    dotToCy = dotLastToCy;
                } else {
                    float offsetX = messageView.getX() + listView.getX() - getX();
                    float offsetY = messageView.getY() + listView.getY() - getY();

                    messageRect.set(
                            (int) (messageView.getBackgroundDrawableLeft() + offsetX),
                            (int) (messageView.getBackgroundDrawableTop() + offsetY),
                            (int) (messageView.getBackgroundDrawableRight() + offsetX),
                            (int) (messageView.getBackgroundDrawableBottom() + offsetY)
                    );

                    toCx = messageView.getRadialProgress().getProgressRect().centerX() + offsetX;
                    toCy = messageView.getRadialProgress().getProgressRect().centerY() + offsetY;

                    int voiceOffset = messageView.getVoiceOffsetX();

                    float timeAudioX = messageView.timeAudioX + voiceOffset;

                    dotToCx = messageView.getRecordDotCenterX((int)timeAudioX) + offsetX;
                    dotToCy = messageView.getRecordDotCenterY() + offsetY;
                }

                lastToCx = toCx;
                lastToCy = toCy;

                dotLastToCx = dotToCx;
                dotLastToCy = dotToCy;

                float cx = floatEvaluator.evaluate(xProgress, fromCx, toCx);
                float cy = floatEvaluator.evaluate(progress, fromCy, toCy);

                float toRadius = messageView.getRadialProgress().getProgressRect().height() / 2;
                float radius = floatEvaluator.evaluate(progress, fromRadius, toRadius);


                float dotCx = floatEvaluator.evaluate(xProgress, dotFromCx, dotToCx);
                float dotCy = floatEvaluator.evaluate(progress, dotFromCy, dotToCy);

                float dotToRadius = messageView.getRecordDotRadius();
                float dotRadius = floatEvaluator.evaluate(progress, dotFromRadius, dotToRadius);


                rectEvaluator.evaluate(drawRect, progress, enterRect, messageRect);

                Theme.MessageDrawable messageDrawable = Theme.chat_msgOutDrawable;
                messageDrawable.setBounds(drawRect);
                messageDrawable.draw(canvas);

                circlePaint.setColor(ColorUtils.blendARGB(Theme.getColor(Theme.key_chat_messagePanelVoiceBackground), Theme.getColor(messageView.getRadialProgress().getCircleColorKey()), progress));
                recordCircle.drawWaves(canvas, cx, cy, 1f - hideWavesProgress);

                canvas.drawCircle(cx, cy, radius, circlePaint);

                circlePaint.setColor(ColorUtils.blendARGB(Theme.getColor(Theme.key_chat_recordedVoiceDot), messageView.getRecordDotColor(), progress));
                canvas.drawCircle(dotCx, dotCy, dotRadius, circlePaint);

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


                recordCircle.drawIcon(canvas, (int) fromCx, (int) fromCy, 1f - moveProgress);

                recordCircle.skipDraw = false;
                canvas.save();
                canvas.translate(recordCircle.getX() - getX(), recordCircle.getY() - getY());
                recordCircle.draw(canvas);
                canvas.restore();
                recordCircle.skipDraw = true;

            }
        };

        containerView.addView(view);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(valueAnimator -> {
            progress = (float) valueAnimator.getAnimatedValue();
            view.invalidate();
        });

        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(10000);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (view.getParent() != null) {
                    messageView.setTransitionInProgress(false);
                    containerView.removeView(view);
                    messageView.setVisibility(View.VISIBLE);
                    recordCircle.skipDraw = false;
                    recordDot.skipDraw = false;
                }
            }
        });
    }
    private final int[] location = new int[2];
    private final int[] locationTemp = new int[2];

    private void relativeRect(View parent, View child) {
        child.getLocationOnScreen(location);
        parent.getLocationOnScreen(locationTemp);
        location[0]-= locationTemp[0];
        location[1]-= locationTemp[1];
    }

    public void start() {
        animator.start();
    }
}

package org.telegram.ui.Animations.Transitions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageReceiver;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Animations.AnimationManager;
import org.telegram.ui.Animations.AnimationType;
import org.telegram.ui.Animations.Parameter;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.InstantCameraView;
import org.telegram.ui.Components.PipRoundVideoView;
import org.telegram.ui.Components.Point;
import org.telegram.ui.Components.RecyclerListView;

public class VideoMessageTransition extends BaseRecordMessageEnterTransition{

    final InstantCameraView.InstantViewCameraContainer cameraContainer;
    final InstantCameraView instantCameraView;

    Point durationInMessagePoint = new Point();

    public VideoMessageTransition(ActionBar actionBar, FrameLayout containerView, ChatMessageCell messageView, ChatActivityEnterView chatActivityEnterView, RecyclerListView listView, InstantCameraView instantCameraView) {
        super(actionBar, containerView, messageView, chatActivityEnterView, listView);
        this.instantCameraView = instantCameraView;

        PipRoundVideoView pipRoundVideoView = PipRoundVideoView.getInstance();
        if (pipRoundVideoView != null) {
            pipRoundVideoView.showTemporary(true);
        }

        ImageReceiver imageReceiver = messageView.getPhotoImage();
        float w = imageReceiver.getImageWidth();
        org.telegram.ui.Components.Rect rect = instantCameraView.getCameraRect();
        float scale = w / rect.width;
        int[] position = new int[2];
        messageView.getLocationOnScreen(position);
        position[0] += imageReceiver.getImageX() - messageView.getAnimationOffsetX();
        position[1] += imageReceiver.getImageY() - messageView.getTranslationY();
        cameraContainer = instantCameraView.getCameraContainer();
        cameraContainer.setPivotX(0.0f);
        cameraContainer.setPivotY(0.0f);
        AnimatorSet animatorSet = new AnimatorSet();

        cameraContainer.setImageReceiver(imageReceiver);

        instantCameraView.cancelBlur();

        long duration = AnimationManager.getInstance().getDuration(getAnimationType(), null);
        Interpolator scaleInterpolator = AnimationManager.getInstance().getInterpolator(getAnimationType(), Parameter.Scale);
        Interpolator yInterpolator = AnimationManager.getInstance().getInterpolator(getAnimationType(), Parameter.Y);
        Interpolator xInterpolator = AnimationManager.getInstance().getInterpolator(getAnimationType(), Parameter.X);

        AnimatorSet allAnimators = new AnimatorSet();

        animatorSet.playTogether(
                ObjectAnimator.ofFloat(instantCameraView.getSwitchButtonView(), View.ALPHA, 0.0f),
                ObjectAnimator.ofInt(instantCameraView.getPaint(), AnimationProperties.PAINT_ALPHA, 0),
                ObjectAnimator.ofFloat(instantCameraView.getMuteImageView(), View.ALPHA, 0.0f)
        );
        animatorSet.setDuration(100);


        ValueAnimator value = ValueAnimator.ofFloat(0f, 1f);
        value.setInterpolator(new LinearInterpolator());
        value.addUpdateListener(valueAnimator -> {
            if (messageView.getMessageObject().stableId != messageId) {
                stop();
            } else {
                progress = (float) valueAnimator.getAnimatedValue();
                Log.e("progress","progress " + progress);
                setProgresses();
                view.invalidate();
            }
        });
        value.setDuration(duration);

        ObjectAnimator cameraScaleX = ObjectAnimator.ofFloat(cameraContainer, View.SCALE_X, scale);
        cameraScaleX.setInterpolator(scaleInterpolator);
        cameraScaleX.setDuration(duration);
        ObjectAnimator cameraScaleY = ObjectAnimator.ofFloat(cameraContainer, View.SCALE_Y, scale);
        cameraScaleY.setInterpolator(scaleInterpolator);
        cameraScaleY.setDuration(duration);
        ObjectAnimator cameraY = ObjectAnimator.ofFloat(cameraContainer, View.TRANSLATION_Y, position[1] - rect.y);
        cameraY.setInterpolator(yInterpolator);
        cameraY.setDuration(duration);
        ObjectAnimator cameraX = ObjectAnimator.ofFloat(cameraContainer, View.TRANSLATION_X, position[0] - rect.x);
        cameraX.setInterpolator(xInterpolator);
        cameraX.setDuration(duration);

        allAnimators.playTogether(value, cameraX, cameraY, cameraScaleX, cameraScaleY, animatorSet);
        allAnimators.setDuration(duration);


        allAnimators.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (view.getParent() != null) {
                    release();
                }
            }
        });
        animator = allAnimators;
        allAnimators.start();
    }

    float dotLastToCx;
    float dotLastToCy;

    RectF timeRect = new RectF();

    public void animateRecordDot(Canvas canvas) {
        float fromCx = recordDot.drawingCx + dotGetX + enterView.getX();
        float fromCy = recordDot.drawingCy + dotGetY + enterView.getY();

        float toCx;
        float toCy;

        if (messageView.getMessageObject().stableId != messageId) {
            toCx = dotLastToCx;
            toCy = dotLastToCy;
        } else {
            messageView.getDurationRoundPoint(durationInMessagePoint);
            toCx = durationInMessagePoint.x + messageX + messageView.timeWidthAudio + AndroidUtilities.dp(12);
            toCy = durationInMessagePoint.y + messageY + AndroidUtilities.dp(8.3f);
        }

        dotLastToCx = toCx;
        dotLastToCy = toCy;


        float cx = evaluate(xProgress, fromCx, toCx);
        float cy = evaluate(yProgress, fromCy, toCy);

        float toRadius = messageView.getRecordDotRadius();
        float radius = evaluate(scaleProgress, dotFromRadius, toRadius);


        canvas.drawCircle(cx, cy, radius, circlePaint);

        int fromColor = Theme.getColor(Theme.key_chat_recordedVoiceDot);
        int toColor = Theme.getColor(Theme.key_chat_serviceText);
        int color = evaluateColor(colorProgress, fromColor, toColor);
        circlePaint.setColor(color);
        canvas.drawCircle(cx, cy, radius, circlePaint);
    }

    @Override
    public void animationDraw(Canvas canvas) {
        super.animationDraw(canvas);
        setBubbleRectEnd();
        setStartEnterEnter();
        drawTime(canvas);
        animateSlide(canvas);
        animateTimer(canvas);
        animateRecordDot(canvas);
        drawTime(canvas);
    }

    float timeLastToX;
    float timeLastToY;

    public void animateTimer(Canvas canvas) {
        float fromX = timerGetX + enterView.getX();
        float fromY = timerGetY + enterView.getY() + timeOffsetY;

        float toX;
        float toY;

        if (messageView.getMessageObject().stableId != messageId) {
            toX = timeLastToX;
            toY = timeLastToY;
        } else {
            messageView.getDurationRoundPoint(durationInMessagePoint);
            toX = messageX + durationInMessagePoint.x + AndroidUtilities.dp(4);
            toY = messageY + durationInMessagePoint.y  + AndroidUtilities.dp(1.7f);
        }

        timeLastToX = toX;
        timeLastToY = toY;

        TextPaint timerPaint = timerView.getTextPaint();


        float x = evaluate(xProgress, fromX, toX);
        float y = evaluate(yProgress, fromY, toY);

        timeRect.set(x - AndroidUtilities.dp(4), y - AndroidUtilities.dp(1.7f), x - AndroidUtilities.dp(4) + messageView.timeWidthAudio + AndroidUtilities.dp(8 + 12 + 2), y - AndroidUtilities.dp(1.7f) + AndroidUtilities.dp(17));

        int oldAlpha = Theme.chat_actionBackgroundPaint.getAlpha();
        Theme.chat_actionBackgroundPaint.setAlpha((int) (oldAlpha * colorProgress));
        canvas.drawRoundRect(timeRect, AndroidUtilities.dp(4), AndroidUtilities.dp(4), Theme.chat_actionBackgroundPaint);
        Theme.chat_actionBackgroundPaint.setAlpha(oldAlpha);

        float textSize = (int) Math.ceil(evaluate(yProgress, fromTimeSize, toTimeSize));


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
        int colorEnd = Theme.getColor(Theme.key_chat_serviceText);
        int color = evaluateColor(colorProgress, colorStart, colorEnd);

        int secondsSave = canvas.save();
        canvas.translate(x, y);
        textPaint.setTextSize(textSize);
        textPaint.setColor(color);
        timerView.transitionInProgress = true;
        timerView.overrideY = 0f;
        timerView.isOverrideY = true;
        timerView.drawText(canvas, durationTime, textPaint);
        timerView.isOverrideY = false;
        timerView.transitionInProgress = false;
        canvas.restoreToCount(secondsSave);
    }


    @Override
    public void start() {

    }

    @Override
    protected AnimationType getAnimationType() {
        return AnimationType.Video;
    }

    @Override
    public void release() {
        super.release();
        messageView.setVisibility(View.VISIBLE);
        messageView.invalidate();
        instantCameraView.hideCamera(true);
        instantCameraView.setVisibility(View.INVISIBLE);
    }
}
